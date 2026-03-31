# AGENTS.md 完全指南（中文翻译）

**来源**: https://www.aihero.dev/a-complete-guide-to-agents-md  
**作者**: AI Hero  
**日期**: January 18, 2026

---

你是否曾经担心过 `AGENTS.md` 文件的大小？

也许你应该担心。一个糟糕的 `AGENTS.md` 文件可能会让你的 Agent 困惑，成为维护噩梦，并在每次请求上花费你的 token。

所以你最好知道如何修复它。

---

## 什么是 AGENTS.md？

`AGENTS.md` 文件是一个你提交到 Git 的 Markdown 文件，用于自定义 AI 编码 Agent 在你的仓库中的行为方式。它位于对话历史的顶部，就在系统提示词下方。

可以将其视为 Agent 基础指令和实际代码库之间的配置层。该文件可以包含两种类型的指导：

- **个人范围**：你的提交风格偏好、你喜欢的编码模式
- **项目范围**：项目是做什么的、你使用哪个包管理器、你的架构决策

`AGENTS.md` 文件是一个开放标准，受到许多（虽然不是所有）工具的支持。

<details>
  <summary>CLAUDE.md</summary>

值得注意的是，Claude Code 不使用 `AGENTS.md`——它使用 `CLAUDE.md`。你可以通过符号链接让两者保持一致：

```bash
# 从 AGENTS.md 创建指向 CLAUDE.md 的符号链接
ln -s AGENTS.md CLAUDE.md
```

</details>

---

## 为什么庞大的 `AGENTS.md` 文件是个问题

有一个自然的反馈循环会导致 `AGENTS.md` 文件变得危险地大：

1. Agent 做了你不喜欢的事情
2. 你添加一条规则来阻止它
3. 几个月内重复数百次
4. 文件变成了一团"泥球"

不同的开发者添加相互冲突的意见。没有人进行完整的风格审查。结果？一个实际上损害 Agent 性能的不可维护的混乱。

另一个罪魁祸首：自动生成的 `AGENTS.md` 文件。永远不要使用初始化脚本来自动生成你的 `AGENTS.md`。它们用对"大多数场景有用"的东西淹没文件，但这些东西最好通过渐进式披露来提供。生成的文件优先考虑全面性而非克制。

### 指令预算

