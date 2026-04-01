# Bug 跟进工作流

> 供团队成员阅读。当前为**手动版本**，AI 辅助完成大部分工作，人工参与 4 个关键节点。

**前置依赖**：两个 Skills 已集成到项目，无需额外安装
- `.agents/skills/systematic-debugging/` — 系统性根因分析
- `.agents/skills/kuikly-app-runner/` — KuiklyUI 多平台编译运行

> Skills 兼容方案：[HARNESS_OVERVIEW.md 附录 B](../../harness-engineer/HARNESS_OVERVIEW.md#附录-b多工具-skills-兼容)

---

## 流程总览

```
👤 描述问题
     ↓
🤖 Phase 1：构建复现环境（AI 说明方案 → 👤 确认）
     ↓
🤖 Phase 2：诊断分析（添加 KLog 日志）
     ↓
🤖 Phase 3：编译部署运行（kuikly-app-runner）→ 👤 操作复现
     ↓
🤖 Phase 4：读日志 → 根因确认（👤 每次循环确认）
     ↙          ↘
日志不足        根因确认
补充日志 ↩      ↓
              🤖 Phase 5：给出修复方案 → 👤 确认
                   ↓
              🤖 实施修复 → 👤 Review → 🤖 提交 MR
```

**人工参与的 4 个节点**：确认复现方案 / 操作复现 / 每次根因循环确认 / Review 修复代码

---

## 详细流程

### 启动

在对话中描述问题即可，AI 会自动识别并启动 Bug 跟进流程：

```
[平台：iOS/Android/HarmonyOS]
[现象：...]
[复现步骤：...]
[预期 vs 实际：...]
```

---

### Phase 1：构建复现环境

**AI 做的事**：
- 分析问题描述，在 `demo/` 目录下确定复现 Demo 的构建思路
- 说明打算如何构建（使用哪个 DSL、模拟哪个场景）
- **等待开发者确认**后再动手

**[👤 PAUSE]** 确认复现方案，或提供更准确的复现思路

> Demo 命名规范：`BugRepro{功能名}Page.kt`，放在 `demo/src/commonMain/.../pages/debug/`

---

### Phase 2：诊断分析

**AI 做的事**（遵循 `systematic-debugging` 方法论）：
- 搜索相关代码，列出 2-3 个可能原因并排优先级
- 在 Demo 中加入关键 `KLog` 诊断日志，覆盖每个假设的验证点
- **不在此阶段提出修复**（systematic-debugging 铁律：根因未确认前不修复）

---

### Phase 3：编译部署运行

**AI 做的事**（调用 `kuikly-app-runner`）：
- 清理旧日志：`rm -rf logs && mkdir -p logs`
- 编译对应平台 App 并部署到模拟器/真机
- 提示开发者可以开始操作

**[👤 PAUSE]** 在设备/模拟器上操作，复现问题

---

### Phase 4：根因确认（可循环）

**AI 做的事**：
- 读取 `./logs/` 下的日志
- 对照各假设，分析日志证据
- 给出结论：`根因置信度 [高/中/低]：...`

**[👤 PAUSE]** 开发者确认：根因分析是否合理？

```
选项 A：根因确认 → 进入 Phase 5
选项 B：需要更多日志 → AI 补充日志，回到 Phase 3
选项 C：假设方向错误 → AI 重新分析，回到 Phase 2
```

> 每次循环都需要开发者确认，防止 AI 在错误方向上无限迭代

---

### Phase 5：修复

**AI 做的事**：
- 给出修复方案（说明修改点和理由）
- **等待开发者确认**后再实施

**[👤 PAUSE]** 确认修复方案是否合理

**AI 继续**：
- 实施修复
- 提交代码，通过工蜂 MCP（`mcp__gongfeng__create_merge_request`）发起 MR

**[👤 PAUSE]** Review 修复代码

---

## TODO

- [ ] **沉淀为 Skills**：将本流程固化为 `kuikly-debug` SKILL.md，引用 `systematic-debugging` 方法论和 `kuikly-app-runner`，通过明确的 `[PAUSE]` 节点控制人工确认点，实现一键触发

- [ ] **探索自动化测试**：调研用自动化测试工具（如 UI 自动化、截图对比）替代「开发者操作复现」这一步，让 AI 能自主完成完整的复现-验证闭环，进一步减少人工参与
