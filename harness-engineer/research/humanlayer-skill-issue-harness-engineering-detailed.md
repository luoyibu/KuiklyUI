# Skill Issue: Harness Engineering for Coding Agents | HumanLayer Blog 精读笔记

**来源**: https://www.humanlayer.dev/blog/skill-issue-harness-engineering-for-coding-agents  
**作者**: Kyle (HumanLayer)  
**日期**: March 12, 2026  
**精读重点**: Harness Engineering 实践指南 + 配置点详解

---

## 文章引言

> We've spent the past year watching coding agents fail in every conceivable way: ignoring instructions, executing dangerous commands un-prompted, and going in circles on the simplest of tasks.
> 
> （过去一年里，我们见证了编码 Agent 以所有可以想象的方式失败：忽略指令、未经提示执行危险命令、在最简单的任务上原地打转）

作者坦诚分享：
> We've seen teams ship immense amounts of slop. We've even shipped a little bit of slop ourselves.
> （我们看到团队提交了大量垃圾代码。我们自己也提交了一点垃圾代码）

每次失败时的本能反应：
- "We just need better models, GPT-6 will fix it"（我们只需要更好的模型，GPT-6 会解决它）
- "We just need better instruction-following"（我们只需要更好的指令遵循）
- "It'll work once [niche library I'm using] is in the training data"（一旦[我使用的小众库]进入训练数据，它就会起作用）

但作者的核心结论：
> it's not a model problem. It's a **configuration problem**.
> （这不是模型问题。这是**配置问题**）

**核心洞见**：
> Yes, models will get smarter, and some existing failure modes will disappear. And then because they are smarter, we will give them new problems which are bigger and harder, and they will **continue to fail in unexpected ways**. Unexpected failures modes are a fundamental problem for non-deterministic systems.
> 
> （是的，模型会变得更智能，一些现有的失败模式会消失。然后因为它们更智能，我们会给它们更大更难的新问题，它们将**继续以意想不到的方式失败**。意外的失败模式是非确定性系统的根本问题）

**【KuiklyUI 借鉴思考】**

这与 Mitchell Hashimoto 的观点一致：不要等待更好的模型，而是要从今天的模型中获得最大收益。

对于 KuiklyUI：
- 不要抱怨 AI 不懂 KMP 跨平台开发
- 而是通过 Harness Engineering 教会它
- 通过配置和工具弥补模型的不足

---

## Harness Engineering 定义

### 什么是 Harness？

> `coding agent = AI model(s) + harness`
> 
> These are all technically separate concepts, but they are all part of the coding agent's configuration surface. We call this the **coding agent's harness**, and we think of it as the agent's runtime, or as its peripherals: what does the model use to interact with its environment?
> 
> （这些在技术上是独立的概念，但它们都是编码 Agent 配置面的一部分。我们称之为**编码 Agent 的 harness**，我们将其视为 Agent 的运行时，或其外设：模型用什么与其环境交互？）

### Harness Engineering 定义

> **Harness engineering**, coined by Viv, describes the practice of leveraging these configuration points to customize and improve your coding agent's output quality and reliability.
> 
> （Harness engineering 由 Viv 提出，描述利用这些配置点来定制和改进编码 Agent 输出质量和可靠性的实践）

引用 Mitchell Hashimoto：
> [...] is the idea that anytime you find an agent makes a mistake, you take the time to engineer a solution such that the agent never makes that mistake again.
> （每当发现 Agent 犯错，就花时间设计解决方案，确保 Agent 永远不会再犯同样的错误）

### Harness Engineering vs Context Engineering

> We view harness engineering as a subset of context engineering.
> 
> （我们将 harness engineering 视为 context engineering 的子集）

Context Engineering（Dex 在 12-factor agents 中提出）是"prompt engineering"的超集，包含各种系统性地提高 AI Agent 可靠性的技术。

Harness Engineering 则是专注于利用 harness 配置点来仔细管理编码 Agent 上下文窗口的 context engineering 子集。

它回答的问题：
- 如何给编码 Agent 新能力？
- 如何教它代码库中训练数据没有的东西？
- 如何在系统消息之外添加确定性？
- 如何为特定代码库调整 Agent 行为？
- 如何提高任务成功率，超越"神奇提示词"？
- 如何防止上下文窗口过快膨胀或充满坏上下文？

