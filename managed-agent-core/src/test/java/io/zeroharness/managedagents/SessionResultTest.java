package io.zeroharness.managedagents;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SessionResultTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    private Event event(String json) throws Exception {
        var node = JSON.readTree(json);
        return new Event(node.path("id").asText(null), node.path("type").asText(), node, node);
    }

    @Test
    void ignoresOldTurnAndDeduplicatesCompleteMessages() throws Exception {
        var result = SessionResult.forInput("input-2");
        result.accept(event("""
            {"id":"old-idle","type":"session.status_idle","stop_reason":{"type":"end_turn"}}
            """));
        assertEquals(SessionResult.State.RUNNING, result.snapshot().state());
        result.accept(event("""
            {"id":"input-2","type":"user.message","processed_at":"2026-11-06T00:00:00Z"}
            """));
        result.accept(event("""
            {"id":"message","type":"event_delta","delta":"preview"}
            """));
        var complete = event("""
            {"id":"message","type":"agent.message","content":[{"type":"text","text":"full answer"}]}
            """ );
        result.accept(complete);
        result.accept(complete);
        result.accept(event("""
            {"id":"done","type":"session.status_idle","stop_reason":{"type":"end_turn"}}
            """));
        assertEquals(SessionResult.State.COMPLETED, result.snapshot().state());
        assertEquals("full answer", result.snapshot().text());
        assertEquals(1, result.snapshot().messages().size());
    }

    @Test
    void distinguishesActionUnknownIdleAndFailure() throws Exception {
        var result = SessionResult.forNewSession();
        result.accept(event("{\"id\":\"running\",\"type\":\"session.status_running\"}"));
        result.accept(event("""
            {"id":"action","type":"session.status_idle","stop_reason":{"type":"requires_action","event_ids":["tool-1"]}}
            """));
        assertEquals(SessionResult.State.NEEDS_ACTION, result.snapshot().state());
        assertEquals(java.util.List.of("tool-1"), result.snapshot().blockingEventIds());
        result.accept(event("{\"id\":\"resumed\",\"type\":\"session.status_running\"}"));
        assertTrue(result.snapshot().blockingEventIds().isEmpty());
        result.accept(event("{\"id\":\"unknown\",\"type\":\"session.status_idle\"}"));
        assertEquals(SessionResult.State.UNKNOWN, result.snapshot().state());
        result.accept(event("{\"id\":\"error\",\"type\":\"session.error\",\"error\":{\"message\":\"failed\"}}"));
        assertEquals(SessionResult.State.FAILED, result.snapshot().state());
        assertNotNull(result.snapshot().error());
    }

    @Test
    void queuedInputCannotEstablishTurnBoundary() throws Exception {
        var result = SessionResult.forInput("input");
        result.accept(event("{\"id\":\"input\",\"type\":\"user.message\",\"processed_at\":null}"));
        result.accept(event("{\"id\":\"old-idle\",\"type\":\"session.status_idle\",\"stop_reason\":{\"type\":\"end_turn\"}}"));
        assertEquals(SessionResult.State.RUNNING, result.snapshot().state());
        result.accept(event("{\"id\":\"input\",\"type\":\"user.message\",\"processed_at\":\"2026-11-06T00:00:00Z\"}"));
        result.accept(event("{\"id\":\"done\",\"type\":\"session.status_idle\",\"stop_reason\":{\"type\":\"end_turn\"}}"));
        assertEquals(SessionResult.State.COMPLETED, result.snapshot().state());
    }
}
