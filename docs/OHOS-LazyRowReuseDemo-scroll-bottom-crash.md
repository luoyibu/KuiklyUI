# OHOS LazyRowReuseDemo 滑到底 CppCrash — 主干复测说明

> 目的：在**新会话 / 主干（`origin/main`）**上复现「滑到底 crash」，判断是否与 prefetch 分支无关。
> 记录日期：2026-07-14
> 设备：`FMR0223C13000246`（鸿蒙真机）
> 包名：`com.tencent.kuiklyohosdemo`

---

## 1. Demo 完整路径（绝对路径）

> 当前 worktree 根：`/Users/qibu/.cursor/worktrees/KuiklyUI/p1ui2`
> 新会话若用别的 clone / worktree，把前缀换成该仓库根即可；**相对仓库根的后缀不变**。

| 项 | 绝对路径 / 值 |
|---|---|
| Demo 源码（本分支 worktree） | `/Users/qibu/.cursor/worktrees/KuiklyUI/p1ui2/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/compose/LazyRowReuseDemo.kt` |
| Demo 源码（对照主干 `origin/main`） | 同仓库路径：`demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/compose/LazyRowReuseDemo.kt`（`git show origin/main:.../LazyRowReuseDemo.kt`） |
| `@Page` 路由名 | **`LazyRowReuseDemo`** |
| 触发 UI | 同文件 `CardItem`：`Text` → `Modifier.width(80.dp)` + `textAlign = TextAlign.Center`，文案 `"$rowIndex.$colIndex"` |
| 原生绘制（崩溃点） | `/Users/qibu/.cursor/worktrees/KuiklyUI/p1ui2/core-render-ohos/src/main/cpp/libohos_render/expand/components/richtext/KRRichTextView.cpp` → `OnForegroundDraw` |
| OHOS App | `/Users/qibu/.cursor/worktrees/KuiklyUI/p1ui2/ohosApp/` |
| 签名 HAP（编完后） | `/Users/qibu/.cursor/worktrees/KuiklyUI/p1ui2/ohosApp/entry/build/default/outputs/default/entry-default-signed.hap` |
| 本说明文档 | `/Users/qibu/.cursor/worktrees/KuiklyUI/p1ui2/docs/OHOS-LazyRowReuseDemo-scroll-bottom-crash.md` |
| 诊断笔记 | `/Users/qibu/.cursor/worktrees/KuiklyUI/p1ui2/BugFix/ohos-lazyrow-scroll-bottom-crash.md` |

### 主干 Demo vs 本分支 Demo：**不是整文件完全一样**

对比 `origin/main` 与本分支 `cursor/7a7f8852`（`git diff origin/main -- .../LazyRowReuseDemo.kt`）：

| 部分 | 是否相同 | 说明 |
|---|---|---|
| **整文件** | **否** | 分支多约 +81 行：`firstScreenMode`、prefetch / CacheWindow、`beyondBoundsItemCount`、首屏日志等 |
| **`CardItem`（含触发 crash 的 Text）** | **是，字节级一致** | 两边都是 `width(80.dp)` + `TextAlign.Center` + 短文案 |
| **页面结构** | 基本相同 | 外层 LazyColumn + LazyRow / Grid / Pager 交替；分支只是给外层 LazyColumn 加了 mode/prefetch |

**主干复现 crash 时**：用主干自带的 `LazyRowReuseDemo` 即可，**不必**拷分支的 firstScreenMode / prefetch 改动。
触发点是 **`CardItem` 的 Text**，主干已有。

可疑 Text（主干 = 分支）：

```kotlin
// .../LazyRowReuseDemo.kt → CardItem
Text(
    text = "$rowIndex.$colIndex",
    fontSize = 12.sp,
    color = Color(0xFF666666),
    modifier = Modifier.width(80.dp).padding(top = 4.dp),
    textAlign = TextAlign.Center,
)
```

进页：主干用 **Router → `LazyRowReuseDemo`** 即可（本分支的 `FirstScreenLauncher` 主干没有，复现不需要）。

---

## 2. 主干复现步骤（推荐）

### 2.1 编译 Debug OHOS Demo

在**主干仓库根目录**（干净 `main`，不要带本分支未提交的 render 诊断日志）：

