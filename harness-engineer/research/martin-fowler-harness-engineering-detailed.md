# Harness Engineering (Martin Fowler) - 精读笔记

**原文**: https://martinfowler.com/articles/exploring-gen-ai/harness-engineering.html  
**作者**: Birgitta Böckeler (Thoughtworks Distinguished Engineer)  
**发布时间**: 2026年2月17日  
**精读时间**: 2026-03-30

---

## 一、文章概述

### 1.1 写作背景

本文是对 OpenAI "Harness Engineering" 文章的深度分析与思考。OpenAI 团队通过"完全不手动编写代码"的强制约束，构建了一个用于维护大型应用的 Harness 系统，5个月后构建了一个超过 100 万行代码的真实产品。

**术语来源**：
- 文章标题提到 "Harness Engineering"，但正文中只提到一次 "harness"
- 可能受到 Mitchell Hashimoto 博客文章的启发
- 作者喜欢 "harness" 这个词来描述用于约束 AI Agent 的工具和实践

### 1.2 核心分类

作者将 OpenAI 团队的 Harness 组件按照**确定性方法**和**LLM 方法**混合的方式，归纳为 **3 个类别**：

1. **Context Engineering**（上下文工程）
2. **Architecture Constraints**（架构约束）
3. **Garbage Collection**（垃圾收集）

---

## 二、三个核心领域详解

### 2.1 Context Engineering（上下文工程）

**定义**：
- 代码库中**持续增强的知识库**
- Agent 访问**动态上下文**（可观测性数据、浏览器导航等）

**关键洞察**：
- 不仅是静态文档，还包括运行时数据
- 需要持续维护和更新
- 与 OpenAI 文章中提到的"渐进式披露"理念一致

**对 KuiklyUI 的启示**：
- 知识库需要包含静态规范 + 动态运行时信息
- 考虑如何集成 KuiklyUI 的跨平台运行时数据
- 建立知识库的持续更新机制

---

### 2.2 Architecture Constraints（架构约束）

**定义**：
- 不仅由基于 LLM 的 Agent 监控
- 还包括**确定性的自定义 linter** 和**结构测试**

**关键洞察**：
- 纯 LLM 监控不够可靠，需要确定性工具作为补充
- 自定义 linter 强制执行架构规则
- 结构测试验证模块边界和依赖关系

**对 KuiklyUI 的启示**：
- 需要建设自定义 linter 体系（Kotlin/KMP 生态）
- 定义模块边界规则（跨平台架构）
- 结构测试验证依赖方向（如：common 不能依赖 platform-specific）

**具体实践方向**：
```
Architecture Constraints for KuiklyUI:
├── Lint Rules
│   ├── 模块依赖规则（common → platform-specific 禁止反向依赖）
│   ├── API 可见性规则（internal vs public）
│   └── 平台特定代码标记规则（expect/actual 规范）
├── Structural Tests
│   ├── 架构分层测试（UI layer → Business Logic → Data）
│   └── 跨平台一致性测试（各平台 API 对齐）
└── Custom Linters
    ├── KMP 最佳实践检查
    └── 性能反模式检测
```

---

### 2.3 Garbage Collection（垃圾收集）

**定义**：
- **定期运行的 Agent**，发现文档不一致或架构约束违规
- **对抗熵增和腐烂**

**关键洞察**：
- 代码库会随时间腐烂（entropy and decay）
- 需要主动的"清理"机制
- 不仅检查代码，还检查文档与代码的一致性

**对 KuiklyUI 的启示**：
- 建立定期扫描机制（weekly/monthly）
- 检查项：
  - 文档与代码是否一致
  - 是否有违反架构约束的代码
  - 是否存在重复或废弃代码
- 自动化修复或生成修复建议

---

## 三、迭代式改进理念

### 3.1 核心原则

OpenAI 团队的迭代方法：
> "When the agent struggles, we treat it as a signal: identify what is missing — tools, guardrails, documentation — and feed it back into the repository, always by having Codex itself write the fix."

**翻译**：当 Agent 遇到困难时，将其视为信号：识别缺少什么（工具、防护栏、文档），并将其反馈到代码库中，始终让 Codex 自己编写修复。

### 3.2 对 KuiklyUI 的启示

1. **建立反馈循环**：
   - 收集 AI 开发过程中的痛点
   - 识别缺失的知识或工具
   - 让 AI 参与修复和改进 Harness

2. **持续改进 Harness**：
   - Harness 不是一次性建设完成的
   - 根据实际使用情况不断调整
   - 每次迭代都增强系统能力

---

