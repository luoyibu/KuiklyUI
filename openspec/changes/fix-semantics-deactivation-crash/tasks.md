## 1. 代码修改

- [x] 1.1 修改 `SemanticsNode.kt` 的 `fillOneLayerOfSemanticsWrappers()` 方法，在 `if (child.isAttached)` 判断中增加 `&& !child.isDeactivated`

## 2. 编译验证

- [x] 2.1 运行 `./gradlew :compose:compileDebugKotlinAndroid` 确认 compose 模块编译通过
- [ ] 2.2 运行 `./gradlew :compose:compileKotlinIosSimulatorArm64` 确认 iOS 编译通过（在 Apple Silicon Mac 上）

## 3. Android 功能测试

- [x] 3.1 构建 Android Demo：`./gradlew :androidApp:assembleDebug`
- [x] 3.2 安装到 Android 模拟器并打开 `ComposeAllSample`
- [x] 3.3 反复滚动列表，验证不再崩溃
- [x] 3.4 进入二级页面（如 `LazyColumnStickyHeader`、`TextDemo`、`Gesture`）并滚动，验证无崩溃

## 4. iOS 功能测试

- [ ] 4.1 在 iOS 模拟器上构建并运行 Demo
- [ ] 4.2 打开 `ComposeAllSample` 并滚动列表，验证无崩溃
- [ ] 4.3 进入二级页面并滚动，验证无崩溃

## 5. 文档更新

- [x] 5.1 更新 `BugFix/ComposeAllSampleScrollCrash.md`，记录修复方案和验证结果
- [x] 5.2 创建 openspec change 文档（`openspec/changes/fix-semantics-deactivation-crash/`）
