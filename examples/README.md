# Session/Event demo

Requires JDK 17+ and Maven 3.9+. From the repository root:

```sh
mvn install
mvn -q -pl examples exec:java \
  -Dexec.mainClass=io.zeroharness.managedagents.examples.OfflineDemo \
  -Dexec.classpathScope=test
```

The offline demo calls the production adapters against local HTTP/SSE fixtures.
Both bindings run the same `SessionDemo.run` code, submit once, disconnect, and
recover `Complete result.` from paginated history. No provider account is used.
It also runs `ConversationDemo.run`: one session, two sequential submissions,
and a separate result for each input event. These synthetic responses verify
session reuse and result boundaries; they do not prove model memory.

## 调用代码

- [`SessionDemo.java`](src/main/java/io/zeroharness/managedagents/examples/SessionDemo.java)：选择厂商、读取预建资源、创建会话、提交消息、断流后读回结果，以及恢复已提交任务。
- [`ConversationDemo.java`](src/main/java/io/zeroharness/managedagents/examples/ConversationDemo.java)：复用同一会话连续运行两轮；每轮单独按输入事件 ID 取结果，只有正常完成才提交下一轮。
- [`OfflineDemo.java`](src/test/java/io/zeroharness/managedagents/examples/OfflineDemo.java)：无需账号的双厂商演示，使用真实适配器访问本地协议服务。

应用安装 `io.zeroharness:managed-agent-core:0.1.0-SNAPSHOT` 和所需的
`managed-agent-anthropic` / `managed-agent-kimi` 模块。当前先运行上面的
`mvn install` 安装到本地 Maven 仓库。厂商只在客户端装配处选择：

```java
// Anthropic：使用官方 SDK 3.6.0，显式指定凭据与服务地址。
ManagedAgentClient client = new AnthropicManagedAgents(
    AnthropicOkHttpClient.builder()
        .apiKey(System.getenv("ANTHROPIC_MANAGED_API_KEY"))
        .baseUrl("https://api.anthropic.com")
        .build());

// 切换到 Kimi 时，将上面的客户端替换为：
// ManagedAgentClient client = new KimiManagedAgents(System.getenv("KIMI_MANAGED_API_KEY"));

try (client) {
    Session session = client.beta().sessions().create(
        new SessionCreateParams(agentId, environmentId, "我的任务"));
    var events = client.beta().sessions().events();
    EventSendResult accepted;
    try (var stream = events.streamStreaming(session.id())) {
        accepted = events.send(session.id(), "汇总项目的主要功能");
    }
    // 先持久化 session.id() 与 accepted.eventIds()。断流后只读历史，不重发任务。
    SessionResult turn = accepted.eventIds().isEmpty()
        ? SessionResult.forNewSession() // 仅用于全新、独占、只提交过一次的会话
        : SessionResult.forInput(accepted.eventIds().get(0));
    var result = SessionEvents.await(events, session.id(), turn, Duration.ofMinutes(2));
    switch (result.state()) {
        case COMPLETED -> System.out.println(result.text());
        case NEEDS_ACTION -> System.out.println("等待操作：" + result.blockingEventIds());
        case RUNNING -> System.out.println("本次等待已超时，可继续读取同一任务");
        default -> System.out.println("请检查状态：" + result.state());
    }
}
```

公共类型在 `io.zeroharness.managedagents` 包，两个适配器分别在其
`anthropic`、`kimi` 子包；`Duration` 来自 `java.time`，
`AnthropicOkHttpClient` 来自 `com.anthropic.client.okhttp`。

To run against a real account, configure existing Agent and Environment IDs:

```sh
export ANTHROPIC_MANAGED_API_KEY='...'
export ANTHROPIC_MANAGED_AGENT_ID='...'
export ANTHROPIC_MANAGED_ENVIRONMENT_ID='...'
mvn -q -pl examples exec:java \
  -Dexec.mainClass=io.zeroharness.managedagents.examples.SessionDemo \
  -Dexec.args='anthropic "Reply with a short greeting"'
```

For Kimi, set `KIMI_MANAGED_API_KEY`, `KIMI_MANAGED_AGENT_ID` and
`KIMI_MANAGED_ENVIRONMENT_ID`, then change the first argument to `kimi`.
These dedicated credential names avoid silently using credentials for unrelated
Anthropic-compatible chat gateways.

## 同一会话连续两轮

使用上面配置的厂商凭据和资源 ID，执行：

```sh
mvn -q -pl examples exec:java \
  -Dexec.mainClass=io.zeroharness.managedagents.examples.ConversationDemo \
  -Dexec.args='anthropic "Remember this project code: ORCHID-42. Reply OK." "What project code did I give you?"'
```

切换 Kimi 时把第一个参数改为 `kimi`。两轮使用同一个 Session ID，
第二轮不重复发送第一轮文本，由厂商保存上下文。真实账号联调时应核对第二轮
回复包含 `ORCHID-42`；本地固定样本不具备模型推理能力，不能替代这项验收。

同一会话须由一个写入方串行提交：上一轮完成后才能提交下一轮。
遇到待操作、失败、超时或归属不明时，示例停止提交后续消息。
如果输入已受理但未返回事件 ID，示例保留并输出 Session ID，要求查历史后继续，
不会重发输入或猜测第二轮的边界。恢复旧轮次时只输出该轮的完整消息，
不会混入后续轮次；若在终态前读到另一条已处理输入，结果标记为 `UNKNOWN`。

The program prints the session and accepted input IDs before waiting. Continue
observing an existing task without submitting it again:

```sh
mvn -q -pl examples exec:java \
  -Dexec.mainClass=io.zeroharness.managedagents.examples.SessionDemo \
  -Dexec.args='anthropic resume SESSION_ID INPUT_EVENT_ID'
```

Resume must use the original provider/account. If a provider accepts input without
returning its ID, inspect that exclusively owned session's durable history to find
the input ID before using this resume command. `SessionEvents.await` returns a
snapshot on timeout; it does not cancel remote work, and an in-flight HTTP read
can outlast the polling deadline. `NEEDS_ACTION` is a pause, including tool
confirmation or budget exhaustion; it is not successful completion. This slice
reports blocking event IDs but does not yet submit approvals or tool results.

Live acceptance remains separate: verify account access and resource readiness,
two sequential turns sharing context, actual network loss, history retention,
and tool-action continuation. Offline fixtures cannot establish those guarantees.
