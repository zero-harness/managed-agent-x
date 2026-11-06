package io.zeroharness.managedagents;

import java.util.stream.Stream;

/** One-shot stream. Closing it releases the HTTP connection, not the remote task. */
public interface EventStream extends AutoCloseable {
    Stream<StreamFrame> stream();

    @Override
    void close();
}
