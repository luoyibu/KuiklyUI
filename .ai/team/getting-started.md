# KuiklyUI AI 开发上手指南

> 面向团队成员的 AI 辅助开发实践指南。

---

## KuiklyUI Harness 设计方案

本指南基于 KuiklyUI 团队的 AI 编程 Harness 工程建设规划，详见：
[KuiklyUI Harness 工程建设总览](../../harness-engineer/HARNESS_OVERVIEW.md)

---

## 支持的工具

以下工具均已集成知识库和 Skills，开箱即用，不推荐特定工具，按个人习惯选择：

| 工具 | 说明 |
|------|------|
| **Claude Code** | Anthropic 官方 CLI，终端内使用 |
| **Claude-internal** | 腾讯内部 Claude 接入 |
| **OpenCode** | 开源 AI 编程工具 |
| **Cursor** | IDE 集成，可视化 diff 体验好 |
| **CodeBuddy** | 腾讯内部 AI 编程助手 |

---

## AI 工作流

AI 辅助开发不只是「让 AI 写代码」，而是把 AI 融入你的开发流程——你负责方向判断，AI 负责执行。下面是两种最常见的场景。

### Bug 跟进

想象一下：遇到 bug 不用自己打开 IDE，不用逐行加断点，不用等编译——把现象告诉 AI，让它全程跑通复现、分析根因、给出方案，你只需要在几个关键节点点头或者给方向就好。这是我们目前最成熟的 AI 工作流。

详见：[Bug 跟进工作流](./workflows-debug.md)

### 新功能开发

> TODO：待补充（负责人：@elixxli）

---

## 经验总结

---

### ⚠️ 1. 发现 AI 遇到困难，主动让 AI 自己总结优化

使用 AI 的过程中，你会遇到各种挫败的瞬间——AI 绕圈、反复犯同一个错、给出莫名其妙的答案。这些瞬间需要我们**主动识别，让 AI 自己总结优化**。这样 AI 才会越用越好用。

> "Anytime you find an agent makes a mistake, you take the time to engineer a solution such that the agent never makes that mistake again."
>（每当发现 Agent 犯错，就花时间设计解决方案，确保 Agent 永远不会再犯同样的错误）
>
> — [Mitchell Hashimoto - My AI Adoption Journey](https://mitchellh.com/writing/my-ai-adoption-journey)

> "When the agent struggles, we treat it as a signal: identify what is missing — tools, guardrails, documentation — and feed it back into the repository, always by having Codex itself write the fix."
>
> — [OpenAI Harness Engineering](https://openai.com/zh-Hans-CN/index/harness-engineering/)

**实践方式**：

1. 发现 AI 反复在某类任务上出错或卡住
2. 识别缺少什么：知识库文档不准确？错误案例没覆盖？缺少 lint 规则？
3. 让 AI 自己生成修复（补文档、补错误案例、更新知识库）
4. 提 PR 合入仓库，下次相同场景 AI 表现更好

**常用 Prompt**：
```
我发现你在 [xxx] 遇到了阻塞，帮我复盘一下，
然后对相关的 skills、知识库、流程做一个优化建议，
整理成文档提 PR。
```

**这是一个正向飞轮**：越用越好，而不是越用越烦。

---

### 2. 上下文管理：AI 变慢变蠢时怎么办

> 部分模型会表现出**「上下文焦虑」**（context anxiety）：接近上下文限制时提前结束工作、开始走捷径。
>
> — [Anthropic - Harness Design for Long-running Apps](https://www.anthropic.com/engineering/harness-design-long-running-apps)

> "Keep utilization in the 40-60% range... designing your entire development process around context management"
>
> — [HumanLayer - Advanced Context Engineering for Coding Agents](https://www.humanlayer.dev/blog/advanced-context-engineering)

长会话会积累大量日志输出、重试记录、错误信息，导致 AI 的有效注意力被稀释。
**表现**：绕圈、反复犯同样错误、分析质量明显下降。

**建议时机**：上下文使用量超过 60%，或者明显感觉 AI 变慢变蠢时。

**第一步：让 AI 把进度总结到文件**

```
把我们到目前为止做的所有事情写到 progress.md，确保记录：
- 最终目标
- 我们采用的方法
- 已完成的步骤
- 当前正在处理的问题或阻碍
```

**第二步：新开会话，读取进度文件继续**

```
请先阅读 progress.md，理解我们当前任务的背景和进度，然后跟我确认下一步工作
```

---

### 3. 子 Agent 作为上下文防火墙

> "Sub-agents are about context control... use a fresh context window for lookup/search/summarize, enabling the parent to start working directly without polluting its context window with Glob/Grep/Read calls"
>
> — [HumanLayer - Advanced Context Engineering for Coding Agents](https://www.humanlayer.dev/blog/advanced-context-engineering)

主 Agent 的上下文是宝贵资源，不要让它做大量搜索和查找——这些应该交给子 Agent，主 Agent 只接收结果，保持干净的上下文专注于实现。

**常见场景和做法**：

| 场景 | 做法 |
|------|------|
| 需要在代码库中搜索相关代码 | 告诉 AI「开一个子 Agent 去搜索，把结果汇报给我」 |
| 需要阅读多个文件了解背景 | 告诉 AI「开一个子 Agent 去读这些文件，总结关键信息」 |
| 需要生成调研报告 | 让子 Agent 专门负责调研，主 Agent 基于报告做决策 |

**直观判断**：如果一个任务需要大量 Grep/Read/Glob 操作，就适合交给子 Agent。

---

## 模型选择

个人观感，优先按以下顺序选择，取决于大家的 token 消耗情况：

1. **Claude Sonnet 4.6** — 日常首选
2. **Claude Opus 4.6** — 复杂问题、根因隐蔽时升级
3. **Kimi 2.5** — 多次尝试仍无进展时，换模型换思路
