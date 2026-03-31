# Anthropic - Effective harnesses for long-running agents 精读总结

**原文链接**: https://www.anthropic.com/engineering/effective-harnesses-for-long-running-agents  
**发布时间**: 2025年11月26日  
**作者**: Justin Young (Anthropic)

---

## 一、核心问题：长时间运行 Agent 的挑战

### 1.1 问题本质

Agent 必须在**离散的会话**中工作，每个新会话开始时**没有之前的记忆**。

**类比**: 就像工程师轮班工作，每个新工程师上班时**完全不知道**上一个班次发生了什么。

### 1.2 两个主要失败模式

| 失败模式 | 表现 | 原因 |
|---------|------|------|
| **一次性尝试** | Agent 试图一次性完成整个应用 | 在实现过程中耗尽上下文，留下半成品功能 |
| **过早宣布完成** | 看到已有进展就认为工作已完成 | 缺乏明确的完成标准 |

**后果**:
- 下一个会话从半成品开始，没有文档
- Agent 必须猜测发生了什么
- 花费大量时间重新让基础功能工作

---

## 二、解决方案：双 Agent 架构

### 2.1 架构概述

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  Initializer    │────▶│     Coding      │────▶│     Coding      │
│     Agent       │     │     Agent       │     │     Agent       │
│   (第1次会话)    │     │   (第2次会话)    │     │   (第N次会话)    │
└─────────────────┘     └─────────────────┘     └─────────────────┘
        │                       │                       │
        ▼                       ▼                       ▼
   初始化环境               增量开发                 增量开发
   创建基础结构             留下清晰产物              留下清晰产物
```

### 2.2 Initializer Agent（初始化智能体）

**职责**: 第一次会话，设置初始环境

**产物**:
1. **`init.sh` 脚本** - 运行开发服务器
2. **`claude-progress.txt`** - 进度日志文件
3. **初始 git commit** - 显示添加的文件
4. **`feature_list.json`** - 功能需求清单

### 2.3 Coding Agent（编码智能体）

**职责**: 每次后续会话，进行增量开发

**行为**:
- 一次只处理一个功能
- 每次会话结束时留下结构化更新
- 保持环境处于"干净状态"

**干净状态定义**:
- 没有重大 Bug
- 代码有序且文档完善
- 开发者可以立即开始新功能，无需先清理混乱

---

## 三、关键组件详解

### 3.1 Feature List（功能清单）

**目的**: 解决 Agent 一次性尝试或过早完成的问题

**格式**: JSON（比 Markdown 更不容易被不当修改）

**示例**:
```json
{
  "category": "functional",
  "description": "New chat button creates a fresh conversation",
  "steps": [
    "Navigate to main interface",
    "Click the 'New Chat' button",
    "Verify a new conversation is created",
    "Check that chat area shows welcome state",
    "Verify conversation appears in sidebar"
  ],
  "passes": false
}
```

**关键设计**:
- 所有功能初始标记为 **"failing"**
- Agent 只能修改 `passes` 字段
- 强指令："删除或编辑测试是不可接受的"

### 3.2 Incremental Progress（增量进展）

**核心原则**: 一次只做一个功能

**会话结束时的要求**:
1. **Git commit** - 描述性提交信息
2. **Progress file 更新** - 记录进度摘要
3. **干净状态** - 可以立即开始下一个功能

**好处**:
- Agent 可以使用 git 回滚错误的代码更改
- 恢复代码库的工作状态
- 消除猜测和恢复时间

### 3.3 Testing（测试）

**问题**: Claude 倾向于在没有适当测试的情况下标记功能完成

**解决方案**: 显式提示使用浏览器自动化工具，像人类用户一样进行端到端测试

**工具**: Puppeteer MCP 服务器

**实际效果**:
- 截图验证 UI 状态
- 识别和修复代码中不明显的 Bug
- 显著提高性能

**限制**:
- Claude 的 vision 有限
- 无法看到浏览器原生 alert 弹窗
- 依赖这些弹窗的功能往往更 buggy

---

## 四、会话启动流程

### 4.1 标准启动步骤

每个 Coding Agent 会话开始时执行：

```
1. Run `pwd` to see the directory you're working in
   （查看工作目录）

2. Read the git logs and progress files to get up to speed
   （读取 git 日志和进度文件，了解最近的工作）

