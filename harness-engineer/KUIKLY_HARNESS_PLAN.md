# KuiklyUI Harness AI 编程工程建设规划

**文档状态**: 草案（待确认）  
**最后更新**: 2026-03-30  
**目标**: 构建支持多 AI 工具的 Harness 工程体系，提升 KuiklyUI 开发效率

---

## 一、支持的 AI 工具（已确认）

| 工具 | 类型 | 特点 | 优先级 |
|------|------|------|--------|
| **Claude Code** | CLI 工具 | Anthropic 官方，深度集成 Claude | P0 |
| **OpenCode** | CLI 工具 | 开源，可扩展性强 | P0 |
| **Cursor** | IDE 插件 | 可视化强，用户体验好 | P0 |
| **CodeBuddy** | IDE 插件 | 腾讯内部工具（假设） | P0 |

**兼容性策略**（待确认）:
- [x] 方案 A: 统一 AGENTS.md 格式，所有工具共用
- [ ] 方案 B: 工具特定格式，各自维护
- [ ] 方案 C: 基础通用 + 工具特定扩展

---

## 二、AI 知识库组织形式（关键决策点）

### 2.1 参考方案 1: OpenAI 渐进式披露模型 ⭐

**来源**: OpenAI Harness Engineering 实践

```
AGENTS.md (约100行) → 内容目录/地图
├── ARCHITECTURE.md (架构顶层地图)
└── docs/ (结构化知识库 - 记录系统)
    ├── design-docs/ (设计文档)
    │   ├── index.md
    │   ├── core-beliefs.md
    │   └── ...
    ├── exec-plans/ (执行计划)
    │   ├── active/ (进行中)
    │   ├── completed/ (已完成)
    │   └── tech-debt-tracker.md (技术债务追踪)
    ├── generated/ (自动生成文档)
    │   └── db-schema.md
    ├── product-specs/ (产品规范)
    │   ├── index.md
    │   └── new-user-onboarding.md
    └── references/ (参考资料)
        ├── design-system-reference-llms.txt
        └── ...
```

**核心原则**:
- AGENTS.md 是地图，不是说明书
- 渐进式披露：从稳定切入点开始，指导下一步去哪里
- 所有知识版本控制，集中存放

**优点**:
- 经过 OpenAI 大规模实践验证
- 解决情境稀缺问题
- 防止知识库腐烂

**待明确问题**:
- [ ] 每个目录的具体维护责任人和流程
- [ ] 更新频率和审核机制
- [ ] 与代码的同步策略
- [ ] 多工具兼容性处理

### 2.2 其他参考方案（待调研）

- [ ] Anthropic 官方推荐结构
- [ ] Cursor Rules 最佳实践
- [ ] 其他开源项目实践

### 2.3 KuiklyUI 定制方案（待确定）

**需要考虑的 KuiklyUI 特殊性**:
- 跨平台（Android/iOS/HarmonyOS/Web/小程序/macOS）
- Kotlin Multiplatform 技术栈
- 开源项目，贡献者众多
- 已有 `.cursor/rules/kuikly.mdc`

**决策点**（需确认）:
1. [ ] 是否完全采用 OpenAI 结构？
2. [ ] 如何整合现有 `.cursor/rules/`？
3. [ ] 跨平台知识如何组织？
4. [ ] 开源协作流程如何适配？

---

## 三、关键借鉴（待补充）

> 注：原第三章节（规范架构与品味详解）已移除，核心结论已转化为 todo 项。
> 
> 从 OpenAI 2.4 节提取的待办事项：
> - [ ] 建设 lint 规则体系
> - [ ] 建设 error 处理知识库
> - [ ] 设计 KuiklyUI 架构分层指引（给 AI 的代码放置规范）

---

## 四、维护机制（关键决策点）

### 4.1 版本控制

**待确认**:
- [x] 所有 AI 配置纳入 Git 管理？
- [ ] 语义化版本控制？
- [ ] 重大变更需要 Code Review？

### 4.2 更新机制

**OpenAI 实践**:
- 专职 linter 和 CI 作业验证
- "doc-gardening" 智能体定期扫描
- 自动发起修复 PR

**KuiklyUI 方案**（待确定）:
- [ ] 更新频率（每月/每迭代）
- [ ] 审核流程
- [ ] 自动化检查范围

### 4.3 防腐烂策略

