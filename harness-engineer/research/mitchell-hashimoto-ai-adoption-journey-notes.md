# Mitchell Hashimoto "My AI Adoption Journey" 精读笔记

**来源**: https://mitchellh.com/writing/my-ai-adoption-journey  
**作者**: Mitchell Hashimoto (Ghostty 创始人、Vagrant 作者)  
**日期**: February 5, 2026  
**精读重点**: Step 5 "Engineer the Harness"  
**笔记创建**: 2026-03-31

---

## 一、文章概述

Mitchell Hashimoto 分享了他从 AI 怀疑者到重度使用者的完整历程。文章分为 6 个阶段，从放弃聊天机器人到最终建立 Harness Engineering 体系。这是一篇非常务实、非炒作的技术文章。

### 六个发展阶段

| 阶段 | 名称 | 核心转变 |
|------|------|----------|
| Step 1 | Drop the Chatbot | 放弃聊天界面，转向 Agent |
| Step 2 | Reproduce Your Own Work | 强制自己用 Agent 复现手动工作 |
| Step 3 | End-of-Day Agents | 每天最后 30 分钟启动 Agent |
| Step 4 | Outsource the Slam Dunks | 让 Agent 处理确定性高的任务 |
| **Step 5** | **Engineer the Harness** | **建立 Harness Engineering 体系** |
| Step 6 | Always Have an Agent Running | 始终保持一个 Agent 运行 |

---

## 二、Step 5 "Engineer the Harness" 深度分析

### 2.1 核心定义

**Harness Engineering** 是 Mitchell Hashimoto 首次明确提出的术语：

> "It is the idea that **anytime you find an agent makes a mistake, you take the time to engineer a solution such that the agent never makes that mistake again.**"
> 
> （核心理念：每当发现 Agent 犯错，就花时间设计一个解决方案，确保 Agent 永远不会再犯同样的错误）

### 2.2 两种实现形式

Harness Engineering 通过以下两种形式实现：

#### 形式 1: Better implicit prompting (AGENTS.md)

- **适用场景**: 简单问题，如 Agent 反复运行错误命令、找错 API
- **实现方式**: 更新 `AGENTS.md`（或等效文件）
- **特点**: 轻量级、快速迭代

#### 形式 2: Actual, programmed tools

- **适用场景**: 需要确定性验证的复杂问题
- **实现方式**: 编写脚本、测试、工具等
- **特点**: 通常配合 AGENTS.md 更新，告知 Agent 工具存在
- **示例**: 截图脚本、过滤测试、自定义 linter 等

### 2.3 关键洞见

1. **预防优于修复**: 与其反复纠正 Agent 的同一个错误，不如一次性投资建立防护机制
2. **持续积累**: 每个错误都是改进 Harness 的机会，形成复利效应
3. **双向验证**: 不仅要防止 Bad Thing，还要让 Agent 能验证 Good Thing

---

## 三、Ghostty AGENTS.md 示例分析

Mitchell 提供了 Ghostty 项目的真实示例：

**文件位置**: `src/inspector/AGENTS.md`  
**链接**: https://github.com/ghostty-org/ghostty/blob/ca07f8c3f775fe437d46722db80a755c2b6e6399/src/inspector/AGENTS.md

### 3.1 完整内容

```markdown
# Inspector Subsystem

The inspector is a feature of Ghostty that works similar to a
browser's developer tools. It allows the user to inspect and modify the
terminal state.

- See the full C API by finding `dcimgui.h` in the `.zig-cache` folder
  in the root: `find . -type f -name dcimgui.h`. Use the newest version.
- See full examples of how to use every widget by loading this file:
  <https://raw.githubusercontent.com/ocornut/imgui/refs/heads/master/imgui_demo.cpp>
- On macOS, run builds with `-Demit-macos-app=false` to verify API usage.
- There are no unit tests in this package.
```

### 3.2 示例特点分析

| 特点 | 说明 |
|------|------|
| **简洁** | 仅 12 行，10 行有效代码 |
| **问题导向** | 每行都对应一个 Agent 曾犯的错误 |
| **具体可执行** | 提供精确的命令和路径 |
| **上下文相关** | 放在 `src/inspector/` 目录下，而非根目录 |

### 3.3 每条规则的问题背景推测

根据 Mitchell 的描述 "Each line in that file is based on a bad agent behavior"，推测每条规则对应的原始问题：

| 规则 | 推测的原始问题 |
|------|---------------|
| `find . -type f -name dcimgui.h` | Agent 找不到 C API 定义文件 |
| `imgui_demo.cpp` 链接 | Agent 不知道如何正确使用 ImGui widget |
| `-Demit-macos-app=false` | Agent 在 macOS 上使用了错误的构建参数 |
| "no unit tests" | Agent 浪费时间寻找不存在的测试 |

