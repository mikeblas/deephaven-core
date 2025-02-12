package io.deephaven.db.v2.sources.chunk.util.pools;

import io.deephaven.db.v2.sources.chunk.Chunk;
import io.deephaven.util.SafeCloseable;

/**
 * Marker interface for {@link Chunk} subclasses that can be kept with in a {@link ChunkPool}, and whose
 * {@link #close()} method will return them to the appropriate pool.
 */
public interface PoolableChunk extends SafeCloseable {
}
