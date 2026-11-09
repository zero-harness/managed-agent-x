package io.zeroharness.managedagents.examples;

import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.zeroharness.managedagents.*;
import io.zeroharness.managedagents.anthropic.AnthropicManagedAgents;
import io.zeroharness.managedagents.kimi.KimiManagedAgents;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Real adapter/official-SDK calls against local protocol fixtures; no paid services. */
class ProviderContractTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    @ParameterizedTest
    @ValueSource(strings = {"anthropic", "kimi"})
    void sameResourceCallsRecoverWithoutResubmitting(String provider) throws Exception {
        try (var fixture = new Fixture(provider); var client = fixture.client()) {
            assertEquals("agent-1", client.beta().agents().retrieve("agent-1").path("id").asText());
            assertEquals("environment-1", client.beta().environments().retrieve("environment-1").path("id").asText());
            var session = client.beta().sessions().create(new SessionCreateParams("agent-1", "environment-1", "Demo"));
            assertEquals(session.id(), client.beta().sessions().retrieve(session.id()).id());
            var events = client.beta().sessions().events();
            var result = SessionResult.forNewSession();
            String cursor = null;
            try (var stream = events.streamStreaming(session.id())) {
                assertEquals(List.of("input-1"), events.send(session.id(), "Do the task").eventIds());
                var frames = stream.stream().toList();
                for (var frame : frames) {
                    if (frame.event() != null) result.accept(frame.event());
                    if (frame.cursor() != null) cursor = frame.cursor();
                }
            }
            assertEquals(SessionResult.State.RUNNING, result.snapshot().state());
            assertEquals("Complete result.", result.snapshot().text());
            if (provider.equals("kimi")) {
                assertEquals("cursor:/message+?opaque", cursor);
                assertNotEquals("message-1", cursor);
                try (var resumed = events.streamStreaming(session.id(), cursor)) {
                    resumed.stream().forEach(frame -> { if (frame.event() != null) result.accept(frame.event()); });
                }
            } else {
                assertNull(cursor);
                assertThrows(IllegalArgumentException.class, () -> events.streamStreaming(session.id(), "message-1"));
                try (var resumed = SessionEvents.replayAndStream(events, session.id())) {
                    resumed.stream().forEach(frame -> { if (frame.event() != null) result.accept(frame.event()); });
                }
                int secondStream = fixture.calls.lastIndexOf("stream");
                assertTrue(secondStream < fixture.calls.indexOf("history"), "Live connection must precede history reads");
            }
            // History remains sufficient after process restart and uses both native pages.
            var restored = SessionResult.forInput("input-1");
            SessionEvents.readHistory(events, session.id(), restored);
            assertEquals(SessionResult.State.COMPLETED, restored.snapshot().state());
            assertEquals("Complete result.", restored.snapshot().text());
            assertEquals(1, restored.snapshot().messages().size());
            assertEquals(SessionResult.State.COMPLETED, result.snapshot().state());
            assertEquals(1, result.snapshot().messages().size());
            assertEquals(1, fixture.submissions);
            assertTrue(fixture.pages.contains("page:/2+?opaque"));
            if (provider.equals("kimi")) {
                assertTrue(fixture.cursors.contains("cursor:/message+?opaque"));
                assertEquals("cursor:/history2+?opaque", events.list(session.id(), "page:/2+?opaque").streamCursor());
            }
            JsonNode create = fixture.created;
            assertEquals("agent-1", create.path(provider.equals("kimi") ? "agent_id" : "agent").asText());
            assertEquals("environment-1", create.path("environment_id").asText());
            JsonNode input = fixture.sent.path("events").get(0);
            assertEquals("user.message", input.path("type").asText());
            JsonNode payload = provider.equals("kimi") ? input.path("data") : input;
            assertEquals("Do the task", payload.path("content").get(0).path("text").asText());
            assertNull(fixture.handlerFailure);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"anthropic", "kimi"})
    void sequentialTurnsShareSessionButKeepTheirResultsSeparate(String provider) throws Exception {
        try (var fixture = new Fixture(provider); var client = fixture.client()) {
            var results = ConversationDemo.run(client,
                    new SessionCreateParams("agent-1", "environment-1", "Two turns"),
                    "Do the task", "Continue using the previous context");
            assertEquals(2, results.size());
            assertEquals("Complete result.", results.get(0).text());
            assertEquals("Follow-up result.", results.get(1).text());
            assertTrue(results.stream().allMatch(result -> result.state() == SessionResult.State.COMPLETED));
            assertEquals(1, fixture.creations);
            assertEquals(2, fixture.submissions);
            assertTrue(fixture.pages.contains("page:/3+?opaque"));
            var restored = SessionResult.forInput("input-1");
            SessionEvents.readHistory(client.beta().sessions().events(), "session-1", restored);
            assertEquals("Complete result.", restored.snapshot().text());
            assertEquals(1, restored.snapshot().messages().size());
            assertNull(fixture.handlerFailure);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"anthropic", "kimi"})
    void conversationDoesNotSendAnotherTurnWhenActionIsRequired(String provider) throws Exception {
        try (var fixture = new Fixture(provider); var client = fixture.client()) {
            fixture.action = true;
            var results = ConversationDemo.run(client,
                    new SessionCreateParams("agent-1", "environment-1", "Action required"),
                    "Do the task", "Must not be sent");
            assertEquals(1, results.size());
            assertEquals(SessionResult.State.NEEDS_ACTION, results.get(0).state());
            assertEquals(List.of("tool-event-1"), results.get(0).blockingEventIds());
            assertEquals(1, fixture.submissions);
            assertNull(fixture.handlerFailure);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"anthropic", "kimi"})
    void distinguishesActionFromCompletion(String provider) throws Exception {
        try (var fixture = new Fixture(provider); var client = fixture.client()) {
            fixture.action = true;
            var result = SessionResult.forInput("input-1");
            SessionEvents.readHistory(client.beta().sessions().events(), "session-1", result);
            assertEquals(SessionResult.State.NEEDS_ACTION, result.snapshot().state());
            assertEquals(List.of("tool-event-1"), result.snapshot().blockingEventIds());
            assertEquals("requires_action", result.snapshot().stopReason());
            assertEquals(0, fixture.submissions);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"anthropic", "kimi"})
    void ambiguousPostIsNeverRetried(String provider) throws Exception {
        try (var fixture = new Fixture(provider); var client = fixture.client()) {
            fixture.failWrite = true;
            var failure = assertThrows(SubmissionUnknownException.class,
                    () -> client.beta().sessions().events().send("session-1", "Do the task"));
            assertEquals("session-1", failure.sessionId());
            assertEquals(500, failure.statusCode());
            assertEquals("request-1", failure.requestId());
            assertEquals(1, fixture.submissions);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"anthropic", "kimi"})
    void streamFailureDoesNotDeclareExecutionComplete(String provider) throws Exception {
        try (var fixture = new Fixture(provider); var client = fixture.client()) {
            fixture.streamOverride = "event: error\ndata: {\"error\":{\"type\":\"api_error\",\"message\":\"stream failed\"}}\n\n";
            var result = SessionResult.forNewSession();
            try (var stream = client.beta().sessions().events().streamStreaming("session-1")) {
                assertThrows(RuntimeException.class, () -> stream.stream().forEach(frame -> {
                    if (frame.event() != null) result.accept(frame.event());
                }));
            }
            assertEquals(SessionResult.State.RUNNING, result.snapshot().state());
            assertEquals(0, fixture.submissions);
        }
    }

    @Test
    void kimiMultilineSsePreservesControlCursorAndOriginalEvent() throws Exception {
        try (var fixture = new Fixture("kimi"); var client = fixture.client()) {
            fixture.streamOverride = """
                : heartbeat

                event: control
                id: cursor-control
                data: {"cursor":"cursor-control","reason":"tail"}

                id: cursor-message
                event: agent.message
                data: {"id":"business-event","type":"agent.message",
                data: "data":{"content":[{"type":"text","text":"Multiline result"}]}}

                """;
            try (var stream = client.beta().sessions().events().streamStreaming("session-1")) {
                var frames = stream.stream().toList();
                assertEquals(2, frames.size());
                assertNull(frames.get(0).event());
                assertEquals("cursor-control", frames.get(0).cursor());
                assertEquals("cursor-message", frames.get(1).cursor());
                assertEquals("business-event", frames.get(1).event().id());
                assertEquals("Multiline result", frames.get(1).event().text());
                assertTrue(frames.get(1).event().nativeEvent().has("data"));
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"anthropic", "kimi"})
    void lostWriteAcknowledgementDoesNotResubmit(String provider) throws Exception {
        try (var fixture = new Fixture(provider); var client = fixture.client()) {
            fixture.dropWriteResponse = true;
            var failure = assertThrows(SubmissionUnknownException.class,
                    () -> client.beta().sessions().events().send("session-1", "Do the task"));
            assertEquals("session-1", failure.sessionId());
            assertEquals(1, fixture.submissions);
            // The user can recover by reading; the SDK never reissues the input.
            SessionEvents.readHistory(client.beta().sessions().events(), "session-1", SessionResult.forInput("input-1"));
            assertEquals(1, fixture.submissions);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"anthropic", "kimi"})
    void rejectedReadKeepsStatusAndRequestId(String provider) throws Exception {
        try (var fixture = new Fixture(provider); var client = fixture.client()) {
            fixture.reject = true;
            var failure = assertThrows(ManagedAgentException.class,
                    () -> client.beta().sessions().retrieve("session-1"));
            assertEquals(401, failure.statusCode());
            assertEquals("request-1", failure.requestId());
            assertEquals(0, fixture.submissions);
        }
    }

    static final class Fixture implements AutoCloseable {
        final String provider;
        final HttpServer server;
        final List<String> calls = new ArrayList<>();
        final List<String> pages = new ArrayList<>();
        final List<String> cursors = new ArrayList<>();
        volatile int submissions;
        volatile int creations;
        volatile int streams;
        volatile boolean action;
        volatile boolean failWrite;
        volatile boolean dropWriteResponse;
        volatile boolean reject;
        volatile JsonNode created;
        volatile JsonNode sent;
        volatile Throwable handlerFailure;
        volatile String streamOverride;

        Fixture(String provider) throws IOException {
            this.provider = provider;
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/v1/", this::handle);
            server.start();
        }

        ManagedAgentClient client() {
            URI base = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
            if (provider.equals("kimi")) return new KimiManagedAgents(base, "offline-key");
            return new AnthropicManagedAgents(AnthropicOkHttpClient.builder()
                    .apiKey("offline-key").timeout(java.time.Duration.ofSeconds(3))
                    .baseUrl(base.toString()).build());
        }

        String sample(String file) throws IOException {
            try (var input = getClass().getResourceAsStream("/contracts/" + provider + "/" + file)) {
                if (input == null) throw new IOException("Missing fixture " + file);
                return new String(input.readAllBytes(), StandardCharsets.UTF_8);
            }
        }

        String query(HttpExchange exchange, String name) {
            String query = exchange.getRequestURI().getRawQuery();
            if (query == null) return null;
            for (String pair : query.split("&")) {
                String[] parts = pair.split("=", 2);
                if (parts[0].equals(name)) return URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
            }
            return null;
        }

        void handle(HttpExchange exchange) throws IOException {
            try {
                if (provider.equals("kimi")) {
                    assertEquals("Bearer offline-key", exchange.getRequestHeaders().getFirst("Authorization"));
                    assertEquals(KimiManagedAgents.API_VERSION, exchange.getRequestHeaders().getFirst("kimi-api-version"));
                } else {
                    assertTrue("offline-key".equals(exchange.getRequestHeaders().getFirst("x-api-key"))
                                    || "Bearer offline-key".equals(exchange.getRequestHeaders().getFirst("Authorization")),
                            "Anthropic tests must use supplied offline credentials");
                    assertTrue(exchange.getRequestHeaders().getFirst("anthropic-beta").contains("managed-agents-2026-04-01"));
                }
                exchange.getResponseHeaders().set("request-id", "request-1");
                if (reject) {
                    respond(exchange, 401, "{\"type\":\"error\",\"error\":{\"type\":\"authentication_error\",\"message\":\"Rejected\"}}", false);
                    return;
                }
                String path = exchange.getRequestURI().getPath();
                boolean post = exchange.getRequestMethod().equals("POST");
                if (path.equals("/v1/sessions") && post) {
                    creations++;
                    created = JSON.readTree(exchange.getRequestBody());
                    respond(exchange, 200, sample("session.json"), false);
                } else if (path.endsWith("/events") && post) {
                    submissions++;
                    sent = JSON.readTree(exchange.getRequestBody());
                    if (dropWriteResponse) {
                        exchange.close();
                        return;
                    }
                    String inputId = "input-" + submissions;
                    String response = provider.equals("kimi") ? "{\"event_id\":\"" + inputId + "\"}"
                            : "{\"data\":[{\"id\":\"" + inputId + "\",\"type\":\"user.message\",\"processed_at\":null}]}";
                    respond(exchange, failWrite ? 500 : 202, failWrite
                            ? "{\"type\":\"error\",\"error\":{\"type\":\"api_error\",\"message\":\"Lost acknowledgement\"}}" : response, false);
                } else if (path.endsWith("/events/stream")) {
                    calls.add("stream");
                    cursors.add(query(exchange, "cursor"));
                    streams++;
                    respond(exchange, 200, streamOverride != null ? streamOverride : sample("stream-" + streams + ".sse"), true);
                } else if (path.endsWith("/events")) {
                    calls.add("history");
                    assertEquals("asc", query(exchange, "order"));
                    String token = query(exchange, provider.equals("kimi") ? "page_token" : "page");
                    pages.add(token);
                    String response = sample(token == null ? "history-1.json"
                            : token.equals("page:/3+?opaque") ? "history-3.json" : "history-2.json");
                    if (submissions >= 2 && "page:/2+?opaque".equals(token)) {
                        var page = (com.fasterxml.jackson.databind.node.ObjectNode) JSON.readTree(response);
                        page.put(provider.equals("kimi") ? "next_page_token" : "next_page", "page:/3+?opaque");
                        response = page.toString();
                    }
                    if (action && token != null) {
                        var page = (com.fasterxml.jackson.databind.node.ObjectNode) JSON.readTree(response);
                        page.putArray(provider.equals("kimi") ? "items" : "data").add(JSON.readTree(sample("action.json")));
                        response = page.toString();
                    }
                    respond(exchange, 200, response, false);
                } else if (path.contains("/agents/")) {
                    respond(exchange, 200, "{\"id\":\"agent-1\",\"type\":\"agent\"}", false);
                } else if (path.contains("/environments/")) {
                    respond(exchange, 200, "{\"id\":\"environment-1\",\"type\":\"environment\"}", false);
                } else {
                    respond(exchange, 200, sample("session.json"), false);
                }
            } catch (Throwable failure) {
                handlerFailure = failure;
                respond(exchange, 500, "{}", false);
            }
        }

        void respond(HttpExchange exchange, int status, String text, boolean sse) throws IOException {
            byte[] body = text.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", sse ? "text/event-stream" : "application/json");
            exchange.sendResponseHeaders(status, body.length);
            try (var output = exchange.getResponseBody()) { output.write(body); }
        }

        public void close() { server.stop(0); }
    }
}