---

## 四、与其他 Harness Engineering 实践对比

### 4.1 与 OpenAI 实践的对比

| 维度 | Mitchell Hashimoto | OpenAI |
|------|-------------------|--------|
| **术语** | Harness Engineering | Harness Engineering |
| **AGENTS.md 定位** | 子目录级、问题导向 | 项目根级、渐进式披露地图 |
| **工具建设** | 强调脚本、测试 | 强调 linter、结构测试 |
| **核心理念** | 从错误中学习 | 从架构约束预防 |

### 4.2 与 Martin Fowler 分类的对应

根据 Martin Fowler 的 Harness 三领域模型：

- **Context Engineering**: AGENTS.md 属于此范畴
- **Architecture Constraints**: programmed tools 属于此范畴
- **Garbage Collection**: 文章未明确提及，但 "prevent that bad thing again" 隐含此意

---

## 五、对 KuiklyUI Harness 建设的启示

### 5.1 立即可以采纳的实践

1. **建立子目录级 AGENTS.md**
   - 不仅限于根目录
   - 在复杂模块（如 inspector、render、core）分别建立
   - 每个文件聚焦该模块的特定问题

2. **问题驱动的规则添加**
   - 不预设所有规则
   - 每当 AI 犯错，立即记录并添加规则
   - 形成 "错误 → 规则 → 预防" 的闭环

3. **轻量级优先**
   - 先尝试 AGENTS.md 文本提示
   - 仅当文本不足时才编写工具
   - 避免过度工程化

### 5.2 需要适配的点

| Mitchell 实践 | KuiklyUI 适配考虑 |
|--------------|------------------|
| Zig 项目 | Kotlin Multiplatform 项目 |
| 单一终端模拟器 | 跨平台 UI 框架 |
| Ghostty 规模 | KuiklyUI 更复杂 |
| 个人项目 | 开源协作项目 |

### 5.3 建议的 KuiklyUI AGENTS.md 结构

```
KuiklyUI/
├── AGENTS.md                    # 根级：项目整体地图
├── src/
│   ├── render/
│   │   └── AGENTS.md           # Render 层特定规则
│   ├── core/
│   │   └── AGENTS.md           # Core 层特定规则
│   └── platform/
│       ├── android/AGENTS.md   # Android 平台规则
│       ├── ios/AGENTS.md       # iOS 平台规则
│       └── harmony/AGENTS.md   # HarmonyOS 平台规则
```

---

## 六、关键引用摘录

### 关于 Harness Engineering 定义

> "I don't know if there is a broad industry-accepted term for this yet, but I've grown to calling this 'harness engineering.' It is the idea that anytime you find an agent makes a mistake, you take the time to engineer a solution such that the agent never makes that mistake again."

### 关于 AGENTS.md 效果

> "Each line in that file is based on a bad agent behavior, and it almost completely resolved them all."

### 关于当前状态

> "This is where I'm at today. I'm making an earnest effort whenever I see an agent do a Bad Thing to prevent it from ever doing that bad thing again."

---

## 七、相关资源

1. **原文**: https://mitchellh.com/writing/my-ai-adoption-journey
2. **Ghostty AGENTS.md 示例**: https://github.com/ghostty-org/ghostty/blob/ca07f8c3f775fe437d46722db80a755c2b6e6399/src/inspector/AGENTS.md
3. **Ghostty 项目**: https://github.com/ghostty-org/ghostty
4. **OpenAI Harness Engineering**: https://openai.com/zh-Hans-CN/index/harness-engineering/
5. **Martin Fowler Harness Engineering**: https://martinfowler.com/articles/exploring-gen-ai/harness-engineering.html

---

## 八、笔记总结

Mitchell Hashimoto 的这篇文章是 Harness Engineering 领域最务实、最可操作的实践指南之一。其核心贡献：

1. **明确定义**了 Harness Engineering 术语
2. **提供了两种实现路径**（AGENTS.md + programmed tools）
3. **展示了真实案例**（Ghostty AGENTS.md）
4. **强调了问题驱动**的方法论

对于 KuiklyUI 项目，建议：
- 立即在关键模块建立 AGENTS.md
- 采用 "犯错即记录" 的轻量级流程
- 结合 OpenAI 的渐进式披露和 Martin Fowler 的三领域模型
- 逐步积累，形成适合 KMP 跨平台项目的 Harness 体系

---

*笔记完成时间: 2026-03-31*  
*关联文档: harness-engineer/KUIKLY_HARNESS_PLAN.md*
