# Advanced Context Engineering for Coding Agents | HumanLayer Blog（中文翻译）

**来源**: https://www.humanlayer.dev/blog/advanced-context-engineering  
**作者**: Dex (HumanLayer)  
**日期**: August 29, 2025

---

## 让 AI 在复杂代码库中工作

AI 编码工具在真实生产代码库中表现不佳，这似乎已经成为共识。Stanford 关于 AI 对开发者生产力影响的研究发现：

1. AI 工具产出的很多"额外代码"最终只是在返工上周产出的垃圾代码。
2. 编码 Agent 对新项目或小改动很有效，但在大型已有代码库中，它们往往让开发者*更*低效。

常见的反应介于悲观者的"这永远行不通"和更温和的"也许有一天模型更智能时"之间。

经过几个月的摸索，我发现**如果你拥抱核心上下文工程原则，你可以用今天的模型走得很远**。

这不是另一个"10 倍提升生产力"的推销。我对 AI 炒作机器的态度[相当谨慎](https://hlyr.dev/12fa)。但我们偶然发现的工作流程让我对可能性相当乐观。我们让 Claude Code 处理了 30 万行 Rust 代码库，一天内交付一周的工作量，并保持通过专家审查的代码质量。我们使用一系列我称之为"频繁有意压缩"的技术——刻意构建你在整个开发过程中向 AI 提供上下文的方式。

我现在完全相信，AI 编码不仅适用于玩具和原型，而是一项深度技术工程手艺。

**视频版本**：如果你更喜欢视频，这篇文章基于[8 月 20 日在 Y Combinator 的演讲](https://hlyr.dev/ace)。

### 来自 AI Engineer 的基础背景

AI Engineer 2025 的两个演讲从根本上塑造了我对这个问题的思考。

第一个是 [Sean Grove 关于"Specs are the new code"的演讲](https://www.youtube.com/watch?v=8rABwKRsec4)，第二个是[关于 AI 对开发者生产力影响的 Stanford 研究](https://www.youtube.com/watch?v=tbDDYKRFjhk)。

Sean 认为我们都在*错误地进行氛围编码*。与 AI Agent 聊两个小时，明确你想要什么，然后扔掉所有提示词只提交最终代码的想法……就像一个 Java 开发者编译 JAR 并提交编译后的二进制文件，同时扔掉源代码。

Sean 提出，在 AI 的未来，specs 将成为真正的代码。两年后，你在 IDE 中打开 Python 文件的频率，将与你今天用十六进制编辑器阅读汇编代码的频率差不多（对我们大多数人来说是从不）。

[Yegor 关于开发者生产力的演讲](https://www.youtube.com/watch?v=tbDDYKRFjhk)解决了一个正交问题。他们分析了 10 万开发者的提交，发现：

(1) AI 工具经常导致大量返工，降低了感知到的生产力收益

(2) AI 工具对 greenfield 项目有效，但对 brownfield 代码库和复杂任务往往适得其反

这与我和创始人交谈时听到的相符：

- "太多垃圾代码。"
- "技术债务工厂。"
- "在大仓库中不起作用。"
- "对复杂系统不起作用。"

关于用 AI 编码处理难题的总体氛围往往是

> 也许有一天，当模型更智能时……

甚至 [Amjad](https://x.com/amasad) 在[9 个月前的 Lenny 播客](https://www.lennysnewsletter.com/p/behind-the-product-replit-amjad-masad)中谈到 PM 如何使用 Replit Agent 原型新东西，然后交给工程师实现生产。（免责声明：我最近没和他联系过（好吧，从来没有），这个立场可能已经改变）

每当我听到"也许有一天当模型更智能时"，我通常会跳起来大喊**这就是上下文工程的全部意义**：从*今天的*模型中获得最大收益。

### 今天实际可能做到什么

我稍后会深入探讨，但为了证明这不只是理论，让我概述一个具体例子。几周前，我决定在我们的技术上测试 [BAML](https://github.com/BoundaryML/baml)，一个 30 万行 Rust 代码库，用于与 LLM 工作的编程语言。我最多是个业余 Rust 开发者，从未接触过 BAML 代码库。

大约一小时内，我提交了一个[修复 bug 的 PR](https://github.com/BoundaryML/baml/pull/2259#issuecomment-3155883849)，第二天早上被维护者批准。几周后，[@hellovai](https://x.com/hellovai) 和我合作向 BAML 交付了 3.5 万行代码，添加[cancellation 支持](https://github.com/BoundaryML/baml/pull/2357)和[WASM 编译](https://github.com/BoundaryML/baml/pull/2330)——团队估计每个功能需要资深工程师 3-5 天。我们在大约 7 小时内准备好了两个草稿 PR。

同样，这都是围绕我们称之为[频繁有意压缩](#what-works-even-better-frequent-intentional-compaction)的工作流程构建的——本质上围绕上下文管理设计你的整个开发过程，保持利用率在 40-60% 范围，并在恰到好处的时机建立高杠杆的人工审查。我们使用"research, plan, implement"工作流程，但这里的核心能力/学习远比任何特定工作流程或提示词集更通用。

### 我们到达这里的奇特旅程

我曾与我见过的最高产的 AI 编码者之一合作。每隔几天他们就会提交**2000 行的 Go PR**。而且这不是 nextjs 应用或 CRUD API。这是复杂的、[有竞态条件的系统代码](https://github.com/humanlayer/humanlayer/blob/main/hld/daemon/daemon_subscription_integration_test.go#L45)，通过 unix sockets 做 JSON RPC，并管理来自 fork 的 unix 进程的流式 stdio（主要是 Claude Code SDK 进程，稍后详述 🙂）。

每隔几天仔细阅读 2000 行复杂 Go 代码的想法根本不可持续。我开始有点像 Mitchell Hashimoto 为 Ghostty 添加[AI 贡献必须披露](https://github.com/ghostty-org/ghostty/pull/8289)规则时的感觉。

我们的方法是采用类似 Sean 的**spec-driven development**。

一开始很不舒服。我必须学会放手不阅读每一行 PR 代码。我仍然相当仔细地阅读测试，但 specs 成为我们构建内容和原因的真实来源。

转型花了大约 8 周。对参与的每个人来说都非常不舒服，尤其是我。但现在我们飞起来了。几周前，我一天内提交了 6 个 PR。在过去三个月里，我手动编辑非 markdown 文件的次数一只手就能数过来。

---

## 编码 Agent 的高级上下文工程

我们需要的是：

- 在 Brownfield 代码库中有效工作的 AI
- 解决复杂问题的 AI
- 没有垃圾代码
- 保持团队心智对齐

（当然，让我们尽可能多地花费 token。）

我将深入探讨：

1. 我们将上下文工程应用于编码 Agent 时学到的东西
2. 使用这些 Agent 在哪些维度上是一项深度技术手艺
3. 为什么我不相信这些方法可推广
4. 关于 (3) 我被反复证明错误的次数

### 但首先：管理 Agent 上下文的朴素方式

我们大多数人开始时像使用聊天机器人一样使用编码 Agent。你来回与它交谈（或[醉醺醺地喊叫](https://ghuntley.com/six-month-recap/#:~:text=Last%20week%2C%20over%20Zoom%20margaritas%2C%20a%20friend%20and%20I%20reminisced%20about%20COBOL.)），通过氛围编码解决问题，直到你用尽上下文、放弃，或 Agent 开始道歉。

稍微聪明一点的方式是当你偏离轨道时重新开始，丢弃会话并开启新会话，也许在提示词中多一点引导。

> [原始提示词]，但确保你使用 XYZ 方法，因为 ABC 方法行不通

### 稍微聪明一点：有意压缩

你可能做过我称之为"有意压缩"的事情。无论你是否在正轨上，当你的上下文开始填满时，你可能想暂停工作并用全新的上下文窗口重新开始。为此，你可能使用这样的提示词：

> "把我们到目前为止做的所有事情写到 progress.md，确保记录最终目标、我们采用的方法、已完成的步骤，以及我们正在处理的当前失败"

你也可以[使用提交消息进行有意压缩](https://x.com/dexhorthy/status/1961490837017088051)。

### 我们到底在压缩什么？

什么消耗上下文？

- 搜索文件
- 理解代码流
- 应用编辑
- 测试/构建日志
- 工具产生的大量 JSON

所有这些都会淹没上下文窗口。**压缩**就是将它们提炼成结构化产物。

有意压缩的好输出可能包括：
- 最终目标
- 方法
- 已完成步骤
- 当前失败

### 为什么痴迷于上下文？

正如我们在 [12-factor agents](https://hlyr.dev/12fa) 中深入探讨的，LLM 是无状态函数。影响输出质量的唯一因素（不训练/调整模型本身）是输入的质量。

对于[使用](https://www.youtube.com/watch?v=F_RyElT_gJk)编码 Agent 和一般 Agent 设计都是如此，只是问题空间更小，而且我们不是构建 Agent，而是谈论使用 Agent。

在任何给定点，Claude Code 等 Agent 的一次回合都是无状态函数调用。上下文窗口进，下一步出。

也就是说，上下文窗口的内容是你影响输出质量的**唯一**杠杆。所以是的，值得痴迷。

你应该为以下方面优化上下文窗口：

1. 正确性
2. 完整性
3. 大小
4. 轨迹

换句话说，上下文窗口最糟糕的事情，按顺序是：

1. 错误信息
2. 缺失信息
3. 太多噪音

Geoff Huntley 说：

> 游戏的名字是你只有大约**170k 的上下文窗口**可以使用。所以尽可能少使用它至关重要。你使用的上下文窗口越多，结果就越差。

Geoff 解决这个工程约束的方案是一种他称之为 [Ralph Wiggum as a Software Engineer](https://ghuntley.com/ralph/) 的技术，基本上涉及在 while 循环中永远运行 Agent，使用简单的提示词。

Geoff 将 ralph 描述为解决上下文窗口问题的" hilariously dumb"方案。[我不完全确定它是 dumb 的](https://ghuntley.com/content/images/size/w2400/2025/07/The-ralph-Process.png)。

### 回到压缩：使用子 Agent

子 Agent 是管理上下文的另一种方式，通用子 Agent（即不是[自定义](https://docs.anthropic.com/en/docs/claude-code/sub-agents)的）自早期以来一直是 Claude Code 和许多编码 CLI 的功能。

子 Agent 不是[玩过家家和拟人化角色](https://x.com/dexhorthy/status/1950288431122436597)。子 Agent 是关于上下文控制。

子 Agent 最常见/直接的用例是让你使用全新的上下文窗口进行查找/搜索/总结，使父 Agent 能够直接开始工作，而不会因为 Glob / Grep / Read 等调用而污染其上下文窗口。

理想的子 Agent 响应可能看起来类似于上面的理想临时压缩。

让子 Agent 返回这个并非易事。

### 效果更好：频繁有意压缩

我想谈论的以及我们在过去几个月采用的技术属于我称之为"频繁有意压缩"的范畴。

本质上，这意味着围绕上下文管理设计你的**整个工作流程**，并保持利用率在 40%-60% 范围（取决于问题的复杂性）。

我们的方式是分成三个（左右）步骤。

我说"左右"是因为有时我们跳过研究直接进入规划，有时在准备实施前我们会做多次压缩研究。

对于给定的功能或 bug，我们倾向于做：

**Research（研究）**

理解代码库、与问题相关的文件、信息如何流动，以及问题的潜在原因。

这是我们的[研究提示词](https://github.com/humanlayer/humanlayer/blob/main/.claude/commands/research_codebase.md)。它目前使用自定义子 Agent，但在其他仓库中我使用更通用的版本，使用 Claude Code Task() 工具和 `general-agent`。通用的几乎一样好用。

**Plan（规划）**

概述我们将采取的修复问题的确切步骤，以及需要编辑的文件和方式，对每个阶段的测试/验证步骤非常精确。

这是我们用于规划的[提示词](https://github.com/humanlayer/humanlayer/blob/main/.claude/commands/create_plan.md)。

**Implement（实现）**

逐步执行计划。对于复杂工作，我经常在每个实施阶段验证后将当前状态压缩回原始计划文件。

这是我们使用的[实现提示词](https://github.com/humanlayer/humanlayer/blob/main/.claude/commands/implement_plan.md)。

顺便说一句——如果你经常听到 git worktrees，这是唯一需要在 worktree 中完成的步骤。我们倾向于在主分支上做其他所有事情。

**我们如何管理/共享 markdown 文件**

为简洁起见，我将跳过这部分，但欢迎在 [humanlayer/humanlayer](https://github.com/humanlayer/humanlayer) 中启动 Claude 会话并询问"thoughts tool"如何工作。

### 付诸实践

我每周与 [@vaibhav](https://www.linkedin.com/in/vaigup/) 做一次[直播编码](https://github.com/ai-that-works/ai-that-works)，我们在白板上讨论并编写高级 AI 工程问题的解决方案。这是我一周的亮点之一。

几周前，我[决定分享更多关于这个过程的信息](https://hlyr.dev/he-gh)，好奇我们的内部技术是否能一次性修复 BAML 的 30 万行 Rust 代码库中的问题，BAML 是一个与 LLM 工作的编程语言。我从 @BoundaryML 仓库中挑选了一个（ admittedly 较小的）bug，开始工作。

你可以[观看节目](https://hlyr.dev/he-yt)了解更多过程，但概述如下：

**值得注意的是**：我最多是个业余 Rust 开发者，从未接触过 BAML 代码库。

#### 研究

- 我创建了一份研究，我读了它。Claude 认为 bug 无效，代码库是正确的。
- 我扔掉那份研究，用更多引导启动了新的研究。
- 这是我最终使用的[最终研究文档](https://github.com/ai-that-works/ai-that-works/blob/main/2025-08-05-advanced-context-engineering-for-coding-agents/thoughts/shared/research/2025-08-05_05-15-59_baml_test_assertions.md)

#### 计划

- 当研究运行时，我不耐烦了，在没有研究的情况下启动了一个计划，看看 Claude 是否能直接进入实施计划——[你可以在这里看到](https://github.com/ai-that-works/ai-that-works/blob/main/2025-08-05-advanced-context-engineering-for-coding-agents/thoughts/shared/plans/fix-assert-syntax-validation-no-research.md)
- 研究完成后，我启动了另一个使用研究结果的实施计划——[你可以在这里看到](https://github.com/ai-that-works/ai-that-works/blob/main/2025-08-05-advanced-context-engineering-for-coding-agents/thoughts/shared/plans/baml-test-assertion-validation-with-research.md)

两个计划都相当短，但差异显著。它们以不同方式修复问题，有不同的测试方法。不深入细节，它们都"会奏效"，但有研究的计划在*最佳*位置修复了问题，并规定了符合代码库规范的测试。

#### 实现

- 这都是播客录制前一晚发生的。我并行运行两个计划，并在当晚休息前将两者都作为 PR 提交。

到第二天早上 10 点 PT 我们上节目时，[来自有研究计划的 PR 已经被 @aaron 批准](https://github.com/BoundaryML/baml/pull/2259#issuecomment-3155883849)，他甚至不知道我在为播客做演示 🙂。我们[关闭了另一个](https://github.com/BoundaryML/baml/pull/2258/files)。

所以我们原来的 4 个目标中，我们达成了：

- ✅ 在 brownfield 代码库中工作（30 万行 Rust 项目）
- 解决复杂问题
- ✅ 没有垃圾代码（PR 被合并）
- 保持心智对齐

### 解决复杂问题

Vaibhav 仍然持怀疑态度，我想看看我们是否能解决更复杂的问题。

所以几周后，我们俩花了 7 小时（3 小时研究/规划，4 小时实现）向 BAML 交付了 3.5 万行代码，添加 cancellation 和 wasm 支持。[cancelation PR 上周刚刚合并](https://github.com/BoundaryML/baml/pull/2357)。[WASM 的仍然开放](https://github.com/BoundaryML/baml/pull/2330)，但有一个在浏览器中从 JS 应用调用 wasm 编译的 Rust 运行时的可用演示。

虽然 cancelation PR 需要更多关爱才能完成，但我们在一天内取得了惊人的进展。Vaibhav 估计每个 PR 都需要 BAML 团队的资深工程师 3-5 天才能完成。

✅ 所以我们也能解决复杂问题。

### 这不是魔法

记住例子中我读了研究然后因为错误而扔掉的部分吗？或者我和 Vaibhav 深度投入 7 小时？当你这样做时，你必须投入你的任务，否则它不会起作用。

有一种人总是在寻找能解决所有问题的一个神奇提示词。它不存在。

通过 research/plan/implement 流程进行频繁有意压缩会让你的表现**更好**，但让它**足以应对难题**的是你在流程中建立了高杠杆的人工审查。

### 丢脸的时刻

几周前，[@blakesmith](https://www.linkedin.com/in/bhsmith/) 和我坐了 7 小时，[试图从 parquet java 中移除 hadoop 依赖](https://github.com/dexhorthy/parquet-java/blob/remove-hadoop/thoughts/shared/plans/remove-hadoop-dependencies.md)——关于出错的一切和我的理论，我将留到另一篇文章， suffice it to say 不顺利。简而言之，研究步骤没有足够深入地遍历依赖树，假设类可以向上游移动而不会引入深度嵌套的 hadoop 依赖。

有些大问题你不能仅仅通过提示词在 7 小时内解决，我们仍然好奇和兴奋地与朋友和合作伙伴一起突破边界。我认为另一个教训是你可能至少需要一个代码库专家，在这种情况下，我们俩都不是。

### 关于人类杠杆

如果有一件事你要从这一切中带走，那就是：

一行糟糕的代码……就是一行糟糕的代码。但**计划**中的一行糟糕可能导致数百行糟糕的代码。而**研究**中的一行糟糕，对代码库如何工作或某些功能位于何处的误解，可能导致数千行糟糕的代码。

所以你想将**人类努力和注意力**集中在流程的**最高杠杆**部分。

当你审查研究和计划时，你获得的杠杆比审查代码时更多。

### 代码审查的目的是什么？

人们对代码审查的目的有很多不同意见。

我更喜欢 [Blake Smith 在 Code Review Essentials for Software Teams 中的框架](https://blakesmith.me/2015/02/09/code-review-essentials-for-software-teams.html)，他说代码审查最重要的部分是心智对齐——让团队成员对代码如何变化以及为什么变化保持一致。

还记得那些 2000 行的 golang PR 吗？我关心它们是否正确和设计良好，但团队内部不安和沮丧的最大来源是缺乏心智对齐。**我开始与我们的产品是什么以及它如何工作失去联系。**

我预计任何与高产 AI 编码者合作过的人都有这种经历。

这实际上是我们 research/plan/implement 最重要的部分。每个人提交更多代码的一个必然副作用是，在任何时间点，代码库中更大比例的部分对任何给定工程师来说都会是不熟悉的。

我甚至不会试图说服你 research/plan/implement 对大多数团队来说是正确的方法——它可能不是。但你**绝对**需要一个工程流程：

1. 让团队成员保持一致
2. 让团队成员快速了解代码库中不熟悉的部分

对大多数团队来说，这是 pull requests 和内部文档。对我们来说，现在是 specs、plans 和 research。

我无法每天阅读 2000 行 golang。但我*可以*阅读 200 行写得很好的实现计划。

当某些东西坏了时，我无法花一个多小时在 40 多个文件的守护进程代码中探索（好吧，我可以，但我不想）。我*可以*引导一个研究提示词给我快速介绍我应该在哪里看以及为什么。

### 总结

基本上我们得到了我们需要的一切。

- ✅ 在 brownfield 代码库中工作
- ✅ 解决复杂问题
- ✅ 没有垃圾代码
- ✅ 保持心智对齐

（哦，是的，我们三人团队平均每月在 Opus 上花费约 $12k）

所以你不认为我只是另一个[炒作的留胡子销售 guy](https://www.youtube.com/watch?v=IS_y40zY-hc&lc=UgzFldRM6LU5unLuFn54AaABAg.AMKlTmJAT5ZAMKrOOAMw3I)，我会指出这并不对每个问题都完美有效（我们会回来再试一次，parquet-java）。

8 月整个团队在一个非常棘手的竞态条件上转了两周，这个条件 spiraled 成了 golang 中 MCP sHTTP keepalives 的一系列问题兔子洞和一堆其他死胡同。

但那是现在的例外。总的来说，这对我们很有效。我们的实习生在第一天提交了 2 个 PR，在第 8 天提交了 10 个。我真的怀疑它对其他人是否有效，但我和 Vaibhav 在 7 小时内交付了 3.5 万行可用的 BAML 代码。（如果你没见过 Vaibhav，他是我所知道的代码设计和质量方面最细致的工程师之一。）

### 未来展望

我相当有信心编码 Agent 将会商品化。

困难的部分将是团队和工作流程转型。在 AI 编写我们 99% 代码的世界中，关于协作的一切都将改变。

我非常相信，如果你不解决这个问题，你会被解决它的人超越。

### 显然你有东西要卖给我

我们对 spec-first、agentic 工作流程非常看好，所以我们正在构建工具让它更容易。其中，我痴迷于在大型团队中协作扩展这些"频繁有意压缩"工作流程的问题。

今天，我们正在推出 CodeLayer，我们的新"post-IDE IDE"私人测试版——想想"Claude Code 的 Superhuman"。如果你是 Superhuman 和/或 vim 模式的粉丝，你准备超越"氛围编码"并认真对待用 Agent 构建，我们很乐意让你加入等待名单。

**在 https://humanlayer.dev 注册**。

---

## 对于开源维护者——让我们一起交付一些东西

如果你是复杂开源项目的维护者，并且位于湾区，我的公开提议——我会在周六在 SF 与你面对面配对 7 小时，看看我们是否能交付一些大东西。

我获得了很多关于限制和这些技术在哪里失效的学习（而且，如果运气好的话，一个可以指出的已合并的工作 PR，增加了大量价值）。你以我发现唯一有效的方式学习工作流程——直接 1x1 配对。

## 对于工程领导者

如果你或你认识的人是希望用 AI 将团队生产力提升 10 倍的工程领导者，我们正在与各种规模的团队前向部署，帮助推动过渡到 AI 优先编码世界所需的文化/流程/技术转变。

### 感谢

- 感谢所有听过这篇文章早期 ramble-y 版本的朋友和创始人——Adam、Josh、Andrew 和很多很多其他人
- 感谢 Sundeep 经受住这场疯狂的风暴
- 感谢 Allison、Geoff 和 Gerred 连拖带喊地把我们带入未来

---

*翻译完成时间: 2026-03-31*  
*原文: https://www.humanlayer.dev/blog/advanced-context-engineering*
