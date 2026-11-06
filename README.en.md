# 🚀 Managed Agent X · Java SDK

[简体中文](README.md) · **English**　|　[Java](https://github.com/zero-harness/managed-agent-x) · [Python](https://github.com/zero-harness/managed-agent-python) · [Node.js](https://github.com/zero-harness/managed-agent-x-nodejs)

> 🔥 **In 2027, if you're still building your own Agent Harness and maintaining a ReAct loop, you're already behind at the starting line.**

## One SDK to manage cloud-hosted agents

Our goal is to manage **Managed Agents across providers** from Java, Python, or Node.js. Use the same calls to start a session, send a task, track its progress, and collect the result.

## Managed Agent directory

We list services that **host the agent loop and expose the agent through an API**. The provider runs the reasoning and tool loop, so your application can delegate a task without operating its own harness.

| Provider | Adapter target · SDK in design |
| --- | --- |
| Anthropic | [Claude Managed Agents](https://platform.claude.com/docs/en/managed-agents/sessions) |
| Z.ai | [Z Managed Agents](https://docs.bigmodel.cn/cn/managed-agents/quickstart) |
| Kimi | [Kimi Hosted Agents](https://platform.kimi.com/docs/hosted-agents/overview) |
| Alibaba Cloud | [Model Studio Managed Agents](https://help.aliyun.com/zh/model-studio/session-api/) |
| Volcengine | [Ark Managed Agents](https://docs.volcengine.com/docs/ark/start-session?lang=zh) |
| Qoder | [Qoder Cloud Agents](https://docs.qoder.com/cloud-agents/overview) |
| OpenAI | [Agents API (managed Codex harness)](https://developers.openai.com/api/docs/guides/agents-api/architecture) |
| Google | [Gemini Managed Agents (Antigravity and Deep Research)](https://ai.google.dev/gemini-api/docs/agents) |
| Microsoft | [Foundry Prompt Agents](https://learn.microsoft.com/azure/foundry/agents/concepts/prompt-agents) |
| AWS | [Bedrock Agents Classic (existing customers only)](https://docs.aws.amazon.com/bedrock/latest/userguide/agents.html) |

## How Claude separates the brain from the hands

The harness decides the next step, tools and sandboxes execute it, and the session keeps the history. [Architecture diagram](https://www-cdn.anthropic.com/images/4zrzovbb/website/903b624ada206b10753a24c6a1367e74a869165d-1080x1080.png) · [Engineering article](https://www.anthropic.com/engineering/managed-agents#decouple-the-brain-from-the-hands)
