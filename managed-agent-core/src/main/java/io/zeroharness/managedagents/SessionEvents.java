package io.zeroharness.managedagents;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

/** History recovery uses reads only; it never submits or restarts work. */
public final class SessionEvents {
    private SessionEvents() {}

    public static void readHistory(ManagedAgentClient.EventService events, String sessionId,
                                   SessionResult result) {
        Set<String> pages = new HashSet<>();
        String token = null;
        do {
            EventPage page = events.list(sessionId, token);
            page.data().forEach(result::accept);
            token = page.nextPageToken();
            if (token != null && !pages.add(token)) {
                throw new ManagedAgentException("Provider repeated a history page token", 0, null, null);
            }
        } while (token != null);
    }

    /** Open live first, then read history and deduplicate the overlapping complete events. */
    public static EventStream replayAndStream(ManagedAgentClient.EventService events, String sessionId) {
        EventStream live = events.streamStreaming(sessionId);
        try {
            var history = new java.util.ArrayList<StreamFrame>();
            Set<String> pages = new HashSet<>();
            String token = null;
            do {
                EventPage page = events.list(sessionId, token);
                page.data().forEach(event -> history.add(new StreamFrame(event, null)));
                token = page.nextPageToken();
                if (token != null && !pages.add(token)) {
                    throw new ManagedAgentException("Provider repeated a history page token", 0, null, null);
                }
            } while (token != null);
            Set<String> seen = new HashSet<>();
            Stream<StreamFrame> merged = Stream.concat(history.stream(), live.stream()).filter(frame -> {
                if (frame.event() == null || frame.event().id() == null) return true;
                String type = frame.event().type();
                if (type.equals("event_start") || type.equals("event_delta") || type.equals("agent.delta")) return true;
                return seen.add(frame.event().id());
            });
            return new EventStream() {
                public Stream<StreamFrame> stream() { return merged; }
                public void close() { live.close(); }
            };
        } catch (RuntimeException failure) {
            live.close();
            throw failure;
        }
    }
}