**【KuiklyUI 借鉴思考】**

Harness 组件图（来自 Viv 的文章）：
- System Prompt
- Tools/MCPs
- Context
- Sub-agents

HumanLayer 补充两个：
1. **Hooks** - 用于自动化集成和确定性控制流
2. **Skills** - 用于渐进式知识披露（Dex 称之为"Instruction Modules"）

对于 KuiklyUI，这意味着我们需要配置：
- CLAUDE.md/AGENTS.md（System Prompt）
- MCP 服务器（工具）
- Skills（渐进式知识）
- Sub-agents（上下文控制）
- Hooks（自动化）

---

## CLAUDE.md & AGENTS.md

### 基础配置

> Before touching any other harness configuration points, it's usually worth customizing your CLAUDE.md / AGENTS.md files. These are markdown files at the top-level of your repository that get deterministically injected into the agent's system prompt by the harness.
> 
> （在接触任何其他 harness 配置点之前，通常值得先定制你的 CLAUDE.md / AGENTS.md 文件。这些是仓库顶层的 markdown 文件，由 harness 确定性地注入到 Agent 的系统提示词中）

### ETH Zurich 研究的启示

ETH Zurich 的研究测试了 138 个 agentfiles 在各种仓库中的表现，发现：
- LLM 生成的文件实际上*损害*了性能，同时成本增加 20%+
- 人工编写的文件只帮助了约 4%
- Agent 处理上下文文件指令多花了 14-22% 的推理 token，完成任务需要更多步骤，运行更多工具——都没有提高解决率
- 代码库概览和目录列表完全没有帮助；Agent 自己发现仓库结构就很好

但仔细解读研究会发现，HumanLayer 之前的建议是正确的：

| 研究发现 | HumanLayer 的建议 |
|---------|------------------|
| Agent 生成的文件更糟 | "avoid auto-generating it. You should carefully craft its contents for best results"（避免自动生成。你应该仔细 crafting 内容以获得最佳结果） |
| 太多文件过度引导模型使用特定工具，导致更糟结果 | "Less (instructions) is more"（少即是多） |
| 文件包含不相关上下文 | "Use Progressive Disclosure"（使用渐进式披露） |
| 人工编写的文件帮助不大，因为条件规则太多 | "Keep the contents of your CLAUDE.md concise and universally applicable"（保持 CLAUDE.md 内容简洁且普遍适用） |

HumanLayer 的 CLAUDE.md 不到 60 行。

**【KuiklyUI 借鉴思考】**

关键原则：
1. **不要自动生成** - 人工仔细编写
2. **少即是多** - 指令越少越好
3. **渐进式披露** - 不要一次性给所有信息
4. **简洁通用** - 避免太多条件规则

对于 KuiklyUI 的 CLAUDE.md：
- 保持简洁（< 60 行）
- 聚焦普遍适用的规则
- 避免过度指定工具使用
- 使用渐进式披露（通过 skills 或子目录文档）

---

## MCP Servers Are for Tools（MCP 服务器用于工具）

### MCP 服务器的用途

> MCP servers are primarily for plugging tools into your coding agent to extend its capabilities beyond file I/O and bash commands.
> 
> （MCP 服务器主要用于将工具插入编码 Agent，以扩展其超越文件 I/O 和 bash 命令的能力）

警告：
> because MCP servers' tool descriptions are added to your coding agent's system prompt, never connect to one you don't trust. This can be a dangerous vector for prompt injection!
> 
> （因为 MCP 服务器的工具描述被添加到编码 Agent 的系统提示词中，永远不要连接你不信任的服务器。这可能是提示词注入的危险途径！）

### Too Many Tools Is Bad（工具太多是坏事）

> plug too many MCP tools into your agent, and the context window fills up with tool descriptions, pushing you into the dumb zone much faster
> 
> （将太多 MCP 工具插入 Agent，上下文窗口会被工具描述填满，更快地将你推入"愚蠢区"）

The instruction budget（指令预算）很重要——每个不相关的工具描述都是 Agent 必须处理但没有收益的指令。

