# 🚀 Managed Agent X · Java SDK

**简体中文** · [English](README.en.md)　|　[Java](https://github.com/zero-harness/managed-agent-x) · [Python](https://github.com/zero-harness/managed-agent-python) · [Node.js](https://github.com/zero-harness/managed-agent-x-nodejs)

> 🔥 **2027 年，如果你还在自己写 Agent Harness、自己维护 ReAct Loop，你已经输在了起跑线上。**

## 用一套 SDK 统一管理多厂商托管 Agent

让 Java、Python 和 Node.js 开发者用同一套操作管理各家的 **Managed Agents**：开启会话、派发任务、跟踪进度、取回结果。

## 托管 Agent 名录

只收录**由厂商托管 Agent 执行循环，且开放 API** 的产品。推理、工具调用和后续步骤都由云端驱动，应用可以直接派发任务。

| 厂商 | 适配目标 · SDK 设计中 |
| --- | --- |
| Anthropic | [Claude Managed Agents](https://platform.claude.com/docs/en/managed-agents/sessions) |
| 智谱 | [Z Managed Agents](https://docs.bigmodel.cn/cn/managed-agents/quickstart) |
| Kimi | [Kimi Hosted Agents](https://platform.kimi.com/docs/hosted-agents/overview) |
| 阿里云 | [百炼 Managed Agents](https://help.aliyun.com/zh/model-studio/session-api/) |
| 火山引擎 | [方舟 Managed Agents](https://docs.volcengine.com/docs/ark/start-session?lang=zh) |
| Qoder | [Qoder Cloud Agents](https://docs.qoder.com/cloud-agents/overview) |
| OpenAI | [Agents API（托管 Codex）](https://developers.openai.com/api/docs/guides/agents-api/architecture) |
| Google | [Gemini Managed Agents（Antigravity、Deep Research）](https://ai.google.dev/gemini-api/docs/agents) |
| Microsoft | [Foundry Prompt Agents](https://learn.microsoft.com/azure/foundry/agents/concepts/prompt-agents) |
| AWS | [Bedrock Agents Classic（仅限现有用户）](https://docs.aws.amazon.com/bedrock/latest/userguide/agents.html) |

## Claude 的「脑手分离」架构

Harness 决定下一步，工具和沙箱负责执行，Session 保存历史。[官方架构图](https://www-cdn.anthropic.com/images/4zrzovbb/website/903b624ada206b10753a24c6a1367e74a869165d-1080x1080.png) · [工程原文](https://www.anthropic.com/engineering/managed-agents#decouple-the-brain-from-the-hands)
