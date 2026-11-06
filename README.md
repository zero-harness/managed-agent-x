# 🚀 Managed Agent X · Java SDK

**简体中文** · <a href="README.en.md" target="_blank" rel="noopener noreferrer">English</a>　|　<a href="https://github.com/zero-harness/managed-agent-x" target="_blank" rel="noopener noreferrer">Java</a> · <a href="https://github.com/zero-harness/managed-agent-python" target="_blank" rel="noopener noreferrer">Python</a> · <a href="https://github.com/zero-harness/managed-agent-x-nodejs" target="_blank" rel="noopener noreferrer">Node.js</a>

> 🔥 **2027 年，你还在写 Agent Harness、调 ReAct Loop，隔壁团队的 Agent 已经提了第一个 PR。**
>
> 写出循环只要几行。难的是凌晨两点，Agent 刚建完工单，连接断了。你敢直接重试吗？

## 换一个托管 Agent，要改多少业务代码？

**我们希望你只改一处配置，就能把同一个任务从 Claude 切到 Kimi。** Managed Agent X 正在为 Java、Python 和 Node.js 设计统一 SDK。提交任务、读取结果用同一套调用；需要断线续读或人工审批时，SDK 会明确告诉你哪家支持。

## 哪些厂商已经替你跑 Agent 循环？

这 11 家都提供云端托管的 Agent 和调用 API。厂商负责驱动模型和工具，你把任务交给它，再取回结果。

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

## Claude 的「脑手分离」架构

Harness 决定下一步，工具和沙箱负责执行，Session 保存历史。

![Anthropic：Harness 与 Session、工具、沙箱和调度层的关系](assets/anthropic-managed-agents.png)

<a href="https://www.anthropic.com/engineering/managed-agents#decouple-the-brain-from-the-hands" target="_blank" rel="noopener noreferrer">工程原文</a>
