package io.deephaven.db.v2.tuples.generated;

import io.deephaven.datastructures.util.SmartKey;
import io.deephaven.db.util.tuples.generated.CharShortTuple;
import io.deephaven.db.v2.sources.ColumnSource;
import io.deephaven.db.v2.sources.WritableSource;
import io.deephaven.db.v2.sources.chunk.Attributes;
import io.deephaven.db.v2.sources.chunk.CharChunk;
import io.deephaven.db.v2.sources.chunk.Chunk;
import io.deephaven.db.v2.sources.chunk.ObjectChunk;
import io.deephaven.db.v2.sources.chunk.ShortChunk;
import io.deephaven.db.v2.sources.chunk.WritableChunk;
import io.deephaven.db.v2.sources.chunk.WritableObjectChunk;
import io.deephaven.db.v2.tuples.AbstractTupleSource;
import io.deephaven.db.v2.tuples.TupleSource;
import io.deephaven.db.v2.tuples.TwoColumnTupleSourceFactory;
import io.deephaven.util.type.TypeUtils;
import org.jetbrains.annotations.NotNull;


/**
 * <p>{@link TupleSource} that produces key column values from {@link ColumnSource} types Character and Short.
 * <p>Generated by {@link io.deephaven.db.v2.tuples.TupleSourceCodeGenerator}.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class CharacterShortColumnTupleSource extends AbstractTupleSource<CharShortTuple> {

    /** {@link TwoColumnTupleSourceFactory} instance to create instances of {@link CharacterShortColumnTupleSource}. **/
    public static final TwoColumnTupleSourceFactory<CharShortTuple, Character, Short> FACTORY = new Factory();

    private final ColumnSource<Character> columnSource1;
    private final ColumnSource<Short> columnSource2;

    public CharacterShortColumnTupleSource(
            @NotNull final ColumnSource<Character> columnSource1,
            @NotNull final ColumnSource<Short> columnSource2
    ) {
        super(columnSource1, columnSource2);
        this.columnSource1 = columnSource1;
        this.columnSource2 = columnSource2;
    }

    @Override
    public final CharShortTuple createTuple(final long indexKey) {
        return new CharShortTuple(
                columnSource1.getChar(indexKey),
                columnSource2.getShort(indexKey)
        );
    }

    @Override
    public final CharShortTuple createPreviousTuple(final long indexKey) {
        return new CharShortTuple(
                columnSource1.getPrevChar(indexKey),
                columnSource2.getPrevShort(indexKey)
        );
    }

    @Override
    public final CharShortTuple createTupleFromValues(@NotNull final Object... values) {
        return new CharShortTuple(
                TypeUtils.unbox((Character)values[0]),
                TypeUtils.unbox((Short)values[1])
        );
    }

    @Override
    public final CharShortTuple createTupleFromReinterpretedValues(@NotNull final Object... values) {
        return new CharShortTuple(
                TypeUtils.unbox((Character)values[0]),
                TypeUtils.unbox((Short)values[1])
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public final <ELEMENT_TYPE> void exportElement(@NotNull final CharShortTuple tuple, final int elementIndex, @NotNull final WritableSource<ELEMENT_TYPE> writableSource, final long destinationIndexKey) {
        if (elementIndex == 0) {
            writableSource.set(destinationIndexKey, tuple.getFirstElement());
            return;
        }
        if (elementIndex == 1) {
            writableSource.set(destinationIndexKey, tuple.getSecondElement());
            return;
        }
        throw new IndexOutOfBoundsException("Invalid element index " + elementIndex + " for export");
    }

    @Override
    public final Object exportToExternalKey(@NotNull final CharShortTuple tuple) {
        return new SmartKey(
                TypeUtils.box(tuple.getFirstElement()),
                TypeUtils.box(tuple.getSecondElement())
        );
    }

    @Override
    public final Object exportElement(@NotNull final CharShortTuple tuple, int elementIndex) {
        if (elementIndex == 0) {
            return TypeUtils.box(tuple.getFirstElement());
        }
        if (elementIndex == 1) {
            return TypeUtils.box(tuple.getSecondElement());
        }
        throw new IllegalArgumentException("Bad elementIndex for 2 element tuple: " + elementIndex);
    }

    @Override
    public final Object exportElementReinterpreted(@NotNull final CharShortTuple tuple, int elementIndex) {
        if (elementIndex == 0) {
            return TypeUtils.box(tuple.getFirstElement());
        }
        if (elementIndex == 1) {
            return TypeUtils.box(tuple.getSecondElement());
        }
        throw new IllegalArgumentException("Bad elementIndex for 2 element tuple: " + elementIndex);
    }

    @Override
    public Class<CharShortTuple> getNativeType() { return CharShortTuple.class; }

    protected void convertChunks(@NotNull WritableChunk<? super Attributes.Values> destination, int chunkSize, Chunk<Attributes.Values> [] chunks) {
        WritableObjectChunk<CharShortTuple, ? super Attributes.Values> destinationObjectChunk = destination.asWritableObjectChunk();
        CharChunk<Attributes.Values> chunk1 = chunks[0].asCharChunk();
        ShortChunk<Attributes.Values> chunk2 = chunks[1].asShortChunk();
        for (int ii = 0; ii < chunkSize; ++ii) {
            destinationObjectChunk.set(ii, new CharShortTuple(chunk1.get(ii), chunk2.get(ii)));
        }
        destination.setSize(chunkSize);
    }

    /** {@link TwoColumnTupleSourceFactory} for instances of {@link CharacterShortColumnTupleSource}. **/
    private static final class Factory implements TwoColumnTupleSourceFactory<CharShortTuple, Character, Short> {

        private Factory() {
        }

        @Override
        public TupleSource<CharShortTuple> create(
                @NotNull final ColumnSource<Character> columnSource1,
                @NotNull final ColumnSource<Short> columnSource2
        ) {
            return new CharacterShortColumnTupleSource(
                    columnSource1,
                    columnSource2
            );
        }
    }
}
