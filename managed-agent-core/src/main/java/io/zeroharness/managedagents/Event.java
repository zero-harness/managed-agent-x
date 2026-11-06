package io.zeroharness.managedagents;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;

/** Type and payload follow Anthropic semantics; the original provider event is retained. */
public record Event(String id, String type, JsonNode payload, JsonNode nativeEvent) {
    public String text() {
        StringBuilder text = new StringBuilder();
        for (JsonNode block : payload.path("content")) {
            if ("text".equals(block.path("type").asText())) {
                text.append(block.path("text").asText());
            }
        }
        return text.toString();
    }

    public String stopReason() {
        return payload.path("stop_reason").path("type").asText("");
    }

    public List<String> blockingEventIds() {
        List<String> ids = new ArrayList<>();
        payload.path("stop_reason").path("event_ids").forEach(id -> ids.add(id.asText()));
        return List.copyOf(ids);
    }
}
