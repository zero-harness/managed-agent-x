# Runtime protocol samples

Synthetic, credential-free fixtures based on the sources below, checked 2026-11-06.
They are examples for offline adapter tests, not recordings of a live account.

- Anthropic: [`anthropic-sdk-java` v3.6.0 services](https://github.com/anthropics/anthropic-sdk-java/tree/v3.6.0/anthropic-java-core/src/main/kotlin/com/anthropic/services/blocking/beta), [session events](https://platform.claude.com/docs/en/managed-agents/events-and-streaming).
- Kimi: [session creation and input envelope](https://platform.kimi.com/docs/hosted-agents/sessions), [history, SSE cursors and stop reasons](https://platform.kimi.com/docs/hosted-agents/event-stream), API version `2026-09-01-beta`.

Each provider has two history pages and a deliberately truncated first SSE stream.
The resumed stream repeats a complete message. Tests assert complete output is
deduplicated and that recovery sends no additional input. Kimi's page token,
SSE cursor and business event ID deliberately differ.

The public resource API implements a runtime subset of the Anthropic service shape,
not binary compatibility with every official SDK DTO. Provisioning, tool-result
submission, subthreads, automatic retries and cross-language reference formats are
outside this first slice. `SessionResult.forInput` requires exclusive sequential
turns; `forNewSession` requires a fresh, exclusively owned session. An interrupted
Anthropic turn can also report `end_turn`; the SDK preserves that reason and does
not infer the user's original intent was fulfilled.
