package io.zeroharness.managedagents;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SessionEventsTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    private Event event(String json) throws Exception {
        var node = JSON.readTree(json);
        return new Event(node.path("id").asText(), node.path("type").asText(), node, node);
    }

    @Test
    void queuedHistoryInputDoesNotHideProcessedLiveEcho() throws Exception {
        var queued = event("{\"id\":\"input\",\"type\":\"user.message\",\"processed_at\":null}");
        var processed = event("{\"id\":\"input\",\"type\":\"user.message\",\"processed_at\":\"2026-11-06T00:00:00Z\"}");
        var done = event("{\"id\":\"done\",\"type\":\"session.status_idle\",\"stop_reason\":{\"type\":\"end_turn\"}}");
        var open = new AtomicBoolean();
        var closed = new AtomicBoolean();
        var events = new ReadOnlyEvents() {
            public EventPage list(String sessionId, String token) {
                assertTrue(open.get(), "Open stream before reading history");
                return new EventPage(List.of(queued), null, null);
            }
            public EventStream streamStreaming(String sessionId, String cursor) {
                open.set(true);
                return new EventStream() {
                    public Stream<StreamFrame> stream() {
                        return Stream.of(new StreamFrame(processed, null), new StreamFrame(done, null));
                    }
                    public void close() { closed.set(true); }
                };
            }
        };
        var result = SessionResult.forInput("input");
        try (var stream = SessionEvents.replayAndStream(events, "session")) {
            stream.stream().forEach(frame -> result.accept(frame.event()));
        }
        assertEquals(SessionResult.State.COMPLETED, result.snapshot().state());
        assertTrue(closed.get());
    }

    @Test
    void zeroWaitTimeoutReturnsSnapshotWithoutSendingOrCancelling() throws Exception {
        var events = new ReadOnlyEvents() {
            public EventPage list(String sessionId, String token) {
                return new EventPage(List.of(), null, null);
            }
        };
        var snapshot = SessionEvents.await(events, "session", SessionResult.forInput("input"), Duration.ZERO);
        assertEquals(SessionResult.State.RUNNING, snapshot.state());
    }

    @Test
    void repeatedPageTokenFailsInsteadOfLoopingForever() {
        var events = new ReadOnlyEvents() {
            public EventPage list(String sessionId, String token) {
                return new EventPage(List.of(), "same-token", null);
            }
        };
        assertThrows(ManagedAgentException.class,
                () -> SessionEvents.readHistory(events, "session", SessionResult.forNewSession()));
    }

    @Test
    void errorWithoutRunningEventIsFailureAndLaterIdleCannotTurnItIntoSuccess() throws Exception {
        var result = SessionResult.forNewSession();
        result.accept(event("{\"id\":\"error\",\"type\":\"session.error\"}"));
        result.accept(event("{\"id\":\"idle\",\"type\":\"session.status_idle\",\"stop_reason\":{\"type\":\"end_turn\"}}"));
        assertEquals(SessionResult.State.FAILED, result.snapshot().state());
    }

    private abstract static class ReadOnlyEvents implements ManagedAgentClient.EventService {
        public EventSendResult send(String sessionId, String text) { throw new AssertionError("Recovery must not submit"); }
        public EventStream streamStreaming(String sessionId, String cursor) { throw new AssertionError("Unexpected stream"); }
    }
}
