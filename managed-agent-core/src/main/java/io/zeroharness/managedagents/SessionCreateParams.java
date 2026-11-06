package io.zeroharness.managedagents;

import java.util.Objects;

public record SessionCreateParams(String agentId, String environmentId, String title) {
    public SessionCreateParams {
        Objects.requireNonNull(agentId, "agentId");
        Objects.requireNonNull(environmentId, "environmentId");
        if (agentId.isBlank() || environmentId.isBlank()) {
            throw new IllegalArgumentException("Agent and environment IDs must not be blank");
        }
    }
}
