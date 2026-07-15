# 给外工程 / AI 的接入 Prompt（可直接复制）

---

请在我的 Kuikly 工程里接入 **LazyList 滑动性能压测页**，用于对比 **prefetch 开/关** 时的真机滑动 FPS。按下面做，不要改无关代码。

## 背景

- 来源仓库：KuiklyUI（需 `compose` 带 LazyList prefetch：`ComposeFoundationFlags`、`Modifier.enableLazyListPrefetch`）
- **本地 Maven 已发布**（`mavenLocal()`）：
  - `com.tencent.kuikly-open:core:1.3.140-prefetch-local-2.1.21`
  - `com.tencent.kuikly-open:compose:1.3.140-prefetch-local-2.1.21`
  - `com.tencent.kuikly-open:core-annotations:1.3.140-prefetch-local-2.1.21`
  - KSP：`com.tencent.kuikly-open:core-ksp:1.3.140-prefetch-local-2.1.21`
  - Android render：`com.tencent.kuikly-open:core-render-android:1.3.140-prefetch-local-2.1.21`
- 压测页路由名：`LazyListScrollPerfDemo`
- 源文件路径（从 KuiklyUI 拷贝）：
  - `demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/compose/LazyListScrollPerfDemoPage.kt`
  - 可选全局开关：`demo/src/commonMain/kotlin/com/tencent/kuikly/demo/DemoLazyPrefetchBootstrap.kt`
  - 可选 iOS FPS 离页日志：`iosApp/iosApp/KuiklyRenderExpand/Controller/KuiklyRenderViewController.m` 中的 `p_logPerformanceReportOnPageExit` 与 `viewDidDisappear` 调用

## 你要完成的接入

### 1. Kotlin Demo 页

1. 将 `LazyListScrollPerfDemoPage.kt` 复制到我工程的 demo 模块 `pages/compose/`，**修改 package** 为我工程的包名。
2. 若我没有 `ComposeNavigationBar`，把 `setContent { ComposeNavigationBar("...") { ... } }` 改成我工程现有的导航壳，或直接 `setContent { LazyListScrollPerfContent() }`（`LazyListScrollPerfContent` 在同文件 private，可改为 internal 或提到同文件顶层）。
3. 确认 KSP 能扫描 `@Page("LazyListScrollPerfDemo")`，无需手动注册路由。
4. 编译：`./gradlew :demo:compileDebugKotlinAndroid`（或我工程的 demo 模块等价任务）必须通过。

### 2. 开启 Prefetch（二选一或都要）

**页内 A/B（必须保留）：** 压测页顶部 `prefetch` 芯片已用 `Modifier.enableLazyListPrefetch()` / `enableLazyListPrefetch(false)`，无需额外代码。

**全局开关（可选）：** 复制 `DemoLazyPrefetchBootstrap.kt`，在 router 或 `BasePager.created()` 里调用一次：

```kotlin
DemoLazyPrefetchBootstrap.applyGlobalPrefetchForDebug()
```

其内部设置 `ComposeFoundationFlags.isLazyListPrefetchEnabled = true`，且 **`isLazyListPrefetchTraceEnabled = false`**（测 FPS 不要开 trace）。

### 3. iOS FPS（可选但强烈建议）

在我工程的 `KuiklyRenderViewController`（或等价 VC）中：

1. `init` 里已有则保持：`[delegator.performanceManager setMonitorType:KRMonitorType_ALL];`
2. 从 KuiklyUI 拷贝 `p_logPerformanceReportOnPageExit` 方法，在 `viewDidDisappear` 末尾调用。
3. 日志 tag 为 `[KuiklyPerfReport]`，包含 `thread=main` 与 `thread=kotlin` 的 avg/min/max FPS。

### 4. 验证方式（不要自动跑长时间构建，除非我要求）

告诉我：

- 修改了哪些文件路径
- 如何跳转：`pageName = LazyListScrollPerfDemo`
- 如何抓日志：`devicectl --console` + `rg 'KuiklyPerfReport|ScrollPerf'`

## 测试说明（给我人工执行）

1. Release 真机安装 App。
2. 进入 `LazyListScrollPerfDemo`。
3. Round A：`prefetch:ON`，手动 fling 10～15s，返回上一页。
4. Round B：`prefetch:OFF`，同样操作，返回。
5. 把含 `KuiklyPerfReport` 与 `ScrollPerf` 的日志片段发我，对比 kotlin FPS avg/min。

## 约束

- 不要开启 `isLazyListPrefetchTraceEnabled` 做 FPS 测试（println 干扰）。
- 不要改 `compose` 核心 prefetch 调度逻辑，只接 demo + 可选 iOS 日志。
- 保持 `beyondBoundsItemCount = 0`（压测页已写死，勿删）。

---

（完）