```bash
# Kotlin Native so
./2.0_ohos_demo_build.sh
# 或等价：
# KUIKLY_AGP_VERSION=7.4.2 KUIKLY_KOTLIN_VERSION=2.0.21-KBA-010 \
#   ./gradlew -c settings.2.0.ohos.gradle.kts :demo:linkSharedDebugSharedOhosArm64

# HAP
export PATH="/Applications/DevEco-Studio.app/Contents/tools/ohpm/bin:/Applications/DevEco-Studio.app/Contents/tools/hvigor/bin:/Applications/DevEco-Studio.app/Contents/sdk/default/openharmony/toolchains:$PATH"
export DEVECO_SDK_HOME="/Applications/DevEco-Studio.app/Contents/sdk"
cd ohosApp && ohpm install
hvigorw assembleHap --mode module -p module=entry@default -p product=default -p buildMode=debug --no-daemon
```

产物绝对路径示例（按你的仓库根替换前缀）：
`<REPO>/ohosApp/entry/build/default/outputs/default/entry-default-signed.hap`

### 2.2 安装并启动

```bash
TARGET=FMR0223C13000246   # 按 hdc list targets 改
BUNDLE=com.tencent.kuiklyohosdemo
hdc -t $TARGET install <REPO>/ohosApp/entry/build/default/outputs/default/entry-default-signed.hap
hdc -t $TARGET shell aa start -a EntryAbility -b $BUNDLE
```

### 2.3 进页操作

1. App 打开后进 **Compose / Router**，打开页面 **`LazyRowReuseDemo`**
2. **快速向下滑外层 LazyColumn，一直滑到底**（多滑几轮、多复用几次更易触发）
3. 观察是否闪退；`pidof com.tencent.kuiklyohosdemo` 变空即进程挂了

### 2.4 抓 crash 日志

```bash
hdc -t $TARGET shell "hilog -x | grep -E 'SIGTRAP|prepareRoundRect|KRRichTextView|Cpp Crash|APP_CRASH|CPP_CRASH'"
```

---

## 3. 问题分析（已在 prefetch 分支用 Debug 日志验证）

### 3.1 现象

- HarmonyOS 真机，进 `LazyRowReuseDemo`，**滑到底附近**进程 CppCrash
- Release / Debug 均可复现（Debug 上已用诊断日志确认机制）

### 3.2 根因链（Kuikly 侧逻辑）

`KRRichTextView::OnForegroundDraw`（`core-render-ohos`）里：

```cpp
// 伪代码
if (fabs(measureWidth - frameWidth) > 1 || textAlign != LEFT) {
    needReLayout = true;
}
if (needReLayout) {
    OH_Drawing_TypographyLayout(typo, frameWidth * dpi);
    ResetTextAlign(); // 只把 align 标记改成 LEFT，measureWidth 仍是短文本宽
}
OH_Drawing_TypographyPaint(...);
```

对 `CardItem`：

| 量 | 典型值 |
|---|---|
| `measureWidth`（字本身） | ~16–30 |
| `frameWidth`（View） | **80** |
| `textAlign` | Center → 首次 Layout 后 flag 被改成 LEFT |

`ResetTextAlign` **不会**更新 `measureWidth`，于是下一帧仍满足 `measureWidth ≠ frameWidth` → **几乎每帧都 TypographyLayout + Paint**。

Debug 诊断 tag `BD_RichText`（仅某次验证构建存在）曾看到：

- `draw≈872` / `relayout≈720`（约 **82%** 绘制强制 Layout）
- `afterAlignedMeasureOnly` 累计增大：已 Layout 过且 align 已是 LEFT，仍因 measure≠frame 再 Layout

列表滑到底时大量 Text 复用 + 同帧高频 Layout/Paint → 打进系统文本库断言。

### 3.3 与 prefetch 分支关系（主干不崩之后的结论）

**主干复测结果（2026-07-15）：主干滑到底不崩溃。**

因此：**不是「主干也必崩的纯 CardItem + needReLayout」单独必现**；分支上还有差异把 Text 的 Layout/Paint 密度打高，才踩中系统断言。

| 差异 | 主干 | 本分支 | 是否更可能致崩 |
|---|---|---|---|
| `CardItem` Text（80+Center） | 有 | 同 | 触发条件相同，主干有也不崩 |
| `KRRichTextView` needReLayout | 有 | 未改逻辑（仅本地加过 BD 日志） | 同病根，密度不够时不爆 |
| 外层 LazyColumn | 默认 `beyondBoundsItemCount=3`，无 prefetch | **A**：beyond=5；**B**：beyond=0 + **prefetch + CacheWindow ahead1x** | **B 强相关** |
| OHOS Compose Runtime | `1.7.3-kuikly2` | **`1.9.3-kuikly1`**（含 PausableComposition） | 可能改变调度，次要嫌疑 |