**OpenAI 方法**:
- 机械检查（覆盖率、新鲜度、所有权、交叉链接）
- 智能体扫描过时文档
- 自动化修复 PR

**KuiklyUI 方案**（待确定）:
- [ ] 检查项清单
- [ ] 自动化工具选择
- [ ] 责任人分配

---

## 六、待确认决策清单

### 高优先级（阻塞实施）

- [ ] **决策 1**: AI 知识库组织形式选择
  - 选项 A: 完全采用 OpenAI 结构
  - 选项 B: 基于 OpenAI 定制
  - 选项 C: 完全自定义

- [ ] **决策 2**: 多工具兼容性策略
  - 选项 A: 统一格式
  - 选项 B: 工具特定
  - 选项 C: 基础 + 扩展

- [ ] **决策 3**: 维护责任人
  - 谁负责 AGENTS.md 更新？
  - 谁负责知识库审核？
  - 自动化维护如何分配？

### 中优先级（可并行）

- [ ] **决策 4**: 目录具体划分
  - docs/ 下子目录定义
  - 每个目录的内容范围
  - 目录间关系

- [ ] **决策 5**: 更新频率
  - 定期回顾周期
  - 触发更新的条件
  - 紧急更新流程

- [ ] **决策 6**: lint 规则范围
  - 哪些规则需要强制执行
  - 自定义 lint 工具选择
  - 与现有工具集成

### 低优先级（后续优化）

- [ ] **决策 7**: 度量指标
  - 效率提升如何度量
  - 质量指标定义
  - 反馈收集机制

- [ ] **决策 8**: 开源协作
  - 外部贡献者如何使用
  - 知识库贡献流程
  - 社区维护机制

## 八、实施 Todo 清单

### P0 - 高优先级（阻塞实施）

- [ ] **设计 AI 知识库组织形式**
  - 参考 OpenAI 渐进式披露模型
  - 适配 KuiklyUI 跨平台特性
  - 确定目录结构和维护机制

- [ ] **建设 lint 规则体系**
  - 基于 OpenAI 2.4 节（规范架构与品味）
  - Kotlin/KMP 生态适配
  - 模块依赖检查
  - API 使用规范

- [ ] **建设 error 处理知识库**
  - 常见错误模式整理
  - 修复指令模板
  - 与 lint 规则联动

- [ ] **设计 KuiklyUI 架构分层指引**
  - 给 AI 的代码放置规范
  - 模块职责定义
  - 依赖方向规则

### P1 - 中优先级（可并行）

- [ ] **创建 AGENTS.md 地图**
  - 约 100 行，作为内容目录
  - 指向结构化知识库
  - 支持多工具兼容

- [ ] **创建 ARCHITECTURE.md**
  - 架构顶层地图
  - 域和包分层说明

- [ ] **整合现有 `.cursor/rules/`**
  - 迁移 kuikly.mdc 内容
  - 统一到新的知识库结构

### P2 - 低优先级（后续优化）

- [ ] **实现 AI 自动修复 bug 能力**
  - 基于 OpenAI 2.6 节
  - Bug 重现 → 修复 → 验证流程
  - 需要完善的工具和测试基础

- [ ] **建设垃圾收集机制**
  - 基于 OpenAI 2.7 节
  - 定期扫描代码漂移
  - 自动化重构 PR

- [ ] **度量体系建设**
  - 效率提升度量
  - 质量指标定义

### 待明确项

- [ ] **是否采用 Anthropic 双 Agent 模式**
  - 背景：Anthropic 文章提出 Initializer + Coding Agent 分离，每次重新开新会话
  - 当前方案：OpenSpec 文件驱动，已满足计划与执行分离的理念
  - 疑问：是否需要每次重新开新会话？还是保持单一 Agent 模式？
  - 决策：暂缓，先用 OpenSpec 实践，后续根据实际效果再决定

---

## 九、OpenAI 文章分析结论

### 2.2 渐进式披露 ⭐⭐⭐ 高度适用
- **结论**: 作为知识库组织的主要参考
- **行动**: 采用 AGENTS.md 作为地图，docs/ 作为记录系统

### 2.3 智能体可读性 ⭐⭐⭐ 高度适用
- **结论**: 所有知识必须编码到代码库
- **行动**: 建立 docs/ 结构化知识库，纳入版本控制

