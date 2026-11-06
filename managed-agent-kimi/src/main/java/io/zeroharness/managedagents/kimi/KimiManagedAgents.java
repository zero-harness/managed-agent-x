package io.zeroharness.managedagents.kimi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zeroharness.managedagents.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/** Kimi Hosted Agents REST/SSE protocol, kimi-api-version 2026-09-01-beta. */
public final class KimiManagedAgents implements ManagedAgentClient {
    public static final String API_VERSION = "2026-09-01-beta";
    private static final ObjectMapper JSON = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final String baseUrl;
    private final String apiKey;
    private final BetaService beta = new Beta();

    public KimiManagedAgents(String apiKey) {
        this(URI.create("https://api.moonshot.cn"), apiKey);
    }

    public KimiManagedAgents(URI baseUrl, String apiKey) {
        this.baseUrl = Objects.requireNonNull(baseUrl).toString().replaceAll("/+$", "");
        this.apiKey = Objects.requireNonNull(apiKey);
        if (apiKey.isBlank()) throw new IllegalArgumentException("apiKey must not be blank");
    }

    public String provider() { return "kimi"; }
    public BetaService beta() { return beta; }

    private static String encode(String value) {
        return URLEncoder.encode(Objects.requireNonNull(value), StandardCharsets.UTF_8).replace("+", "%20");
    }

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(URI.create(baseUrl + "/v1/" + path))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + apiKey)
                .header("kimi-api-version", API_VERSION);
    }

    private static String requestId(HttpResponse<?> response) {
        return response.headers().firstValue("x-request-id")
                .or(() -> response.headers().firstValue("request-id")).orElse(null);
    }

    private static void check(HttpResponse<?> response, boolean write, String sessionId) {
        int status = response.statusCode();
        if (status >= 200 && status < 300) return;
        var failure = new ManagedAgentException("Kimi request failed", status, requestId(response), null);
        if (write && (status >= 500 || status == 408)) {
            throw new SubmissionUnknownException(sessionId, failure);
        }
        throw failure;
    }

    private JsonNode exchange(String path, JsonNode body, String sessionId) {
        boolean write = body != null;
        var builder = request(path).header("Accept", "application/json");
        if (write) {
            builder.header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()));
        }
        try {
            var response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            check(response, write, sessionId);
            return response.body().isBlank() ? JSON.createObjectNode() : JSON.readTree(response.body());
        } catch (IOException failure) {
            if (write) throw new SubmissionUnknownException(sessionId, failure);
            throw new ManagedAgentException("Kimi response could not be read", 0, null, failure);
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            if (write) throw new SubmissionUnknownException(sessionId, failure);
            throw new ManagedAgentException("Kimi request interrupted", 0, null, failure);
        }
    }

    private static Session session(JsonNode node) {
        return new Session(node.path("id").asText(), node.path("status").asText(), node);
    }

    private static Event event(JsonNode node) {
        String type = node.path("type").asText();
        JsonNode payload = node.path("data");
        if (type.equals("session.status")) {
            String status = payload.path("status").asText();
            if (List.of("running", "idle", "terminated").contains(status)) type = "session.status_" + status;
        }
        return new Event(node.path("id").asText(null), type, payload, node);
    }

    private final class Beta implements BetaService {
        public ResourceService agents() { return id -> exchange("agents/" + encode(id), null, null); }
        public ResourceService environments() { return id -> exchange("environments/" + encode(id), null, null); }
        public SessionService sessions() { return new Sessions(); }
    }

    private final class Sessions implements SessionService {
        public Session create(SessionCreateParams params) {
            var body = JSON.createObjectNode().put("agent_id", params.agentId())
                    .put("environment_id", params.environmentId());
            if (params.title() != null) body.put("title", params.title());
            JsonNode response = exchange("sessions", body, null);
            if (response.path("id").asText().isBlank()) {
                throw new SubmissionUnknownException(null, new IOException("Session response has no ID"));
            }
            return session(response);
        }

        public Session retrieve(String sessionId) {
            return session(exchange("sessions/" + encode(sessionId), null, null));
        }

        public EventService events() { return new Events(); }
    }

    private final class Events implements EventService {
        public EventSendResult send(String sessionId, String text) {
            var body = JSON.createObjectNode();
            var message = body.putArray("events").addObject().put("type", "user.message");
            message.putObject("data").putArray("content").addObject()
                    .put("type", "text").put("text", Objects.requireNonNull(text, "text"));
            JsonNode response = exchange("sessions/" + encode(sessionId) + "/events", body, sessionId);
            String id = response.path("event_id").asText(null);
            return new EventSendResult(id == null ? List.of() : List.of(id));
        }

        public EventPage list(String sessionId, String pageToken) {
            String path = "sessions/" + encode(sessionId) + "/events?order=asc&page_size=100";
            if (pageToken != null) path += "&page_token=" + encode(pageToken);
            JsonNode response = exchange(path, null, null);
            if (!response.path("items").isArray()) {
                throw new ManagedAgentException("Kimi history response has no items array", 0, null, null);
            }
            List<Event> events = new ArrayList<>();
            response.path("items").forEach(node -> events.add(event(node)));
            return new EventPage(events, response.path("next_page_token").asText(null),
                    response.path("stream_cursor").asText(null));
        }

        public EventStream streamStreaming(String sessionId, String cursor) {
            String path = "sessions/" + encode(sessionId) + "/events/stream?cursor="
                    + encode(cursor == null ? "now" : cursor);
            try {
                var response = http.send(request(path).header("Accept", "text/event-stream").build(),
                        HttpResponse.BodyHandlers.ofInputStream());
                try {
                    check(response, false, sessionId);
                } catch (RuntimeException failure) {
                    response.body().close();
                    throw failure;
                }
                return new SseStream(response.body());
            } catch (IOException failure) {
                throw new ManagedAgentException("Kimi stream could not be opened", 0, null, failure);
            } catch (InterruptedException failure) {
                Thread.currentThread().interrupt();
                throw new ManagedAgentException("Kimi stream interrupted", 0, null, failure);
            }
        }
    }

    private static final class SseStream implements EventStream {
        private final InputStream body;
        private final BufferedReader reader;
        private boolean consumed;
        private String cursor;

        SseStream(InputStream body) {
            this.body = body;
            this.reader = new BufferedReader(new InputStreamReader(body, StandardCharsets.UTF_8));
        }

        public Stream<StreamFrame> stream() {
            if (consumed) throw new IllegalStateException("Event stream already consumed");
            consumed = true;
            var frames = new Spliterators.AbstractSpliterator<StreamFrame>(Long.MAX_VALUE, Spliterator.ORDERED) {
                public boolean tryAdvance(Consumer<? super StreamFrame> action) {
                    String kind = "";
                    StringBuilder data = new StringBuilder();
                    boolean hasId = false;
                    try {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.isEmpty()) {
                                if (data.isEmpty() && !hasId) continue;
                                if (kind.equals("error")) {
                                    throw new ManagedAgentException("Kimi event stream failed", 0, null, null);
                                }
                                JsonNode node = data.isEmpty() ? JSON.createObjectNode() : JSON.readTree(data.toString());
                                if (kind.equals("control") && node.hasNonNull("cursor")) cursor = node.path("cursor").asText();
                                Event value = kind.equals("control") || data.isEmpty() ? null : event(node);
                                action.accept(new StreamFrame(value, cursor));
                                return true;
                            }
                            if (line.startsWith(":")) continue;
                            int colon = line.indexOf(':');
                            String field = colon < 0 ? line : line.substring(0, colon);
                            String value = colon < 0 ? "" : line.substring(colon + 1);
                            if (value.startsWith(" ")) value = value.substring(1);
                            switch (field) {
                                case "event" -> kind = value;
                                case "id" -> {
                                    if (value.indexOf('\0') < 0) { cursor = value; hasId = true; }
                                }
                                case "data" -> {
                                    if (!data.isEmpty()) data.append('\n');
                                    data.append(value);
                                }
                                default -> { }
                            }
                        }
                        // A partial frame at EOF is discarded. Resume from the last delivered frame.
                        return false;
                    } catch (IOException failure) {
                        throw new ManagedAgentException("Kimi event stream disconnected", 0, null, failure);
                    }
                }
            };
            return StreamSupport.stream(frames, false).onClose(this::close);
        }

        public void close() {
            try { body.close(); }
            catch (IOException failure) {
                throw new ManagedAgentException("Kimi stream could not be closed", 0, null, failure);
            }
        }
    }
}
