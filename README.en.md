# 🚀 Managed Agent X · Java SDK

<a href="README.md" target="_blank" rel="noopener noreferrer">简体中文</a> · **English**　|　<a href="https://github.com/zero-harness/managed-agent-x" target="_blank" rel="noopener noreferrer">Java</a> · <a href="https://github.com/zero-harness/managed-agent-python" target="_blank" rel="noopener noreferrer">Python</a> · <a href="https://github.com/zero-harness/managed-agent-x-nodejs" target="_blank" rel="noopener noreferrer">Node.js</a>

> 🔥 **It’s 2027. You’re tuning the ReAct loop in your agent harness. The team next door just got its first PR from a cloud agent.**
>
> The loop takes a few lines. The hard part comes at 2 a.m. Your agent just filed a ticket and the connection dropped. Would you hit retry?

## Why Zero Harness?

We want teams to build agent applications with a harness supplied and operated by their cloud provider. The provider runs the model and tool loop and maintains session state. Developers define the task, connect their business tools, and decide which actions need approval.

**Managed Agent X takes Anthropic’s Claude Managed Agents SDK as its baseline.** The Java, Python, and Node.js editions will follow its Agent, Environment, Session, and Event semantics. Adapters will map other providers onto the same calls so an application can switch to another provider that meets its requirements by changing one binding. <a href="https://github.com/anthropics/anthropic-sdk-java/tree/v3.6.0/anthropic-java-core/src/main/kotlin/com/anthropic/services/blocking/beta" target="_blank" rel="noopener noreferrer">Anthropic Java SDK source</a>

## Why use a managed agent?

- **Less infrastructure to maintain.** The platform manages the agent loop and context. Code can run in an isolated sandbox.
- **Long tasks can run in the background.** Reconnect to check saved progress and results.
- **Project conventions carry over.** Attach a memory store so new sessions can read the same conventions.

<a href="https://platform.claude.com/docs/en/managed-agents/overview" target="_blank" rel="noopener noreferrer">Claude runtime</a> · <a href="https://docs.qoder.com/cloud-agents/sessions" target="_blank" rel="noopener noreferrer">Qoder sessions</a> · <a href="https://platform.kimi.com/docs/hosted-agents/memory" target="_blank" rel="noopener noreferrer">Kimi memory</a>

## Who already runs the agent loop for you?

| Provider | Adapter target · SDK in design |
| --- | --- |
| Anthropic | <a href="https://platform.claude.com/docs/en/managed-agents/sessions" target="_blank" rel="noopener noreferrer">Claude Managed Agents</a> |
| Z.ai | <a href="https://docs.bigmodel.cn/cn/managed-agents/quickstart" target="_blank" rel="noopener noreferrer">Z Managed Agents</a> |
| Kimi | <a href="https://platform.kimi.com/docs/hosted-agents/overview" target="_blank" rel="noopener noreferrer">Kimi Hosted Agents</a> |
| Alibaba Cloud | <a href="https://help.aliyun.com/zh/model-studio/session-api/" target="_blank" rel="noopener noreferrer">Model Studio Managed Agents</a> |
| Volcengine | <a href="https://docs.volcengine.com/docs/ark/start-session?lang=zh" target="_blank" rel="noopener noreferrer">Ark Managed Agents</a> |
| Meituan | <a href="https://catpaw.meituan.com/docs#/agent/api-reference" target="_blank" rel="noopener noreferrer">CatPaw Managed Agents</a> |
| Qoder | <a href="https://docs.qoder.com/cloud-agents/overview" target="_blank" rel="noopener noreferrer">Qoder Cloud Agents</a> |
| OpenAI | <a href="https://developers.openai.com/api/docs/guides/agents-api/architecture" target="_blank" rel="noopener noreferrer">Agents API (managed Codex harness)</a> |
| Google | <a href="https://ai.google.dev/gemini-api/docs/custom-agents" target="_blank" rel="noopener noreferrer">Gemini Managed Agents (configurable Antigravity)</a> |
| Microsoft | <a href="https://learn.microsoft.com/azure/foundry/agents/concepts/prompt-agents" target="_blank" rel="noopener noreferrer">Foundry Prompt Agents</a> |
| AWS | <a href="https://docs.aws.amazon.com/bedrock-agentcore/latest/devguide/harness-vs-runtime.html" target="_blank" rel="noopener noreferrer">Amazon Bedrock AgentCore harness</a> |

## How Meituan CatPaw separates planning from execution

The CatPaw harness plans each step and selects a tool. Sandboxes isolate code execution and file operations, while server tools run independently. The session queue records events so the harness can rebuild its context after a restart.

![CatPaw architecture with the harness on the left, sandboxes and server tools on the right, and a recoverable event queue above](assets/catpaw-brain-hand-decoupling.png)

<a href="https://catpaw.meituan.com/docs#/agent/architecture" target="_blank" rel="noopener noreferrer">Source: Meituan CatPaw Managed Agents architecture</a>
