# A Guide to Which AI to Use in the Agentic Era (Ethan Mollick) - 精读笔记

**原文**: https://www.oneusefulthing.org/p/a-guide-to-which-ai-to-use-in-the  
**作者**: Ethan Mollick (Wharton School, University of Pennsylvania)  
**发布时间**: 2026年2月18日  
**精读时间**: 2026-03-30

---

## 一、文章概述

### 1.1 核心框架：Models, Apps, and Harnesses

Ethan Mollick 在这篇文章中提出了一个**突破性的 AI 使用框架**，将 AI 系统分解为三个层次：

1. **Models（模型）**: 底层的 AI 大脑
2. **Apps（应用）**: 用户与模型交互的产品界面
3. **Harnesses（Harness）**: 让 AI 能力转化为实际工作的系统

**关键洞察**：
- 过去几个月，"使用 AI" 的含义发生了巨大变化
- 从"与聊天机器人对话"转变为"将 AI 作为 Agent 分配任务"
- 同样的模型在不同 Harness 中表现完全不同

---

## 二、三个核心概念详解

### 2.1 Models（模型）

**定义**：底层的 AI 大脑，决定系统的智能程度。

**当前三大前沿模型**（截至 2026年2月）：
- **Claude Opus 4.6** (Anthropic)
- **Gemini 3.0 Pro** (Google)
- **GPT-5.2/5.3** (OpenAI)

**模型决定什么**：
- 推理能力
- 写作/编码/分析能力
- 图像理解和生成能力
- 错误率

**重要提醒**：
- 免费模型针对"聊天"优化，速度快但准确性差
- 做真正的工作需要付费（至少 $20/月）选择高级模型
- 模型差异现在很小，**App 和 Harness 比模型更重要**

---

### 2.2 Apps（应用）

**定义**：用户实际用来与模型对话的产品，让模型做实际工作。

**常见 Apps**：
- 聊天网站：chatgpt.com, claude.ai, gemini.google.com
- 编码工具：OpenAI Codex, Claude Code
- 桌面工具：Claude Cowork

**关键变化**：
- 同样的模型在不同 App 中表现不同
- 差异不仅在于界面，还在于 Harness（工具访问能力）

**示例对比**（Claude Opus 4.6 回答同样的问题"Compare ChatGPT and Claude and Gemini"）：

| 环境 | 结果 |
|------|------|
| 无 Harness | 信息过时 |
| Claude.ai 网站 | 更新信息 + 可验证来源 |
| Claude Cowork | 复杂分析 + 格式良好的对比 |

---

### 2.3 Harnesses（Harness）⭐ 核心概念

**定义**：让 AI 模型的能力做实际工作的系统，就像马具让马的力量拉车或犁地。

**Harness 的作用**：
- 让 AI 使用工具
- 采取行动
- 完成多步骤任务

**Harness 的层次**：

1. **基础 Harness**（聊天网站）：
   - 网络搜索
   - 代码编写
   - 特定问题的处理指令（如创建电子表格、平面设计）

2. **扩展 Harness**（Claude Code）：
   - 虚拟计算机
   - Web 浏览器
   - 代码终端
   - 串联工具完成复杂任务（研究、构建、测试网站）

3. **独立 Harness**：
   - Manus（被 Meta 收购）：可包装多个模型
   - OpenClaw：本地运行，连接任何 AI 模型

**关键洞察**：
> "Claude Opus 4.6 talking to you in a chat window is a very different experience from Claude Opus 4.6 operating inside Claude Code, autonomously writing and testing software for hours at a stretch."

**翻译**：Claude Opus 4.6 在聊天窗口与你对话，与在 Claude Code 中自主编写和测试软件数小时，是完全不同的体验。

---

## 三、当前 AI 应用全景

### 3.1 聊天机器人界面（Chatbot Interfaces）

**三大平台对比**：

| 特性 | ChatGPT | Claude | Gemini |
|------|---------|--------|--------|
| **捆绑功能** | 图像生成、学习研究、深度研究、购物研究 | 仅深度研究 | nano banana (图像)、Veo 3.1 (视频)、Guided Learning、深度研究 |
| **Harness 能力** | 强（代码执行、文件、研究） | 强（代码执行、文件、研究） | 弱（无法生成电子表格和 PowerPoint） |
| **数据连接** | 邮件、日历、文件、其他应用 | 邮件、日历、文件、其他应用 | 邮件、日历、文件、其他应用 |

**关键发现**：
- OpenAI 和 Anthropic 在 Harness 方面领先 Google
- Gemini 网站能力较弱，尽管底层模型同样强大
- Google 预计会很快追赶

---

