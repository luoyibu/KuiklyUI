# OHOS RichText 重复 Layout：表现、原因与修复建议

## 结论

OHOS `KRRichTextView` 当前有两类相关问题：

1. **正确性问题（现有对照已支持）**
   `SetShadow()` 换入新的 typography 后，仍沿用上一个 typography 的
   `last_draw_frame_width_`。Compose prefetch / CacheWindow 会提高“不离树逻辑复用”
   的频率，同一个原生 RichText View 在不同宽度的 Text 节点之间切换时，旧宽度缓存
   会让新 typography 发生一次本不需要的跨宽度 Layout。现有 A/B 对照表明，这类
   Layout 与 `librosen_text` Paint 阶段的 `SIGTRAP` 高度相关。

2. **性能问题（仍有优化空间）**
   `OnForegroundDraw()` 用 `MainMeasureSize().width` 判断 typography 是否已按
   View 宽度排版。但这个值是 Layout 后“受约束上限截断的最长行宽度”，不是
   `OH_Drawing_TypographyLayout()` 实际使用的容器宽度。短文本放在固定宽容器中时，
   两者天然不相等，因此同一 typography、同一 frame 可能反复 Layout。

建议先合入正确性修复，再单独处理性能优化，避免两类改动混在一起。

## 问题表现

复现页面为 `LazyRowReuseDemo`。卡片中的 Text 使用固定宽度和居中对齐：

```kotlin
Text(
    text = "$rowIndex.$colIndex",
    modifier = Modifier.width(80.dp),
    textAlign = TextAlign.Center,
)
```

在 OHOS 真机上，开启 LazyList prefetch 和一屏 CacheWindow 后快速滚动，进程会在
系统文本库中崩溃：

```text
SIGTRAP(TRAP_BRKPT)
librosen_text TextLine::iterateThroughVisualRuns
  → TextLine::prepareRoundRect
  → TextLine::paint
  → Typography::Paint
  → KRRichTextView::OnForegroundDraw
```

诊断日志还显示，大部分绘制都会进入 `TypographyLayout`：

- 修复前 B 路径：`draw=9476 / relayout=8310`，约 87.8%
- 修复后 B 路径：`draw=18608 / relayout=15450`，约 83.0%

这说明 crash 已解决，但重复 Layout 的性能问题仍然存在。

## 为什么会发生两类无用 Layout

### 1. crash 相关：旧 frame 宽度缓存作用到了新 typography

相关代码：

- `core-render-ohos/.../richtext/KRRichTextView.cpp`
- `KRRichTextView::SetShadow()`
- `KRRichTextView::OnForegroundDraw()`

`last_draw_frame_width_` 是 `KRRichTextView` 的成员，只在
`DidRemoveFromParentView()` 中重置。传统原生 View 回收通常会经过这个生命周期，
但 Compose 的 `ReusableContentHost/setContentWithReuse` 可以在 View 不离开父节点
的情况下更新内容和 shadow。

于是会出现以下顺序：

1. 同一个 `KRRichTextView` 之前按宽度 28 绘制，缓存 `lastDrawW=28`
2. Compose 逻辑复用该 View，并通过 `SetShadow()` 换入一个已按 20.9231 测量的新 typography
3. 新 typography 的 `measureW` 已等于当前 `frameW=20.9231`，且 align 已是 LEFT
4. 旧缓存仍为 28，`reason_frame_changed` 被错误触发
5. 对新 typography 再执行一次冗余 `TypographyLayout(20.9231)`
6. 随后立即 Paint；现有对照支持该错误 Layout 是 SIGTRAP 的触发条件

崩溃前最后一条日志正是这个状态：

```text
measureW=20.9231 frameW=20.9231 lastDrawW=28 align=0
reasons{measureDiff=0,alignNotLeft=0,frameChanged=1}
```

这次 Layout 没有任何排版必要，唯一触发原因是属于旧 typography 的宽度缓存。

### 2. 性能问题：把测量结果宽度当成了排版宽度

`KRRichTextShadow::BuildTextTypography()` 会执行：

