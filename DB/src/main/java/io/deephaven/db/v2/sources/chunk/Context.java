package io.deephaven.db.v2.sources.chunk;

import io.deephaven.util.SafeCloseable;

/**
 * Base interface for state/mutable data that needs to be kept over the course of an evaluation session for a
 * Chunk Source, Functor or Sink.
 *
 * @IncludeAll
 */
public interface Context extends SafeCloseable {
    /**
     * Release any resources associated with this context. The context should not be used afterwards.
     */
    default void close() {
    }

}
