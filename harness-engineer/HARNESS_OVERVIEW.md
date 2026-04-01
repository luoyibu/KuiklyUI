# KuiklyUI Harness 工程建设总览

**文档定位**: 整体方案地图，每个模块有独立的详细规划文档
**文档状态**: 草案
**最后更新**: 2026-03-31

---

## 什么是 Harness 工程？

> Agent = Model + Harness
>
> 模型提供智能，Harness 让智能变得有用。

Harness 是围绕 AI 模型构建的一套系统：知识库、工作流规范、架构约束、自动化工具。它决定了 AI 在你的代码库中能干多少、干得多好。


## 整体架构

```
KuiklyUI Harness
├── 模块 1：知识库管理          ← AI 上下文的基础
├── 模块 2：开发工作流          ← 需求研发的标准流程
├── 模块 3：问题调试工作流      ← Bug 跟进的标准流程
├── 模块 4：上下文管理策略      ← 保持 AI 状态的方法
├── 模块 5：架构约束与 Lint     ← 防止 AI 写坏代码的护栏
├── 模块 6：定期扫描机制        ← 对抗知识库腐烂和代码熵增
└── 模块 7：团队使用要点        ← 团队成员的实践指南
```

---

## 模块 1：知识库管理

**状态**：✅ 已完成
**目标**：让 AI 每次会话都能快速理解 KuiklyUI 的架构、规范和当前状态。

### 设计参考

