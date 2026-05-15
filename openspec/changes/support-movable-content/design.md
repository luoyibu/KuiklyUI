## Context

**DSL 模式**：Compose DSL（`ComposeContainer` + `setContent{}`）

KuiklyUI 的 Compose 集成层通过 `KuiklyApplier` 将 Compose runtime 的节点操作映射到 Kuikly 视图树。当前的 `KNode.removeAt` 在移除节点时调用 `internalRemoveChild` → `didRemoveFromParentView()`，执行不可逆清理（注销 nativeRef、清除 observer/attr/event）。这使得 `movableContentOf` 在从旧位置 remove 后无法在新位置正常 re-insert。

Compose runtime 的 movableContent 实现保证 **先 remove 后 insert**（通过 applyChanges/applyLateChanges 两阶段），且节点会收到 `ComposeNodeLifecycleCallback.onRelease()` 作为最终销毁信号。

**无 NativeBridge 交互**：改动在 KMP 共享层（compose/ + core/），不涉及 native render 通信。

## Goals / Non-Goals

**Goals:**
- Compose DSL 中 `movableContentOf` 正常工作：节点可跨容器移动并保持 remember 状态
- 不破坏现有功能：普通 composable 创建/销毁、LazyList 复用、SubcomposeLayout 动态布局
- 代码改动最小化，不修改 Compose runtime 层

**Non-Goals:**
- 跨 Pager 的 movableContent 移动
- 自研 DSL 的相关适配
- `movableContentWithReceiverOf` 支持

## Decisions

### 1. removeAt 改为轻量移除，清理拆分到 detach + onRelease

**选择**：`removeAt` 不调用 `didRemoveFromParentView`，改为只移除 children 引用和清零 parentRef。

**替代方案考虑**：
- A) 在 removeAt 时区分 movableContent / 普通移除 → 放弃：remove 时无法判断节点后续是否会 re-insert
- B) removeAt 保持完整清理，insertTopDown 时完全重新 init → 放弃：丢失 movableContent 状态保持语义

**清理分层**：
- `detach()`：清理 `nativeViewRef`（保持 pager view map 一致性）
- `onRelease()`：完整清理（attr/event/renderView），仅在节点最终销毁时触发

### 2. 用 isInitialized 标记区分首次/重新插入

**选择**：在 KNode 上增加 `isInitialized: Boolean` 标记。

**原因**：insert 时 `childView.parent` 已为 null（parentRef 在轻量移除时被置 0），无法通过 parent 判断。标记是唯一可靠的区分方式。

### 3. LayoutNode.insertAt 保持原始断言

**选择**：不做 defensive remove 处理，保持 `checkPrecondition(instance._foldedParent == null)`。

**原因**：Compose runtime 源码明确保证 "deletes are performed in applyChanges and all inserts are performed in applyLateChanges"。如果断言触发说明有 bug，应该暴露而非静默处理。

### 4. SubcomposeLayout 手动节点的清理

**选择**：依赖 `detach()` 中清理 nativeViewRef，不对 SubcomposeLayout 代码做修改。

**原因**：SubcomposeLayout 手动创建的 VirtualNodeView 不在 slot table 中不会收到 onRelease，但其 attr/event 是空实现（`ContainerAttr`/`Event`），唯一需要清理的 nativeViewRef 在 detach 时处理。

### 5. View 对象连续性保证（播放器 / 富媒体场景）

**结论：`renderView` 对象在整个 movableContent 移动过程中保持同一实例，不重建。**

move 前后完整路径：

```
removeChildForMove():
  willRemoveFromParentView()   // 通知即将离开，无破坏操作
  children.remove()            // 从父 children 列表摘除
  parentRef = 0                // 解除父引用
  ✅ 不调 didRemoveFromParentView() — renderView 字段不清零

reinsertChild():
  pagerId 重设
  willMoveToParentComponent()
  children.add(index)
  parentRef = nativeRef
  reRegisterViewTree()         // 重新注册子树 nativeRef
  ✅ 不调 willInit/init/didInit  — 跳过整个初始化
```

`didRemoveFromParentView()` 是唯一会执行 `renderView = null` 的地方，轻量移除路径完全绕开了它。因此：
- 播放器 view 持有的解码器、Surface、AVPlayer session 等原生资源 **全程不受影响**
- 视频帧不会因 move 发生黑屏/重载

## Risks / Trade-offs

| Risk | Mitigation |
|------|-----------|
| 轻量移除后 remove→detach 间有瞬间 parentRef=0 但 nativeViewRef 仍在 map 中 | detach 在同一调用栈中紧跟执行（onChildRemoved→detach），窗口为 0 |
| SubcomposeLayout VirtualNodeView 的 attr/event 不被 onRelease 清理 | VirtualNodeView 的 attr/event 是空的 ContainerAttr/Event，无实际资源占用 |
| 普通节点 onRelease 延迟清理 attr/event | remove 和 onRelease 在同一个 applyChangesInLocked 内顺序执行，时间窗口为同一帧内几毫秒 |
| Compose DSL 是否有使用 ReactiveObserver 的边界情况 | 已验证 compose/src/ 中无任何 ReactiveObserver 引用 |

## File Changes by Module

### `compose/` 模块
- `compose/src/commonMain/kotlin/com/tencent/kuikly/compose/ui/node/KNode.kt` — 核心改动
- `compose/src/commonMain/kotlin/com/tencent/kuikly/compose/ui/node/LayoutNode.kt` — 恢复断言+缩进

### `core/` 模块
- `core/src/commonMain/kotlin/com/tencent/kuikly/core/base/ViewContainer.kt` — 新增方法

### `demo/` 模块
- `demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/compose/MovableContentDemo.kt` — 清理 println