### 2.4 规范架构与品味 ⭐⭐ 部分适用
- **结论**: 通过 lint 强制执行不变量
- **行动**: 建设 lint 规则体系 + error 处理知识库

### 2.5 吞吐量改变合并 ❌ 不适用
- **结论**: 宽松合并策略不适合开源项目
- **原因**: KuiklyUI 需要严格审核保证质量

### 2.6 自主水平提升 ⭐⭐ 未来适用
- **结论**: 端到端自主能力是长期目标
- **行动**: P2 实现 AI 自动修复 bug

### 2.7 熵与垃圾收集 ⭐⭐ 部分适用
- **结论**: 需要机制防止 AI 复用劣质代码
- **行动**: 
  - 定义黄金原则（编码规范、反模式清单）
  - 建立定期清理机制（待确定扫描工具和频率）
  - 自动化重构流程
- **OpenAI 做法**: 后台 Codex 任务定期扫描 → 更新质量等级 → 发起重构 PR → 快速合并
- **待调研**: 具体扫描工具、质量指标、自动化程度

---

## 十、关键决策记录

### 决策 1: 需求开发工作流 ⭐ 已确认

**决策**: 采用 OpenSpec 作为需求开发的工作流

**背景**: 
- 精读 "Effective harnesses for long-running agents" 文章后确认
- OpenSpec 是文件驱动的方法，与 OpenAI 渐进式披露理念一致
- 支持计划与执行分离，符合 Harness Engineering 核心思想

**实施方案**:
1. 产品需求以 OpenSpec 文件形式定义
2. AI 读取 OpenSpec 后生成任务列表
3. 按任务列表逐项实现
4. 每个任务完成后更新状态

**与 Anthropic 双 Agent 模式的关系**:
- Anthropic 提出 Initializer + Coding Agent 分离，每次重新开新会话
- OpenSpec 方案：文件驱动，保持单一 Agent 模式
- **决策**: 暂缓采用双 Agent 模式，先用 OpenSpec 实践，后续根据效果再决定

### 决策 2: 评估器机制 ⭐ 待确认 (P1)

**背景**: 精读 Anthropic "Harness design for long-running apps" 后

**核心发现**: 生成器-评估器分离架构（GAN 启发）显著提升质量
- 生成器倾向于自我肯定，独立评估器可调校得更客观
- 评估标准具象化（将主观判断转为可评分标准）
- Sprint 合约机制（生成器与评估器协商完成标准）

**待决策**:
- [ ] 是否在 KuiklyUI 引入 Code Review Agent 概念？
- [ ] 评估标准如何定义（架构合规、代码质量、测试覆盖）？
- [ ] 成本考量（Anthropic 实验 $200 vs $9，需轻量级实现）

**详细分析**: 见 [research/anthropic-harness-design-long-running-apps-detailed.md](./research/anthropic-harness-design-long-running-apps-detailed.md)

### 思考项 1: 上下文焦虑与会话管理

**来源**: Anthropic 文章

**关键发现**:
- AI 存在"上下文焦虑"：接近上下文限制时提前结束工作
- 解决方案：上下文重置（开启新会话）+ 结构化交接文件
- **但**: Opus 4.6 不再需要上下文重置，新压缩技术足够好

**待思考问题**:
- [ ] KuiklyUI 开发场景是否需要长会话？
- [ ] 如果需要，什么时候触发新会话？
- [ ] 如何设计状态交接文件格式？
- [ ] OpenSpec 文件驱动是否已部分解决此问题？

### 思考项 2: OpenSpec 与 Sprint 合约机制的借鉴

**来源**: Anthropic 文章

**Anthropic 机制**:
- 规划器不指定很细的实现细节
- 生成器提议：要构建什么 + 如何验证成功
- 评估器审核：确保生成器构建正确的东西
- 两者对抗协商，达成一致后开始编码

**OpenSpec 借鉴思考**:
- [ ] OpenSpec 是否应增加"验收标准"章节？
- [ ] AI 实现功能后如何自检是否符合标准？
- [ ] 是否需要引入"生成器-评估器"对抗机制到 OpenSpec 工作流？
- [ ] 如何平衡规划的细节程度？（太细容易错，太粗容易偏）

---

## 十一、Martin Fowler Harness Engineering 分析结论

