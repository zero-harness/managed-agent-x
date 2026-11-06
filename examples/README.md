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
