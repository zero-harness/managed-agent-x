package io.zeroharness.managedagents;

/** A control frame has a null event. Cursor is scoped to its provider and session. */
public record StreamFrame(Event event, String cursor) {}