```cpp
OH_Drawing_TypographyLayout(typography, constraintWidth * dpi);
```

但随后保存到 `context_measure_size_.width` / `MainMeasureSize().width` 的值来自
`OH_Drawing_TypographyGetLongestLine()`：它是 Layout 后的测量结果，表示受约束
上限截断的最长行宽度，并不是传给 `TypographyLayout()` 的排版宽度。

例如文本 `"1.2"` 放在 80vp 的容器中：

- typography 的排版容器可能已经是 80
- 最长文本行只有约 16～30
- `OnForegroundDraw()` 比较 `measureW != frameW`，仍会认为需要 Layout

居中对齐首次 Layout 后，`ResetTextAlign()` 只把 align 标记改为 LEFT，并不会修改
`MainMeasureSize().width`。下一次 Draw 仍满足 `measureW != frameW`，因此同一
typography、同一 frame 会再次 Layout。

本次修复后日志中：

- `afterAlignedMeasureOnly=2031`
- 含义是：frame 没变、align 已处理，但仍仅因内容宽度与 frame 不同而重复 Layout

这些属于可以明确消除的无用工作。

## 证据

### A：无 prefetch

- Compose runtime：`1.9.3-kuikly1`
- `beyondBounds=5`
- prefetch 关闭
- 连续滚动约 155 秒
- `draw=8766 / relayout=7500`
- `byFrame=0`
- 未崩溃

### B：prefetch + CacheWindow，修复前

- 全新进程，单次进入页面
- `prefetch=true`
- `CacheWindow=ahead1x`
- 约 80.9 秒后 SIGTRAP
- `draw=9476 / relayout=8310`
- `byFrame=444`
- 最后一条 `frameChanged` 日志距 SIGTRAP 约 87ms

### B：仅重置宽度缓存，修复后

- 配置与修复前完全相同
- 连续滚动约 142 秒
- `draw=18608 / relayout=15450`
- 测试强度约为崩溃轮次的 1.96 倍
- `byFrame` 从 444 降为 3，下降约 99.3%
- 采样日志中的 `frameChanged=1` 从 13 条降为 0
- 进程存活，无 SIGTRAP

这组对照说明：

- prefetch 不是独立的 crash 根因，它放大了跨宽度逻辑复用
- 高 relayout 比例不是充分崩溃条件
- 现有对照支持：旧宽度缓存作用到新 typography 后产生的跨宽度冗余 Layout，
  是本次 crash 的触发条件

## 推荐修复方案

### 方案一：先修正确性问题

在 `KRRichTextView::SetShadow()` 中，每次接收最新 main-thread typography 时重置
宽度缓存：

```cpp
void KRRichTextView::SetShadow(
    const std::shared_ptr<IKRRenderShadowExport>& shadow
) {
    last_draw_frame_width_ = -1.0;
    shadow_ = shadow;
    // ...
}
```

原因：`last_draw_frame_width_` 描述的是当前 typography 的绘制状态，而不是原生
View 的永久状态。只要 typography 被替换，该缓存就必须失效，不能依赖
`DidRemoveFromParentView()`。

该修改在一次约 142 秒、测试强度约为崩溃轮次 1.96 倍的同配置压力测试中，
未再复现 SIGTRAP。

### 方案二：折中优化，同一 typography、同一 frame 最多 Layout 一次

当前 `measureDiff` 每帧都会参与判断。建议把首次排版与 frame 变化分开：

```cpp
const bool firstLayoutForTypography = last_draw_frame_width_ < 0;
const bool frameChanged =
    !firstLayoutForTypography &&
    fabs(last_draw_frame_width_ - frameWidth) > 0.01;

const bool needInitialContainerLayout =
    firstLayoutForTypography &&
    (measureDiff || textAlign != TEXT_ALIGN_LEFT);

const bool needReLayout =
    needInitialContainerLayout || frameChanged;
```

无论首次 Draw 是否需要补充 Layout，都要在判断完成后记录
`last_draw_frame_width_ = frameWidth`，否则首次未 Layout 时该值会一直为负，
后续无法识别 frame 变化。这样可以消除 `afterAlignedMeasureOnly` 代表的重复
Layout。

