package io.zeroharness.managedagents;

import com.fasterxml.jackson.databind.JsonNode;

/** Resource status alone does not establish the outcome of a submitted turn. */
public record Session(String id, String status, JsonNode nativeSession) {}
