## 1. core/ 模块修改

- [x] 1.1 在 `ViewContainer.kt` 中新增 `removeChildForMove` 方法（轻量移除：willRemoveFromParentView + children.remove + parentRef=0，不调 didRemoveFromParentView）
- [x] 1.2 在 `ViewContainer.kt` 中新增 `reinsertChild` 方法（设 pagerId + willMoveToParentComponent + children.add + parentRef + didMoveToParentView）
- [x] 1.3 在 `ViewContainer.kt` 中新增 `removeChildrenForMoveAll` 方法（批量调用 removeChildForMove）
- [x] 1.4 删除 `ViewContainer.kt` 中的 `addChildWithoutInit` 方法（由 reinsertChild 替代）

## 2. compose/ 模块 - KNode 修改

- [x] 2.1 在 `KNode.kt` 中新增 `isInitialized: Boolean` 属性（默认 false，首次 insertTopDown 后设为 true）
- [x] 2.2 修改 `KNode.insertTopDown`：根据 `isInitialized` 区分首次插入（addChild + init）和重新插入（reinsertChild）
- [x] 2.3 修改 `KNode.removeAt`：用 `removeChildrenForMove` 扩展函数替代原有的 `removeChildrenAt`（轻量移除 + removeDomSubView）
- [x] 2.4 修改 `KNode.removeAll`：用 `removeChildrenForMoveAll` 替代原有的 `removeChildren`
- [x] 2.5 修改 `KNode.detach()`：增加 `getPager().removeNativeViewRef(view.nativeRef)` 调用（保持 view map 一致性）
- [x] 2.6 在 `KNode` 中 override `onRelease()`：执行完整 view 清理（通过调用 view.didRemoveFromParentView()）

## 3. compose/ 模块 - LayoutNode 修改

- [x] 3.1 恢复 `LayoutNode.insertAt` 的原始 `checkPrecondition(instance._foldedParent == null)` 断言（撤回当前分支的修改）
- [x] 3.2 修复 `LayoutNode.removeAt` 的缩进问题（当前分支引入的多余空格）

## 4. demo/ 模块

- [x] 4.1 清理 `MovableContentDemo.kt` 中的 `println` 调试日志

## 5. 编译验证

- [x] 5.1 运行 `./gradlew :core:compileDebugKotlinAndroid` 确认 core 模块编译通过
- [x] 5.2 运行 `./gradlew :compose:compileDebugKotlinAndroid` 确认 compose 模块编译通过
- [x] 5.3 运行 `./gradlew :androidApp:assembleDebug` 确认 Android Demo 构建成功

## 6. 功能测试（Android）

- [x] 6.1 在 Android 模拟器上运行 MovableContentDemo，验证基础移动（Column↔Row）状态保持
- [x] 6.2 验证跨容器移动（左右面板切换）状态保持
- [x] 6.3 验证 LazyColumn 正常滑动、item 回收无异常

## 7. 功能测试（iOS）

- [x] 7.1 在 iOS 模拟器上运行 MovableContentDemo，验证基础移动状态保持
- [x] 7.2 验证 LazyColumn 正常滑动无异常

## 7.5 补充 Demo6（view 对象连续性 / 播放器场景）

- [x] 7.5.1 新增 `VideoViewMoveDemo`：movableContent 内嵌真实 `VideoView`，通过 `playTimeDidChanged` 回调观察 move 前后播放进度是否归零
  - testTag: `demo6_btn_move`、`demo6_move_count_text`、`demo6_play_time_text`、`demo6_status_text`
- [~] 7.5.2 iOS 模拟器运行验证
  - **阻塞原因**：`KRVideoView` 初始化时 `WMPlayer.IsiPhoneX` 内部调用 `[[UIApplication sharedApplication].delegate window]`，而 demo iosApp 使用 SwiftUI 生命周期，AppDelegate 无 `window` 属性，导致 `NSInvalidArgumentException` crash
  - 这是 **WMPlayer 第三方库的已有 bug**，与 movableContent 实现无关
  - Demo6 的 DemoCard 已从 iOS 页面暂时移除，`VideoViewMoveDemo` 函数保留代码供 Android 验证或后续 WMPlayer 修复后使用
  - **架构层面已通过代码分析确认**：`reinsertChild` 路径完全绕过 `didRemoveFromParentView()`，`renderView` 字段从不清零，native player 资源保留（见 design.md §5）

## 8. 功能测试（HarmonyOS）

- [ ] 8.1 构建 ohosApp 验证编译通过（`./2.0_ohos_demo_build.sh`）
