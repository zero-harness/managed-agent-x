package io.zeroharness.managedagents;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Reduces complete primary-thread events for a single turn, in history order. Not thread-safe. */
public final class SessionResult {
    public enum State { RUNNING, COMPLETED, NEEDS_ACTION, FAILED, TERMINATED, UNKNOWN }

    public record Snapshot(State state, List<Event> messages, List<String> blockingEventIds,
                           String stopReason, Event error) {
        public Snapshot {
            messages = List.copyOf(messages);
            blockingEventIds = List.copyOf(blockingEventIds);
        }

        public String text() {
            return messages.stream().map(Event::text).reduce("", String::concat);
        }

        public boolean stopped() { return state != State.RUNNING; }
    }

    private final String inputEventId;
    private final Set<String> seen = new HashSet<>();
    private final List<Event> messages = new ArrayList<>();
    private boolean started;
    private State state = State.RUNNING;
    private List<String> blockingIds = List.of();
    private String stopReason = "";
    private Event error;

    private SessionResult(String inputEventId) {
        this.inputEventId = inputEventId;
    }

    public static SessionResult forInput(String eventId) {
        return new SessionResult(Objects.requireNonNull(eventId, "eventId"));
    }

    /** Only use for a newly created, exclusively owned session with one submitted input. */
    public static SessionResult forNewSession() {
        return new SessionResult(null);
    }

    public void accept(Event event) {
        // Previews may share IDs with complete messages. Never deduplicate on a preview ID.
        if (event.type().equals("event_start") || event.type().equals("event_delta")
                || event.type().equals("agent.delta")) return;
        // Queued user events have no processed_at and cannot establish an execution boundary.
        if (event.type().equals("user.message") && !event.nativeEvent().hasNonNull("processed_at")) return;
        if (event.id() == null || event.id().isBlank() || !seen.add(event.id())) return;
        if (!started) {
            started = inputEventId != null ? inputEventId.equals(event.id())
                    : event.type().equals("user.message") || event.type().equals("session.status_running")
                            || event.type().equals("agent.message") || event.type().equals("session.error");
            if (!started) return;
        }
        if (event.type().equals("agent.message")) messages.add(event);
        switch (event.type()) {
            case "session.status_running" -> {
                state = State.RUNNING;
                blockingIds = List.of();
                stopReason = "";
            }
            case "session.status_idle" -> {
                if (state == State.FAILED || state == State.TERMINATED) return;
                stopReason = event.stopReason();
                blockingIds = event.blockingEventIds();
                state = switch (stopReason) {
                    case "end_turn" -> State.COMPLETED;
                    case "requires_action", "budget_reached" -> State.NEEDS_ACTION;
                    default -> State.UNKNOWN;
                };
            }
            case "session.error" -> { state = State.FAILED; error = event; }
            case "session.status_terminated" -> state = State.TERMINATED;
            default -> { }
        }
    }

    public Snapshot snapshot() {
        return new Snapshot(state, messages, blockingIds, stopReason, error);
    }
}
