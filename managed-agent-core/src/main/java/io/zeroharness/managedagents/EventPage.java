package io.zeroharness.managedagents;

import java.util.List;

/** Both tokens are opaque. A page token is never a stream cursor. */
public record EventPage(List<Event> data, String nextPageToken, String streamCursor) {
    public EventPage {
        data = List.copyOf(data);
    }
}
