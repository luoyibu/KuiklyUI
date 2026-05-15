## Context

**DSL 模式**：Compose DSL（`ComposeContainer` + `setContent{}`）

KuiklyUI 的 Compose 集成层通过 `KuiklyApplier` 将 Compose runtime 的节点操作映射到 Kuikly 视图树。当前的 `KNode.removeAt` 在移除节点时调用 `removeDomSubView` → `removeRenderView()` → `BridgeManager.removeRenderView`，执行不可逆清理（通知 native 销毁 ObjC 对象、清零 renderView 字段）。这使得 `movableContentOf` 在从旧位置 remove 后无法在新位置正常 re-insert——native view 已被销毁，re-insert 时只能重建，导致播放器资源丢失、视频从头重播。

Compose runtime 的 movableContent 实现保证 **先 remove 后 insert**（通过 applyChanges/applyLateChanges 两阶段），且节点会收到 `ComposeNodeLifecycleCallback.onRelease()` 作为最终销毁信号（movableContent 移动时**不会**收到 onRelease，普通销毁时**会**收到）。

**无 NativeBridge 交互**：改动在 KMP 共享层（compose/ + core/），不涉及 native render 通信协议。

## Goals / Non-Goals

**Goals:**
- Compose DSL 中 `movableContentOf` 正常工作：节点可跨容器移动并保持 remember 状态
- native view 资源（AVPlayer、Surface、解码器等）在 move 过程中全程保留，不重建
- 不破坏现有功能：普通 composable 创建/销毁、LazyList 复用、SubcomposeLayout 动态布局
- 代码改动最小化，不修改 Compose runtime 层

**Non-Goals:**
- 跨 Pager 的 movableContent 移动
- 自研 DSL 的相关适配
- `movableContentWithReceiverOf` 支持

## Decisions

### 1. remove 路径拆分为两层：轻量移除 vs 最终销毁

**最终方案**：
- `removeDomSubViewForMove()`（新增）：只移除 flex layout 节点，**不销毁 native render view**
- `removeChildForMove()`（新增）：只从 children 列表移除 + parentRef=0，不调 `didRemoveFromParentView`
- `KNode.onRelease()`（新增 override）：真正销毁时调 `view.removeRenderView()` + `view.didRemoveFromParentView()`

**关键洞察**：Compose Runtime 保证——
- movableContent move：`removeAt` 后**不会**调 `onRelease`（slot 只是移动）
- 普通节点销毁：`removeAt` 后同帧内**一定会**调 `onRelease`

因此普通节点行为等价（只是延迟了一帧内的清理顺序），movableContent 节点则完全保留 native view。

**早期 snapshot 方案（已废弃）**：
- 在 removeAt 时 snapshot renderView 引用，在 reinsert 时 restore
- **根本缺陷**：`BridgeManager.removeRenderView` 已通知 native 销毁 ObjC 对象，restore Kotlin 侧引用后 native registry 已无对应项，`insertSubRenderView` 找不到对应 native view，行为未定义

### 2. 用 isInitialized 标记区分首次/重新插入

**选择**：在 KNode 上增加 `isInitialized: Boolean` 标记。

**原因**：insert 时 `childView.parent` 已为 null（parentRef 在轻量移除时被置 0），无法通过 parent 判断。标记是唯一可靠的区分方式。

**注意**：`isInitialized=true` 不等价于 "是 movableContent 节点"——普通 if/else 切换的节点也会置 true。但对普通节点使用轻量移除路径是安全的，因为 `onRelease` 同帧内一定会补做完整清理。

### 3. LayoutNode.insertAt 保持原始断言

**选择**：不做 defensive remove 处理，保持 `checkPrecondition(instance._foldedParent == null)`。

**原因**：Compose runtime 源码明确保证 "deletes are performed in applyChanges and all inserts are performed in applyLateChanges"。如果断言触发说明有 bug，应该暴露而非静默处理。

### 4. SubcomposeLayout 手动节点的清理

**选择**：依赖 `detach()` 中清理 nativeViewRef，不对 SubcomposeLayout 代码做修改。

**原因**：SubcomposeLayout 手动创建的 VirtualNodeView 不在 slot table 中不会收到 onRelease，但其 attr/event 是空实现（`ContainerAttr`/`Event`），唯一需要清理的 nativeViewRef 在 detach 时处理。

