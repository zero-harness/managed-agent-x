# 🚀 Managed Agent X · Java SDK

<a href="README.md" target="_blank" rel="noopener noreferrer">简体中文</a> · **English**　|　<a href="https://github.com/zero-harness/managed-agent-x" target="_blank" rel="noopener noreferrer">Java</a> · <a href="https://github.com/zero-harness/managed-agent-python" target="_blank" rel="noopener noreferrer">Python</a> · <a href="https://github.com/zero-harness/managed-agent-x-nodejs" target="_blank" rel="noopener noreferrer">Node.js</a>

> 🔥 **In 2027, if you're still building your own Agent Harness and maintaining a ReAct loop, you're already behind at the starting line.**

## One SDK to manage cloud-hosted agents

Our goal is to manage **Managed Agents across providers** from Java, Python, or Node.js. Use the same calls to select an agent, submit input, and read output. Session continuation, event replay, and approvals depend on the provider.

## Managed Agent directory

We list services that **host the agent loop and expose the agent through an API**. The provider runs the reasoning and tool loop, so your application can delegate a task without operating its own harness.

| Provider | Adapter target · SDK in design |
| --- | --- |
| Anthropic | <a href="https://platform.claude.com/docs/en/managed-agents/sessions" target="_blank" rel="noopener noreferrer">Claude Managed Agents</a> |
| Z.ai | <a href="https://docs.bigmodel.cn/cn/managed-agents/quickstart" target="_blank" rel="noopener noreferrer">Z Managed Agents</a> |
| Kimi | <a href="https://platform.kimi.com/docs/hosted-agents/overview" target="_blank" rel="noopener noreferrer">Kimi Hosted Agents</a> |
| Alibaba Cloud | <a href="https://help.aliyun.com/zh/model-studio/session-api/" target="_blank" rel="noopener noreferrer">Model Studio Managed Agents</a> |
| Volcengine | <a href="https://docs.volcengine.com/docs/ark/start-session?lang=zh" target="_blank" rel="noopener noreferrer">Ark Managed Agents</a> |
| Qoder | <a href="https://docs.qoder.com/cloud-agents/overview" target="_blank" rel="noopener noreferrer">Qoder Cloud Agents</a> |
| OpenAI | <a href="https://developers.openai.com/api/docs/guides/agents-api/architecture" target="_blank" rel="noopener noreferrer">Agents API (managed Codex harness)</a> |
| Google | <a href="https://ai.google.dev/gemini-api/docs/custom-agents" target="_blank" rel="noopener noreferrer">Gemini Managed Agents (configurable Antigravity)</a> |
| Microsoft | <a href="https://learn.microsoft.com/azure/foundry/agents/concepts/prompt-agents" target="_blank" rel="noopener noreferrer">Foundry Prompt Agents</a> |
| AWS | <a href="https://docs.aws.amazon.com/bedrock-agentcore/latest/devguide/harness-vs-runtime.html" target="_blank" rel="noopener noreferrer">Amazon Bedrock AgentCore harness</a> |

## How Claude separates the brain from the hands

The harness decides the next step, tools and sandboxes execute it, and the session keeps the history.

![Anthropic architecture showing the harness, session log, tools, sandbox, and orchestration](assets/anthropic-managed-agents.png)

<a href="https://www.anthropic.com/engineering/managed-agents#decouple-the-brain-from-the-hands" target="_blank" rel="noopener noreferrer">Engineering article</a>