### 3.2 专业应用和 Harness

#### 编码 Harness

**Claude Code, OpenAI Codex, Google Antigravity**

**特点**：
- 面向程序员
- 访问代码库
- 终端访问
- 自主编写、运行、测试代码

**Ethan Mollick 的实例**：
- 想制作一个包含 GPT-1 所有权重的纸质版 LLM（80 卷书）
- 让 Claude Code 执行
- AI 在约一小时内：
  - 制作了 80 卷精美排版的卷册
  - 设计了封面（可视化内部权重）
  - 创建了网站（含动画）
  - 接入 Stripe 支付
  - 接入 Lulu 按需印刷
  - 测试整个流程
  - 发布上线
- 作者从未接触或查看任何代码
- 20 本书当天售罄

**意义**：
- 小项目想法不再需要大量工作
- AI 可以自主执行

---

#### 办公应用 Harness

**Claude for Excel 和 PowerPoint**

**特点**：
- 特定应用内的 Harness
- Claude for Excel：像初级分析师一样工作
- 结果在 Excel 中，易于检查

**对比**：
- Google Sheets 集成不如 Claude for Excel 深入
- OpenAI 没有直接等价产品

---

#### 桌面级 Harness：Claude Cowork ⭐

**定义**：Claude Code 的非技术工作版本

**特点**：
- 在桌面运行
- 直接处理本地文件和浏览器
- 比 Claude Code 更安全（VM 中运行，默认拒绝网络，硬隔离）
- 用户描述结果，Claude 制定计划、分解任务、在计算机上执行

**使用场景**：
- 整理费用报告
- 从 PDF 提取数据到电子表格
- 起草摘要

**意义**：
- AI 不只是谈论工作，而是实际做工作
- 发展方向：AI 作为真正的助手

**构建背景**：
- 基于与 Claude Code 相同的 Agent 架构
- 主要由 Claude Code 在约两周内构建

---

#### 知识管理 Harness：NotebookLM

**定义**：Google 的解决方案，用于理解大量信息

**功能**：
- 自主深度研究
- 上传论文、视频、网站、文件
- 构建交互式知识库
- 查询、生成幻灯片、思维导图、视频
- AI 生成播客（两个主持人讨论材料）

**目标用户**：
- 学生
- 研究人员
- 需要理解大量文档的人

---

#### 实验性 Harness：OpenClaw

**定义**：开源 AI Agent，2025年1月走红

**特点**：
- 本地运行在计算机上
- 连接任何 AI 模型
- 通过 WhatsApp 或 iMessage 聊天界面交互
- 浏览网页、管理文件、发送邮件、运行命令
- 24/7 个人助手

**警告**：
- 严重安全风险
- 给 AI 广泛访问计算机和账户
- 未知危险

**意义**：标志发展方向

---

## 四、实践建议

### 4.1 初学者

**步骤**：
1. 选择三大系统之一（ChatGPT, Claude, 或 Gemini）
2. 支付 $20/月
3. 选择高级模型
4. 邀请 AI 参与你做的每件事
5. 开始用于实际工作
6. 上传实际工作的文档
7. 给 AI 复杂任务（RFP 或 SOP 形式）
8. 进行来回对话，推动 AI

**核心**：这比任何指南都更能教会你

---

### 4.2 已熟悉聊天机器人的用户

**推荐尝试**：

1. **NotebookLM**（免费，易用，好的起点）
2. **Claude Code**（最强大的编码 Harness）
3. **Claude Cowork**（桌面级 Agent）
4. **Claude PowerPoint 和 Excel 插件**（办公自动化）

**关键**：
- 不是作为演示，而是用于真正需要完成的事情
- 观察 AI 做什么
- 出错时引导它
- 你不是在"提示"，而是在"管理"

---

## 五、关键洞察与趋势

### 5.1 从聊天机器人到 Agent 的转变

**重要性**：
- 自 ChatGPT 推出以来，人们使用 AI 的最重要变化
- 工具仍然难以理解和使用
- 会做出令人困惑的事情

**价值**：
> "An AI that does things is fundamentally more useful than an AI that says things"

**翻译**：做事情的 AI 比说事情的 AI 根本更有用

---

### 5.2 Harness 比模型更重要

**趋势**：
- 模型能力差异越来越小
- 同样的模型在不同 Harness 中表现完全不同
- 未来竞争焦点：Harness 设计

**对 KuiklyUI 的启示**：
- 不仅要关注模型选择
- 更要关注如何设计 Harness
- 让 AI 能够自主完成跨平台开发任务

---

### 5.3 组织设计的挑战

