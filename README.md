# Managed Agent X · Java SDK

**简体中文** · [English](README.en.md)　|　[Java](https://github.com/zero-harness/managed-agent-x) · [Python](https://github.com/zero-harness/managed-agent-python) · [Node.js](https://github.com/zero-harness/managed-agent-x-nodejs)

## 2027 年，还要自己写 Harness 吗？

**到 2027 年，如果你还在自己写 Harness、自己维护 ReAct 循环，你已经输在了起跑线上。**

让 Agent 读工单、查日志、调用工具，再根据结果决定下一步，跑通一次并不难。真正占用工程时间的是之后的事：工具执行到一半服务重启了，要不要重做？审批人半小时后才回来，任务从哪里继续？这些都是托管 Agent 已经开始替应用团队承担的工作。

## 项目目标

Managed Agent X 的目标是让开发者用一套 SDK 接入多家云端托管 Agent。本仓库是 Java 版本。我们希望应用只需选择一个已配置的 Agent，就能提交任务、查看进度、处理需要人工介入的操作，并取回结果。换到另一家满足任务要求的服务时，同一段业务代码可以继续使用。

Java、Python 和 Node.js 将遵循相同的行为约定。对于工具审批、断线续读等能力，SDK 将告诉调用方当前服务能提供什么保证。项目目前处于设计阶段。

## 托管 Agent 能做什么

一次 HTTP 请求结束后，云端 Agent 可以继续工作。派发代码修复任务的服务可以稍后回来取 PR；研究助手可以在后台收集资料，完成后交回报告。平台负责驱动工具循环，并可提供会话记录、执行沙箱和故障恢复。具体服务决定可以保留哪些状态、在哪一步恢复。[后台执行](https://ai.google.dev/gemini-api/docs/background-execution) · [会话事件与恢复](https://platform.kimi.com/docs/hosted-agents/event-stream)

以下是按场景整理的**适配目标**。每一项都有厂商公开的托管 Agent 调用接口。截至 2026-11-06，所有条目均待本 SDK 适配和验证。优先接入通用运行时，再按场景评估专用服务。

| 场景 | 可以交给 Agent 的工作 | 适配目标与官方文档 |
| --- | --- | --- |
| 通用任务 | 创建会话、提交任务、读取执行事件 | [Claude Managed Agents](https://platform.claude.com/docs/en/managed-agents/sessions) · [智谱 Z Managed Agents](https://docs.bigmodel.cn/cn/managed-agents/quickstart) · [Kimi Hosted Agents](https://platform.kimi.com/docs/hosted-agents/overview) |
| 通用任务 | 配置 Agent 和运行环境，执行多步任务 | [阿里云百炼](https://help.aliyun.com/zh/model-studio/session-api/) · [火山方舟](https://docs.volcengine.com/docs/ark/start-session?lang=zh) · [Qoder Cloud Agents](https://docs.qoder.com/cloud-agents/overview) |
| 通用任务 | 将工作交给托管的 Codex 或 Antigravity 执行 | [OpenAI Agents API](https://developers.openai.com/api/docs/guides/agents-api/architecture) · [Gemini Managed Agents](https://ai.google.dev/gemini-api/docs/custom-agents) |
| 企业工作流 | 调用已配置的业务 Agent，查询知识或调用业务工具 | [Amazon Bedrock Agents](https://docs.aws.amazon.com/bedrock/latest/APIReference/API_agent-runtime_InvokeAgent.html) · [Microsoft Foundry Prompt Agents](https://learn.microsoft.com/azure/foundry/agents/concepts/prompt-agents) |
| 软件工程 | 指定仓库和任务，跟踪远程执行与 PR | [Cursor Cloud Agents](https://cursor.com/docs/cloud-agent/api/endpoints) |
| 深度研究 | 提交研究问题，在后台收集资料并生成报告 | [Gemini Deep Research](https://ai.google.dev/gemini-api/docs/agents) |
| 浏览器自动化 | 用自然语言发起云端浏览器任务，读取执行结果 | [Browser Use Cloud](https://github.com/browser-use/browser-use#readme) |
| 语音与外呼 | 选择语音 Agent 发起电话，获得通话标识 | [ElevenLabs Agents](https://elevenlabs.io/docs/eleven-agents/api-reference/integrations/twilio/outbound-call) |

## Anthropic 的「脑手分离」架构图

Claude 与 Harness 构成“大脑”，决定下一步做什么。沙箱和工具相当于“手”，负责执行。Session 独立保存事件历史，因此替换执行环境后，仍可以查到任务的来龙去脉。

[查看 Anthropic 官方架构原图](https://www-cdn.anthropic.com/images/4zrzovbb/website/903b624ada206b10753a24c6a1367e74a869165d-1080x1080.png) · [阅读工程文章的对应章节](https://www.anthropic.com/engineering/managed-agents#decouple-the-brain-from-the-hands)
