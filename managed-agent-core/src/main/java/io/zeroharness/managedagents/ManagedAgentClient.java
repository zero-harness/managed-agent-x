package io.zeroharness.managedagents;

import com.fasterxml.jackson.databind.JsonNode;

/** Runtime subset of the Anthropic Managed Agents resource API. */
public interface ManagedAgentClient extends AutoCloseable {
    String provider();
    BetaService beta();

    @Override
    default void close() {}

    interface BetaService {
        ResourceService agents();
        ResourceService environments();
        SessionService sessions();
    }

    /** v0.1 consumes prebuilt resources; provisioning remains provider-specific. */
    interface ResourceService {
        JsonNode retrieve(String id);
    }

    interface SessionService {
        Session create(SessionCreateParams params);
        Session retrieve(String sessionId);
        EventService events();
    }

    interface EventService {
        /** Sends once. An ambiguous write failure must never be retried automatically. */
        EventSendResult send(String sessionId, String text);
        EventPage list(String sessionId, String pageToken);

        default EventPage list(String sessionId) {
            return list(sessionId, null);
        }

        EventStream streamStreaming(String sessionId, String cursor);

        default EventStream streamStreaming(String sessionId) {
            return streamStreaming(sessionId, null);
        }
    }
}
