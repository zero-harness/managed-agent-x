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

    @Test
    void restoringOneTurnDoesNotIncludeLaterTurns() throws Exception {
        var first = SessionResult.forInput("input-1");
        var second = SessionResult.forInput("input-2");
        for (String json : java.util.List.of(
                """
                {"id":"input-1","type":"user.message","processed_at":"2026-11-09T00:00:00Z"}
                """,
                """
                {"id":"answer-1","type":"agent.message","content":[{"type":"text","text":"First answer"}]}
                """,
                """
                {"id":"done-1","type":"session.status_idle","stop_reason":{"type":"end_turn"}}
                """,
                """
                {"id":"input-2","type":"user.message","processed_at":"2026-11-09T00:01:00Z"}
                """,
                """
                {"id":"running-2","type":"session.status_running"}
                """,
                """
                {"id":"answer-2","type":"agent.message","content":[{"type":"text","text":"Second answer"}]}
                """,
                """
                {"id":"done-2","type":"session.status_idle","stop_reason":{"type":"end_turn"}}
                """)) {
            Event value = event(json);
            first.accept(value);
            second.accept(value);
        }
        assertEquals("First answer", first.snapshot().text());
        assertEquals("Second answer", second.snapshot().text());
        assertEquals(SessionResult.State.COMPLETED, first.snapshot().state());
        assertEquals(SessionResult.State.COMPLETED, second.snapshot().state());
    }

    @Test
    void anotherProcessedInputBeforeOutcomeMakesTurnUnknown() throws Exception {
        var result = SessionResult.forInput("input-1");
        result.accept(event("""
            {"id":"input-1","type":"user.message","processed_at":"2026-11-09T00:00:00Z"}
            """));
        result.accept(event("""
            {"id":"input-2","type":"user.message","processed_at":"2026-11-09T00:01:00Z"}
            """));
        result.accept(event("""
            {"id":"next-answer","type":"agent.message","content":[{"type":"text","text":"Unattributable"}]}
            """));
        result.accept(event("""
            {"id":"next-done","type":"session.status_idle","stop_reason":{"type":"end_turn"}}
            """));
        assertEquals(SessionResult.State.UNKNOWN, result.snapshot().state());
        assertEquals("", result.snapshot().text());
    }
}
