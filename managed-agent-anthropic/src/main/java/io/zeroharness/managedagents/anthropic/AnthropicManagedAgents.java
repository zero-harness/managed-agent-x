package io.zeroharness.managedagents.anthropic;

import com.anthropic.client.AnthropicClient;
import com.anthropic.core.ObjectMappers;
import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.models.beta.sessions.events.BetaManagedAgentsUserMessageEventParams;
import com.anthropic.models.beta.sessions.events.EventListParams;
import com.anthropic.models.beta.sessions.events.EventSendParams;
import com.fasterxml.jackson.databind.JsonNode;
import io.zeroharness.managedagents.*;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/** Uses the official anthropic-java 3.6.0 services and serializers, including beta headers. */
public final class AnthropicManagedAgents implements ManagedAgentClient {
    private final AnthropicClient sdk;
    private final BetaService beta = new Beta();

    /** The adapter owns the supplied client and closes it when closed. */
    public AnthropicManagedAgents(AnthropicClient sdk) {
        // The official SDK otherwise retries some POST failures. Acceptance may be ambiguous.
        this.sdk = Objects.requireNonNull(sdk).withOptions(options -> options.maxRetries(0));
    }

    public String provider() { return "anthropic"; }
    public BetaService beta() { return beta; }
    public void close() { sdk.close(); }

    private static JsonNode json(Object value) {
        return ObjectMappers.jsonMapper().valueToTree(value);
    }

    private static Session session(JsonNode node) {
        return new Session(node.path("id").asText(), node.path("status").asText(), node);
    }

    private static Event event(Object value) {
        JsonNode node = json(value);
        return new Event(node.path("id").asText(null), node.path("type").asText(), node, node);
    }

    private static <T> T read(Supplier<T> request) {
        try {
            return request.get();
        } catch (AnthropicServiceException failure) {
            throw serviceFailure(failure);
        }
    }

    private static ManagedAgentException serviceFailure(AnthropicServiceException failure) {
        return new ManagedAgentException("Anthropic request failed", failure.statusCode(),
                failure.requestId().orElse(null), failure);
    }

    private static <T> T write(String sessionId, Supplier<T> request) {
        try {
            return request.get();
        } catch (AnthropicServiceException failure) {
            if (failure.statusCode() >= 500 || failure.statusCode() == 408) {
                throw new SubmissionUnknownException(sessionId, serviceFailure(failure));
            }
            throw serviceFailure(failure);
        } catch (RuntimeException failure) {
            throw new SubmissionUnknownException(sessionId, failure);
        }
    }

    private final class Beta implements BetaService {
        public ResourceService agents() {
            return id -> read(() -> json(sdk.beta().agents().retrieve(id)));
        }

        public ResourceService environments() {
            return id -> read(() -> json(sdk.beta().environments().retrieve(id)));
        }

        public SessionService sessions() { return new Sessions(); }
    }

    private final class Sessions implements SessionService {
        public Session create(SessionCreateParams params) {
            var builder = com.anthropic.models.beta.sessions.SessionCreateParams.builder()
                    .agent(params.agentId()).environmentId(params.environmentId());
            if (params.title() != null) builder.title(params.title());
            var request = builder.build();
            return write(null, () -> session(json(sdk.beta().sessions().create(request))));
        }

        public Session retrieve(String sessionId) {
            return read(() -> session(json(sdk.beta().sessions().retrieve(sessionId))));
        }

        public EventService events() { return new Events(); }
    }

    private final class Events implements EventService {
        public EventSendResult send(String sessionId, String text) {
            var message = BetaManagedAgentsUserMessageEventParams.builder()
                    .addTextContent(Objects.requireNonNull(text, "text")).build();
            var request = EventSendParams.builder().sessionId(sessionId).addEvent(message).build();
            return write(sessionId, () -> {
                var response = sdk.beta().sessions().events().send(request);
                return new EventSendResult(response.data().orElse(List.of()).stream()
                        .map(value -> value.id()).toList());
            });
        }

        public EventPage list(String sessionId, String pageToken) {
            return read(() -> {
                var request = EventListParams.builder().sessionId(sessionId)
                        .order(EventListParams.Order.ASC).page(pageToken).build();
                var page = sdk.beta().sessions().events().list(request);
                return new EventPage(page.data().stream().map(AnthropicManagedAgents::event).toList(),
                        page.nextPageRaw().orElse(null), null);
            });
        }

        public EventStream streamStreaming(String sessionId, String cursor) {
            if (cursor != null) {
                throw new IllegalArgumentException("Anthropic recovery requires stream plus history; use SessionEvents.replayAndStream");
            }
            var response = read(() -> sdk.beta().sessions().events().streamStreaming(sessionId));
            return new EventStream() {
                public java.util.stream.Stream<StreamFrame> stream() {
                    return response.stream().map(value -> new StreamFrame(event(value), null));
                }

                public void close() { response.close(); }
            };
        }
    }
}
