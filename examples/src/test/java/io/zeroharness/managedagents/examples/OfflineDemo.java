package io.zeroharness.managedagents.examples;

import io.zeroharness.managedagents.SessionCreateParams;
import io.zeroharness.managedagents.SessionResult;

/** Runs the same production business code against each provider's local protocol fixtures. */
public final class OfflineDemo {
    public static void main(String[] args) throws Exception {
        for (String provider : new String[]{"anthropic", "kimi"}) {
            try (var fixture = new ProviderContractTest.Fixture(provider); var client = fixture.client()) {
                var result = SessionDemo.run(client,
                        new SessionCreateParams("agent-1", "environment-1", "Offline demo"), "Do the task");
                if (result.state() != SessionResult.State.COMPLETED || fixture.submissions != 1) {
                    throw new IllegalStateException("Offline demo failed for " + provider);
                }
                System.out.println("state=" + result.state() + " text=" + result.text()
                        + " submissions=" + fixture.submissions);
            }
            try (var fixture = new ProviderContractTest.Fixture(provider); var client = fixture.client()) {
                var turns = ConversationDemo.run(client,
                        new SessionCreateParams("agent-1", "environment-1", "Offline conversation"),
                        "Do the task", "Continue using the previous context");
                if (turns.size() != 2 || fixture.creations != 1 || fixture.submissions != 2
                        || !turns.get(0).text().equals("Complete result.")
                        || !turns.get(1).text().equals("Follow-up result.")) {
                    throw new IllegalStateException("Offline conversation failed for " + provider);
                }
                System.out.println("conversation sessions=" + fixture.creations + " submissions=" + fixture.submissions);
            }
        }
    }
}