这个方案改动小，但仍然使用 `measureW` 近似判断首次是否需要按容器宽度排版。
内容宽度碰巧等于 frameWidth，并不能严格证明 typography 原本就是按该 frame
排版。因此它只适合作为折中优化，不建议在缺少完整回归时直接落地。

### 方案三：推荐的性能方案，记录真实 typography layout width

在 `KRRichTextShadow` 中记录传给 `OH_Drawing_TypographyLayout()` 的有效排版宽度，
例如 `context_layout_width_`，并随 typography 一起同步到主线程。字段建议统一保存
vp，调用系统接口时再乘 dpi：

```cpp
const double effectiveLayoutWidth =
    constraintWidth == 0 ? MAX_LAYOUT_WIDTH : constraintWidth;
context_layout_width_ = effectiveLayoutWidth; // vp
OH_Drawing_TypographyLayout(typography, effectiveLayoutWidth * dpi);
```

当前实现会把 `constraintWidth == 0` 转成一个很大的有效宽度，因此不能直接保存
归一化前的 0。比较 layout width 与 frameWidth 时也必须保证单位一致。

`KRRichTextView::OnForegroundDraw()` 应比较：

```text
typography 已布局宽度 vs 当前 frameWidth
```

而不是：

```text
最长文本行内容宽度 vs 当前 frameWidth
```

如果 typography 已按当前 frame 宽度 Layout，就直接 Paint。只有以下情况才需要
重新 Layout：

- typography 刚替换，且原排版宽度与 frame 不同
- 同一个 typography 对应的 frame 宽度发生变化
- 其他确实改变排版结果的参数发生变化

这个字段必须与 typography 成对同步，不能作为 shadow 上独立更新的普通状态：

- context 线程构建 typography 时，同时生成对应的 layout width
- 主线程任务将 typography、layout width、measure size、align 作为同一份快照同步
- 主线程补充 Layout 后，只更新当前 typography 对应的 main-thread layout width
- typography 替换时，旧 layout width 与其他排版缓存一起失效

不要把 `MainMeasureSize().width` 改写成 frameWidth。它仍是 Flex/Yoga 测量需要的
Layout 后测量结果，应该新增独立字段记录排版宽度。

## 两条工程约束

- typography 相关状态必须作为同一份快照同步并绑定 typography 生命周期；
  `SetShadow()` 后旧缓存全部失效，不能依赖 `DidRemoveFromParentView()` 清理逻辑复用状态
- 测量结果尺寸与排版约束尺寸必须分字段保存；relayout 应比较真实 layout width，
  不能用最长文本行宽度代替

## 回归验证建议

正确性修复至少覆盖：

- prefetch + CacheWindow 快速滚动
- 同一 RichText View 在不同宽度 Text 之间复用
- Center / Left 对齐
- 单行、多行、换行、maxLines、ellipsis
- fontSizeScale 变化
- 带 `lineBreakMargin` 和 image span 的 V1 RichText

性能优化额外确认：

- 同一 typography、同一 frame 的连续 Draw 不再重复 Layout
- frame 宽度变化时仍能重新排版
- typography 替换后不会沿用上一个 typography 的 layout width
- Flex/Yoga 的测量结果不受影响

## 相关文件

- `core-render-ohos/src/main/cpp/libohos_render/expand/components/richtext/KRRichTextView.cpp`
- `core-render-ohos/src/main/cpp/libohos_render/expand/components/richtext/KRRichTextView.h`
- `core-render-ohos/src/main/cpp/libohos_render/expand/components/richtext/KRRichTextShadow.cpp`
- `core-render-ohos/src/main/cpp/libohos_render/expand/components/richtext/KRRichTextShadow.h`
- `core/src/commonMain/kotlin/com/tencent/kuikly/core/views/RichTextView.kt`
- `compose/src/commonMain/kotlin/com/tencent/kuikly/compose/ui/layout/SubcomposeLayout.kt`
