# Claude Code SDK 与 HaaS（Harness as a Service）的诞生（中文翻译）

**来源**: https://www.vtrivedy.com/posts/claude-code-sdk-haas-harness-as-a-service  
**作者**: Vivek Trivedy  
**日期**: September 23, 2025

---

随着任务需要 Agent 表现出更多的自主行为，与 AI 协作的核心原语正在从 **LLM API（聊天式端点）** 转变为 **Harness API（可定制运行时）**。我称之为 **Harness as a Service（HaaS，Harness 即服务）**。通过丰富的 Agent Harness 生态系统，快速构建、定制和共享 Agent。今天我们将介绍如何定制 Harness 以快速构建可用的 Agent，以及在开放 Harness 世界中 Agent 开发的未来。

```
client.chat.completions.create() --> client.responses.create() --> agent.query()
```

> **工作定义 — Agent Harness：** 增强模型运行时执行的外部功能集。示例包括：(1) 对话和上下文管理，(2) 工具调用层（MCP/SDK 工具），(3) 权限控制，(4) 会话和文件系统状态，(5) 循环控制和错误处理，(6) 基础可观测性/遥测。

> **注意**：像 ChatGPT Web 应用或 iOS 应用这样的 LLM 产品已经将模型包装在它们自己的 Harness 中，用于安全、工具使用等。但今天使用 LLM API 需要你将模型包装在你自己的 Harness 中。随着 Claude Code 的 SDK 推出，这种情况正在改变——他们现有的 Harness 可以很容易地用你自己的提示词、工具、上下文、权限进行扩展。用户获得了一个开箱即用的可定制 Agent 运行时。

以下是我们将介绍的 3 个主要观点：

1. 为什么 Claude Code 的 SDK 目前是最好的、开箱即用的构建和暴露可用 Agent 的方式。
2. 作为构建者，你的工作是精心定制 Harness 以适应你的任务。（附示例）
3. 通过 Harness 进行 Agent 开发的未来，以及开放 Harness 生态系统的承诺。

在未来的博客文章中，我们将深入探讨一个自定义项目，实现 Claude Code SDK 的详细信息和高级功能，超越下面分享的示例。让我们开始吧。

---

## 开箱即用 = 速度 = 你的 Agent 真正存在

Agent 构建领域很嘈杂：agents、框架、工具、MCP、Codex、Claude Code、Cursor CLI，你懂的。但退一步看。除非你是 Agent 框架公司，否则目标是解决你的实际问题，而不是构建 Agent 基础设施。对于考虑使用 Agent 解决问题的团队来说，一个（事后看来）显而易见但经常被忽视的事实是：

> 好的 Agent 构建是一种迭代练习。如果没有 v0.1，你就无法进行迭代。开箱即用的设置让你的 Agent 进入内部团队的手中。然后你可以在循环中编辑。

### 为什么关心这个？Agent 构建是一种动量练习

在 Agent 构建中，工具/能力可能在一夜之间改变，这很棒，因为你可以测试之前无法工作的杀手级功能。但要在这里取得成功，你需要能够快速进行内部（和外部）测试。Claude Code SDK 通过作为你的 Agent 快速启动工具来减少你的 TTFF（首次反馈时间），就像 `create-react-app --> create-agent-app` 一样。