- [A Complete Guide to AGENTS.md](https://www.aihero.dev/a-complete-guide-to-agents-md) — 渐进式披露、< 60 行原则、少即是多
- [Writing a Good CLAUDE.md](https://www.humanlayer.dev/blog/writing-a-good-claude-md) — 触发条件写法、只放代码管不了的东西
- [Harness Engineering](https://openai.com/zh-Hans-CN/index/harness-engineering/)（OpenAI）— 知识库组织形式参考（我们采用了渐进式披露思路，但结构有所不同）

### 核心原则

- **少即是多**：根目录 `AGENTS.md` 只放普遍适用内容，控制在合理范围内
- **渐进式披露**：`AGENTS.md` 是地图 + 路由表，细节放在 `.ai/` 子文档中；子文档前 5 行写场景说明，AI 按需读取
- **只放代码管不了的东西**：能用 lint 强制的规范，不要放进 AGENTS.md
- **主动触发**：每份子文档前 5 行明确写「以下场景读取本文件」，AI 先读前 5 行判断相关性再决定是否读完整内容

### 多工具兼容策略

| 工具 | 读取文件 | 说明 |
|------|---------|------|
| **Claude Code** | `CLAUDE.md` | 软链接到 `AGENTS.md` |
| **OpenCode** | `AGENTS.md` | 无 AGENTS.md 时回退读 `CLAUDE.md` |
| **CodeBuddy** | `CODEBUDDY.md` | 无 CODEBUDDY.md 时回退读 `AGENTS.md` |
| **Cursor** | `AGENTS.md` | 支持根目录及子目录 |

**实现**：`AGENTS.md` 为真实文件，`CLAUDE.md` 为软链接（`ln -s AGENTS.md CLAUDE.md`）。

### 已实施的知识库结构

```
AGENTS.md                          # 根目录入口（模块速查 + 依赖边界 + 知识库索引）
CLAUDE.md -> AGENTS.md             # 软链接，Claude Code 兼容
.ai/
├── architecture/
│   └── AGENTS.md                  # 三层架构、模块结构、依赖关系、核心类
├── coding-standards/
│   └── AGENTS.md                  # 版权头、包名规则、KR 命名、日志规范
├── self-dsl/
│   └── AGENTS.md                  # 自研 DSL 开发指南（组件 API 索引到官网文档）
├── compose-dsl/
│   └── AGENTS.md                  # Compose DSL 开发指南（包名规则、架构原理）
└── references/
    ├── AGENTS.md                  # references 路由表
    ├── common-errors.md           # 错误路由表
    ├── errors/
    │   ├── self-dsl-errors.md     # 自研 DSL 专属错误
    │   ├── compose-errors.md      # Compose DSL 专属错误
    │   └── kmp-errors.md          # KMP 通用错误
    ├── native-bridge.md           # 原生扩展开发指南
    ├── native-bridge-internals.md # 通信原理深度参考
    ├── lazy-scroll.md             # LazyList/Grid 滚动机制
    ├── nested-scroll.md           # 嵌套滚动实现原理
    └── publish.md                 # 发布管理
```

---

## 模块 2：开发工作流（需求研发）

**目标**：用标准化流程管理 AI 驱动的功能开发，避免 AI 一次性乱来。

### 核心工作流：Research → Plan → Implement

```
1. Research 阶段
   - 理解代码库相关部分、信息流动
   - 识别潜在影响范围
   - 输出：research.md（存放在任务目录）

2. Plan 阶段（OpenSpec）
   - 定义要构建什么
   - 明确每步的验证方式（验收标准）
   - 人工审查此阶段（高杠杆点）
   - 输出：plan.md / OpenSpec 文件

3. Implement 阶段
   - 按计划逐项实现
   - 每个子任务完成后更新状态
   - 遇到阻碍时压缩上下文、记录进度
   - 输出：代码 + 更新后的 plan.md
```

**关键原则**：
- Plan 阶段一行错误 → 数百行错误代码；Research 阶段一行错误 → 数千行错误代码
- 人工审查集中在 Research 和 Plan，Implement 相对低杠杆
- 子任务粒度：每个任务可在一个会话内完成

### OpenSpec 文件格式（待设计）

```markdown
# [功能名称] OpenSpec

## 背景
## 目标
## 技术方案
## 任务列表
  - [ ] 子任务 1
  - [ ] 子任务 2
## 验收标准
## 不在范围内
```

### 待完成

- [ ] 制定 OpenSpec 文件模板（含验收标准章节）
- [ ] 制定 Research / Plan slash command 提示词
- [ ] 明确"压缩触发条件"（上下文用量达到多少时）

---

## 模块 3：问题调试工作流

**目标**：标准化 Bug 排查流程，避免 AI 乱猜、循环修复。

### 核心工作流

```
1. 问题定义
   - 明确复现步骤
   - 明确预期行为 vs 实际行为
   - 记录到 bug-report.md

2. 根因分析
   - 子 Agent 搜索相关代码（隔离上下文）
   - 形成假设 → 验证假设
   - 记录到 debug-notes.md

3. 修复与验证
   - 实现最小化修复
   - 在所有平台验证（Android/iOS/HarmonyOS）
   - 写回归测试

4. 知识沉淀
   - 将新发现的错误模式更新到 docs/references/common-errors.md
   - Harness 迭代改进信号
```

**关键原则**：
- 不要让主 Agent 做大量搜索，用子 Agent 隔离上下文
- Bug 修复后要问：这个错误的根因是 Harness 配置不足导致的吗？

### 待完成

- [ ] 制定 debug slash command 提示词模板
- [ ] 建立 `docs/references/common-errors.md` 初始内容

---

## 模块 4：上下文管理策略

**目标**：让 AI 在长任务中保持高质量输出，避免"上下文焦虑"导致的提前放弃或质量下降。

### 核心策略：频繁有意压缩（Frequent Intentional Compaction）

```
监控上下文用量
  ↓ 达到 40-60% 时
主动压缩：将进度写入文件
  ↓
开启新会话，读取进度文件继续
```

### 进度压缩提示词

```
把我们到目前为止做的所有事情写到 progress.md，确保记录：
- 最终目标
- 我们采用的方法
- 已完成的步骤
- 当前正在处理的问题或阻碍
```

### 子 Agent 用作上下文防火墙

- **用子 Agent 做**：代码搜索、文件查找、文档总结
- **主 Agent 专注**：实现逻辑，保持上下文干净
- 好处：避免大量 Grep/Read 污染主 Agent 上下文窗口

### 待完成

- [ ] 制定进度文件（progress.md）标准格式
- [ ] 在 AGENTS.md 中说明何时压缩上下文

---

## 模块 5：架构约束与 Lint

**目标**：用确定性工具（而非提示词）强制执行架构规则，防止 AI 写出违反设计的代码。

**核心原则**：不要让 LLM 做 Linter 的工作。

### KuiklyUI 核心约束（待完善具体规则）

```
架构层面
├── 模块依赖规则：Render 层 / Core 层独立，禁止反向依赖
├── 平台代码规范：expect/actual 使用规范
└── API 可见性规范：internal vs public 使用边界

代码质量层面
├── KMP 最佳实践（禁止平台特定 API 进入公共模块）
├── 性能反模式检测（如不必要的对象创建）
└── 废弃 API 使用检查
```

### 实现方式

| 约束类型 | 工具 | 优先级 |
|---------|------|--------|
| 模块依赖检查 | ArchUnit（Kotlin 生态） | P0 |
| 代码风格 | ktlint / detekt | P0 |
| 自定义架构规则 | detekt 自定义规则 | P1 |
| CI 集成 | GitHub Actions | P1 |

### Claude Code Hook 集成

在 AI 停止工作前自动运行 lint，确保每次 AI 修改后代码都符合规范：

```json
{
  "hooks": {
    "Stop": ["./scripts/lint-check.sh"]
  }
}
```

### 待完成

- [ ] 定义 KuiklyUI 模块依赖规则（具体边界）
- [ ] 配置 detekt + ktlint 基础规则集
- [ ] 实现核心自定义 lint 规则（模块依赖、API 可见性）
- [ ] 配置 Claude Code Stop Hook 自动运行 lint

---

## 模块 6：定期扫描机制（垃圾收集）

**目标**：定期检测文档腐烂、架构违规、废弃代码，对抗 AI 辅助开发中的熵增。

**触发信号**：当 AI 遇到困难，先问「是 Harness 配置不足导致的吗？」，然后让 AI 自己编写修复。

### 扫描类型

| 扫描类型 | 内容 | 频率 | 工具 |
|---------|------|------|------|
| 文档-代码一致性 | public API 与文档是否同步 | 月度 | 待定 |
| 架构约束违规 | lint 规则之外的结构性问题 | 迭代结束时 | ArchUnit |
| 重复代码 | 相似代码块检测 | 月度 | PMD CPD |
| 废弃代码 | 未被引用的 public 方法 | 月度 | 待定 |
| Harness 有效性 | AI 频繁出错的模式收集 | 月度 | 人工 + 日志 |

### Harness 迭代改进循环

```
日常开发中 AI 遇到困难
    ↓
记录：什么任务 / 什么错误 / 重试了几次
    ↓
每月回顾：分析 AI 困难点模式
    ↓
识别缺失：工具？文档？lint 规则？
    ↓
让 AI 生成修复 → 合入 Harness
    ↓
下次相同场景表现更好
```

### 待完成

- [ ] 建立 AI 困难点记录机制（格式待定）
- [ ] 制定月度 Harness 回顾流程
- [ ] 配置 ArchUnit 结构性测试
- [ ] 评估文档-代码一致性扫描工具

---

## 模块 7：团队使用要点

**目标**：确保团队成员能有效使用 Harness，发挥最大价值。

### 每个成员都需要了解的

**1. 从哪里开始？**
- 每次新会话，AI 会自动读取 `AGENTS.md`
- 复杂任务请先告诉 AI 用 Research → Plan → Implement 流程

**2. 什么时候该管 AI？**
- Research 产出后：检查理解是否正确
- Plan 产出后：审查方案是否合理（最高杠杆点）
- Implement 过程中：AI 卡住时引导一下，不要等它自己猜

**3. 如何判断 AI 在走弯路？**
- 同一个错误反复出现
- 输出代码不符合 KuiklyUI 模块规范
- 修改了不该修改的层（如 Render 层依赖了 Core 层）

**4. 发现问题怎么处理？**
- 及时修正，并考虑是否要更新 `docs/references/common-errors.md`
- 如果是 AI 反复犯的错，升级为 lint 规则或文档改进

### 工具使用优先级

| 场景 | 推荐工具 |
|------|---------|
| 新功能开发 | Claude Code / OpenCode（配合 OpenSpec 流程） |
| 代码审查 | Cursor（可视化 diff 体验好） |
| 快速问答 | 任意工具 |
| 跨文件重构 | Claude Code（多文件编辑能力强） |

### 待完成

- [ ] 编写团队 Onboarding 文档（新成员如何上手 Harness）
- [ ] 制定 AI 使用最佳实践示例（好的 prompt vs 坏的 prompt）
- [ ] 收集团队初期使用反馈，持续改进

---

## 实施路线图

### Phase 1：基础建设（优先）

| 任务 | 模块 | 预估工作量 |
|------|------|-----------|
| 编写 AGENTS.md | 模块 1 | 小 |
| 编写 ARCHITECTURE.md | 模块 1 | 中 |
| 建立 docs/ 目录结构 | 模块 1 | 中 |
| 制定 OpenSpec 模板 | 模块 2 | 小 |
| 配置 detekt + ktlint | 模块 5 | 中 |

### Phase 2：流程固化

| 任务 | 模块 | 预估工作量 |
|------|------|-----------|
| 实现核心 lint 规则 | 模块 5 | 大 |
| 制定 debug 工作流提示词 | 模块 3 | 小 |
| 配置 Claude Code Hook | 模块 5 | 小 |
| 建立错误知识库初始内容 | 模块 3 | 中 |

### Phase 3：持续优化

| 任务 | 模块 | 预估工作量 |
|------|------|-----------|
| 配置定期扫描 CI 作业 | 模块 6 | 中 |
| 建立 Harness 迭代改进循环 | 模块 6 | 中 |
| 编写团队 Onboarding 文档 | 模块 7 | 中 |

---

## 已确认的关键决策

| 决策 | 结论 | 依据 |
|------|------|------|
| 需求开发工作流 | 采用 OpenSpec 文件驱动 | Anthropic 长运行 Agent 实践 |
| 知识库组织 | 渐进式披露（AGENTS.md + docs/） | OpenAI Harness Engineering |
| AGENTS.md 规模 | < 60 行，只含普遍适用内容 | HumanLayer 最佳实践 |
| lint 策略 | 用确定性工具，不让 LLM 做 linter | Martin Fowler / HumanLayer |
| 多工具兼容 | 统一 AGENTS.md 格式 | 兼容 Claude Code / Cursor / OpenCode |

## 待确认的关键决策

| 决策 | 选项 | 阻塞项 |
|------|------|-------|
| 是否引入评估器机制 | 生成器-评估器双 Agent vs 单 Agent | 成本评估 |
| 上下文压缩触发时机 | 40-60% / 手动触发 / 基于质量判断 | 实践验证 |
| 扫描自动化程度 | 全人工 / 部分自动化 / 全自动 | 工具调研 |

## 知识库建设 TODO

### 已完成
- [x] 确定多工具兼容策略（`AGENTS.md` 主文件，`CLAUDE.md` 软链接）
- [x] 编写根目录 `AGENTS.md` 初稿
- [x] 建立 `.ai/` 目录结构
- [x] 迁移 `architecture.md` + `project-structure.md` → `.ai/architecture/AGENTS.md`
- [x] 迁移并精简 `coding-standards.md` → `.ai/coding-standards/AGENTS.md`
- [x] 迁移 `lazy-scroll-contentsize.md` → `.ai/references/lazy-scroll.md`
- [x] 迁移 `nested-scroll.md` → `.ai/references/nested-scroll.md`
- [x] 建立 `.ai/references/AGENTS.md` 子目录索引

### 待完成
- [x] 迁移 `patterns/self-dsl.md` → `.ai/self-dsl/AGENTS.md`
- [x] 迁移 `patterns/compose-dsl.md` → `.ai/compose-dsl/AGENTS.md`
- [x] 迁移 `patterns/native-bridge.md` → `.ai/references/native-bridge.md`
- [x] 旧 `.ai/index.md` 核心原则提炼到规划文档，原文件可删
- [x] 旧 `.ai/architecture.md` 多版本构建说明迁移到 `AGENTS.md`，原文件可删
- [x] 建立 `.ai/references/common-errors.md`（整合 kuikly.mdc 错误章节）
- [x] 创建 `CLAUDE.md` 软链接（`ln -s AGENTS.md CLAUDE.md`）
- [ ] 将 coding-standards 关键规则（版权头、包名）内联到 AGENTS.md（待评估）

### 待调研 TODO
- [ ] 调研 CodeX Plugin / Claude Code 对抗性审查机制，评估是否引入 KuiklyUI code review 流程

## 知识库维护规范

新增或修改 `.ai/` 下的文档时，每份文档**前 5 行必须包含场景说明**（`> 以下场景读取本文件：...`），确保：

- 写**触发条件**，不写功能摘要——描述什么情况下需要读这个文件
- 列举具体触发关键词：类名、API 名、报错症状
- 覆盖同义词和症状描述——用户不一定用技术术语
- 至少列出 3 个触发场景
- 禁止写流程摘要（"第一步...第二步..."）

---

*文档版本: v1.0-draft*
*创建时间: 2026-03-31*
*替代文档: KUIKLY_HARNESS_PLAN.md（旧版，保留供参考）*