**SubcomposeLayout dispose 顺序**：`composition.dispose()` 在 `root.removeAt(i, 1)` 之前调用，即 `onRelease` 可能在 `removeAt` 之前触发。此时 `onRelease` 里 `view.removeRenderView()` 执行后 renderView 已为 null，`removeAt` 走 `removeDomSubViewForMove` 时 `removeFlexNode` 是幂等的，安全。

### 5. View 对象连续性保证（播放器 / 富媒体场景）

**结论：`renderView` 对象在整个 movableContent 移动过程中保持同一实例，不重建。**

move 前后完整路径：

```
KNode.removeAt(i, n)
  → removeChildrenForMove(index, count)
      → removeDomSubViewForMove()  // 只移 flexNode，不动 renderView ✅
      → removeChildForMove()       // children.remove + parentRef=0
  → super.removeAt()
      → onChildRemoved → detach()  // 清 nativeViewRef，不调 didRemoveFromParentView ✅

KNode.insertTopDown(index, instance)  [isInitialized=true]
  → reinsertChild()                // 重设 pagerId + children.add + reRegisterViewTree
  → insertDomSubView()
      → insertSubRenderView()
          → createRenderView()     // renderView != null → no-op ✅
          → BridgeManager.insertSubRenderView(parentRef, childRef, index)
                                   // iOS: insertSubview:atIndex: 自动从旧父移到新父 ✅
      → createFlexNode()           // 重设 flexNode 回调
      → flexNode.addChildAt()      // 重新加入父 flex tree
```

`didRemoveFromParentView()` 是唯一会执行 `renderView = null` 的地方，轻量移除路径完全绕开了它。播放器 view 持有的解码器、Surface、AVPlayer session 等原生资源**全程不受影响**，视频帧不会因 move 发生黑屏/重载。

### 6. Compose 相关代码的组织

**ViewContainer.kt 内部**：`removeChildForMove`、`removeChildrenForMoveAll`、`reinsertChild`、`reRegisterViewTree`、`removeDomSubViewForMove` 需要访问 `private didCreateFlexNode` 和 `protected children`，只能留在类内，用 `// region Compose movableContent support` 注释分组。

**新建扩展文件**：`ViewContainerMovableContentExt.kt`（compose 模块 `ui/node/` 目录）放 `removeChildrenForMove(index, count)` 扩展函数，它只用 public API，天然适合放独立文件。

## Risks / Trade-offs

| Risk | Mitigation |
|------|-----------|
| 轻量移除后 parentRef=0 但 nativeViewRef 仍在 map 中（短暂不一致） | detach 在同一调用栈中紧跟执行（onChildRemoved→detach），窗口为 0 |
| 普通节点 onRelease 延迟清理 renderView | remove 和 onRelease 在同一个 applyChangesInLocked 内顺序执行，时间窗口为同帧内几毫秒 |
| SubcomposeLayout dispose 顺序（onRelease 先于 removeAt） | onRelease 里 removeRenderView 后 renderView=null，removeDomSubViewForMove 的 removeFlexNode 是幂等的，安全 |
| Compose DSL 是否有使用 ReactiveObserver 的边界情况 | 已验证 compose/src/ 中无任何 ReactiveObserver 引用 |

## File Changes by Module

### `compose/` 模块
- `compose/src/commonMain/kotlin/com/tencent/kuikly/compose/ui/node/KNode.kt` — 核心改动（isInitialized、insertTopDown、removeAt、removeAll、detach、onRelease）
- `compose/src/commonMain/kotlin/com/tencent/kuikly/compose/ui/node/ViewContainerMovableContentExt.kt` — 新增，removeChildrenForMove 扩展函数
- `compose/src/commonMain/kotlin/com/tencent/kuikly/compose/ui/semantics/SemanticsNode.kt` — 修复 deactivated 节点 crash

### `core/` 模块
- `core/src/commonMain/kotlin/com/tencent/kuikly/core/base/ViewContainer.kt` — 新增 movableContent 支持方法（region 分组）

### `core-render-ios/` 模块
- `core-render-ios/Extension/AdvancedComps/KRVideoView.m` — 删除临时 debug log 和幂等 playControl 保护

### `demo/` 模块
- `demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/compose/MovableContentDemo.kt` — 新增，9 个 Demo 覆盖各场景
- `demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/compose/VideoView.kt` — 新增，Video composable 封装
