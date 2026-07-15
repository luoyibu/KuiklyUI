# LazyRowReuseDemo 首屏耗时对比（beyond vs prefetch）

> 测试日期：2026-07-14
> 场景：外层 LazyColumn 嵌 LazyRow / Grid / Pager（`LazyRowReuseDemo`）
> 流程：App 先进 **Native 壳页**，等热身后点击进 Demo，杀进程后冷启再测下一组，避免 App 冷启动污染首屏。

## 对比配置

| 组 | 配置 |
|---|---|
| **A** | 外层 LazyColumn `beyondBoundsItemCount=5`，不开启 prefetch |
| **B** | `beyondBoundsItemCount=0` + `Modifier.enableLazyListPrefetch()` + CacheWindow `aheadFraction=1`（一屏 ahead） |

主指标：`pageLoadTime.firstPaintCost`（ms）。

---

## iOS（Release 真机）

设备：iPhone 13 Pro Max
入口：`RootViewController` 按钮进页，右上角原生「返回」。

| 轮次 | A · beyond=5 | B · prefetch + 1屏 | Δ |
|---|---|---|---|
| 1 | 103 | 81 | B −22 |
| 2 | 108 | 79 | B −29 |
| **均值** | **105.5** | **80** | **B 约快 24%** |

**iOS 结论**：B 首屏更短，两轮稳定；约快 **20～30 ms（~24%）**。

---

## Android（Release 真机）

设备：Vivo V2141A（PD2141）
入口：`FirstScreenLauncherActivity` 按钮进页，系统返回键回壳页。
包：`com.tencent.kuikly.android.demo` Release APK。

| 轮次 | A · beyond=5 | B · prefetch + 1屏 | 备注 |
|---|---|---|---|
| （剔除） | ~~375~~ | 141 | A 首次异常偏高，**不入结论** |
| 有效 1 | 161 | 135 | 杀进程后进页 |
| 有效 2 | 175 | — | 再测 A |

有效样本：

| | A（有效） | B（有效） |
|---|---|---|
| 样本 | 161、175 | 141、135 |
| **均值** | **168** | **138** |
| **Δ** | — | **B 约 −30 ms（~18%）** |

**Android 结论**（已去掉 375 ms 异常值）：B 首屏更短，约快 **30 ms（~18%）**；方向与 iOS 一致。

---

## HarmonyOS（Release 真机）

设备：`FMR0223C13000246`
入口：`FirstScreenLauncher` → `LazyRowReuseDemo`
包：`com.tencent.kuiklyohosdemo` Release HAP（`sharedReleaseShared` + `buildMode=release`）。
Runtime：本地 `1.9.3-kuikly1`（含 PausableComposition）。

| 轮次 | A · beyond=5 | B · prefetch + 1屏 | 备注 |
|---|---|---|---|
| （剔除） | ~~91~~ | — | 安装后首次偏高，**不入结论** |
| 有效 1 | 79 | 50 | |
| 有效 2 | 77 | 49 | |

有效样本：

| | A（有效） | B（有效） |
|---|---|---|
| 样本 | 79、77 | 50、49 |
| **均值** | **78** | **49.5** |
| **Δ** | — | **B 约 −28.5 ms（~37%）** |

**HarmonyOS 结论**（已去掉安装后首次 A=91）：B 首屏更短，约快 **29 ms（~37%）**；方向与 iOS / Android 一致。

> Debug 参考（同机、勿与 Release 比绝对值）：A 356；B 180 / 199。

---

## 总结论

1. **首屏上 B 优于 A**（iOS / Android / HarmonyOS 同向）：首帧不必为 beyond 多 compose 若干屏外项；prefetch + CacheWindow 的补齐发生在 idle，不阻塞 first paint。
2. **量级**：iOS 约 **−24%**；Android（剔异常后）约 **−18%**；HarmonyOS Release（剔安装首次后）约 **−37%**。绝对值受机型与功耗策略影响，不可跨端直接比 ms。
3. **不代表滚动 FPS 收益**：本次只测 first paint；快速 fling 下的 FPS 需另测（prefetch 主战场在入屏 jank，而非 avg FPS）。
4. **推荐用法**：首屏敏感列表可用 `beyond=0` + opt-in prefetch + 合理 CacheWindow；不必用大 `beyondBoundsItemCount` 硬换「预渲染」。

---

## 相关入口（本仓库本地改动，未必进 PR）

| 端 | 入口 |
|---|---|
| iOS | `RootViewController` → `LazyRowReuseDemo` + `firstScreenMode` |
| Android | `FirstScreenLauncherActivity` → 同 pageData |
| OHOS | `FirstScreenLauncher` → 同 pageData |
| Demo | `demo/.../LazyRowReuseDemo.kt`（`beyond5` / `prefetch_cache`） |
| 日志 | iOS：`[KuiklyFirstPaint]` / `FirstScreenPerf`；Android/OHOS：`FirstScreenPerf` / `LaunchData.firstPaintCost` |
