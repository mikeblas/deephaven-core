package io.deephaven.db.v2.sources.chunk;

import io.deephaven.db.v2.sources.chunk.Attributes.Any;
import org.jetbrains.annotations.NotNull;

import java.nio.Buffer;

/**
 * Data structure for a contiguous region of data.
 *
 * @param <ATTR> {@link Attributes} that apply to this chunk
 *
 * @IncludeAll
 */
public interface Chunk<ATTR extends Any> {
    /**
     * The threshold at which we should use System.arrayCopy rather than our own copy
     */
    int SYSTEM_ARRAYCOPY_THRESHOLD = 16;
    /**
     * The threshold at which we should use Array.fill rather than our own fill
     */
    int SYSTEM_ARRAYFILL_THRESHOLD = 16;
    /**
     * The maximum number of elements a chunk can contain.
     */
    int MAXIMUM_SIZE = Integer.MAX_VALUE;

    /**
     * Make a new Chunk that represents either exactly the same view on the underlying data as this Chunk, or a
     * subrange of that view. The view is defined as [0..size) (in the coordinate space of this Chunk).
     * @param offset Offset of the new Chunk, relative to this Chunk. 0 ≤ offset ≤ this.size
     * @param capacity Capacity and initial size of the new Chunk. 0 ≤ capacity ≤ this.size - {@code offset}.
     * @return The new Chunk. A new Chunk will always be returned, even if the Chunks represent the same view.
     */
    Chunk<ATTR> slice(int offset, int capacity);

    /**
     * Copy a subrange of this Chunk to the subrange of the 'dest' writable chunk.
     *  @param srcOffset  Starting position in 'this' (the source)
     * @param dest       Destination writable chunk.
     * @param destOffset Starting offset in the destination.
     * @param size       Number of values to copy
     */
    void copyToChunk(int srcOffset, WritableChunk<? super ATTR> dest, int destOffset, int size);

    /**
     * Copy a subrange of this Chunk to the subrange of the 'dest' array.
     *
     * @param srcOffset  Starting position in 'this' (the source)
     * @param dest       Destination array.
     * @param destOffset Starting offset in the destination.
     * @param size       Number of values to copy
     */
    @SuppressWarnings("unused")
    void copyToArray(int srcOffset, Object dest, int destOffset, int size);

    /**
     * <p>Copy a sub-range of this chunk to a {@link Buffer}. This is an optional method, as some
     * chunk types do not have a corresponding buffer type.
     *
     * <p>Implementations are free to copy data as efficiently as they may, and will use absolute rather than positional
     * access where possible. To facilitate this pattern, {@code destOffset} is an absolute offset from position 0,
     * rather than a relative offset from {@code destBuffer.position()}.
     *
     * <p><It is required that {@code destBuffer.limit()} is at least {@code destOffset + length}.
     *
     * <p>{@code destBuffer}'s position may be modified, but will always be restored to its initial value upon successful
     * return.
     *
     * @param srcOffset  The offset into this chunk to start copying from
     * @param destBuffer The destination {@link Buffer}
     * @param destOffset The absolute offset into {@code destBuffer} to start copying to
     * @param length     The number of elements to copy
     */
    default void copyToBuffer(int srcOffset, @NotNull Buffer destBuffer, int destOffset, int length) {
        throw new UnsupportedOperationException();
    }

    /**
     * @return The length of the data in the chunk
     */
    int size();

    /**
     * @return The underlying chunk type
     */
    ChunkType getChunkType();

    /**
     * @return true iff this and array are aliases, that is they refer to the same underlying data
     */
    boolean isAlias(Object object);

    /**
     * @return true iff this and chunk are aliases, that is they refer to the same underlying data
     */
    boolean isAlias(Chunk chunk);

    default ByteChunk<ATTR> asByteChunk() {
        return (ByteChunk<ATTR>) this;
    }
    default BooleanChunk<ATTR> asBooleanChunk() {
        return (BooleanChunk<ATTR>) this;
    }
    default CharChunk<ATTR> asCharChunk() {
        return (CharChunk<ATTR>) this;

    }
    default ShortChunk<ATTR> asShortChunk() {
        return (ShortChunk<ATTR>) this;
    }
    default IntChunk<ATTR> asIntChunk() {
        return (IntChunk<ATTR>) this;
    }
    default LongChunk<ATTR> asLongChunk() {
        return (LongChunk<ATTR>) this;
    }
    default FloatChunk<ATTR> asFloatChunk() {
        return (FloatChunk<ATTR>) this;
    }
    default DoubleChunk<ATTR> asDoubleChunk() {
        return (DoubleChunk<ATTR>) this;
    }
    default <T> ObjectChunk<T, ATTR> asObjectChunk() {
        return (ObjectChunk<T, ATTR>) this;
    }

    /**
     * Downcast the attribute.
     *
     * When you know the data in this chunk which you plan to read is a more specific sub-type, you can downcast
     * the attribute with this helper method.  This might be necessary, for instance, when you have a KeyIndices chunk
     * which you sort, and now want to treat it as an OrderedKeyIndices.
     *
     * @apiNote Upcast should not be necessary on read-only chunks, as a read-only chunk method should accept an
     *          upper bound wildcard.
     */

    static <ATTR extends Any, ATTR_DERIV extends ATTR> Chunk<ATTR_DERIV> downcast(Chunk<ATTR> self) {
        //noinspection unchecked
        return (Chunk<ATTR_DERIV>) self;
    }
}
