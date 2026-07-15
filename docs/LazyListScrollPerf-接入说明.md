# LazyList 滑动性能压测页 — 外工程接入说明

> 本仓库已内置压测页，可直接复制到你的 **Kuikly 业务工程**（需依赖与本仓库对齐的 `compose` prefetch 能力）。

## 〇、本地 Maven（已发布到 `~/.m2`）

在本机 worktree 已执行 `publishToMavenLocal`（含 LazyList prefetch 改动）：

| 项 | 值 |
|----|-----|
| **groupId** | `com.tencent.kuikly-open` |
| **version** | `1.3.140-prefetch-local-2.1.21` |
| **artifactId（常用）** | `core`、`compose`、`core-annotations`、`core-render-android`；KSP 用 `core-ksp`（jvm） |

外工程 `repositories { mavenLocal(); ... }`，`commonMain` / `androidMain` 示例：

```kotlin
implementation("com.tencent.kuikly-open:core:1.3.140-prefetch-local-2.1.21")
implementation("com.tencent.kuikly-open:compose:1.3.140-prefetch-local-2.1.21")
```

Android App / `androidMain` 还需 render 层：

```kotlin
implementation("com.tencent.kuikly-open:core-render-android:1.3.140-prefetch-local-2.1.21")
```

KSP（版本与 Kotlin 2.1.21 对齐，见本仓库 `Version.getKSPVersion()`）：

```kotlin
ksp("com.tencent.kuikly-open:core-ksp:1.3.140-prefetch-local-2.1.21")
```

**在本仓库重新发布**（换版本时改 `KUIKLY_VERSION`）：

```bash
export KUIKLY_VERSION=1.3.140-prefetch-local
bash ./gradlew :core:publishToMavenLocal :core-annotations:publishToMavenLocal \
  :core-ksp:publishToMavenLocal :compose:publishToMavenLocal \
  :core-render-android:publishToMavenLocal --rerun-tasks
```

> iOS 仍通常走 **CocoaPods / 本地 framework**；Maven 主要给 KMP `commonMain` / Android。压测页源码仍需复制（见下文），**不在** compose AAR 里。

## 一、要复制的文件

| 文件（本仓库路径） | 放到你的工程 | 说明 |
|-------------------|-------------|------|
| `demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/compose/LazyListScrollPerfDemoPage.kt` | `demo/.../pages/compose/`（包名改成你的） | 压测 UI + prefetch 开关日志 |
| `demo/src/commonMain/kotlin/com/tencent/kuikly/demo/DemoLazyPrefetchBootstrap.kt`（可选） | `demo/.../` | **全局**打开 LazyList prefetch |
| iOS：`iosApp/iosApp/KuiklyRenderExpand/Controller/KuiklyRenderViewController.m` 中 `p_logPerformanceReportOnPageExit` 与 `viewDidDisappear` 调用（可选） | 你的 `KuiklyRenderViewController` 同类文件 | 离页打印 **kotlin/main FPS** |

**路由名（固定）：** `LazyListScrollPerfDemo`（`@Page` 注解，KSP 自动注册，一般无需改 `settings.gradle`）

**导航壳：** 页内使用 `ComposeNavigationBar`。若外工程没有，把 `setContent { ComposeNavigationBar { ... } }` 换成你自己的顶栏或 `setContent { LazyListScrollPerfContent() }`。

---

## 二、依赖要求

- `compose` 模块含 LazyList prefetch（`Modifier.enableLazyListPrefetch`、`ComposeFoundationFlags.isLazyListPrefetchEnabled`）
- `core` + KSP 能扫描 `@Page`
- iOS：Kotlin/Native target，`isPrefetchSupported = true`（与官方 demo 一致即可）

---

## 三、如何开启 Prefetch

### 方式 A：仅压测页开关（推荐做 A/B）

压测页顶部 **`prefetch:ON/OFF`** 使用：

- ON → `Modifier.enableLazyListPrefetch()`
- OFF → `Modifier.enableLazyListPrefetch(false)`（**压过**全局 flag）

无需改 App 启动代码。

### 方式 B：全局打开（模拟「业务默认全开」）

1. 复制 `DemoLazyPrefetchBootstrap.kt`，包名改为你的 demo 包。
2. 在 **首个会创建的 Pager** 的 `created()` 里调用一次（与 router 首页同级即可）：

```kotlin
import com.your.demo.DemoLazyPrefetchBootstrap

override fun created() {
    super.created()
    DemoLazyPrefetchBootstrap.applyGlobalPrefetchForDebug()
    // ...
}
```

3. `applyGlobalPrefetchForDebug()` 内默认：
   - `ComposeFoundationFlags.isLazyListPrefetchEnabled = true`
   - `isLazyListPrefetchTraceEnabled = false`（**测 FPS 务必关 trace**，`println` 会拖慢滚动）

### 方式 C：业务代码里一行（任意时机、任意模块）

```kotlin
@OptIn(ExperimentalFoundationApi::class)
ComposeFoundationFlags.isLazyListPrefetchEnabled = true
```

---

## 四、iOS：开启 Kuikly 线程 FPS + 离页报告

`KRPerformanceManager` 在 **KuiklyRenderViewController** 初始化时已支持，需保证：

### 1. 打开监控类型（一般已有）

```objc
[_delegator.performanceManager setMonitorType:KRMonitorType_ALL];
```

在 `initWithPageName:pageData:` 里设置（本仓库 demo 已写）。

### 2. 离页打印 FPS（本仓库已加）

`viewDidDisappear` 里调用 `p_logPerformanceReportOnPageExit`，日志 tag：**`KuiklyPerfReport`**

外工程：从本仓库 `KuiklyRenderViewController.m` 拷贝 `p_logPerformanceReportOnPageExit` 及 `viewDidDisappear` 中的调用即可。

### 3. 抓日志（真机）

```bash
# 前台启动并落盘（在项目根）
xcrun devicectl device process launch --device <DEVICE_ID> \
  --terminate-existing --console <BUNDLE_ID> >> ./logs/kuikly_console.log 2>&1 &
```

Kotlin `println` **不会**进 macOS `log stream`，必须用 **`devicectl --console`**。

过滤：

```bash
rg 'KuiklyPerfReport|ScrollPerf|DemoLazyPrefetch' logs/kuikly_console.log
```

---

## 五、推荐测试步骤

1. **Release** 装真机（Debug 也可，但 FPS 以 Release 为准）。
2. 路由跳转：`LazyListScrollPerfDemo`。
3. **Round A**：`prefetch:ON`，`heavy:OFF`，手动快速 fling 10～15s → **返回上一页**。
4. **Round B**：`prefetch:OFF`，同样滑动 → 返回。
5. 对比离页日志：
   - `[KuiklyPerfReport] thread=kotlin avg=... min=... max=...`
   - `[ScrollPerf] phase=page_did_disappear prefetch_modifier=... prefetch_global=...`

| 日志字段 | 含义 |
|---------|------|
| `prefetch_modifier` | 页内 Modifier 开关 |
| `prefetch_global` | `ComposeFoundationFlags.isLazyListPrefetchEnabled` |
| `thread=kotlin avg/min/max` | Kuikly 线程 FPS（performanceManager） |

---

## 六、本仓库 Demo 入口

- 路由：`LazyListScrollPerfDemo`
- 或：Compose 示例列表 → **LazyList滑动性能**

---

## 七、从本仓库编译验证

```bash
bash ./gradlew :demo:compileDebugKotlinAndroid
# iOS：generateDummyFramework + pod install + xcodebuild
```
