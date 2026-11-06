package io.zeroharness.managedagents;

import java.util.List;

/** Empty IDs mean accepted without input correlation, not permission to resend. */
public record EventSendResult(List<String> eventIds) {
    public EventSendResult {
        eventIds = List.copyOf(eventIds);
    }
}
