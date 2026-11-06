# Managed Agent X · Java SDK

[简体中文](README.md) · **English**　|　[Java](https://github.com/zero-harness/managed-agent-x) · [Python](https://github.com/zero-harness/managed-agent-python) · [Node.js](https://github.com/zero-harness/managed-agent-x-nodejs)

## Still writing your own harness in 2027?

**If you're still writing your own harness and maintaining your own ReAct loop in 2027, you're already behind at the starting line.**

Getting an agent to read a ticket, check logs, call a tool, and decide what to do next is the easy part. What happens when your service restarts halfway through a tool call? Where does the task pick up when an approver returns half an hour later? Managed agents are already taking on that work.

## Project goals

Managed Agent X aims to give developers one SDK for cloud-hosted agents across providers. This repository is the Java edition. An application should be able to select a configured agent, submit work, observe progress, handle requests for human input, and collect the result. The same application code should work with another provider that offers the capabilities the task requires.

The Java, Python, and Node.js editions will share a behavioral contract. For capabilities such as tool approval and stream recovery, the SDK will tell callers which guarantees a service offers. The project is in the design stage.

## What managed agents can do

A hosted agent can keep working after an HTTP request ends. A service that dispatches a code fix can check back for a pull request. A research assistant can gather evidence in the background and deliver a report when it finishes. The platform drives the tool loop and can provide session history, sandboxed execution, and recovery. Each service defines which state survives and where execution can resume. [Background execution](https://ai.google.dev/gemini-api/docs/background-execution) · [Session events and recovery](https://platform.kimi.com/docs/hosted-agents/event-stream)

The following **adapter targets** have publicly documented managed-agent interfaces. As of 2026-11-06, SDK integration and verification are pending for every entry. General-purpose runtimes come first, followed by evaluation of specialized services for each use case.

| Use case | Work the service exposes | Adapter targets and official documentation |
| --- | --- | --- |
| General-purpose tasks | Create sessions, submit tasks, and read execution events | [Claude Managed Agents](https://platform.claude.com/docs/en/managed-agents/sessions) · [Zhipu Z Managed Agents](https://docs.bigmodel.cn/cn/managed-agents/quickstart) · [Kimi Hosted Agents](https://platform.kimi.com/docs/hosted-agents/overview) |
| General-purpose tasks | Configure an agent and its environment to execute multi-step tasks | [Alibaba Cloud Model Studio](https://help.aliyun.com/zh/model-studio/session-api/) · [Volcengine Ark](https://docs.volcengine.com/docs/ark/start-session?lang=zh) · [Qoder Cloud Agents](https://docs.qoder.com/cloud-agents/overview) |
| General-purpose tasks | Delegate work to a hosted Codex or Antigravity agent | [OpenAI Agents API](https://developers.openai.com/api/docs/guides/agents-api/architecture) · [Gemini Managed Agents](https://ai.google.dev/gemini-api/docs/custom-agents) |
| Enterprise workflows | Invoke a configured agent to look up information or call business tools | [Amazon Bedrock Agents](https://docs.aws.amazon.com/bedrock/latest/APIReference/API_agent-runtime_InvokeAgent.html) · [Microsoft Foundry Prompt Agents](https://learn.microsoft.com/azure/foundry/agents/concepts/prompt-agents) |
| Software engineering | Target a repository and track a remote run and its pull request | [Cursor Cloud Agents](https://cursor.com/docs/cloud-agent/api/endpoints) |
| Deep research | Submit a research question and generate a report in the background | [Gemini Deep Research](https://ai.google.dev/gemini-api/docs/agents) |
| Browser automation | Send a natural-language task to a hosted browser agent and collect the result | [Browser Use Cloud](https://github.com/browser-use/browser-use#readme) |
| Voice and telephony | Start an outbound call with a voice agent and receive a call identifier | [ElevenLabs Agents](https://elevenlabs.io/docs/eleven-agents/api-reference/integrations/twilio/outbound-call) |

## Anthropic’s architecture diagram

Claude and the harness form the “brain” that decides what to do next. Sandboxes and tools are the “hands” that act. The session records events independently, so the task history remains available when the execution environment is replaced.

[Open Anthropic’s original architecture diagram](https://www-cdn.anthropic.com/images/4zrzovbb/website/903b624ada206b10753a24c6a1367e74a869165d-1080x1080.png) · [Read the engineering article](https://www.anthropic.com/engineering/managed-agents#decouple-the-brain-from-the-hands)
