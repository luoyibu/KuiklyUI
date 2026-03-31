# 如何写好 CLAUDE.md（中文翻译）

**来源**: https://www.humanlayer.dev/blog/writing-a-good-claude-md  
**作者**: Kyle (HumanLayer)  
**日期**: November 25, 2025

---

*注：这篇文章同样适用于 `AGENTS.md`*，它是 `CLAUDE.md` 的开源等效物，用于 OpenCode、Zed、Cursor 和 Codex 等 Agent 和 Harness。

---

## 原则：LLM 是（大部分）无状态的

LLM 是无状态函数。它们的权重在用于推理时就已经冻结，因此不会随时间学习。模型对你的代码库唯一了解的就是你输入给它的 token。

同样，编码 Agent Harness（如 Claude Code）通常要求你显式管理 Agent 的记忆。`CLAUDE.md`（或 `AGENTS.md`）是唯一一个默认进入你与 Agent 的*每一次对话*的文件。

**这有三个重要含义：**

1. 编码 Agent 在每个会话开始时对你的代码库绝对一无所知。
2. 每次开始会话时，你必须告诉 Agent 关于代码库的任何重要信息。
3. `CLAUDE.md` 是执行此操作的首选方式。

---

## `CLAUDE.md` 让 Claude 加入你的代码库

由于 Claude 在每个会话开始时对代码库一无所知，你应该使用 `CLAUDE.md` 让 Claude 加入你的代码库。在高层次上，这意味着它应该涵盖：

- **WHAT**：告诉 Claude 技术、你的技术栈、项目结构。给 Claude 一个代码库的地图。这在 monorepos 中尤其重要！告诉 Claude 应用是什么、共享包是什么、一切都是做什么的，这样它就知道在哪里查找东西
- **WHY**：告诉 Claude 项目的*目的*以及仓库中的一切都在做什么。项目的不同部分的目的是什么？
- **HOW**：告诉 Claude 它应该如何处理项目。例如，你使用 `bun` 而不是 `node` 吗？你希望包含它实际在项目中做有意义工作所需的所有信息。Claude 如何验证 Claude 的更改？它如何运行测试、类型检查和编译步骤？

但这样做的方式很重要！不要试图把 Claude 可能需要运行的每一个命令都塞进你的 `CLAUDE.md` 文件中——你会得到次优的结果。

---

## Claude 经常忽略 `CLAUDE.md`

无论你使用哪个模型，你可能会注意到 Claude 经常忽略 `CLAUDE.md` 文件的内容。

你可以通过使用 `ANTHROPIC_BASE_URL` 在 claude code CLI 和 Anthropic API 之间放置一个日志代理来自己调查这一点。Claude code 在你的 `CLAUDE.md` 文件中向 Agent 的用户消息注入以下系统提醒：

```
<system-reminder>
       IMPORTANT: this context may or may not be relevant to your tasks.
       You should not respond to this context unless it is highly relevant to your task.
</system-reminder>
```

因此，如果 Claude 决定它与当前任务无关，它将忽略 `CLAUDE.md` 的内容。文件中包含的**非普遍适用**的信息越多，Claude 忽略文件中指令的可能性就越大。

*Anthropic 为什么添加这个？* 很难说，但我们可以推测一下。我们遇到的大多数 `CLAUDE.md` 文件包含一堆*并非*广泛适用的指令。许多用户将该文件视为添加"热修复"的方式，通过追加不一定广泛适用的许多指令。

我们只能假设 Claude Code 团队发现，通过告诉 Claude 忽略坏指令，Harness 实际上产生了更好的结果。

---

## 创建一个好的 `CLAUDE.md` 文件