**评论者洞察**（Josh Rowe）：
> "What really struck me reading this is how quickly the problem shifts from model capability to organisational design. Once AI can reliably handle multi-step work, the hard question stops being which system is smartest and becomes how companies actually structure delegation, supervision, and accountability when the 'worker' is software."

**翻译**：真正让我震惊的是，问题如何迅速从模型能力转向组织设计。一旦 AI 能够可靠地处理多步骤工作，难题就不再是哪个系统最聪明，而是公司如何在"工人"是软件的情况下，实际构建委托、监督和问责的结构。

**对 KuiklyUI 的启示**：
- Harness 工程不仅是技术问题
- 也是组织流程问题
- 需要考虑 AI 如何融入开发流程

---

## 六、对 KuiklyUI Harness 工程的启示

### 6.1 三个层次的应用

基于 Ethan Mollick 的框架，KuiklyUI 的 Harness 应该考虑：

| 层次 | KuiklyUI 对应 | 说明 |
|------|--------------|------|
| **Models** | Claude/GPT 等 | 基础模型能力 |
| **Apps** | Claude Code, OpenCode, Cursor, CodeBuddy | AI 工具选择 |
| **Harnesses** | KuiklyUI 自定义 Harness | 让 AI 能够自主完成 KMP 跨平台开发 |

### 6.2 Harness 设计的核心问题

**关键问题**：
- KuiklyUI 需要什么样的 Harness？
- 如何让 AI 能够：
  - 理解 KMP 跨平台架构
  - 在多个平台（Android/iOS/HarmonyOS/Web/小程序/macOS）间协调
  - 自主编写、测试、调试跨平台代码
  - 维护架构约束和质量标准

### 6.3 参考现有 Harness

**可以借鉴的 Harness**：

1. **Claude Code 模式**：
   - 访问代码库
   - 终端执行
   - 自主编写、运行、测试

2. **Claude Cowork 模式**：
   - 桌面级 Agent
   - 处理本地文件
   - 安全隔离（VM）

3. **NotebookLM 模式**：
   - 知识库构建
   - 多源信息整合
   - 交互式查询

### 6.4 KuiklyUI Harness 的特殊需求

**跨平台开发的 Harness 需求**：

1. **多平台工具访问**：
   - Android Studio / Xcode / DevEco Studio
   - 各平台模拟器/真机
   - 跨平台构建工具

2. **架构约束执行**：
   - 模块依赖检查
   - API 一致性验证
   - 平台特定代码规范

3. **测试 Harness**：
   - 跨平台测试执行
   - UI 测试自动化
   - 性能测试

4. **知识库集成**：
   - KMP 最佳实践
   - 平台特定文档
   - 历史代码模式

---

## 七、关键引用

### 7.1 关于 Harness 定义

> "Harnesses are what let the power of AI models do real work, like a horse harness takes the raw power of the horse and lets it pull a cart or plow. A harness is a system that lets the AI use tools, take actions, and complete multi-step tasks on its own."

**翻译**：Harness 是让 AI 模型能力做实际工作的系统，就像马具让马的力量拉车或犁地。Harness 是一个让 AI 使用工具、采取行动、自主完成多步骤任务的系统。

### 7.2 关于模型与 Harness 的关系

> "The same model can behave very differently depending on what harness it's operating in."

**翻译**：同样的模型在不同的 Harness 中表现可以非常不同。

### 7.3 关于 Agent 时代

> "The shift from chatbot to agent is the most important change in how people use AI since ChatGPT launched."

**翻译**：从聊天机器人到 Agent 的转变，是自 ChatGPT 推出以来人们使用 AI 的最重要变化。

### 7.4 关于管理的转变

> "You aren't prompting, you are managing."

**翻译**：你不是在"提示"，你是在"管理"。

---

## 八、总结

Ethan Mollick 的这篇文章通过 **Models-Apps-Harnesses** 三个概念的框架，清晰地解释了 AI 使用的演进：

1. **Models** 是基础能力
2. **Apps** 是交互界面
3. **Harnesses** 是实际工作的关键

**核心洞察**：
- 同样的模型在不同 Harness 中表现完全不同
- Harness 设计将成为未来竞争的焦点
- 从"提示 AI"转变为"管理 AI"
- AI 从"说事情"进化为"做事情"

**对 KuiklyUI 的核心启示**：
- 需要为 KMP 跨平台开发设计专门的 Harness
- Harness 应让 AI 能够自主完成多平台协调、编码、测试
- 需要结合架构约束、知识库、工具访问能力
- 这是一个组织设计问题，不仅是技术问题

---

*精读完成时间: 2026-03-30*  
*原文长度: ~3000 词*  
*精读笔记长度: ~4500 词*
