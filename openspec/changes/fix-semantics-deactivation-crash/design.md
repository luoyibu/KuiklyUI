## Context

**DSL 模式**：Compose DSL（`ComposeContainer` + `setContent{}`）

`LayoutNode` 在 Subcompose/Lazy 场景下会被停用（deactivated）以保留节点供后续重用。`LayoutNode.collapsedSemantics` 在 `!isAttached || isDeactivated` 时返回 `null`，这是正确行为——停用的节点不应暴露语义信息。

然而，`SemanticsNode.fillOneLayerOfSemanticsWrappers()` 在收集语义树时只检查了 `child.isAttached`，没有检查 `child.isDeactivated`。当停用节点的父节点触发语义树重建时，停用节点仍会被包装成 `SemanticsNode`，随后访问 `layoutNode.collapsedSemantics!!` 导致 NPE。

**NativeBridge 交互**：无。改动在 KMP 共享层（`compose/`），不涉及 native render 通信。

## Goals / Non-Goals

**Goals:**
- 修复 `ComposeAllSample` 滚动时的 `NullPointerException`
- 确保语义树收集正确跳过 deactivated 节点
- 不引入新的公共 API 或破坏现有语义树契约

**Non-Goals:**
- 修改 `collapsedSemantics` 的返回值逻辑
- 为 deactivated 节点创建空语义配置
- 修改节点生命周期管理（`onDeactivate`/`onReuse`）

## Decisions

### 1. 在 `fillOneLayerOfSemanticsWrappers()` 中增加 `!child.isDeactivated` 判断

**选择**：修改 `SemanticsNode.kt` 的 `fillOneLayerOfSemanticsWrappers()` 方法，在 `if (child.isAttached)` 判断中增加 `&& !child.isDeactivated`。

**替代方案考虑**：
- A) 修改 `SemanticsNode` 构造函数允许 `collapsedSemantics == null` → 放弃：会改变语义树契约，影响其他调用方
- B) 在 `collapsedSemantics` getter 中为 deactivated 节点返回空配置 → 放弃：违背了 `collapsedSemantics` 返回 null 的原有语义

**原因**：
- `LayoutInfo.isDeactivated` 的文档明确说明 deactivated 节点是"保留供重用"的节点
- 语义树不应包含这些节点，因为它们不代表当前 UI 状态
- 最小化改动，只影响语义树收集路径

### 2. 不修改 `unmergedChildren()` 的其他逻辑

**选择**：只修改 `fillOneLayerOfSemanticsWrappers()`，不修改 `unmergedChildren()` 的调用逻辑。

**原因**：`fillOneLayerOfSemanticsWrappers()` 是递归遍历的入口，在此处过滤 deactivated 节点即可阻止整棵子树被收集。

## Risks / Trade-offs

| Risk | Mitigation |
|------|-----------|
| 跳过 deactivated 节点可能导致语义树不完整 | Deactivated 节点是缓存节点，不代表当前 UI，跳过是正确的 |
| 其他平台（iOS/HarmonyOS/Web）是否也有类似问题 | 改动在 KMP 共享层，所有平台同时修复 |
| 是否需要在 `collapsedSemantics` 返回 null 的其他位置加保护 | 当前只修复了已崩溃的路径，后续可审计其他 `!!` 调用 |

## File Changes by Module

### `compose/` 模块

- `compose/src/commonMain/kotlin/com/tencent/kuikly/compose/ui/semantics/SemanticsNode.kt` — 修改 `fillOneLayerOfSemanticsWrappers()` 方法，增加 `!child.isDeactivated` 判断