以下部分提供了关于如何根据[上下文工程最佳实践](https://github.com/humanlayer/12-factor-agents/blob/d20c728368bf9c189d6d7aab704744decb6ec0cc/content/factor-03-own-your-context-window.md)编写好的 `CLAUDE.md` 文件的一些建议。

*你的结果可能会有所不同。* 并非所有这些规则对每个设置都必然是最优的。和其他任何事情一样，一旦……

1. 你理解何时以及为什么打破它们是可以的
2. 你有充分的理由这样做

……请随意打破规则。

### 少（指令）即是多

试图把 Claude 可能需要运行的每一个命令以及你的代码标准和风格指南都塞进 `CLAUDE.md` 是很诱人的。**我们建议不要这样做。**

虽然这个话题还没有被极其严格地研究过，但[一些研究](https://arxiv.org/pdf/2507.11538)已经完成，表明以下几点：

1. **前沿思考型 LLM 可以合理地遵循约 150-200 条指令。** 较小的模型可以比大型模型关注更少的指令，非思考型模型可以比思考型模型关注更少的指令。
2. **较小的模型变得更糟的速度要快得多**。具体来说，随着指令数量的增加，较小的模型往往表现出指令遵循性能的指数衰减，而较大的前沿思考型模型表现出线性衰减（见下文）。因此，我们建议不要将较小的模型用于多步骤任务或复杂的实现计划。
3. **LLM 偏向于提示词边缘的指令**：在非常开头（Claude Code 系统消息和 `CLAUDE.md`），以及在非常结尾（最近的用户消息）
4. **随着指令数量增加，指令遵循质量均匀下降**。这意味着当你给 LLM 更多指令时，它不仅仅是忽略较新的（"文件中更靠下"）指令——它开始**均匀地忽略所有指令**

![Instruction following](/blog/writing-a-good-claude-md/instructionfollowing.png)

我们对 Claude Code Harness 的分析表明，**Claude Code 的系统提示词包含约 50 条单独的指令**。根据你使用的模型，这可能是你的 Agent 可以可靠遵循的指令的近三分之一——而且这还是在规则、插件、技能或用户消息之前。

这意味着你的 `CLAUDE.md` 文件应该包含尽可能少的指令——理想情况下只有对你的任务普遍适用的指令。

### `CLAUDE.md` 文件长度与适用性

在其他条件相同的情况下，**当 LLM 的上下文窗口充满专注、相关的上下文**（包括示例、相关文件、工具调用和工具结果）时，相比上下文窗口有大量不相关上下文时，LLM 在任务上表现更好。

由于 `CLAUDE.md` 进入*每一次会话*，你应该确保其内容尽可能普遍适用。

例如，避免包含关于（例如）如何构建新数据库模式的指令——当你在做其他不相关的事情时，这无关紧要，会分散模型的注意力！

长度方面，*少即是多*原则同样适用。虽然 Anthropic 没有关于 `CLAUDE.md` 文件应该多长的官方建议，但普遍共识是 < 300 行最好，越短越好。

在 HumanLayer，我们的根 `CLAUDE.md` 文件*不到六十行*。

### 渐进式披露

编写一个涵盖你希望 Claude 知道的一切的简洁 `CLAUDE.md` 文件可能具有挑战性，尤其是在较大的项目中。

为了解决这个问题，我们可以利用**渐进式披露**的原则，确保 Claude 只在需要时看到特定于任务或项目的指令。

我们建议将任务特定的指令保存在项目中某个地方具有自描述名称的*单独 Markdown 文件*中，而不是在 `CLAUDE.md` 文件中包含关于构建项目、运行测试、代码约定或其他重要上下文的所有不同指令。

例如：

```
agent_docs/
  |- building_the_project.md
  |- running_tests.md
  |- code_conventions.md
  |- service_architecture.md
  |- database_schema.md
  |- service_communication_patterns.md
```

然后，在你的 `CLAUDE.md` 文件中，你可以包含这些文件的列表，并简要描述每个文件，并指示 Claude 决定哪些（如果有）是相关的，并在开始工作之前阅读它们。或者，让 Claude 向你展示它想要阅读的文件以供批准，然后再阅读它们。

**优先选择指针而非副本**。如果可能的话，不要在这些文件中包含代码片段——它们会很快过时。相反，包含 `file:line` 引用以指向权威上下文。

从概念上讲，这与 [Claude Skills](https://code.claude.com/docs/en/skills) 的预期工作方式非常相似，尽管技能更侧重于工具使用而非指令。

### Claude 不是（昂贵的）linter

我们在 `CLAUDE.md` 文件中看到人们放置的最常见的东西之一是代码风格指南。**永远不要派 LLM 去做 linter 的工作**。与传统 linter 和格式化程序相比，LLM 相对昂贵且*极其*缓慢。我们认为你应该*尽可能始终使用确定性工具*。

代码风格指南将不可避免地添加一堆指令和大部分不相关的代码片段到你的上下文窗口中，降低 LLM 的性能和指令遵循能力，并消耗你的上下文窗口。

**LLM 是上下文学习者**！如果你的代码遵循一定的风格指南或模式，你会发现，只要进行几次代码库搜索（或一份好的研究文档！），你的 Agent 应该倾向于遵循现有的代码模式和约定，而无需被告知。

如果你对此非常强烈，你甚至可以考虑设置一个 [Claude Code `Stop` hook](https://code.claude.com/docs/en/hooks#stop)，运行你的格式化程序和 linter 并将错误呈现给 Claude 以修复。不要让 Claude 自己发现格式化问题。

**加分项**：使用可以自动修复问题的 linter（我们喜欢 Biome），并仔细调整关于什么可以安全自动修复的规则，以获得最大（安全）覆盖率。

你也可以创建一个 [Slash Command](https://code.claude.com/docs/en/slash-commands)，包含你的代码指南并指向版本控制中的更改，或指向你的 `git status` 或类似内容。这样，你可以分别处理实现和格式化。**结果你会看到两者都有更好的结果**。

### 不要使用 `/init` 或自动生成你的 `CLAUDE.md`

Claude Code 和其他带有 OpenCode 的 Harness 都有自动生成 `CLAUDE.md` 文件（或 `AGENTS.md`）的方法。

因为 `CLAUDE.md` 进入与 Claude code 的*每一次会话*，它是 Harness 的**最高杠杆点之一**——根据你如何使用它，可能更好也可能更糟。

一行坏代码就是一行坏代码。实现计划中的一行坏代码有可能产生**很多**坏代码。研究中的一行坏代码误解了系统如何工作，有可能导致计划中有很多坏代码，因此结果会有**更多**坏代码。

但 `CLAUDE.md` 文件影响**工作流程的每一个阶段**以及它产生的每一个工件。因此，我们认为你应该花一些时间非常仔细地思考其中的每一行：

![Leverage](/blog/writing-a-good-claude-md/leverage.png)

---

## 总结

1. `CLAUDE.md` 用于让 Claude 加入你的代码库。它应该定义项目的 **WHY**、**WHAT** 和 **HOW**。
2. **少（指令）即是多**。虽然你不应该省略必要的指令，但你应该在文件中包含尽可能少的指令。
3. 保持 `CLAUDE.md` 的内容**简洁且普遍适用**。
4. 使用**渐进式披露**——不要把你可能希望 Claude 知道的所有信息都告诉它。相反，告诉它*如何找到*重要信息，这样它就可以找到并使用它，但只在需要时，以避免膨胀你的上下文窗口或指令数量。
5. Claude 不是 linter。使用 linter 和代码格式化程序，并根据需要使用 [Hooks](https://code.claude.com/docs/en/hooks) 和 [Slash Commands](https://code.claude.com/docs/en/slash-commands) 等其他功能。
6. **`CLAUDE.md` 是 Harness 的最高杠杆点**，因此避免自动生成它。你应该仔细精心设计其内容以获得最佳结果。

---

*翻译完成时间: 2026-03-31*  
*原文: https://www.humanlayer.dev/blog/writing-a-good-claude-md*
