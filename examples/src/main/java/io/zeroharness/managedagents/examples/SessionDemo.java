package io.zeroharness.managedagents.examples;

import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import io.zeroharness.managedagents.*;
import io.zeroharness.managedagents.anthropic.AnthropicManagedAgents;
import io.zeroharness.managedagents.kimi.KimiManagedAgents;
import java.time.Duration;
import java.util.Locale;

/** The business call sequence below is identical for either provider. */
public final class SessionDemo {
    private SessionDemo() {}

    public static void main(String[] args) throws Exception {
        if (args.length < 2 || !(args[0].equals("anthropic") || args[0].equals("kimi"))) {
            throw new IllegalArgumentException("Usage: SessionDemo <anthropic|kimi> <message> OR <provider> resume <sessionId> <inputEventId>");
        }
        String provider = args[0];
        try (ManagedAgentClient client = configuredClient(provider)) {
            SessionResult.Snapshot result;
            if (args[1].equals("resume")) {
                if (args.length != 4) throw new IllegalArgumentException("Resume requires sessionId and inputEventId");
                // Resume is deliberately read-only. Binding changes must not redirect old session IDs.
                result = SessionEvents.await(client.beta().sessions().events(), args[2],
                        SessionResult.forInput(args[3]), Duration.ofMinutes(2));
            } else {
                result = run(client, configuredSession(provider), args[1]);
            }
            System.out.println("state=" + result.state() + " stopReason=" + result.stopReason());
            System.out.println(result.text());
            if (!result.blockingEventIds().isEmpty()) System.out.println("blockingEventIds=" + result.blockingEventIds());
        }
    }

    static ManagedAgentClient configuredClient(String provider) {
        String prefix = provider.toUpperCase(Locale.ROOT) + "_MANAGED_";
        return switch (provider) {
            case "anthropic" -> new AnthropicManagedAgents(AnthropicOkHttpClient.builder()
                    .apiKey(required(prefix + "API_KEY")).baseUrl("https://api.anthropic.com").build());
            case "kimi" -> new KimiManagedAgents(required(prefix + "API_KEY"));
            default -> throw new IllegalArgumentException("Provider must be anthropic or kimi");
        };
    }

    static SessionCreateParams configuredSession(String provider) {
        String prefix = provider.toUpperCase(Locale.ROOT) + "_MANAGED_";
        return new SessionCreateParams(required(prefix + "AGENT_ID"),
                required(prefix + "ENVIRONMENT_ID"), "Managed Agent X demo");
    }

    public static SessionResult.Snapshot run(ManagedAgentClient client, SessionCreateParams params,
                                             String message) throws InterruptedException {
        client.beta().agents().retrieve(params.agentId());
        client.beta().environments().retrieve(params.environmentId());
        Session session = client.beta().sessions().create(params);
        System.out.println("provider=" + client.provider() + " sessionId=" + session.id());
        var events = client.beta().sessions().events();
        EventSendResult accepted;
        // Establish observation before writing. Detach immediately to demonstrate history recovery.
        try (var stream = events.streamStreaming(session.id())) {
            accepted = events.send(session.id(), message);
            System.out.println("inputEventIds=" + accepted.eventIds());
        }
        SessionResult result = accepted.eventIds().isEmpty() ? SessionResult.forNewSession()
                : SessionResult.forInput(accepted.eventIds().get(0));
        return SessionEvents.await(events, session.id(), result, Duration.ofMinutes(2));
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Set " + name);
        return value;
    }
}