3. Read the features list file and choose the highest-priority 
   feature that's not yet done to work on
   （读取功能清单，选择最高优先级的未完成功能）
```

### 4.2 实际会话示例

```
[Assistant] I'll start by getting my bearings and understanding 
the current state of the project.

[Tool Use] <bash - pwd>
[Tool Use] <read - claude-progress.txt>
[Tool Use] <read - feature_list.json>

[Assistant] Let me check the git log to see recent work.
[Tool Use] <bash - git log --oneline -20>

[Assistant] Now let me check if there's an init.sh script to 
restart the servers.
<Starts the development server>

[Assistant] Excellent! Now let me navigate to the application 
and verify that some fundamental features are still working.
<Tests basic functionality>

[Assistant] Based on my verification testing, I can see that 
the fundamental functionality is working well. Now let me 
review the tests.json file more comprehensively...
<Starts work on a new feature>
```

---

## 五、失败模式与解决方案总结

| 问题 | Initializer Agent 解决 | Coding Agent 解决 |
|------|----------------------|-------------------|
| **过早宣布胜利** | 设置功能清单文件 | 读取功能清单，选择单个功能工作 |
| **留下 Bug 或未记录进度** | 初始 git repo 和进度文件 | 读取进度文件和 git 日志，运行基本测试，结束时写 git commit 和进度更新 |
| **过早标记功能完成** | 设置功能清单文件 | 自验证所有功能，仔细测试后才标记为 passing |
| **花费时间 figuring out 如何运行应用** | 编写 `init.sh` 脚本 | 会话开始时读取 `init.sh` |

---

## 六、对 KuiklyUI 的启示

### 6.1 立即可应用

**1. 功能清单系统**
- 为复杂任务创建 `feature_list.json`
- 明确每个功能的完成标准
- 防止 AI 过早宣布完成

**2. 进度追踪文件**
- `claude-progress.txt` 记录每次会话的工作
- Git 历史作为补充
- 新会话快速了解状态

**3. 初始化脚本**
- `init.sh` 统一环境启动
- 确保每次会话从已知状态开始
- 运行基础测试验证环境

### 6.2 需要适配

**KuiklyUI 的特殊性**:
- 跨平台（Android/iOS/HarmonyOS/Web/小程序/macOS）
- 需要多个开发环境
- Kotlin Multiplatform 构建复杂

**适配方案**:
- 每个平台可能有不同的 `init.sh`
- 功能清单需要按平台分类
- 测试需要覆盖多平台

### 6.3 长期考虑

**多 Agent 架构**:
文章提到未来方向可能是：
- 专门的测试 Agent
- 质量保证 Agent
- 代码清理 Agent

这对 KuiklyUI 的启发：
- 可以针对不同平台有专门的 Agent
- 代码审查 Agent
- 文档生成 Agent

---

## 七、关键引用

> "Agents need a way to bridge the gap between coding sessions."
> （Agent 需要一种方法来弥合编码会话之间的差距）

> "By 'clean state' we mean the kind of code that would be appropriate 
> for merging to a main branch."
> （"干净状态"指的是适合合并到主分支的代码）

> "The key insight here was finding a way for agents to quickly 
> understand the state of work when starting with a fresh context window."
> （关键洞察是找到一种方法，让 Agent 在从全新上下文窗口开始时快速理解工作状态）

---

## 八、与 OpenAI 文章的对比

| 维度 | OpenAI Harness | Anthropic Long-running Agents |
|------|---------------|------------------------------|
| **核心问题** | 知识库组织、架构规范 | 多会话状态管理 |
| **解决方案** | AGENTS.md 地图、渐进式披露 | 双 Agent 架构、功能清单 |
| **适用场景** | 长期项目维护 | 长时间运行任务 |
| **关键文件** | AGENTS.md、docs/ | feature_list.json、claude-progress.txt |
| **Agent 数量** | 单一 Agent | Initializer + Coding Agents |

**互补性**:
- OpenAI 解决"知识如何组织"
- Anthropic 解决"状态如何传递"
- 两者结合：组织良好的知识库 + 清晰的状态管理

---

*总结时间: 2026-03-30*  
*原文链接: https://www.anthropic.com/engineering/effective-harnesses-for-long-running-agents*
