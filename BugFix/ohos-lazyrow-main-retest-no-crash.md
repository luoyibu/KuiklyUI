# OHOS LazyRowReuseDemo 主干复测：未崩溃

> 对应前置说明：`p1ui2/docs/OHOS-LazyRowReuseDemo-scroll-bottom-crash.md`
> 复测目的：判断「滑到底 CppCrash」是否在干净 `origin/main` 上也能复现（与 prefetch 分支无关）。
> 记录日期：2026-07-14
> 复测 worktree：`/Users/qibu/.cursor/worktrees/KuiklyUI/dfp7`

---

## 1. 复测结论（先看这个）

| 项 | 结果 |
|---|---|
| **是否 crash** | **否**（人工滑到底多轮，进程仍存活） |
| **hilog 是否有本次 `prepareRoundRect` + `KRRichTextView` + `SIGTRAP`** | **否**（复测窗口内无新 CppCrash / PROCESS_KILL） |
| **结论** | **主干默认 `LazyRowReuseDemo` 不易复现**；同 bug 栈此前只在 **prefetch 分支**稳定出现。`needReLayout` 逻辑主干仍在，但更像被 prefetch / CacheWindow **放大到断言阈值**，而非「main 无 bug、只有分支才有」的纯分支引入。 |

**一句话**：主干同机同页滑到底 **不崩**；今天设备上的 SIGTRAP 记录属于 **prefetch 会话的历史 crash**，不要当成主干复测结果。

---

## 2. 环境

| 项 | 值 |
|---|---|
| 代码 | `origin/main` @ `2dea9a56`（`fix: iOS and Ohos touch event dispatch… #1508`） |
| 构建 | Debug HAP：`./2.0_ohos_demo_build.sh` + `hvigorw assembleHap … -p buildMode=debug` |
| 设备 | `FMR0223C13000246` |
| 包名 | `com.tencent.kuiklyohosdemo` |
| 主干复测进程 | **pid `39737`**（安装启动约 `19:44:52`，复测后仍存活） |
| Demo | Router → **`LazyRowReuseDemo`**（主干自带，无 `firstScreenMode` / prefetch） |
| 操作 | 快速下滑外层 LazyColumn 至底部，多轮（日志见 `AceScrollable` `HandleCrashBottom/Top`，此为滚动越界回调名，**不是** App crash） |

产物：

- HAP：`ohosApp/entry/build/default/outputs/default/entry-default-signed.hap`
- 全量 hilog：`logs/kuikly_ohos_hilog_full.dump`（约 26 万行）
- 关键字摘录：`logs/kuikly_ohos_crash_grep.log`

---

## 3. 日志分析

### 3.1 主干复测窗口（pid 39737，约 19:44 之后）

证据链：

1. **进程未死**：复测后 `hdc shell pidof com.tencent.kuiklyohosdemo` 仍为 `39737`。
2. **有真实滑动**：`19:54:43`～`19:54:55` 连续出现
   `AceScrollable … UpdateCurrentOffset==>[HandleCrashBottom/Top()]`
   → 用户确实滑到顶/底；**HandleCrash\*** 是 Ace 滚动组件 API 名，与 CppCrash 无关。
3. **无本次 kill**：对 `19:50+` + pid `39737` 检索 `onAbilityDied` / `SIGTRAP` / `Cpp Crash` / `PROCESS_KILL` → **0 命中**。
4. **无新的 Faultlogger 落盘**（相对本次会话）；权限不足无法列 `/data/log/faultlog/faultlogger`，但不影响上述 hilog 结论。

### 3.2 易混淆：19:44:53 Bugly「NativeCrash」不是本次崩

主干 App 刚启动时 Bugly 会 **回放/上报本地未上传的历史 crash**。关键字段：

```text
LAUNCH TIME:  … 19:18:06
CRASH TIME:   … 19:18:18
PROCESS ID:   34244          ← 不是当前 39737
CRASH NAME:   SIGTRAP(5,1)
栈顶: librosen_text prepareRoundRect → KRRichTextView::OnForegroundDraw(+4656)
```

这与 **19:18:18 / pid 34244** 的 prefetch Debug 复现是同一条记录；当前进程只是在上传，**自身未死**。

### 3.3 同日历史 crash（prefetch 分支，供对照）

| 时间 | pid | 信号 | 特征栈 |
|---|---|---|---|
| 18:00:53 / 18:00:57 | 23979 / 24047 | SIGTRAP | `prepareRoundRect` → … →（Bugly） |
| 19:18:18 | **34244** | SIGTRAP(TRAP_BRKPT) | `prepareRoundRect` → `KRRichTextView::OnForegroundDraw`；AMS `kill reason: Cpp Crash` |

