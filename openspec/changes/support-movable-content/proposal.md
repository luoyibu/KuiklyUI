## Why

KuiklyUI 的 Compose DSL 层当前不支持 `movableContentOf` API。当 composable 内容在不同容器间移动（如从 Column 移到 Row）时，节点会被完全销毁再重建，导致 `remember` 状态丢失。这是 Jetpack Compose 的标准 API，社区开发者期望它能正常工作。

## What Changes

- **KNode 节点生命周期重构**：将 `removeAt` 从"完整销毁"改为"轻量移除"，让 movableContent 节点在移动过程中保留 view 对象及其配置
- **新增 `isInitialized` 标记**：在 `KNode` 上区分首次插入（需要 init）和重新插入（跳过 init，走 reinsert 路径）
- **ViewContainer 新增移动语义方法**：`removeChildForMove` + `reinsertChild`，提供不触发破坏性清理的移除/重插入路径
- **KNode.detach() 清理 nativeViewRef**：保持 pager view map 一致性
- **KNode.onRelease() 执行完整清理**：节点最终销毁时执行 `attr.viewDidRemove` / `event.onViewDidRemove` 等清理
- **LayoutNode.insertAt 恢复原始断言**：Compose runtime 两阶段执行保证 insert 时节点已无 parent
- **新增 MovableContentDemo**：演示 5 种 movableContent 用法场景

## Non-goals

- 不修改 Compose runtime 层（`androidx.compose.runtime.*`），仅适配 Kuikly 的 Applier/Node 层
- 不支持跨 Pager（跨页面）的 movableContent 移动
- 不修改自研 DSL 的响应式系统（ReactiveObserver）
- 不处理 `movableContentWithReceiverOf`（后续版本）

## Capabilities

### New Capabilities
- `compose-movable-content`: Compose DSL 层对 `movableContentOf` 的支持，包括 KNode 生命周期适配、ViewContainer 移动语义、节点状态保持

### Modified Capabilities
<!-- 无需修改已有 spec 的需求 -->

## Impact

- **模块**：`compose/`（KNode、LayoutNode、KuiklyApplier）、`core/`（ViewContainer）、`demo/`（新增 Demo 页面）
- **平台**：Android / iOS / HarmonyOS / Web / 小程序（全平台，因为改动在 KMP 共享层）
- **API**：ViewContainer 新增 public 方法 `removeChildForMove` / `reinsertChild` / `removeChildrenForMoveAll`
- **风险点**：所有 removeAt 改为轻量移除，清理延迟到 detach/onRelease。SubcomposeLayout 手动管理的 VirtualNodeView 依赖 detach 中清理 nativeViewRef
- **依赖**：无新外部依赖
