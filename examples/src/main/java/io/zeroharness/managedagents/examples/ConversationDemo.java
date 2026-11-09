package io.zeroharness.managedagents.examples;

import io.zeroharness.managedagents.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/** Two sequential inputs reuse one remotely hosted session and its context. */
public final class ConversationDemo {
    private ConversationDemo() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            throw new IllegalArgumentException("Usage: ConversationDemo <anthropic|kimi> <first-message> <second-message>");
        }
        try (var client = SessionDemo.configuredClient(args[0])) {
            run(client, SessionDemo.configuredSession(args[0]), args[1], args[2]);
        }
    }

    public static List<SessionResult.Snapshot> run(ManagedAgentClient client, SessionCreateParams params,
                                                  String firstMessage, String secondMessage) throws InterruptedException {
        client.beta().agents().retrieve(params.agentId());
        client.beta().environments().retrieve(params.environmentId());
        Session session = client.beta().sessions().create(params);
        System.out.println("provider=" + client.provider() + " sessionId=" + session.id());
        var events = client.beta().sessions().events();
        var results = new ArrayList<SessionResult.Snapshot>();
        for (String message : List.of(firstMessage, secondMessage)) {
            EventSendResult accepted;
            try (var stream = events.streamStreaming(session.id())) {
                accepted = events.send(session.id(), message);
                System.out.println("inputEventIds=" + accepted.eventIds());
            }
            if (accepted.eventIds().size() != 1) {
                // The input was accepted. Do not retry or send the next turn without correlation.
                throw new IllegalStateException("Input accepted in session " + session.id()
                        + " without a single input event ID; inspect history before continuing; do not resend");
            }
            // Each turn has its own observation boundary even though the session is shared.
            var result = SessionEvents.await(events, session.id(),
                    SessionResult.forInput(accepted.eventIds().get(0)), Duration.ofMinutes(2));
            results.add(result);
            System.out.println("turn=" + results.size() + " state=" + result.state()
                    + " stopReason=" + result.stopReason() + " text=" + result.text());
            if (result.state() != SessionResult.State.COMPLETED) {
                // Includes timeout (RUNNING), action needed, failure and ambiguous outcomes.
                if (!result.blockingEventIds().isEmpty()) {
                    System.out.println("blockingEventIds=" + result.blockingEventIds());
                }
                break;
            }
        }
        return List.copyOf(results);
    }
}
