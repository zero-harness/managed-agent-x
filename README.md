# 🚀 Managed Agent X · Java SDK

**简体中文** · <a href="README.en.md" target="_blank" rel="noopener noreferrer">English</a>　|　<a href="https://github.com/zero-harness/managed-agent-x" target="_blank" rel="noopener noreferrer">Java</a> · <a href="https://github.com/zero-harness/managed-agent-python" target="_blank" rel="noopener noreferrer">Python</a> · <a href="https://github.com/zero-harness/managed-agent-x-nodejs" target="_blank" rel="noopener noreferrer">Node.js</a>

> 🔥 **2027 年，你还在写 Agent Harness、调 ReAct Loop，隔壁团队的 Agent 已经提了第一个 PR。**
>
> 写出循环只要几行。难的是凌晨两点，Agent 刚建完工单，连接断了。你敢直接重试吗？

## 为什么叫 Zero Harness？

我们希望，今后做 Agent 应用，团队可以直接使用云厂商提供的 Harness。模型何时继续推理、何时调用工具、如何保存会话，都交给厂商运行。开发者只需定义任务、接入业务工具，并决定哪些操作需要审批。

**Managed Agent X 以 Anthropic 的 Claude Managed Agents SDK 为基准。** Java、Python 和 Node.js 版将沿用 Agent、Environment、Session 和 Event 的资源语义。其他厂商由适配器映射到同一套调用；切换到满足任务要求的厂商时，业务代码只需换一处绑定。 <a href="https://github.com/anthropics/anthropic-sdk-java/tree/v3.6.0/anthropic-java-core/src/main/kotlin/com/anthropic/services/blocking/beta" target="_blank" rel="noopener noreferrer">Anthropic Java SDK 源码</a>

## Managed Agent 的优点

- **少搭一套运行时**：平台维护推理循环和上下文，代码可在隔离沙箱中执行。
- **长任务可后台运行**：客户端重连后可查看已保存的进度和结果。
- **项目约定可复用**：挂载记忆库后，新会话也能读取已有约定。

<a href="https://platform.claude.com/docs/en/managed-agents/overview" target="_blank" rel="noopener noreferrer">Claude 托管运行时</a> · <a href="https://docs.qoder.com/cloud-agents/sessions" target="_blank" rel="noopener noreferrer">Qoder 会话恢复</a> · <a href="https://platform.kimi.com/docs/hosted-agents/memory" target="_blank" rel="noopener noreferrer">Kimi 记忆库</a>

## 哪些厂商已经替你跑 Agent 循环？

| 厂商 | 适配目标 · SDK 设计中 |
| --- | --- |
| Anthropic | <a href="https://platform.claude.com/docs/en/managed-agents/sessions" target="_blank" rel="noopener noreferrer">Claude Managed Agents</a> |
| 智谱 | <a href="https://docs.bigmodel.cn/cn/managed-agents/quickstart" target="_blank" rel="noopener noreferrer">Z Managed Agents</a> |
| Kimi | <a href="https://platform.kimi.com/docs/hosted-agents/overview" target="_blank" rel="noopener noreferrer">Kimi Hosted Agents</a> |
| 阿里云 | <a href="https://help.aliyun.com/zh/model-studio/session-api/" target="_blank" rel="noopener noreferrer">百炼 Managed Agents</a> |
| 火山引擎 | <a href="https://docs.volcengine.com/docs/ark/start-session?lang=zh" target="_blank" rel="noopener noreferrer">方舟 Managed Agents</a> |
| 美团 | <a href="https://catpaw.meituan.com/docs#/agent/api-reference" target="_blank" rel="noopener noreferrer">CatPaw Managed Agents</a> |
| Qoder | <a href="https://docs.qoder.com/cloud-agents/overview" target="_blank" rel="noopener noreferrer">Qoder Cloud Agents</a> |
| OpenAI | <a href="https://developers.openai.com/api/docs/guides/agents-api/architecture" target="_blank" rel="noopener noreferrer">Agents API（托管 Codex）</a> |
| Google | <a href="https://ai.google.dev/gemini-api/docs/custom-agents" target="_blank" rel="noopener noreferrer">Gemini Managed Agents（可配置的 Antigravity）</a> |
| Microsoft | <a href="https://learn.microsoft.com/azure/foundry/agents/concepts/prompt-agents" target="_blank" rel="noopener noreferrer">Foundry Prompt Agents</a> |
| AWS | <a href="https://docs.aws.amazon.com/bedrock-agentcore/latest/devguide/harness-vs-runtime.html" target="_blank" rel="noopener noreferrer">Amazon Bedrock AgentCore harness</a> |

## 美团 CatPaw 的「脑手分离」架构

CatPaw Harness 规划步骤、选择工具。沙箱隔离执行代码和文件操作，服务端工具独立执行。顶部的 Session Queue 记录事件，供 Harness 重启后恢复上下文。

![美团妙手脑手分离架构：左侧为 Harness 决策循环，右侧为沙箱和服务端工具，顶部为可恢复事件队列](assets/catpaw-brain-hand-decoupling.png)

<a href="https://catpaw.meituan.com/docs#/agent/architecture" target="_blank" rel="noopener noreferrer">来源：美团妙手 Managed Agents 技术架构</a>