Humanlayer 的 Kyle 的[文章](https://www.humanlayer.dev/blog/writing-a-good-claude-md)提到了"指令预算"的概念：

> 前沿思考型 LLM 可以合理地遵循约 150-200 条指令。较小的模型可以比大型模型关注更少的指令，非思考型模型可以比思考型模型关注更少的指令。

`AGENTS.md` 文件中的每个 token 都会在**每次请求**时加载，无论它是否相关。这创造了一个硬预算问题：

| 场景 | 影响 |
| ---- | ---- |
| 小型、专注的 `AGENTS.md` | 更多 token 可用于任务特定指令 |
| 大型、臃肿的 `AGENTS.md` | 实际工作的 token 更少；Agent 会困惑 |
| 不相关的指令 | Token 浪费 + Agent 分心 = 性能更差 |

综上所述，这意味着**理想的 `AGENTS.md` 文件应该尽可能小**。

### 过时的文档会毒害上下文

大型 `AGENTS.md` 文件的另一个问题是过时。

文档很快就会过时。对于人类开发者来说，过时的文档很烦人，但人类通常有足够的内置记忆来对糟糕的文档持怀疑态度。对于每次请求都阅读文档的 AI Agent 来说，过时的信息会主动*毒害*上下文。

当你记录文件系统结构时，这尤其危险。文件路径不断变化。如果你的 `AGENTS.md` 说"认证逻辑位于 `src/auth/handlers.ts`"，而该文件被重命名或移动，Agent 会自信地在错误的地方查找。

与其记录结构，不如描述能力。给出关于事物*可能*在哪里以及项目整体形状的提示。让 Agent 在规划期间生成自己的即时文档。

领域概念（如"organization" vs "group" vs "workspace"）比文件路径更稳定，因此记录它们更安全。但即使这些也可能在快速发展的 AI 辅助代码库中漂移。保持轻触。

---

## 削减大型 `AGENTS.md` 文件

对放入这里的内容要无情。考虑这是绝对最小值：

- **一句话项目描述**（充当基于角色的提示词）
- **包管理器**（如果不是 npm；或者使用 `corepack` 进行警告）
- **构建/类型检查命令**（如果是非标准的）

老实说，就这些。其他一切都应该放在别处。

### 一句话项目描述

这一句话给 Agent 提供了关于*为什么*他们在这个仓库中工作的上下文。它锚定了他们做出的每一个决定。

示例：

```markdown
这是一个用于无障碍数据可视化的 React 组件库。
```

这就是基础。Agent 现在理解了它的范围。

### 包管理器规范

如果你在 JavaScript 项目中使用 npm 以外的任何东西，明确告诉 Agent：

```markdown
这个项目使用 pnpm workspaces。
```

没有这一点，Agent 可能会默认使用 `npm` 并生成错误的命令。

<details>
  <summary>Corepack 也很棒</summary>
你也可以使用 [`corepack`](https://github.com/nodejs/corepack) 让系统自动处理警告，节省你宝贵的指令预算。
</details>

### 使用渐进式披露

与其把所有东西都塞进 `AGENTS.md`，不如使用**渐进式披露**：只给 Agent 它现在需要的东西，并在需要时指向其他资源。

Agent 在导航文档层次结构方面很快。它们足够理解上下文以找到它们需要的东西。

#### 将语言特定规则移到单独文件

如果你的 `AGENTS.md` 目前说：

```markdown
始终使用 const 而不是 let。
永远不要使用 var。
尽可能使用 interface 而不是 type。
使用严格空检查。
...
```

改为移到单独的文件。在你的根 `AGENTS.md` 中：

```markdown
关于 TypeScript 约定，请参阅 docs/TYPESCRIPT.md
```

注意轻触，没有"始终"，没有全大写强制。只是一个对话式引用。

好处：

- TypeScript 规则只在 Agent 编写 TypeScript 时加载
- 其他任务（CSS 调试、依赖管理）不会浪费 token
- 文件保持专注，并在模型变化时可移植

#### 嵌套渐进式披露

你可以更深入。你的 `docs/TYPESCRIPT.md` 可以引用 `docs/TESTING.md`。创建一个可发现的资源树：

```
docs/
├── TYPESCRIPT.md
│   └── references TESTING.md
├── TESTING.md
│   └── references specific test runners
└── BUILD.md
    └── references esbuild configuration
```

你甚至可以链接到外部资源、Prisma 文档、Next.js 文档等。Agent 会高效地导航这些层次结构。

#### 使用 Agent Skills

许多工具支持"Agent skills"——Agent 可以调用的命令或工作流，以学习如何做特定的事情。这是渐进式披露的另一种形式：Agent 只在需要时才拉取知识。

我们将在单独的文章中深入介绍 Agent skills。

---

## Monorepos 中的 `AGENTS.md`

你不限于在根目录只有一个 `AGENTS.md`。你可以在子目录中放置 `AGENTS.md` 文件，它们会**与根级别合并**。

这对于 monorepos 来说很强大：

### 什么放在哪里

| 级别 | 内容 |
| ---- | ---- |
| **根** | Monorepo 目的、如何导航包、共享工具（pnpm workspaces） |
| **包** | 包目的、特定技术栈、包特定约定 |

根 `AGENTS.md`：

```markdown
这是一个包含 web 服务和 CLI 工具的 monorepo。
使用 pnpm workspaces 管理依赖。
有关特定指南，请参阅每个包的 AGENTS.md。
```

包级 `AGENTS.md`（在 `packages/api/AGENTS.md` 中）：

```markdown
这个包是一个使用 Prisma 的 Node.js GraphQL API。
遵循 docs/API_CONVENTIONS.md 了解 API 设计模式。
```

**不要超载任何级别。** Agent 在其上下文中看到所有合并的 `AGENTS.md` 文件。让每个级别专注于该范围相关的内容。

---

## 用这个提示修复损坏的 `AGENTS.md`

如果你开始担心仓库中的 `AGENTS.md` 文件，并且你想重构它以使用渐进式披露，尝试将这个提示复制粘贴到你的编码 Agent 中：

```txt
我想让你重构我的 AGENTS.md 文件以遵循渐进式披露原则。

遵循以下步骤：

1. **找出矛盾**：识别任何相互冲突的指令。对于每个矛盾，问我想要保留哪个版本。

2. **识别 essentials**：只提取属于根 AGENTS.md 的内容：
   - 一句话项目描述
   - 包管理器（如果不是 npm）
   - 非标准构建/类型检查命令
   - 真正与每个任务相关的内容

3. **分组其余内容**：将剩余指令组织成逻辑类别（例如，TypeScript 约定、测试模式、API 设计、Git 工作流）。对于每个组，创建一个单独的 markdown 文件。

4. **创建文件结构**：输出：
   - 一个最小的根 AGENTS.md，带有指向单独文件的 markdown 链接
   - 每个带有其相关指令的单独文件
   - 建议的 docs/ 文件夹结构

5. **标记删除**：识别任何以下指令：
   - 冗余的（Agent 已经知道这一点）
   - 太模糊而无法执行
   - 过于明显（如"编写干净的代码"）
```

---

## 不要构建一团泥球

当你要把某些东西添加到 `AGENTS.md` 时，问自己它属于哪里：

| 位置 | 何时使用 |
| ---- | ---- |
| 根 `AGENTS.md` | 与仓库中的每个任务都相关 |
| 单独文件 | 与一个领域相关（TypeScript、测试等） |
| 嵌套文档树 | 可以按层次组织 |

理想的 `AGENTS.md` 是小巧、专注的，并指向其他地方。它给 Agent 足够的上下文开始工作，并有通往更详细指导的面包屑。

其他一切存在于渐进式披露中：单独文件、嵌套的 `AGENTS.md` 文件或 skills。

这让你的指令预算高效、Agent 专注，并且随着工具和最佳实践的发展，你的设置具有未来保障。

---

*翻译完成时间: 2026-03-31*  
*原文: https://www.aihero.dev/a-complete-guide-to-agents-md*