**崩溃当次模式（有日志）**：

- Debug BD 复现：`19:18:09` pid=34244 → `mode=prefetch_cache`，随后 `19:18:18` SIGTRAP
- 更早一轮：`18:00:54` / `18:00:58` 也是 `prefetch_cache` 后崩

→ **当前最可能的差异：Demo 开了 prefetch + 一屏 CacheWindow（`prefetch_cache`）**，同帧/短时间内预创建、绘制远多于主干默认列表，把「每帧 TypographyLayout」打爆。

**建议隔离验证（本分支即可）**：

1. 只进 **A `beyond5`**（prefetch 关），滑到底是否仍崩
2. 只进 **B `prefetch_cache`**，滑到底是否必崩
若仅 B 崩 → 定论为 prefetch/CacheWindow 放大触发，而非 CardItem 文案本身在主干也会崩。

---

## 4. 堆栈表现（判定是否同 bug）

### 4.1 信号

```
Reason: Signal:SIGTRAP(TRAP_BRKPT)@0x...
kill reason: Cpp Crash
```

### 4.2 典型栈（两次 crash 一致）

```
#00 librosen_text  skia::textlayout::TextLine::iterateThroughVisualRuns(...)
#01 librosen_text  skia::textlayout::TextLine::prepareRoundRect()+116
#02 librosen_text  skia::textlayout::TextLine::paint(...)
#03 librosen_text  ParagraphImpl::paint / Typography::Paint
#06 libkuikly.so   KRRichTextView::OnForegroundDraw(ArkUI_NodeCustomEvent*)
#07 libkuikly.so   KREventDispatchCenter::OnReceiverCustomEvent / OnCustomEvent
     … ArkUI FOREGROUND_DRAW / vsync …
```

**特征**：

- 不在 `libshared.so`（Kotlin），在 **`libkuikly.so` 富文本绘制**
- 系统库 `librosen_text` + `SIGTRAP`（Skia 断言类，不是普通 SIGSEGV）

### 4.3 系统侧断言含义（上游 SkParagraph）

`iterateThroughVisualRuns` 末尾一致性检查（鸿蒙栈顶在 `prepareRoundRect` 内会调到它）：

```text
按 visual run 累加的 totalWidth  ≠  Layout 时缓存的 line.width()
→ SkDEBUGFAIL / SkASSERT → SIGTRAP
```

即：**行宽两种算法对不上** 时断言失败。高频重复 Layout+Paint 更容易触发。

---

## 5. 主干复测后请记录

请在新会话结尾补这三项：

1. **是否 crash**：是 / 否
2. **若 crash**：hilog 是否仍含 `prepareRoundRect` + `KRRichTextView::OnForegroundDraw` + `SIGTRAP`
3. **结论**：主干同 bug → 与 prefetch 无关，应在 `main` 修 `needReLayout`；主干不崩 → 再查 prefetch 是否放大触发

---

## 6. 相关本地材料（本 prefetch 分支 worktree）

| 绝对路径 | 说明 |
|---|---|
| `/Users/qibu/.cursor/worktrees/KuiklyUI/p1ui2/BugFix/ohos-lazyrow-scroll-bottom-crash.md` | 诊断过程与 BD 日志结论 |
| `/Users/qibu/.cursor/worktrees/KuiklyUI/p1ui2/logs/kuikly_ohos_bd_richtext.log` | 带 `BD_RichText` 的一次 Debug 复现 |
| `/Users/qibu/.cursor/worktrees/KuiklyUI/p1ui2/logs/kuikly_ohos_crash.log` | 较早的 fault/crash 摘录 |

---

## 7. 建议修复方向（确认主干同崩后再做）

收紧文件：
`/Users/qibu/.cursor/worktrees/KuiklyUI/p1ui2/core-render-ohos/src/main/cpp/libohos_render/expand/components/richtext/KRRichTextView.cpp`
（主干同路径，相对仓库根：`core-render-ohos/src/main/cpp/libohos_render/expand/components/richtext/KRRichTextView.cpp`）

`OnForegroundDraw` 的 `needReLayout`：

- **仅当** `last_draw_frame_width_ < 0`（首次/复用重置后）或 **frame 宽变化** 时，因居中 / measure≠frame 去 `TypographyLayout`
- 不要在「align 已 Reset、frame 未变」时仍因 `measureWidth ≠ frameWidth` 每帧 Layout

修完后用同一 Demo 滑到底回归。
