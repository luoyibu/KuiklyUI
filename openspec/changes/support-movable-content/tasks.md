## 1. core/ 模块修改

- [x] 1.1 在 `ViewContainer.kt` 中新增 `removeChildForMove` 方法（轻量移除：willRemoveFromParentView + children.remove + parentRef=0，不调 didRemoveFromParentView）
- [x] 1.2 在 `ViewContainer.kt` 中新增 `reinsertChild` 方法（设 pagerId + willMoveToParentComponent + children.add + parentRef + reRegisterViewTree）
- [x] 1.3 在 `ViewContainer.kt` 中新增 `removeChildrenForMoveAll` 方法（批量调用 removeChildForMove）
- [x] 1.4 在 `ViewContainer.kt` 中新增 `removeDomSubViewForMove` 方法（只移除 flexNode，不调 removeRenderView，保留 native view）
- [x] 1.5 用 `// region Compose movableContent support` 注释在 ViewContainer.kt 内分组上述方法

## 2. compose/ 模块 - KNode 修改

- [x] 2.1 在 `KNode.kt` 中新增 `isInitialized: Boolean` 属性（默认 false，首次 insertTopDown 后设为 true）
- [x] 2.2 修改 `KNode.insertTopDown`：根据 `isInitialized` 区分首次插入（addChild + init）和重新插入（reinsertChild，跳过 init，renderView 非 null 时 createRenderView 自动 no-op）
- [x] 2.3 修改 `KNode.removeAt`：使用 `removeChildrenForMove` 扩展函数（`removeDomSubViewForMove` + `removeChildForMove`），保留 native render view
- [x] 2.4 修改 `KNode.removeAll`：使用 `removeDomSubViewForMove` + `removeChildrenForMoveAll`
- [x] 2.5 修改 `KNode.detach()`：增加 `getPager().removeNativeViewRef(view.nativeRef)` 调用（保持 view map 一致性），不调 didRemoveFromParentView
- [x] 2.6 在 `KNode` 中新增 `onRelease()` override：最终销毁时调 `view.removeRenderView()` + `view.didRemoveFromParentView()`（清理 native view + 完整 Kotlin 侧清理）
- [x] 2.7 新增 `ViewContainerMovableContentExt.kt`：将 `removeChildrenForMove(index, count)` 扩展函数独立到新文件

## 3. compose/ 模块 - SemanticsNode 修复

- [x] 3.1 修复 `SemanticsNode.kt`：children 遍历时增加 `!child.isDeactivated` 守卫，防止 movableContent deactivation 过程中 crash

## 4. core-render-ios/ 模块清理

- [x] 4.1 清理 `KRVideoView.m` 中的临时 debug log（`NSLog([KRVideoDebug]...)`）
- [x] 4.2 清理 `KRVideoView.m` 中的临时幂等 playControl 保护（snapshot 方案时期的临时代码）
- [x] 4.3 清理 `KRVideoView.m` 中的 `setFrame` NSLog

## 5. demo/ 模块

- [x] 5.1 新增 `VideoView.kt`：封装 `Video` composable，支持 `onPlayStateChanged`/`onPlayTimeChanged` 回调
- [x] 5.2 新增 `MovableContentDemo.kt`：包含 9 个 Demo 覆盖完整场景
  - Demo1: Column↔Row 基础移动
  - Demo2: remember 状态（计数器）保持
  - Demo3: 多 movableContent 列表项重排
  - Demo4: movableContentOf 带参数
  - Demo5: 跨容器左右面板切换
  - Demo6: VideoView 播放器跨容器移动（进度不归零验证 native view 连续性）
  - Demo7: LazyColumn item 内含 movableContent（回收重进入）
  - Demo8: 快速连续 move 竞态压测
  - Demo9: if(visible) 条件渲染（保留 vs 重置边界说明）

## 6. 编译验证

- [x] 6.1 `./gradlew :core:compileDebugKotlinAndroid` 通过
- [x] 6.2 `./gradlew :compose:compileDebugKotlinAndroid` 通过
- [x] 6.3 `./gradlew :androidApp:assembleDebug` 通过

## 7. 功能测试（Android）

- [x] 7.1 Demo1（Column↔Row）：布局切换正常
- [x] 7.2 Demo2（状态保持）：计数器 move 后不归零
- [x] 7.3 Demo5（跨容器）：面板左右切换状态保持
- [x] 7.4 LazyColumn 正常滑动、item 回收无异常

## 8. 功能测试（iOS 模拟器）

- [x] 8.1 Demo1 基础移动正常
- [x] 8.2 Demo2 状态保持正常
- [x] 8.3 Demo5 跨容器移动正常
- [x] 8.4 LazyColumn 滑动正常
- [x] 8.5 Demo6 VideoView：move 后播放进度继续递增，不归零 ✅

## 9. 功能测试（iOS 真机）

- [x] 9.1 Demo1-Demo9 全部通过
- [x] 9.2 Demo6 VideoView move 后不重播 ✅

## 10. 功能测试（HarmonyOS）

- [ ] 10.1 构建 ohosApp 验证编译通过（`./2.0_ohos_demo_build.sh`）
