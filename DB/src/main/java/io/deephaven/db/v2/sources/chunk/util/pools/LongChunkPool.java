/* ---------------------------------------------------------------------------------------------------------------------
 * AUTO-GENERATED CLASS - DO NOT EDIT MANUALLY - for any changes edit CharChunkPool and regenerate
 * ------------------------------------------------------------------------------------------------------------------ */
package io.deephaven.db.v2.sources.chunk.util.pools;

import io.deephaven.db.tables.utils.QueryPerformanceRecorder;
import io.deephaven.db.v2.sources.chunk.Attributes.Any;
import io.deephaven.db.v2.sources.chunk.*;
import io.deephaven.util.datastructures.SegmentedSoftPool;
import org.jetbrains.annotations.NotNull;

import static io.deephaven.db.v2.sources.chunk.util.pools.ChunkPoolConstants.*;

/**
 * {@link ChunkPool} implementation for chunks of longs.
 */
@SuppressWarnings("rawtypes")
public final class LongChunkPool implements ChunkPool {

    /**
     * Sub-pools by power-of-two sizes for {@link WritableLongChunk}s.
     */
    private final SegmentedSoftPool<WritableLongChunk>[] writableLongChunks;

    /**
     * Sub-pool of {@link ResettableLongChunk}s.
     */
    private final SegmentedSoftPool<ResettableLongChunk> resettableLongChunks;

    /**
     * Sub-pool of {@link ResettableWritableLongChunk}s.
     */
    private final SegmentedSoftPool<ResettableWritableLongChunk> resettableWritableLongChunks;

    LongChunkPool() {
        //noinspection unchecked
        writableLongChunks = new SegmentedSoftPool[NUM_POOLED_CHUNK_CAPACITIES];
        for (int pcci = 0; pcci < NUM_POOLED_CHUNK_CAPACITIES; ++pcci) {
            final int chunkLog2Capacity = pcci + SMALLEST_POOLED_CHUNK_LOG2_CAPACITY;
            final int chunkCapacity = 1 << chunkLog2Capacity;
            writableLongChunks[pcci] = new SegmentedSoftPool<>(
                    SUB_POOL_SEGMENT_CAPACITY,
                    () -> QueryPerformanceRecorder.recordPoolAllocation(() -> WritableLongChunk.makeWritableChunkForPool(chunkCapacity)),
                    (final WritableLongChunk chunk) -> chunk.setSize(chunkCapacity)
            );
        }
        resettableLongChunks = new SegmentedSoftPool<>(
                SUB_POOL_SEGMENT_CAPACITY,
                () -> QueryPerformanceRecorder.recordPoolAllocation(ResettableLongChunk::makeResettableChunkForPool),
                ResettableLongChunk::clear
        );
        resettableWritableLongChunks = new SegmentedSoftPool<>(
                SUB_POOL_SEGMENT_CAPACITY,
                () -> QueryPerformanceRecorder.recordPoolAllocation(ResettableWritableLongChunk::makeResettableChunkForPool),
                ResettableWritableLongChunk::clear
        );
    }

    @Override
    public final <ATTR extends Any> WritableChunk<ATTR> takeWritableChunk(final int capacity) {
        return takeWritableLongChunk(capacity);
    }

    @Override
    public final <ATTR extends Any> void giveWritableChunk(@NotNull final WritableChunk<ATTR> writableChunk) {
        giveWritableLongChunk(writableChunk.asWritableLongChunk());
    }

    @Override
    public final <ATTR extends Any> ResettableReadOnlyChunk<ATTR> takeResettableChunk() {
        return takeResettableLongChunk();
    }

    @Override
    public final <ATTR extends Any> void giveResettableChunk(@NotNull final ResettableReadOnlyChunk<ATTR> resettableChunk) {
        giveResettableLongChunk(resettableChunk.asResettableLongChunk());
    }

    @Override
    public final <ATTR extends Any> ResettableWritableChunk<ATTR> takeResettableWritableChunk() {
        return takeResettableWritableLongChunk();
    }

    @Override
    public final <ATTR extends Any> void giveResettableWritableChunk(@NotNull final ResettableWritableChunk<ATTR> resettableWritableChunk) {
        giveResettableWritableLongChunk(resettableWritableChunk.asResettableWritableLongChunk());
    }

    public final <ATTR extends Any> WritableLongChunk<ATTR> takeWritableLongChunk(final int capacity) {
        final int poolIndexForTake = getPoolIndexForTake(checkCapacityBounds(capacity));
        if (poolIndexForTake >= 0) {
            final WritableLongChunk result = writableLongChunks[poolIndexForTake].take();
            result.setSize(capacity);
            //noinspection unchecked
            return ChunkPoolReleaseTracking.onTake(result);
        }
        //noinspection unchecked
        return ChunkPoolReleaseTracking.onTake(WritableLongChunk.makeWritableChunkForPool(capacity));
    }

    public final void giveWritableLongChunk(@NotNull final WritableLongChunk writableLongChunk) {
        ChunkPoolReleaseTracking.onGive(writableLongChunk);
        final int capacity = writableLongChunk.capacity();
        final int poolIndexForGive = getPoolIndexForGive(checkCapacityBounds(capacity));
        if (poolIndexForGive >= 0) {
            writableLongChunks[poolIndexForGive].give(writableLongChunk);
        }
    }

    public final <ATTR extends Any> ResettableLongChunk<ATTR> takeResettableLongChunk() {
        //noinspection unchecked
        return ChunkPoolReleaseTracking.onTake(resettableLongChunks.take());
    }

    public final void giveResettableLongChunk(@NotNull final ResettableLongChunk resettableLongChunk) {
        resettableLongChunks.give(ChunkPoolReleaseTracking.onGive(resettableLongChunk));
    }

    public final <ATTR extends Any> ResettableWritableLongChunk<ATTR> takeResettableWritableLongChunk() {
        //noinspection unchecked
        return ChunkPoolReleaseTracking.onTake(resettableWritableLongChunks.take());
    }

    public final void giveResettableWritableLongChunk(@NotNull final ResettableWritableLongChunk resettableWritableLongChunk) {
        resettableWritableLongChunks.give(ChunkPoolReleaseTracking.onGive(resettableWritableLongChunk));
    }
}