与前置诊断一致：崩溃在 **`libkuikly.so` 富文本绘制**，不在 `libshared.so`。

---

## 4. 主干 vs prefetch：为何一边崩一边不崩

### 4.1 相同点（触发条件 / 原生逻辑）

| 点 | 说明 |
|---|---|
| `CardItem` Text | 两边均为 `Modifier.width(80.dp)` + `TextAlign.Center` + 短文案 `"$rowIndex.$colIndex"`（字节级一致） |
| `KRRichTextView::OnForegroundDraw` | **主干与分支提交树相对 `main` 无功能性 diff**；分支 worktree 仅有本地诊断日志改动 |
| `needReLayout`（主干仍在） | `measureWidth ≠ frameWidth` **或** `textAlign != LEFT` 即 Layout；`ResetTextAlign()` 只改 align flag，**不改** measure 宽 → 短文本+固定宽下 **几乎每帧 TypographyLayout + Paint** 的逻辑缺陷仍在 |

主干片段（`core-render-ohos/.../KRRichTextView.cpp`）：

```cpp
if (fabs(textTypoSize.width - frameWidth) > 1 || textAlign != TEXT_ALIGN_LEFT) {
    needReLayout = true;
}
if (needReLayout) {
    OH_Drawing_TypographyLayout(textTypo, frameWidth * dpi);
    ...
    richTextShadow->ResetTextAlign(); // 不更新 measure size
}
```

### 4.2 不同点（复现强度）

| 点 | `origin/main` | prefetch 分支（`p1ui2` / `cursor/7a7f8852` 工作区） |
|---|---|---|
| Demo | 朴素 `LazyColumn { … }`，无 mode | +`firstScreenMode`、`beyondBoundsItemCount`、`LazyLayoutCacheWindow`、`enableLazyListPrefetch()` 等 |
| Compose | 无 LazyList prefetch / CacheWindow 实现 | `compose/` 大量 prefetch 相关改动（相对 main 约 +3k 行量级） |
| 绘制密度 | 默认可见窗口 | prefetch / 更大 beyond / CacheWindow → **同帧更多 Text 复用与绘制** |

**解释（当前最贴证据）**：

- H1（每帧强制 Layout）在主干 **逻辑成立**，但默认 LazyList 压力不够，滑到底 **达不到** `librosen_text` 断言阈值。
- prefetch 分支在相同 Text 触发条件下 **抬高同帧 Layout/Paint 密度**，此前 Debug 统计约 **draw 的 82% 强制 relayout**，从而稳定 SIGTRAP。
- 因此：**不是「prefetch 改坏了 KRRichTextView」**（render 富文本未改），而是 **「prefetch 把主干潜在缺陷放大成可复现 crash」**。

---

## 5. 对修复策略的建议

1. **仍应修主干 `needReLayout`**（与是否 prefetch 无关）：仅在首次 / frame 宽变化时因居中或 measure≠frame 去 `TypographyLayout`；align 已 Reset 且 frame 未变时不要每帧 Layout。
   文件：`core-render-ohos/src/main/cpp/libohos_render/expand/components/richtext/KRRichTextView.cpp`
2. **prefetch 合入前**：应用同一 Demo（含 prefetch mode）做滑到底回归；主干不崩 ≠ prefetch 安全。
3. **可选对照实验**（若需再钉死放大因子）：在 prefetch 分支关 prefetch / 仅 `beyond=0` / 临时去掉 CardItem 的 `width(80)` 或 `Center`，看崩溃率变化。

---

## 6. 日志判读备忘（避免再踩坑）

| 现象 | 含义 |
|---|---|
| Bugly `CRASH TIME` / `PROCESS ID` ≠ 当前 pid | 历史 crash 上报，不是当前进程刚崩 |
| `AceScrollable` `HandleCrashBottom` | 滚动到边界，**不是** Native crash |
| `OnForegroundDraw, shadow or frame not ready` | 复用/首帧 frame 未就绪 skip，主干也有，不等于崩 |
| 判定本次崩 | 看 AMS `PROCESS_KILL` / `Cpp Crash`、Faultlogger 新文件、**pid 是否消失** |

---

## 7. 相关材料

| 路径 | 说明 |
|---|---|
| `p1ui2/docs/OHOS-LazyRowReuseDemo-scroll-bottom-crash.md` | 主干复测说明（提问清单）；本文回答其 §5 |
| `p1ui2/BugFix/ohos-lazyrow-scroll-bottom-crash.md` | prefetch 分支诊断（H1 成立、BD_RichText 统计） |
| `dfp7/logs/kuikly_ohos_hilog_full.dump` | 本次含历史+主干会话的 hilog |
| `dfp7/BugFix/ohos-lazyrow-main-retest-no-crash.md` | 本文 |
