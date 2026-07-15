# OHOS LazyRowReuseDemo 滑到底 crash

## REPRO
- 平台：HarmonyOS 真机 `FMR0223C13000246`
- 包：`com.tencent.kuiklyohosdemo` Release
- 页面：`LazyRowReuseDemo`（crash 前日志多为 `prefetch_cache`）
- 操作：滚动列表到底附近
- 结果：CppCrash ×2（约 18:00:53 / 18:01:00）

## DIAGNOSE

### 崩溃事实
- Reason：`SIGTRAP(TRAP_BRKPT)`（系统断言，非空指针）
- 栈：`librosen_text` `TextLine::iterateThroughVisualRuns` → `prepareRoundRect` → `paint` → `Typography::Paint` → **`KRRichTextView::OnForegroundDraw`**
- 不在 `libshared.so`（Kotlin），属 OHOS render 富文本 V1 绘制路径（`KR_TEXT_RENDER_V2_ENABLED=false`）

### Demo 触发条件
`CardItem` 的 Text：
- `Modifier.width(80.dp)`（frame 宽固定 80）
- `textAlign = TextAlign.Center`
- 文案很短（`"$rowIndex.$colIndex"`），**measure 宽 ≪ 80**

### 根因假设（按可能性）

#### H1（最高）：OnForegroundDraw 每帧强制 TypographyLayout，滚动时压垮 librosen_text
`KRRichTextView.cpp`：

```cpp
if (fabs(textTypoSize.width - frameWidth) > 1 || textAlign != TEXT_ALIGN_LEFT) {
    needReLayout = true;
}
if (needReLayout) {
    OH_Drawing_TypographyLayout(textTypo, frameWidth * dpi);
    ...
    if (textAlign != TEXT_ALIGN_LEFT) {
        richTextShadow->ResetTextAlign(); // 只改 flag，不改 measure size
    }
}
OH_Drawing_TypographyPaint(...);
```

对 CardItem：
1. 首次：`textAlign != LEFT` → Layout → `ResetTextAlign()` 把 flag 改成 LEFT
2. 之后每帧：`textTypoSize.width`（测出来的短文本宽）与 `frameWidth=80` 差 > 1 → **仍然 needReLayout=true**
3. 结果：**每个 Text 每一帧都 Layout + Paint**

`ResetTextAlign` 本意是「居中只 Layout 一次」，但 `textTypoSize.width != frameWidth` 条件让它失效。
LazyList 滑到底时大量 Text 复用/同帧绘制 → 高频 `TypographyLayout`/`TypographyPaint` → `prepareRoundRect` 断言（与 Flutter/Skia 同类 SIGTRAP 栈一致）。

#### H2：View 复用放大问题
- `KRRichTextView::ReuseEnable() == true`
- `DidRemoveFromParentView` 清 `shadow_`，但**不** `UnregisterCustomEvent`（仅 `OnDestroy` 注销）
- 复用本身未必直接崩，但会放大 H1 的 Layout/Paint 压力与时序窗口

#### H3：main/context 短暂共享同一 typography（#1358 已防 UAF）
`TaskToMainQueueWhenWillSetShadowToView` 把 context 的 handle 拷到 main；在 context 下次 `BuildTextTypography` 前两边可指向同一对象。#1358 用 `shared_ptr` 防销毁后使用；若仍有同对象交叉 Layout，可能加重 H1。

### 历史相关修复
- #1358：typography 跨线程提前销毁
- #1496：`shadow == null || width == 0` 时 skip draw（`&&`→`||`）
- 均未覆盖「每帧无条件 re-Layout」逻辑 bug

### 验证计划
1. **验证 H1**：改 `needReLayout`，仅在「尚未按 frame 宽 Layout」或「frame 宽变化」时 Layout；或临时去掉 Demo 的 `width(80)`/`Center`，看是否还崩
2. **验证 H2**：入口调 `KRDisableViewReuse()`，看是否仍崩
3. 确认后再出 FIX_PLAN

### 诊断日志结果（2026-07-14 19:18 Debug）
进程 `34244` 再次 CppCrash：`SIGTRAP` @ `prepareRoundRect` → `KRRichTextView::OnForegroundDraw`（新 so，offset +4656，确认是带 BD 的包）。

崩溃前最后一轮 summary（19:18:18.572）：
- **draw=872 / relayout=720（约 82.5% 绘制强制 Layout）**
- byMeasure=716，byAlign=620，byFrame≈4
- **afterAlignedMeasureOnly=91**（已 Layout 过、align 已是 LEFT，仍因 measureW≠frameW 再 Layout）

典型单条：
`measureW=16~30 frameW=80 align=2|0 reasons{measureDiff=1,...}`
以及：`lastDrawW=80 align=0 ... afterAlignedMeasureOnly=1`

**结论：H1 成立。** 根因是 `needReLayout` 条件在「短文本 + 固定宽 + 居中」下几乎每帧触发 `TypographyLayout`，滚动到底高频 Layout/Paint 触发 `librosen_text` 断言。
（复用会把 `lastDrawW` 重置为 -1，进一步放大首次 align 路径，但 `afterAlignedMeasureOnly` 已单独证实「同实例重复 Layout」。）