Anthropic 甚至发布了 [MCP tool search 的实验性支持](https://platform.claude.com/docs/en/agents-and-tools/tool-use/tool-search-tool)，以在连接太多 MCP 工具时渐进式披露工具。

### CLI vs MCP

> if an MCP server duplicates functionality that's already available as a CLI well-represented in training data, it works better to just prompt the agent to use the CLI.
> 
> （如果 MCP 服务器复制了训练数据中已有良好表示的 CLI 功能，直接提示 Agent 使用 CLI 效果更好）

对于 GitHub、Docker 或大多数数据库，编码 Agent 可以直接使用正确的 CLI 和 shell 命令。模型在训练中已经见过这些工具足够多，已经知道如何使用它们。

**HumanLayer 的实践**：

他们曾使用 Linear MCP 服务器，后来意识到只使用了它提供的一小部分工具。于是他们写了一个小 CLI 包装 Linear API，并在 CLAUDE.md 中包含 6 个示例用法：

```markdown
## Linear
Use the Linear CLI for:
- **fetching issues**: `linear get-issue ENG-XXXX`
- **listing issues**: `linear list-issues` or `linear my-issues`
- **adding comments**: `linear add-comment -i ENG-XXXX "comment"`
- **adding links**: `linear add-link ENG-XXXX "url" -t "link title"`
- **updating status**: `linear update-status ENG-XXXX "status name"`
- **get branch name**: `linear get-issue-v2 ENG-XXXX --fields branch`
- **get images from the ticket**: `linear fetch-images ENG-XXXX`
```

这节省了数千个 token（来自 MCP 服务器工具定义），以及更多来自冗长 MCP 服务器响应的 token。

**【KuiklyUI 借鉴思考】**

对于 KuiklyUI：
- 优先使用 CLI 而非 MCP 服务器（gh、gradle、kubectl 等）
- 如果必须使用 MCP，限制工具数量
- 考虑编写精简的 CLI 包装器替代完整的 MCP 服务器
- 在 CLAUDE.md 中提供具体的使用示例

---

## Skills Are for Reusable Knowledge (and Tools)

### Skills 简介

Skills 最初由 Anthropic 为 Claude Code 引入，但已成为 Codex 和 OpenCode 等其他 harness 支持的开放标准。

**安全警告**：
> skill registries have already been caught distributing hundreds of malicious skills. Treat skills like you'd treat `npm install random-package` — read what you're installing.
> 
> （skill 注册表已被发现分发数百个恶意 skills。像对待 `npm install random-package` 一样对待 skills——阅读你正在安装的内容）

### Progressive Disclosure（渐进式披露）

> we kept stuffing every instruction and tool into the system prompt, and the agent kept getting worse. We were blowing through our instruction budget before the agent even started working. Skills solve this through **progressive disclosure** — the agent only gets access to specific instructions, knowledge, or tools when it decides (or you decide for it) that it needs them.
> 
> （我们一直把每个指令和工具塞进系统提示词，Agent 却越来越糟。在 Agent 开始工作之前，我们就已经耗尽了指令预算。Skills 通过**渐进式披露**解决这个问题——只有当 Agent 决定（或你为它决定）需要时，它才能访问特定的指令、知识或工具）

### Skill Activation（Skill 激活）

当 skill 被激活时：
- `SKILL.md` 文件被加载到 Agent 的上下文窗口作为用户消息
- Agent 被告知 skill 文件加载的目录
- `SKILL.md` 可以告知 Agent 捆绑的其他内容

示例结构：
```
example-skill/
|--- SKILL.md
|--- response_template.md
|--- CLIs/
    |--- linear-cli
    |--- tunnel-cli
```

由于每个 skill 有自己的目录，你可以更有创意地进行渐进式披露：
- 在 skill 中捆绑多个 markdown 文件
- 每个文件包含不同功能或不同用途的不同信息
- 主 `SKILL.md` 文件告诉 Agent 其他文件是什么，以及何时应该阅读它们

### Distributing Tools with Skills

> it's not possible to bundle MCP servers or custom agent tools directly into a skill - you have to write them into an executable, a CLI, an NPM package, or something else which you can either distribute with your skill or instruct the agent to install in the skill file.
> 
> （不能直接将 MCP 服务器或自定义 Agent 工具捆绑到 skill 中——你必须将它们写入可执行文件、CLI、NPM 包，或其他你可以随 skill 分发或指示 Agent 在 skill 文件中安装的东西）

例如，可以不配置 Playwright MCP 服务器，而是给 Agent 提供一个使用 BrowserBase 的 [agent browser skills](https://github.com/browserbase/skills) 或 Vercel 的 [agent browser CLI](https://github.com/vercel-labs/agent-browser) 进行网页浏览的 skill。

**【KuiklyUI 借鉴思考】**

对于 KuiklyUI，可以设计以下 skills：

1. **kmp-platform-skill**
   - 包含 KMP 跨平台开发知识
   - expect/actual 模式
   - 平台特定代码组织

2. **kuikly-render-skill**
   - Render 层开发指南
   - 组件实现模式
   - 性能优化建议

3. **kuikly-testing-skill**
   - 跨平台测试策略
   - 测试工具使用
   - 示例测试代码

每个 skill 结构：
```
kuikly-render-skill/
|--- SKILL.md              # 主入口，描述 skill 内容和激活条件
|--- basics.md             # 基础概念
|--- patterns.md           # 常见模式
|--- examples/             # 示例代码
    |--- Button.kt
    |--- Text.kt
```

---

## Sub-Agents Are for Context Control（子 Agent 用于上下文控制）

### 核心用途

> Sub-agents are a popular but often misunderstood harness configuration point. We tried the "frontend engineer" sub-agent and "backend engineer" sub-agent and "data analyst" sub-agent thing. It doesn't work. What does work is using sub-agents for context control.
> 
> （子 Agent 是一个流行但经常被误解的 harness 配置点。我们尝试过"前端工程师"子 Agent、"后端工程师"子 Agent、"数据分析师"子 Agent。这不起作用。真正起作用的是使用子 Agent 进行上下文控制）

子 Agent 提供了一种封装整个编码 Agent 会话工作的方式：
- 调度 Agent 只看到它写给子 Agent 的提示词
- 调度 Agent 只看到子 Agent 的最终结果
- 中间工具调用、工具结果或其他消息都不会进入父编码 Agent 的上下文窗口

### Context Firewall（上下文防火墙）

> sub-agents are a particularly powerful lever. When working on hard problems that require many, many context windows to solve, **sub-agents are the key to maintaining coherency across many sessions**. Sub-agents **function as a "context firewall"** that ensures discrete tasks can run in isolated context windows so none of the intermediate noise accumulates in your parent thread which is responsible for orchestration, and you can maintain coherency for much, much longer.
> 
> （子 Agent 是一个特别强大的杠杆。在处理需要很多很多上下文窗口才能解决的难题时，**子 Agent 是在多个会话中保持连贯性的关键**。子 Agent**充当"上下文防火墙"**，确保离散任务可以在隔离的上下文窗口中运行，这样中间噪音都不会积累在你的父线程中（父线程负责编排），你可以保持连贯性更久更久）

### Context Rot（上下文腐烂）

Chroma 的 [context rot research](https://research.trychroma.com/context-rot) 提供了实证支持：
- 模型在更长的上下文长度上表现更差
- 当问题与上下文中的相关信息语义相似度低时，退化更*陡峭*
- 每个中间工具调用、每个 grep 结果、父会话中每个不相关的文件读取都是潜在的分心物
- 分心效应在更长的上下文窗口中*复合*

### Long-Context Models 的局限性

> When a lab offers an extended-context version of a given model, you are usually not getting a bigger model with a larger "instruction budget" - you're getting the same model with some clever math (e.g. YaRN) to extend the length of the sequence the model can attend to.
> 
> （当实验室提供给定模型的扩展上下文版本时，你通常没有获得具有更大"指令预算"的更大模型——你获得的是相同的模型，只是用一些巧妙的数学（如 YaRN）来扩展模型可以关注的序列长度）

比喻：
> A bigger context window doesn't make the model better at finding the needle — it just makes the haystack bigger.
> 
> （更大的上下文窗口不会让模型更擅长找到针——它只是让干草堆更大）

解决方案：
> Sub-agents solve this structurally: each one gets a fresh, small, high-relevance context window with a fresh "instruction budget" for its task, and only the condensed result flows back to the parent - allowing you to stitch together many context windows for a single problem.
> 
> （子 Agent 从结构上解决这个问题：每个子 Agent 获得一个新鲜的、小的、高相关性的上下文窗口，为其任务提供新鲜的"指令预算"，只有压缩后的结果流回父 Agent——允许你为单个问题拼接许多上下文窗口）

### Sub-Agent Use-Cases（子 Agent 用例）

适合使用子 Agent 的任务：
- 在代码库中定位特定定义或实现
- 分析代码库以识别特定工作类型的模式
- 追踪信息在代码库中的流动，例如跨服务边界追踪请求
- 其他一般代码/文档/网页研究任务

这些任务类型通常有简单的问题和简单的答案，但需要大量中间工具调用，你不希望或不需要在父会话中。

子 Agent 应该返回高度压缩的响应，同时遵循渐进式披露原则。例如，子 Agent 提供问题的答案，但也以 `filepath:line` 格式或 URL 引用来源，这样父 Agent 不会暴露于子 Agent 使用的所有来源，但如果需要更多细节或确认，它有信息去找到相关上下文。

### Sub-Agents Are (Also) for Cost Control（子 Agent 也用于成本控制）

> We use an expensive model (Opus) for the parent session where thinking-heavy tasks like planning and orchestration happen, and a cheaper, faster model like Sonnet or Haiku for each sub-agent.
> 
> （我们在父会话中使用昂贵的模型（Opus）进行思考和编排等重任务，为每个子 Agent 使用更便宜、更快的模型如 Sonnet 或 Haiku）

子 Agent 接收更小、更离散的任务，可以由智能较低、"指令预算"较小的模型处理——不需要在代码库 grep 上浪费 Opus token。

### MCP Server 实现 Sub-Agent 模式

有些 harness 根本不支持子 Agent！Codex 也是最近才支持，而且仍是[实验性](https://developers.openai.com/codex/multi-agent/)的。

可以通过编写 MCP 服务器来实现：
- 提供启动新 Agent 会话的工具
- 接收父 Agent 的提示词
- 以该提示词作为用户消息启动新编码 Agent 会话
- 将子 Agent 的最终响应消息返回给父 Agent

**警告**：
> using this pattern with a coding agent that supports sub-agents will allow the harness's native sub-agents to dispatch sub-agents via MCP. This can result in an unpredictable game of telephone
> 
> （在支持子 Agent 的编码 Agent 上使用这种模式，会让 harness 的原生子 Agent 通过 MCP 调度子 Agent。这可能导致不可预测的电话游戏）

编写子 Agent 系统提示词时要非常小心：
- Agent 的角色是什么——应该做什么，不应该做什么
- Agent 应该返回什么信息，如何返回
- 子 Agent 应该有什么工具？

**【KuiklyUI 借鉴思考】**

对于 KuiklyUI 的复杂任务，可以设计以下子 Agent：

1. **Research Agent**
   - 使用 Sonnet 或 Haiku
   - 任务：理解代码库结构、找到相关文件
   - 返回：文件列表 + 关键代码片段 + 架构说明

2. **Implementation Agent**
   - 使用 Opus 进行复杂实现
   - 使用 Sonnet 进行标准实现
   - 任务：按规划实现功能
   - 返回：修改的文件列表 + 关键变更说明

3. **Verification Agent**
   - 使用 Haiku（快速）
   - 任务：运行测试、检查代码风格
   - 返回：测试结果摘要

工作流程：
```
Parent Agent (Opus)
├── Research Agent (Sonnet) → 返回代码库理解
├── Plan (Parent Agent) → 创建实现计划
├── Implementation Agent (Opus/Sonnet) → 执行实现
└── Verification Agent (Haiku) → 验证结果
```

---

## Hooks Are for Control Flow（Hooks 用于控制流）

### Hooks 简介

Claude Code 有 [hooks](https://code.claude.com/docs/en/hooks) 概念：
- 用户定义的命令或脚本
- 在特定事件发生时自动执行
- 在 Agent 生命周期的各个点执行

Opencode 有类似的 [plugins](https://opencode.ai/docs/plugins/) 概念。

（Codex [没有等效功能](https://github.com/openai/codex/discussions/2150)）

Hooks 类似于 [git hooks](https://git-scm.com/book/en/v2/Customizing-Git-Git-Hooks)，但更灵活。可用于：
- 添加新功能
- 与外部服务集成
- 自动化例行操作
- 修改权限
- 配置默认行为

### Hook 能力

- 在事件发生时自动但静默地运行某些东西
- 在工具被调用时运行，并向 Agent 返回额外上下文（除了工具结果）
- 在编码 Agent 完成之前向其显示构建/类型错误，强制它在完成前解决错误

### Common Use Cases（常见用例）

1. **Notifications（通知）**
   - Agent 完成或需要关注时播放声音

2. **Approvals（审批）**
   - 基于输入值和更具表达性的规则自动批准或拒绝工具调用
   - 示例：自动拒绝任何尝试运行迁移的 `Bash()` 工具调用，并指示要求用户运行

3. **Integrations（集成）**
   - Agent 完成时发送 Slack 消息
   - 创建 GitHub PR
   - 设置预览环境

4. **Verification（验证）**
   - 如果框架和仓库能在几秒钟内运行类型检查或构建，每次 Agent 停止时运行它——将错误显示给它

### Example Hook（示例 Hook）

当 Claude 停止时，运行 biome formatter 和 TypeScript 类型检查。如果有错误，向 Claude 报告。如果没有，脚本静默退出。

```bash
#!/bin/bash
cd "$CLAUDE_PROJECT_DIR"

# prebuild generates types and builds internal SDK packages so typecheck has
# everything it needs. runs bun install afterward to pick up any new generated files.
PREBUILD_OUTPUT=$(bun run generate-cache-key && turbo run build --filter=@humanlayer/hld-sdk && bun install 2>&1)
if [ $? -ne 0 ]; then
  echo "prebuild failed:" >&2
  echo "$PREBUILD_OUTPUT" >&2
  exit 2
fi

# biome and typecheck run in parallel to keep the feedback loop tight.
# one quirk: biome --write exits with code 1 if it made any changes, even if it
# successfully fixed everything. so we run it twice with ||: if the first pass
# makes changes and exits 1, the second pass will exit 0 since there's nothing
# left to fix. if there are unfixable errors, both passes fail and exit 2.
OUTPUT=$(bun run --parallel \
  "biome check . --write --unsafe || biome check . --write --unsafe" \
  "turbo run typecheck" 2>&1)

if [ $? -ne 0 ]; then
  echo "$OUTPUT" >&2
  exit 2
fi
```

成功时 hook 完全静默——没有任何东西进入 Agent 的上下文。失败时，只显示错误，退出码 `2` 告诉 harness 重新激活 Agent 以便它在完成前修复错误。

**【KuiklyUI 借鉴思考】**

对于 KuiklyUI，可以设计以下 hooks：

1. **Pre-commit Hook**
   ```bash
   # 运行 Kotlin 格式检查
   ./gradlew ktlintCheck
   
   # 运行类型检查
   ./gradlew compileCommonMainKotlinMetadata
   
   # 检查 API 兼容性
   ./scripts/check-api-compatibility.sh
   ```

2. **Post-edit Hook**
   ```bash
   # 自动运行相关测试
   ./gradlew test --tests "*${CHANGED_FILE}*"
   
   # 检查跨平台一致性
   ./scripts/check-cross-platform-consistency.sh
   ```

3. **Notification Hook**
   ```bash
   # Agent 完成时发送通知
   osascript -e 'display notification "Agent completed" with title "Claude Code"'
   ```

---

## Back-Pressure Increases Your Chances of Success（反向压力提高成功率）

> your likelihood of successfully solving a problem with a coding agent is strongly correlated with the agent's ability to verify its own work.
> 
> （用编码 Agent 成功解决问题的可能性与 Agent 验证自己工作的能力强烈相关）

这是**他们花时间做的最高杠杆的事情之一**。

验证机制示例：
- typechecks 和 build steps（最好在强类型语言中）
- 单元测试和/或集成测试
- 代码覆盖率报告（有 `Stop` hook 提示 Agent 在覆盖率下降时增加覆盖率）
- UI 交互和测试集成（playwright、agent-browser 等）

关键：**这些验证机制需要是上下文高效的**。

> early on we had our agent run the full test suite after every change, and 4,000 lines of passing tests would flood the context window. The agent would then lose track of the actual task and start hallucinating about test files it had just read.
> 
> （早期我们让 Agent 在每次更改后运行完整测试套件，4000 行通过的测试会淹没上下文窗口。然后 Agent 会失去对实际任务的跟踪，开始对它刚刚阅读的测试文件产生幻觉）

解决方案：
> swallow the output and only surface errors
> （吞掉输出，只显示错误）

成功是静默的，只有失败产生详细输出。

**【KuiklyUI 借鉴思考】**

对于 KuiklyUI：

1. **快速验证**（每次编辑后）
   - 编译检查：`./gradlew compileCommonMainKotlinMetadata`
   - 相关单元测试：只运行与修改文件相关的测试
   - 静默成功，只显示错误

2. **完整验证**（阶段性）
   - 完整构建：`./gradlew build`
   - 跨平台测试
   - 代码覆盖率检查

3. **Hook 集成**
   ```bash
   # stop-hook.sh
   # 在 Agent 停止时运行
   
   # 1. 运行快速类型检查
   ./gradlew compileCommonMainKotlinMetadata 2>&1 | grep -i "error" || true
   
   # 2. 运行相关测试（只显示失败）
   ./gradlew test --tests "*${CHANGED_FILE}*" 2>&1 | grep -E "(FAILED|PASSED|tests)" || true
   
   # 3. 检查代码覆盖率
   ./scripts/check-coverage.sh
   ```

---

## Closing Notes（结束语）

> It is entirely possible to spend more time optimizing your coding agent setup than actually shipping code with it — we've been there.
> 
> （完全有可能花更多时间优化编码 Agent 设置，而不是实际用它提交代码——我们也经历过）

他们的方法：偏向交付。只在真正让他们更快交付更多高质量代码时才花时间配置 harness。

### What Didn't Work（什么不起作用）

- 在遇到真实失败之前就试图设计理想的 harness 配置
- "以防万一"安装几十个 skills 和 MCP 服务器
- 在每个 Agent 会话结束时运行整个测试套件（5+ 分钟）（改为运行子集）
- 试图微观优化哪些子 Agent 可以访问哪些工具。这导致了很多工具抖动，结果更糟。大多数编码 Agent 对此没有强大的配置面。

### What Did Work（什么起作用）

- 从简单开始，只在 Agent 实际失败时才添加配置
- 设计、测试、迭代——扔掉不起作用的东西。扔掉的 hooks 比实际使用的多得多
- 通过仓库级配置将经过实战检验的配置分发给整个团队
- 优化迭代速度，而不是"首次尝试一次成功的可能性"
- 给 Agent 一组能力（Linear），然后在知道需要什么后仔细削减暴露给模型的内容

最后的话：
> The next time your coding agent isn't performing the way you expect, before you blame the model, check the harness. Agentfiles, MCP servers, skills, sub-agents, hooks, and back-pressure — that's where we've found most of the leverage. The model is probably fine. It's just a skill issue.
> 
> （下次你的编码 Agent 表现不如预期时，在责怪模型之前，先检查 harness。Agentfiles、MCP 服务器、skills、子 Agent、hooks 和反向压力——那是我们发现大部分杠杆的地方。模型可能没问题。这只是 skill 问题）

---

## 对 KuiklyUI 的核心启示

### 1. Harness 配置优先级

按优先级顺序配置：

1. **CLAUDE.md/AGENTS.md**（基础配置）
   - 保持简洁（< 60 行）
   - 人工编写，不要自动生成
   - 渐进式披露，避免条件规则

2. **CLI > MCP**（工具选择）
   - 优先使用训练数据良好的 CLI（gh、gradle）
   - 如需 MCP，限制工具数量
   - 考虑编写精简 CLI 包装器

3. **Skills**（渐进式知识）
   - 按功能模块设计 skills
   - 每个 skill 包含 SKILL.md + 详细文档
   - 需要时才激活

4. **Sub-Agents**（上下文控制）
   - 父 Agent（Opus）负责编排
   - 子 Agent（Sonnet/Haiku）负责具体任务
   - 充当"上下文防火墙"

5. **Hooks**（自动化）
   - 验证 hook：类型检查、测试
   - 通知 hook：完成提醒
   - 审批 hook：危险操作拦截

6. **Back-Pressure**（反向压力）
   - 快速验证：编译、相关测试
   - 静默成功，只显示错误
   - 完整验证阶段性进行

### 2. 实施路线图

**Phase 1: 基础配置（本周）**
- [ ] 优化 CLAUDE.md（< 60 行，聚焦通用规则）
- [ ] 列出常用 CLI 命令，准备添加到 CLAUDE.md
- [ ] 识别第一个适合用子 Agent 的任务

**Phase 2: Skills 建设（2-4 周）**
- [ ] 设计 3-5 个核心 skills（kmp-platform、kuikly-render、kuikly-testing）
- [ ] 编写 SKILL.md 和详细文档
- [ ] 在团队内测试和迭代

**Phase 3: 高级配置（1-2 月）**
- [ ] 实现关键 hooks（验证、通知）
- [ ] 建立子 Agent 工作流
- [ ] 优化 back-pressure 机制

### 3. 关键原则

1. **从简单开始**
   - 不要预设所有配置
   - 只在 Agent 失败时才添加配置
   - 愿意扔掉不起作用的配置

2. **渐进式披露**
   - 不要一次性给所有信息
   - 通过 skills 按需加载知识
   - 保持主上下文窗口简洁

3. **上下文防火墙**
   - 使用子 Agent 隔离噪音
   - 父 Agent 专注编排
   - 只让压缩后的结果流回

4. **验证优先**
   - 建立快速验证机制
   - 静默成功，只显示错误
   - 让 Agent 能验证自己的工作

5. **迭代速度 > 一次成功**
   - 优化反馈循环
   - 快速失败，快速修复
   - 不要追求首次尝试完美

### 4. 避免的反模式

❌ **过度配置**
- 安装几十个 MCP 服务器"以防万一"
- 在真实失败前设计理想配置
- 试图微观优化每个工具访问权限

❌ **上下文膨胀**
- 在系统提示词中塞入所有指令
- 运行完整测试套件（4000 行输出）
- 不使用子 Agent 隔离噪音

❌ **模型依赖**
- 等待 GPT-6 解决所有问题
- 责怪模型而不是检查配置
- 忽视 harness 工程的价值

---

## 相关资源

**本文**：
- 原文：https://www.humanlayer.dev/blog/skill-issue-harness-engineering-for-coding-agents

**相关文章**：
- Advanced Context Engineering：https://www.humanlayer.dev/blog/advanced-context-engineering
- Writing a good CLAUDE.md：https://www.humanlayer.dev/blog/writing-a-good-claude-md
- 12 Factor Agents：https://github.com/humanlayer/12-factor-agents
- Context-Efficient Backpressure：https://www.humanlayer.dev/blog/context-efficient-backpressure

**参考文章**：
- Viv's Harness Engineering：https://www.vtrivedy.com/posts/claude-code-sdk-haas-harness-as-a-service
- Viv's Anatomy of an Agent Harness：https://blog.langchain.com/the-anatomy-of-an-agent-harness/
- Mitchell Hashimoto's AI Adoption Journey：https://mitchellh.com/writing/my-ai-adoption-journey
- OpenAI Harness Engineering：https://openai.com/index/harness-engineering/

**研究**：
- ETH Zurich Agentfiles Study：https://arxiv.org/abs/2602.11988
- Chroma Context Rot Research：https://research.trychroma.com/context-rot
- Terminal Bench 2.0：https://terminalbench.com/

---

*精读完成时间: 2026-03-31*  
*关联文档: harness-engineer/KUIKLY_HARNESS_PLAN.md, mitchell-hashimoto-ai-adoption-journey-detailed.md, humanlayer-advanced-context-engineering-detailed.md*