## 四、关键思考与假设

### 4.1 Harness 会成为未来的服务模板吗？

**观察**：
- 大多数组织只有 2-3 个主要技术栈
- 服务模板帮助团队基于"黄金路径"启动新服务

**预测**：
- Harness（包含自定义 linter、结构测试、基础上下文和知识文档、额外上下文提供者）可能成为新的服务模板
- 团队将其作为起点，然后根据应用 specifics 进行调整

**挑战**：
- 服务模板的更新同步问题：团队贡献经验，但其他团队难以整合更新
- Harness 是否会面临类似的分叉和同步挑战？

**对 KuiklyUI 的启示**：
- 考虑为 KuiklyUI 创建 Harness 模板
- 包含：基础架构、lint 规则、知识库结构、CI/CD 配置
- 设计更新机制，避免分叉过多

---

### 4.2 运行时必须受约束才能提高 AI 自主性？

**早期假设**：
- LLM 提供无限灵活性：任何语言、任何模式都能生成

**现实发现**：
- 为了可维护的、可信任的 AI 生成代码，**必须约束解决方案空间**
- 特定架构模式、强制边界、标准化结构
- 放弃一些"生成任何东西"的灵活性，换取提示词、规则和充满技术细节的 Harness

**对 KuiklyUI 的启示**：
- 明确定义 KuiklyUI 的"约束空间"：
  - 允许的架构模式（MVVM、MVI 等）
  - 强制的模块边界
  - 标准化的代码结构
- 为 AI 提供清晰的"轨道"，而非开放的"旷野"

---

### 4.3 技术栈会收敛到有限数量吗？

**趋势预测**：
- 编码变得不再是打字，而是引导生成
- AI 可能推动我们走向**更少的技术栈**
- 框架和 SDK 的可用性仍然重要（对人类好的对 AI 也好）
- 开发者品味在细节层面变得不那么重要
- 选择具有良好 Harness 的栈，优先考虑"AI 友好性"

**代码库结构和拓扑也会收敛**：
- 我们可能默认选择更容易用 AI 维护的结构
- OpenAI 团队讨论架构刚性和强制执行规则
- 重点领域：保持数据结构稳定、定义和强制执行模块边界

**对 KuiklyUI 的启示**：
- KuiklyUI 作为跨平台框架，需要考虑"AI 友好性"
- 定义清晰的代码库拓扑结构
- 提供标准化的项目模板

---

### 4.4 两个未来世界：AI 前 vs AI 后应用维护？

**问题**：
- 如果我们开发出良好的 Harness 技术，将 AI 自主性提升到 9 并增加对结果的信任
- 哪些技术可以应用于现有应用？
- 哪些只适用于从一开始就考虑 Harness 构建的应用？

**遗留代码挑战**：
- 老代码库可能非标准化、充满熵增
- 改造 Harness 可能不值得
- 类似于在从未运行过静态代码分析工具的代码库上运行分析工具，然后淹没在警报中

**对 KuiklyUI 的启示**：
- **新项目**：从一开始就设计 Harness
- **现有项目**：
  - 评估改造成本和收益
  - 可能需要先进行重构和标准化
  - 渐进式引入 Harness 组件

---

## 五、实践建议

### 5.1 你今天的 Harness 是什么？

作者建议反思：
- 你有 pre-commit hook 吗？里面有什么？
- 你有自定义 linter 的想法吗？
- 你想对代码库施加哪些架构约束？
- 你尝试过 ArchUnit 等结构测试框架吗？

**对 KuiklyUI 的具体建议**：

1. **立即可以做的**：
   - 审查现有的 lint 规则
   - 定义模块边界规则
   - 创建基础的 AGENTS.md

2. **短期（1-2 个月）**：
   - 建设自定义 linter
   - 建立结构测试
   - 创建知识库框架

3. **长期（3-6 个月）**：
   - 完整的 Harness 系统
   - 自动化垃圾收集
   - 度量和反馈循环

---

## 六、与 OpenAI 文章的对比

### 6.1 共同点

- 都强调 Harness 的重要性
- 都提到 Context Engineering、Architecture Constraints、Garbage Collection
- 都认为这是一个迭代过程

### 6.2 Martin Fowler 文章的补充

- **更深入的思考**：提出了 Harness 作为服务模板、技术栈收敛等前瞻性观点
- **质疑和反思**：质疑 OpenAI 的动机（有既得利益），提出遗留代码问题
- **实践导向**：提出"你今天的 Harness 是什么"的具体问题
- **术语澄清**：将 OpenAI 的实践归纳为三个清晰的类别