**来源**: [Martin Fowler - Harness Engineering](https://martinfowler.com/articles/exploring-gen-ai/harness-engineering.html) (2026-02-17)

### 11.1 Harness 三领域模型

**结论**: Harness 应包含三个核心领域（Context Engineering、Architecture Constraints、Garbage Collection），混合使用确定性工具和 LLM 方法。

**出处**: Martin Fowler 对 OpenAI 实践的分类归纳

---

### 11.2 Context Engineering（上下文工程）

**结论**: 知识库应包含静态文档 + 动态运行时数据，而非仅静态文档。

**出处**: "Continuously enhanced knowledge base in the codebase, plus agent access to dynamic context like observability data and browser navigation"

**KuiklyUI 具体化**:
- **静态知识库**: AGENTS.md（地图）+ docs/ 目录（设计文档、执行计划、参考资料）+ ARCHITECTURE.md
- **动态运行时数据**: 构建日志、跨平台运行时日志、性能指标、测试覆盖率数据

**待明确**:
- [ ] 动态数据如何采集和更新？
- [ ] 哪些运行时数据对 AI 最有价值？

---

### 11.3 Architecture Constraints（架构约束）

**结论**: 需要确定性工具（自定义 linter、结构测试）补充 LLM 监控，不能仅靠 LLM。

**出处**: "Monitored not only by the LLM-based agents, but also deterministic custom linters and structural tests"

**KuiklyUI 核心规则（P0）**:
- [ ] **模块依赖规则**: Render 层、Core 层独立，禁止编译依赖（待完善具体规则）
- [ ] **API 可见性规范**: internal vs public 使用规范
- [ ] **平台特定代码组织**: expect/actual 规范

**实践方向**:
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

### 11.4 Garbage Collection（垃圾收集）

**结论**: 需要定期运行的机制检查文档-代码一致性、架构约束违规、重复/废弃代码，对抗熵增。

**出处**: "Agents that run periodically to find inconsistencies in documentation or violations of architectural constraints, fighting entropy and decay"

**KuiklyUI 实现方案（P0）**:
- [ ] **文档-代码一致性扫描**: 提取 public API 与文档对比
- [ ] **架构约束违规扫描**: 使用 ArchUnit 等静态分析工具（Kotlin 生态）
- [ ] **重复代码扫描**: 使用 PMD CPD 等相似度检测工具
- [ ] **废弃代码扫描**: 检测未被引用的 public 方法

**待明确**:
- [ ] 扫描频率（weekly/monthly）
- [ ] 自动化修复流程
- [ ] 质量指标定义

---

### 11.5 迭代式改进与反馈循环

**结论**: 当 AI 遇到困难时，应识别缺少什么（工具、防护栏、文档），并让 AI 自己编写修复。

**出处**: "When the agent struggles, we treat it as a signal: identify what is missing — tools, guardrails, documentation — and feed it back into the repository, always by having Codex itself write the fix."

**落实方案**:
1. **人工发现 + AI 修复**: 团队在日常开发中记录 AI 困难点，定期让 AI 生成修复
2. **自动化收集**: 在 AI 开发过程中自动记录错误和重试，分析模式
3. **定期回顾**: 每周/每月回顾 AI 开发痛点，更新 Harness

**关键**: 需要团队共识和规范，主动人工发现是主要方式

---

### 11.6 Harness 作为服务模板

**结论**: Harness（包含自定义 linter、结构测试、知识文档、上下文提供者）可能成为新的服务模板，团队作为起点后根据 specifics 调整。

**出处**: "Will harnesses — with custom linters, structural tests, basic context and knowledge documentation, and additional context providers — become the new service templates?"

**KuiklyUI Harness 模块组成（待完善）**:
```
KuiklyUI Harness Template:
├── 知识库（Knowledge Base）
│   ├── AGENTS.md（入口地图）
│   ├── docs/（结构化文档）
│   └── 运行时数据集成
├── 架构约束（Architecture Constraints）
│   ├── Lint 规则
│   ├── 结构测试
│   └── 模块边界定义
├── 自动化工具（Automation）
│   ├── 垃圾收集
│   ├── 反馈循环
│   └── CI/CD 集成
└── 项目模板（Project Template）
    ├── 标准目录结构
    ├── 基础配置
    └── 示例代码
```

---

### 11.7 运行时约束与灵活性取舍

**结论**: 为了可维护的 AI 生成代码，必须约束解决方案空间（特定架构模式、强制边界、标准化结构），放弃"生成任何东西"的灵活性。

**出处**: "For maintainable, AI-generated code at scale that we can trust, something has to give... constraining the solution space: specific architectural patterns, enforced boundaries, standardized structures"

**KuiklyUI 约束定义（与 11.3 呼应）**:
- 允许的架构模式（MVVM、MVI 等）
- 强制的模块边界（Render/Core 独立）
- 标准化的代码结构

---

### 11.8 代码库拓扑结构

**结论**: 我们可能默认选择更容易用 AI 维护的结构，但目前没有具体推荐拓扑。

**出处**: "We might default to structures that are easier to maintain with AI because they're easier to harness"

**现状**: Martin Fowler 和 OpenAI 文章都未给出具体代码库拓扑推荐，只是提出趋势。

**KuiklyUI 方案**:
- 参考 KMP 官方推荐结构
- 参考其他跨平台框架（React Native、Flutter）
- 自行设计适合 AI 维护的拓扑结构

**待完成**:
- [ ] 设计 KuiklyUI 标准项目结构
- [ ] 定义模块边界和依赖规则
- [ ] 创建项目模板

---

## 十二、HumanLayer "Skill Issue: Harness Engineering" 分析结论

**来源**: [HumanLayer - Skill Issue: Harness Engineering for Coding Agents](https://www.humanlayer.dev/blog/skill-issue-harness-engineering-for-coding-agents) (2026-03-12)

### 12.1 Agent 配置问题 vs 模型问题

**结论**: Agent 失败通常是配置问题，而非模型问题。通过改进配置（CLAUDE.md/AGENTS.md、MCP 服务器、Skills、子 Agent、Hooks、Back-pressure）可以解决大部分问题。

**出处**: "Agent failures are configuration problems, not model problems"

---

### 12.2 子 Agent 作为上下文防火墙 ⭐⭐⭐ 高度适用

**结论**: 子 Agent 最有效的用途是隔离上下文，防止主 Agent 的上下文被污染。通过子 Agent 执行搜索/查找/总结任务，父 Agent 保持干净的上下文窗口直接开始工作。

**出处**: "Sub-agents are about context control... use a fresh context window for lookup/search/summarize, enabling the parent to start working directly without polluting its context window with Glob/Grep/Read calls"

**KuiklyUI 实践建议**:
- 使用子 Agent 进行代码库搜索和文件查找
- 使用子 Agent 生成研究文档和总结
- 主 Agent 专注于实现，保持上下文干净

**效果评估**: ⭐⭐⭐ 高 - 上下文隔离效果显著

---

### 12.3 AGENTS.md 配置效果存疑

**结论**: 多数项目的 AGENTS.md 配置不佳，需要进一步研究最佳实践。

**出处**: 文中提到 "Most projects configure AGENTS.md poorly"，并链接到 Mitchell Hashimoto 的文章需要精读

**待完成**:
- [ ] 精读 Mitchell Hashimoto "My AI Adoption Journey" 中关于 AGENTS.md 的部分
- [ ] 参考 Ghostty 的 AGENTS.md 实践
- [ ] 总结 KuiklyUI 的 AGENTS.md 最佳实践

**效果评估**: ❓ 待验证 - 需要更多研究和实践

---

### 12.4 其他 Harness 配置点评估

基于精读后的评估：

| 配置点 | 效果评估 | 说明 |
|--------|----------|------|
| 子 Agent | ⭐⭐⭐ 高 | 上下文隔离效果显著 |
| AGENTS.md | ❓ 待验证 | 多数项目配置不佳，需进一步研究 |
| MCP 服务器 | - | 未在精读中重点提及 |
| Skills | - | 未在精读中重点提及 |
| Hooks | - | 未在精读中重点提及 |
| Back-pressure | - | 未在精读中重点提及 |

---

## 十三、HumanLayer "Advanced Context Engineering" 分析结论

**来源**: [HumanLayer - Advanced Context Engineering for Coding Agents](https://www.humanlayer.dev/blog/advanced-context-engineering) (2025-08-29)

### 13.1 频繁有意压缩（Frequent Intentional Compaction）⭐⭐⭐ 高度适用

**结论**: 当上下文占用达到 40-60%，或感觉 AI 变蠢时，应主动压缩上下文到文件并开启新会话。

**出处**: "Keep utilization in the 40-60% range... designing your entire development process around context management"

**团队提示词（Prompt）**:
```
把我们到目前为止做的所有事情写到 progress.md，确保记录：
- 最终目标
- 我们采用的方法
- 已完成的步骤
- 我们正在处理的当前失败
```

**KuiklyUI 实践建议**:
- 监控上下文利用率，保持在 40-60% 范围
- 当 AI 响应质量下降时触发压缩
- 使用结构化文件记录进度（progress.md）
- 压缩后开启新会话继续工作

**效果评估**: ⭐⭐⭐ 高 - 显著提升 AI 在复杂代码库中的表现

---

### 13.2 Research → Plan → Implement 工作流 ⭐⭐⭐ 高度适用

**结论**: 采用三阶段工作流：研究（Research）→ 规划（Plan）→ 实现（Implement），每个阶段都有明确的提示词和输出要求。

**出处**: HumanLayer 团队在 BAML 30 万行 Rust 代码库上的实践经验

**参考提示词**:

**1. 研究阶段（Research）**
- 理解代码库、相关文件、信息流动
- 识别问题潜在原因
- 输出：研究文档（research.md）
- 参考: https://github.com/humanlayer/humanlayer/blob/main/.claude/commands/research_codebase.md

**2. 规划阶段（Plan）**
- 概述修复问题的确切步骤
- 明确需要编辑的文件和方式
- 定义每个阶段的测试/验证步骤
- 输出：实施计划（plan.md）
- 参考: https://github.com/humanlayer/humanlayer/blob/main/.claude/commands/create_plan.md

**3. 实现阶段（Implement）**
- 逐步执行计划
- 复杂工作在每阶段验证后压缩回计划文件
- 输出：代码变更 + 更新后的计划
- 参考: https://github.com/humanlayer/humanlayer/blob/main/.claude/commands/implement_plan.md

**关键原则**:
- 人类审查应集中在高杠杆部分（Research 和 Plan）
- 代码审查（Implement）相对低杠杆
- Research 中的一行错误可能导致数千行错误代码
- Plan 中的一行错误可能导致数百行错误代码

---

### 13.3 Markdown 文件管理机制

**结论**: HumanLayer 团队开发了特定的 Markdown 文件管理方法（称为 "thoughts tool"），用于在 AI 会话间共享和更新文档。

**出处**: "How we manage/share markdown files... ask 'thoughts tool' how it works"

**待研究**:
- [ ] 研究 humanlayer/humanlayer 仓库中的 Markdown 管理机制
- [ ] 了解 "thoughts tool" 的具体实现
- [ ] 评估是否适用于 KuiklyUI 的工作流

**相关资源**:
- 仓库: https://github.com/humanlayer/humanlayer
- 研究示例: https://github.com/ai-that-works/ai-that-works/tree/main/2025-08-05-advanced-context-engineering-for-coding-agents/thoughts/shared/research
- 计划示例: https://github.com/ai-that-works/ai-that-works/tree/main/2025-08-05-advanced-context-engineering-for-coding-agents/thoughts/shared/plans

---

## 十四、HumanLayer "Writing a good CLAUDE.md" 分析结论 ⭐⭐⭐ 关键参考

**来源**: [HumanLayer - Writing a good CLAUDE.md](https://www.humanlayer.dev/blog/writing-a-good-claude-md) (2025-11-25)  
**重要性**: ⭐⭐⭐ **编写 KuiklyUI AGENTS.md 时必须审查的核心规则**

### 14.1 核心原则：少即是多

**结论**: `CLAUDE.md`/`AGENTS.md` 应该尽可能小。前沿模型只能遵循 ~150-200 条指令，而 Claude Code 系统提示已占用 ~50 条。

**关键数据**:
- 前沿思考型 LLM：~150-200 条指令
- Claude Code 系统提示：~50 条指令
- HumanLayer 实践：< 60 行
- 普遍共识：< 300 行，越短越好

**出处**: "Frontier thinking LLMs can follow ~ 150-200 instructions with reasonable consistency"

---

### 14.2 渐进式披露（Progressive Disclosure）⭐⭐⭐

**结论**: 将任务特定指令保存在单独文件中，而非全部塞进 `CLAUDE.md`。

**推荐结构**:
```
agent_docs/
  |- building_the_project.md
  |- running_tests.md
  |- code_conventions.md
  |- service_architecture.md
  |- database_schema.md
```

**在 `CLAUDE.md` 中**:
```markdown
For TypeScript conventions, see docs/TYPESCRIPT.md
```

**出处**: "Prefer pointers to copies"

---

### 14.3 不要用 LLM 做 Linter 的工作

**结论**: 代码风格指南不应放在 `CLAUDE.md` 中。使用确定性工具（linter/formatter），可以用 Hooks 在停止时自动运行。

**推荐做法**:
- 使用 Biome 等可自动修复的 linter
- 设置 Claude Code `Stop` hook 运行 formatter & linter
- 创建 Slash Command 处理格式化

**出处**: "Never send an LLM to do a linter's job"

---

### 14.4 不要自动生成 `CLAUDE.md`

**结论**: `CLAUDE.md` 是 Harness 的最高杠杆点，影响工作流程的每个阶段。应仔细精心设计，而非使用 `/init` 自动生成。

**出处**: "CLAUDE.md is the highest leverage point of the harness"

---

### 14.5 为什么 Claude 经常忽略 `CLAUDE.md`

**原因**: Claude Code 注入系统提醒：
```
<system-reminder>
  IMPORTANT: this context may or may not be relevant to your tasks.
  You should not respond to this context unless it is highly relevant to your task.
</system-reminder>
```

**启示**: 内容必须**普遍适用**，否则会被忽略。

---

### 14.6 KuiklyUI AGENTS.md 编写检查清单

**编写时必须审查**:
- [ ] 文件长度 < 60 行（理想）或 < 300 行（上限）
- [ ] 只包含普遍适用的指令
- [ ] 不包含代码风格指南（移到单独文件或用 linter）
- [ ] 使用渐进式披露，指向其他文档
- [ ] 优先使用 `file:line` 引用而非代码片段
- [ ] 不是自动生成的
- [ ] 包含：一句话项目描述 + 包管理器 + 非标准构建命令

**参考文档**: [research/humanlayer-writing-a-good-claude-md-translation.md](./research/humanlayer-writing-a-good-claude-md-translation.md)

---

## 十五、新增 P0 任务项

基于以上分析，新增以下 P0 任务：

- [ ] **设计 KuiklyUI 架构约束规则**
  - 模块依赖规则（Render/Core 独立）
  - API 可见性规范
  - 平台特定代码组织规范

- [ ] **实现垃圾收集扫描机制**
  - 文档-代码一致性扫描
  - 架构约束违规扫描
  - 重复/废弃代码扫描

- [ ] **设计 KuiklyUI 项目模板和拓扑结构**
  - 标准目录结构
  - 模块边界定义
  - 依赖规则

- [ ] **精读 Mitchell Hashimoto 文章并总结 AGENTS.md 最佳实践**
  - 重点学习 Ghostty 的 AGENTS.md 配置
  - 提炼可复用的配置模式
  - 制定 KuiklyUI 的 AGENTS.md 编写指南

- [ ] **研究 humanlayer/humanlayer 仓库的 Markdown 管理机制**
  - 了解 "thoughts tool" 实现
  - 评估在 KuiklyUI 中的适用性
  - 设计 KuiklyUI 的文档管理方案

- [ ] **制定 KuiklyUI 的 Research → Plan → Implement 工作流**
  - 适配三阶段提示词到 KuiklyUI 场景
  - 定义每个阶段的输出格式
  - 设计进度压缩和会话切换机制

- [ ] **编写 KuiklyUI AGENTS.md（需审查 AGENTS.md 编写指南）**
  - 参考以下文档：
    - [research/humanlayer-writing-a-good-claude-md-translation.md](./research/humanlayer-writing-a-good-claude-md-translation.md) - HumanLayer 的 CLAUDE.md 最佳实践
    - [research/aihero-complete-guide-to-agents-md-translation.md](./research/aihero-complete-guide-to-agents-md-translation.md) - AI Hero 的 AGENTS.md 完全指南
  - 文件长度 < 60 行
  - 使用渐进式披露
  - 不包含代码风格指南

---

*文档版本: v0.8-draft*  
*创建时间: 2026-03-30*  
*最后更新: 2026-03-30*  
*状态: 待评审*
