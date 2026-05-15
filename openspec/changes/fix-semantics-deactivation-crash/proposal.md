## Why

KuiklyUI 的 Compose DSL 在 Android 平台上滚动 `ComposeAllSample` 页面时触发崩溃。崩溃发生在语义树（semantics tree）收集过程中，`SemanticsNode.fillOneLayerOfSemanticsWrappers()` 未过滤已停用的（deactivated）`LayoutNode`，导致访问 `collapsedSemantics!!` 时抛出 `NullPointerException`。

此问题影响所有使用 `SubcomposeLayout` 或 `Lazy` 列表的 Compose DSL 页面，在节点被停用并重用期间触发语义树重建时必现。

## What Changes

- **修改 `SemanticsNode.kt` 的 `fillOneLayerOfSemanticsWrappers()`**：在遍历子节点时增加 `!child.isDeactivated` 判断，跳过已停用的节点
- **不影响 `SemanticsNode` 构造函数**：保持现有语义，不为 deactivated 节点创建空配置

## Non-goals

- 不修改 `LayoutNode.collapsedSemantics` 的现有逻辑（`!isAttached || isDeactivated` 返回 null 是正确的）
- 不修改 `SemanticsNode` 构造函数以接受 null `collapsedSemantics`（这会改变语义树契约）
- 不修改 `onDeactivate()` / `onReuse()` 的生命周期逻辑

## Capabilities

### Modified Capabilities

- `compose-semantics-tree-collection`: 语义树收集现在正确跳过 deactivated 节点，避免 NPE

## Impact

- **模块**：`compose/`（SemanticsNode.kt）
- **平台**：Android / iOS / HarmonyOS / Web / 小程序（改动在 KMP 共享层）
- **API**：无公共 API 变更
- **风险点**：低。仅影响语义树收集；deactivated 节点是 Subcompose/Lazy 的缓存节点，不应参与当前可访问性语义树
- **依赖**：无新外部依赖