### 6.3 缺失的部分

作者指出 OpenAI 文章**缺少**的部分：
- **功能和行为的验证**：所有措施都集中在长期内部质量和可维护性上，缺少对功能和行为的验证

**对 KuiklyUI 的补充**：
- 除了 Harness 建设，还需要：
  - 功能测试策略
  - 跨平台一致性验证
  - 端到端测试

---

## 七、关键引用

### 7.1 关于 Harness 定义

> "I like 'harness' as a word to describe the tooling and practices we can use to keep AI agents in check."

**翻译**：我喜欢 "harness" 这个词来描述用于约束 AI Agent 的工具和实践。

### 7.2 关于迭代改进

> "When the agent struggles, we treat it as a signal: identify what is missing — tools, guardrails, documentation — and feed it back into the repository, always by having Codex itself write the fix."

**翻译**：当 Agent 遇到困难时，将其视为信号：识别缺少什么（工具、防护栏、文档），并将其反馈到代码库中，始终让 Codex 自己编写修复。

### 7.3 关于约束与灵活性

> "For maintainable, AI-generated code at scale that we can trust, something has to give."

**翻译**：为了可信任的、可维护的大规模 AI 生成代码，必须有所取舍。

### 7.4 关于复杂性

> "Unsurprisingly, what they describe sounds like much more work than just generating and maintaining a bunch of Markdown rules files."

**翻译**：不出所料，他们描述的听起来比仅仅生成和维护一堆 Markdown 规则文件要复杂得多。

### 7.5 关于严谨性

> "Our most difficult challenges now center on designing environments, feedback loops, and control systems."

**翻译**：我们现在最困难的挑战集中在设计环境、反馈循环和控制系统。

---

## 八、对 KuiklyUI Harness 工程的具体建议

### 8.1 三个核心领域的落地

#### Context Engineering
- [ ] 建立 AGENTS.md 作为入口点
- [ ] 创建 docs/ 目录结构（设计文档、执行计划、参考资料）
- [ ] 集成 KuiklyUI 运行时数据（日志、性能指标）
- [ ] 建立知识库更新机制

#### Architecture Constraints
- [ ] 定义 KuiklyUI 架构规则：
  - 模块依赖方向
  - API 可见性规范
  - 平台特定代码组织
- [ ] 建设自定义 linter：
  - KMP 最佳实践检查
  - 跨平台一致性验证
  - 性能反模式检测
- [ ] 结构测试：
  - 架构分层验证
  - 依赖关系测试

#### Garbage Collection
- [ ] 定期扫描任务：
  - 文档-代码一致性检查
  - 架构约束违规检测
  - 重复/废弃代码识别
- [ ] 自动化修复流程
- [ ] 质量指标监控

### 8.2 迭代路线图

**阶段 1：基础约束（1-2 周）**
- 定义基础架构规则
- 创建 AGENTS.md
- 建立基本 lint 规则

**阶段 2：知识库建设（2-4 周）**
- 填充 docs/ 内容
- 建立更新机制
- 集成运行时上下文

**阶段 3：自动化（4-6 周）**
- 自定义 linter
- 结构测试
- 垃圾收集机制

**阶段 4：优化（持续）**
- 收集反馈
- 迭代改进
- 扩展覆盖范围

### 8.3 关键决策点

1. **约束程度**：多严格的约束？太松难以维护，太严限制灵活性
2. **技术栈选择**：是否需要限制 AI 可选的技术方案？
3. **遗留代码**：现有代码如何适配 Harness？
4. **验证策略**：如何验证 AI 生成代码的功能正确性？

---

## 九、总结

Martin Fowler 的这篇文章提供了对 OpenAI Harness Engineering 的深度解读和批判性思考。核心贡献：

1. **清晰分类**：将 Harness 归纳为 Context Engineering、Architecture Constraints、Garbage Collection 三个领域
2. **前瞻性思考**：提出 Harness 作为服务模板、技术栈收敛等趋势预测
3. **实践导向**：提出具体问题和建议，帮助读者思考自己的 Harness
4. **批判性视角**：质疑 OpenAI 的动机，指出缺失的功能验证部分

**对 KuiklyUI 的核心启示**：
- Harness 建设是复杂的工程，不仅仅是写文档
- 需要约束运行时而非完全开放
- 三个核心领域都需要建设
- 这是一个迭代过程，需要持续改进
- 需要考虑遗留代码的适配问题

---

*精读完成时间: 2026-03-30*  
*原文长度: ~1500 词*  
*精读笔记长度: ~4000 词*
