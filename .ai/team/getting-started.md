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

## 工作流

### Bug 跟进

详见：[Bug 跟进工作流](./workflows-debug.md)

### 新功能开发

> TODO：待补充

---

## 经验总结

### 1. AI 遇到困难时，主动优化 Harness

> "When the agent struggles, we treat it as a signal: identify what is missing — tools, guardrails, documentation — and feed it back into the repository, always by having Codex itself write the fix."
>
> — [OpenAI Harness Engineering](https://openai.com/zh-Hans-CN/index/harness-engineering/)

AI 遇到困难，不是 AI 的问题，是 Harness 配置不足的信号。

**实践方式**：

1. 发现 AI 反复在某类任务上出错或卡住
2. 识别缺少什么：知识库文档不准确？错误案例没有覆盖？缺少 lint 规则？
3. 让 AI 自己生成修复（补文档、补错误案例、更新知识库）
4. 提 PR 合入仓库，下次相同场景 AI 表现更好

**这是一个正向飞轮**：越用越好，而不是越用越烦。

---

### 2. 上下文管理：AI 变慢变蠢时怎么办

> "Keep utilization in the 40-60% range... designing your entire development process around context management"
>
> — [HumanLayer - Advanced Context Engineering for Coding Agents](https://www.humanlayer.dev/blog/advanced-context-engineering)

长会话积累大量日志输出、重试记录、错误信息，会导致 AI 的有效注意力被稀释。
**表现**：绕圈、反复犯同样错误、分析质量明显下降。

**处理方式**：让 AI 把当前进度总结到文件，然后新开会话继续。

提示词模板：
```
把我们到目前为止做的所有事情写到 progress.md，确保记录：
- 最终目标
- 我们采用的方法
- 已完成的步骤
- 当前正在处理的问题或阻碍
```

---

### 3. 子 Agent 作为上下文防火墙

> "Sub-agents are about context control... use a fresh context window for lookup/search/summarize, enabling the parent to start working directly without polluting its context window with Glob/Grep/Read calls"
>
> — [HumanLayer - Advanced Context Engineering for Coding Agents](https://www.humanlayer.dev/blog/advanced-context-engineering)

让子 Agent 执行搜索、查找、文件阅读等任务，主 Agent 只接收结果，不被大量 Grep/Read 污染上下文。

**实践**：
- 需要搜索代码库时，告诉 AI「开一个子 Agent 去搜索」
- 主 Agent 专注于实现逻辑，保持上下文干净

---

## 模型选择

| 场景 | 推荐模型 |
|------|---------|
| 日常开发、简单 bug | Claude Sonnet 4.6 |
| 复杂 bug、根因隐蔽 | Claude Opus 4.6 |
| 多次尝试仍无进展，换思路 | Kimi 2.5 |