### 主干对照后的结论修正（2026-07-15）
- main（Compose runtime `1.7.3-kuikly2`、无 prefetch）连续滑动约 50 秒：`draw=5446 / relayout=4470`，ratio 约 **82%**，未崩。
- prefetch 分支崩前：ratio 约 **83%**。因此高 relayout 是两边共有的风险条件，**不能单独解释分支为何崩溃，H1 只能视为必要放大条件，尚不是已确认根因**。
- 崩溃进程同一生命周期内依次进入 `B → A → B → B → B`；每次切页均出现 `KTRenderView WillDestroy`，最后一次 B 进入约 15.7 秒后 SIGTRAP。
- 当前仍需隔离的变量：
  1. Compose runtime `1.9.3-kuikly1` 与 `1.7.3-kuikly2`
  2. `enableLazyListPrefetch + CacheWindow`
  3. 多次创建/销毁页面后的残留状态
- 下一轮采用全新安装、单次进入 A（1.9.3、无 prefetch）作为单变量对照，再以全新进程单次进入 B 对比。

### 单次 A 对照结果（2026-07-15 15:04）
- 全新安装后仅进入一次 A：`mode=beyond5 beyondBounds=5 prefetch=false cacheWindow=off`。
- 持续约 155 秒，最终 `draw=8766 / relayout=7500`，ratio **85.6%**，进程未崩，且没有页面销毁/重建。
- 该强度已经超过此前 B 崩溃时的累计 Layout 量，因此排除：
  - Compose runtime `1.9.3-kuikly1` 单独致崩；
  - relayout 数量达到固定阈值致崩；
  - beyond=5 路径本身致崩。
- 嫌疑收敛到 `enableLazyListPrefetch + CacheWindow` 路径，或其与 RichText 复用/绘制的交互。下一轮以全新进程仅进入一次 B 验证。

### 单次 B 结果（2026-07-15 15:07）
- 重启为全新进程后仅进入一次 B：`mode=prefetch_cache beyondBounds=0 prefetch=true cacheWindow=ahead1x`。
- 无页面销毁/重建，约 80.9 秒后复现 `SIGTRAP`；因此排除“必须多次切页后才崩”的假设。
- 崩前最终 `draw=9476 / relayout=8310`，ratio **87.8%**。与 A 最关键的差异不是总量，而是：
  - A：`byFrame=0`
  - B：崩前 `byFrame=444`
- 崩前最后一条（距离 SIGTRAP 约 87ms）：
  `measureW=20.9231 frameW=20.9231 lastDrawW=28 align=0 reasons{measureDiff=0,alignNotLeft=0,frameChanged=1}`
- B 中同一 RichText 实例表现出 `28 → 21.23 → 20.92 → 11.69 → 23.38` 等宽度切换，而 A 未出现 frameChanged。当前根因已收敛为：**prefetch/cache 路径改变了跨类型复用/预组合时序，使 RichText 在不同约束宽度间高频复用和重新 Layout，最终触发 rosen_text 绘制断言**。
- 尚需拆分“默认 prefetch 策略”和“CacheWindow 策略”，确认具体由哪个策略引入高频跨宽度复用。

### 静态追踪后的高概率具体根因
- `last_draw_frame_width_` 是 `KRRichTextView` 的 view 级缓存，仅在 `DidRemoveFromParentView()` 重置。
- Compose `ReusableContentHost/setContentWithReuse` 会在原生 View 仍挂在父节点时逻辑复用，并给同一 view 重新绑定/更新 shadow；此路径不保证调用 `DidRemoveFromParentView()`。
- `SetShadow()` 在主线程 typography 更新后只替换 `shadow_`，没有重置 `last_draw_frame_width_`。因此缓存仍属于旧 typography/旧 Text 节点。
- B 的崩前日志正好符合该状态：新 shadow 的 `measureW` 已等于当前 `frameW=20.9231`、align 已为 LEFT，本来无需 Layout；但旧缓存仍为 `lastDrawW=28`，仅由 `frameChanged=1` 强制对新 typography 再次执行 `TypographyLayout`，87ms 后在 rosen_text Paint 中 SIGTRAP。
- 该缓存源于字体缩放修复 `3cdea387`，当时只考虑物理移除时重置；prefetch/CacheWindow 提高跨类型逻辑复用频率，暴露了未覆盖的复用生命周期。
- 最小且针对性的验证：在 `KRRichTextView::SetShadow()` 每次绑定最新 main-thread typography 时同步将 `last_draw_frame_width_ = -1.0`。这不会关闭 prefetch，也不改变 CacheWindow，只消除“旧 typography 的宽度缓存作用到新 typography”这一变量；一次 B 压测即可验证。

## FIX_RESULT

### SetShadow 重置宽度缓存验证（2026-07-15 15:18）
- 修改：`KRRichTextView::SetShadow()` 在接收最新 main-thread typography 时重置 `last_draw_frame_width_ = -1.0`。
- 保持 B 配置不变：`prefetch=true`、`CacheWindow=ahead1x`，全新安装后单次进入。
- 持续约 142 秒，最终 `draw=18608 / relayout=15450`，强度约为此前崩溃 B（`draw=9476`）的 1.96 倍，进程仍存活且无 SIGTRAP。
- `byFrame` 从崩前 **444** 降为 **3**（约下降 99.3%）；采样日志中 `frameChanged=1` 从 13 条降为 0。
- `measureDiff/alignNotLeft` 导致的常规 relayout 仍保持约 83%，但不再崩，说明高总 relayout 不是充分条件。
- 根因最终确认：**Compose prefetch/CacheWindow 提高了不离树逻辑复用频率；新 shadow/typography 绑定到同一 KRRichTextView 时沿用旧 `last_draw_frame_width_`，触发针对新 typography 的冗余跨宽度 Layout，最终导致 rosen_text Paint 断言。**

## 日志
- crash：`logs/kuikly_ohos_crash.log`
- 诊断：`logs/kuikly_ohos_bd_richtext.log`（hilog 过滤 `BD_RichText`）
