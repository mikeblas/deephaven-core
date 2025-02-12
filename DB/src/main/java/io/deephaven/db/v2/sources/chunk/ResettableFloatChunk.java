/* ---------------------------------------------------------------------------------------------------------------------
 * AUTO-GENERATED CLASS - DO NOT EDIT MANUALLY - for any changes edit ResettableCharChunk and regenerate
 * ------------------------------------------------------------------------------------------------------------------ */
package io.deephaven.db.v2.sources.chunk;
import io.deephaven.db.tables.utils.ArrayUtils;
import io.deephaven.db.v2.sources.chunk.Attributes.Any;
import io.deephaven.db.v2.sources.chunk.util.pools.MultiChunkPool;
import io.deephaven.db.v2.utils.ChunkUtils;

/**
 * {@link ResettableReadOnlyChunk} implementation for float data.
 *
 * @IncludeAll
 */
public final class ResettableFloatChunk<ATTR_UPPER extends Any> extends FloatChunk implements ResettableReadOnlyChunk<ATTR_UPPER> {

    public static <ATTR_BASE extends Any> ResettableFloatChunk<ATTR_BASE> makeResettableChunk() {
        return MultiChunkPool.forThisThread().getFloatChunkPool().takeResettableFloatChunk();
    }

    public static <ATTR_BASE extends Any> ResettableFloatChunk<ATTR_BASE> makeResettableChunkForPool() {
        return new ResettableFloatChunk<>();
    }

    private ResettableFloatChunk(float[] data, int offset, int capacity) {
        super(data, offset, capacity);
    }

    private ResettableFloatChunk() {
        this(ArrayUtils.EMPTY_FLOAT_ARRAY, 0, 0);
    }

    @Override
    public final ResettableFloatChunk slice(int offset, int capacity) {
        ChunkUtils.checkSliceArgs(size, offset, capacity);
        return new ResettableFloatChunk(data, this.offset + offset, capacity);
    }

    @Override
    public final <ATTR extends ATTR_UPPER> FloatChunk<ATTR> resetFromChunk(Chunk<? extends ATTR> other, int offset, int capacity) {
        return resetFromTypedChunk(other.asFloatChunk(), offset, capacity);
    }

    @Override
    public final <ATTR extends ATTR_UPPER> FloatChunk<ATTR> resetFromArray(Object array, int offset, int capacity) {
        final float[] typedArray = (float[])array;
        return resetFromTypedArray(typedArray, offset, capacity);
    }

    @Override
    public final <ATTR extends ATTR_UPPER> FloatChunk<ATTR> resetFromArray(Object array) {
        final float[] typedArray = (float[])array;
        return resetFromTypedArray(typedArray, 0, typedArray.length);
    }

    @Override
    public final <ATTR extends ATTR_UPPER> FloatChunk<ATTR> clear() {
        return resetFromArray(ArrayUtils.EMPTY_FLOAT_ARRAY, 0, 0);
    }

    public final <ATTR extends ATTR_UPPER> FloatChunk<ATTR> resetFromTypedChunk(FloatChunk<? extends ATTR> other, int offset, int capacity) {
        ChunkUtils.checkSliceArgs(other.size, offset, capacity);
        return resetFromTypedArray(other.data, other.offset + offset, capacity);
    }

    public final <ATTR extends ATTR_UPPER> FloatChunk<ATTR> resetFromTypedArray(float[] data, int offset, int capacity) {
        ChunkUtils.checkArrayArgs(data.length, offset, capacity);
        this.data = data;
        this.offset = offset;
        this.capacity = capacity;
        this.size = capacity;
        //noinspection unchecked
        return this;
    }

    @Override
    public final void close() {
        MultiChunkPool.forThisThread().getFloatChunkPool().giveResettableFloatChunk(this);
    }
}