框架释放你的心理容量，让你专注于问题的复杂性。为了快速行动，不要从头开始构建所有东西，将一些工作卸载给现有的工具，这些工具可以让你快速启动，同时具有强大的定制能力以应对未来。这正是你在 [Claude Code SDK](https://docs.claude.com/en/docs/claude-code/sdk/sdk-overview) 中获得的那种卸载。我不会列出每个功能，他们的文档很扎实，这里是一个概述片段。

> 基于驱动 Claude Code 的 Agent Harness 构建，Claude Code SDK 提供了构建生产就绪 Agent 所需的所有构建块。利用我们在 Claude Code 上完成的工作，包括：
> - **上下文管理**：自动压缩和上下文管理，确保你的 Agent 不会耗尽上下文。
> - **丰富的工具生态系统**：文件操作、代码执行、网络搜索和 MCP 可扩展性
> - **高级权限**：对 Agent 能力的细粒度控制
> - **生产必备**：内置错误处理、会话管理和监控
> - **优化的 Claude 集成**：自动提示词缓存和性能优化

正如你从他们的文档中看到的，Claude Code SDK 为你提供了一套非常可用的基础 Agent 原语，这就是你的 "Harness"。这些内置功能节省了你数天到数周的工作，但更重要的是，你的团队现在可以专注于你的问题。

那么你的工作是什么？**精心定制。**

---

## Harness 定制，构建任何 Agent 的方法

![Customize Claude Code's Harness to build and expose any agent](/_astro/cc_harness_updated.DHcnsFFo_Z1WNa8e.webp)
*定制 Harness 并使用 Claude Code SDK 使其可用的思维模型*

每个任务都需要一定的工具集和指令集，你的工作是定制这些输入：**系统提示词、工具/MCP、上下文、子 Agent。** 一旦你有了一些东西，运行它并观察你的 Agent 在做什么，这是你的学习信号。改进你的输入，直到你获得足够好的输出。以下是定制 Harness 每个部分的详细信息和技巧。

### 1. 系统提示词（System Prompt）

这是告诉 Claude Code 关于你的问题的一切的起点：目标、它将操作的环境、它可以使用的工具、要遵循的指令和指南、格式化规则、如何与用户交互等。

在这里花很多时间！提示词工程对于引导模型行为一如既往地重要。在你的系统提示词上投入时间是你在 Agent 构建旅程中能获得的最佳成本效益。

这里有一个你可以使用的模板来开始，但提示词设计是一门艺术。你可以看到一个更长的示例，它在这里效果很好 [here](https://github.com/vtrivedy/claude-banana-story-agent)，这是我用 Claude Code 的 SDK 发布的一个项目，用于从用户主题自主创建故事书（类似于 [Gemini's Storybook Feature](https://gemini.google/overview/storybook/)）。

```
Goal/Persona: "You are "Story Director," an autonomous storybook creation agent that transforms ANY user input into complete illustrated storybooks..."
Environment/Tools Available: ...
Must Follow Instructions: ...
Examples + Tool Usage: ...
Final Checklist: ...
```

Claude Code 提供了两种编辑系统提示词的方式：`appendSystemPrompt` 和 `custom_system_prompt`，用于添加到 Claude 现有的系统提示词或用你自己的完全重写。

### 2. 工具/MCP

Claude Code 带有内置工具（网络搜索、grep、文件读/写等），但你需要为特定于你的用例的工具定义自定义逻辑（例如：图像编辑 API、Slack 集成等）。你不必从头开始构建所有这些，使用在 [Smithery](https://smithery.ai/) 等平台上打包为 MCP 的现有工具集。

对于工具设计，深入思考 3 件事：

1. Agent 需要做什么才能完成我设定的目标。有工具吗？
2. 在我的系统提示词和工具描述中，Agent 何时使用工具是否清楚？
3. 我能否通过将几个工具组合成更原子的结果来减少错误表面积？例如：`generate_image` —> `generate_page_content`

Anthropic 关于 [Writing Effective Tools for Agents](https://www.anthropic.com/engineering/writing-tools-for-agents) 的博客和 Vercel 关于 [MCP for LLMs not devs](https://vercel.com/blog/the-second-wave-of-mcp-building-for-llms-not-developers) 的博客是两篇关于工具/MCP 设计的优秀资源。

### 3. 上下文（Context）

有很多关于 [Context Engineering](https://www.philschmid.de/context-engineering) 的新内容。你给 Agent 的上下文越好，它的表现就越好。一些有用的上下文示例包括：

- **代码文档和代码片段：** 将这些保存为文件系统中的 Markdown 文件。不要让 Agent 搜索网络来获取你已经知道它需要的东西。它可以根据需要引用这些片段。
- **记忆/用户个性化：** 你的 Agent 是否应该根据你的用户表现出不同的行为？最简单的方法是将这些信息注入到 'user_info.md' 文件或更复杂的记忆服务中。

经验法则：将所有关键上下文放在你的系统提示词中，特别是对于第一个版本。将所有其他有用的上下文放在 Markdown 文件中，并告诉你的 Agent 何时以及如何使用它们的内容。

### 4. 子 Agent（可选）

对于 Agent 的第一个版本，我强烈建议在单个 Agent 线程中测试所有内容，以减少复杂性并快速让你的 Agent 进入世界。子 Agent 最初可用于 2 个用例：**专业化（Specialization）** 和 **并行化（Parallelization）**。

子 Agent 通过 YAML 在 `.claude/agents/{subagent_name}.md` 中定义。例如：

```yaml
---
name: character-consistency-checker
description: Expert visual inspector.  Can tell if the character in the generated image matches the character reference image.
tools: Read, Grep, Glob, Bash
---
Your task is to make sure the character in the story matches the reference character.  You will read in 2 images, the character.png and the page.png file.  Then you will output True or False along with a reason for your decision

Make sure to check for the consistency of the size, color, art style, and other factors that would break the flow and overall vibe of the story
```

---

## HaaS，构建自定义 Agent 的未来

我们正迅速走向一个构建者创建自定义 Harness、用户插入其中以进一步编辑或作为产品使用的世界。我们已经开始从 [bolt](https://x.com/boltdotnew/status/1965448120558559683) 等公司看到这种运动，他们帮助开启了氛围编码革命。他们直接在应用构建产品中使用 Codex 和 Claude Code，并可能进行了大量的 Harness 定制以使产品正常工作。对于公司来说，开始使用现有的 Harness 作为应用原语来构建他们的产品体验，这是一个巨大的机会。我敢打赌，在未来 6 个月内，大多数面向用户的 AI 产品将使用现有的 Agent Harness 作为其核心用户交互模式。

对于深深痴迷于问题的构建者来说，所有这些都是好事。他们可以利用持续改进的可定制智能层，同时将时间集中在用户反馈、创建更好的 Agent 输入以及工程更复杂和可靠的体验上。

Claude Code SDK 不会是唯一的游戏，它只是今天构建的最成熟的。OpenAI Codex、Gemini CLI、Cursor CLI、Amp 等已经做了很多伟大的工作。但目标很明确，每个人都希望成为用户插入以获取智能的 Harness。这样做的机会将围绕出色的 DX 和开箱即用的智能。

### 开放 Harness 论题

如果你对这个帖子和像 [Prime Intellects Environment Hub](https://www.primeintellect.ai/blog/environments) 这样的发布感到兴奋，你可能共享一个未来愿景：许多 Harness 是开源的，以便开发者可以扩展它们。原始模型和它们的 Harness 可能不是开源的，但构建产品体验的一切可能是。那个未来更令人兴奋，因为驱动前沿 Harness 的基础模型有一天也可能开源。**这是 Agent 的开放应用商店。**

Harness 使 "Agent 基础设施" 商品化，并将你的努力转移到复利的地方：针对你领域的提示词、工具和上下文。无论你称之为 HaaS 还是只是 "构建 Agent"，Claude Code SDK 都是今天最容易构建的 Harness。从该基线开始，积极专业化，并从其测量输出中改进你的 Agent。

如果这个未来让你兴奋，请联系我，我们正在这里构建。下次见，祝你 Harness 构建愉快。

---

*翻译完成时间: 2026-03-31*  
*原文: https://www.vtrivedy.com/posts/claude-code-sdk-haas-harness-as-a-service*
