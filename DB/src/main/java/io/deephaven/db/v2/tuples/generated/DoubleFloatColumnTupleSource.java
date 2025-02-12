package io.deephaven.db.v2.tuples.generated;

import io.deephaven.datastructures.util.SmartKey;
import io.deephaven.db.util.tuples.generated.DoubleFloatTuple;
import io.deephaven.db.v2.sources.ColumnSource;
import io.deephaven.db.v2.sources.WritableSource;
import io.deephaven.db.v2.sources.chunk.Attributes;
import io.deephaven.db.v2.sources.chunk.Chunk;
import io.deephaven.db.v2.sources.chunk.DoubleChunk;
import io.deephaven.db.v2.sources.chunk.FloatChunk;
import io.deephaven.db.v2.sources.chunk.ObjectChunk;
import io.deephaven.db.v2.sources.chunk.WritableChunk;
import io.deephaven.db.v2.sources.chunk.WritableObjectChunk;
import io.deephaven.db.v2.tuples.AbstractTupleSource;
import io.deephaven.db.v2.tuples.TupleSource;
import io.deephaven.db.v2.tuples.TwoColumnTupleSourceFactory;
import io.deephaven.util.type.TypeUtils;
import org.jetbrains.annotations.NotNull;


/**
 * <p>{@link TupleSource} that produces key column values from {@link ColumnSource} types Double and Float.
 * <p>Generated by {@link io.deephaven.db.v2.tuples.TupleSourceCodeGenerator}.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class DoubleFloatColumnTupleSource extends AbstractTupleSource<DoubleFloatTuple> {

    /** {@link TwoColumnTupleSourceFactory} instance to create instances of {@link DoubleFloatColumnTupleSource}. **/
    public static final TwoColumnTupleSourceFactory<DoubleFloatTuple, Double, Float> FACTORY = new Factory();

    private final ColumnSource<Double> columnSource1;
    private final ColumnSource<Float> columnSource2;

    public DoubleFloatColumnTupleSource(
            @NotNull final ColumnSource<Double> columnSource1,
            @NotNull final ColumnSource<Float> columnSource2
    ) {
        super(columnSource1, columnSource2);
        this.columnSource1 = columnSource1;
        this.columnSource2 = columnSource2;
    }

    @Override
    public final DoubleFloatTuple createTuple(final long indexKey) {
        return new DoubleFloatTuple(
                columnSource1.getDouble(indexKey),
                columnSource2.getFloat(indexKey)
        );
    }

    @Override
    public final DoubleFloatTuple createPreviousTuple(final long indexKey) {
        return new DoubleFloatTuple(
                columnSource1.getPrevDouble(indexKey),
                columnSource2.getPrevFloat(indexKey)
        );
    }

    @Override
    public final DoubleFloatTuple createTupleFromValues(@NotNull final Object... values) {
        return new DoubleFloatTuple(
                TypeUtils.unbox((Double)values[0]),
                TypeUtils.unbox((Float)values[1])
        );
    }

    @Override
    public final DoubleFloatTuple createTupleFromReinterpretedValues(@NotNull final Object... values) {
        return new DoubleFloatTuple(
                TypeUtils.unbox((Double)values[0]),
                TypeUtils.unbox((Float)values[1])
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public final <ELEMENT_TYPE> void exportElement(@NotNull final DoubleFloatTuple tuple, final int elementIndex, @NotNull final WritableSource<ELEMENT_TYPE> writableSource, final long destinationIndexKey) {
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
    public final Object exportToExternalKey(@NotNull final DoubleFloatTuple tuple) {
        return new SmartKey(
                TypeUtils.box(tuple.getFirstElement()),
                TypeUtils.box(tuple.getSecondElement())
        );
    }

    @Override
    public final Object exportElement(@NotNull final DoubleFloatTuple tuple, int elementIndex) {
        if (elementIndex == 0) {
            return TypeUtils.box(tuple.getFirstElement());
        }
        if (elementIndex == 1) {
            return TypeUtils.box(tuple.getSecondElement());
        }
        throw new IllegalArgumentException("Bad elementIndex for 2 element tuple: " + elementIndex);
    }

    @Override
    public final Object exportElementReinterpreted(@NotNull final DoubleFloatTuple tuple, int elementIndex) {
        if (elementIndex == 0) {
            return TypeUtils.box(tuple.getFirstElement());
        }
        if (elementIndex == 1) {
            return TypeUtils.box(tuple.getSecondElement());
        }
        throw new IllegalArgumentException("Bad elementIndex for 2 element tuple: " + elementIndex);
    }

    @Override
    public Class<DoubleFloatTuple> getNativeType() { return DoubleFloatTuple.class; }

    protected void convertChunks(@NotNull WritableChunk<? super Attributes.Values> destination, int chunkSize, Chunk<Attributes.Values> [] chunks) {
        WritableObjectChunk<DoubleFloatTuple, ? super Attributes.Values> destinationObjectChunk = destination.asWritableObjectChunk();
        DoubleChunk<Attributes.Values> chunk1 = chunks[0].asDoubleChunk();
        FloatChunk<Attributes.Values> chunk2 = chunks[1].asFloatChunk();
        for (int ii = 0; ii < chunkSize; ++ii) {
            destinationObjectChunk.set(ii, new DoubleFloatTuple(chunk1.get(ii), chunk2.get(ii)));
        }
        destination.setSize(chunkSize);
    }

    /** {@link TwoColumnTupleSourceFactory} for instances of {@link DoubleFloatColumnTupleSource}. **/
    private static final class Factory implements TwoColumnTupleSourceFactory<DoubleFloatTuple, Double, Float> {

        private Factory() {
        }

        @Override
        public TupleSource<DoubleFloatTuple> create(
                @NotNull final ColumnSource<Double> columnSource1,
                @NotNull final ColumnSource<Float> columnSource2
        ) {
            return new DoubleFloatColumnTupleSource(
                    columnSource1,
                    columnSource2
            );
        }
    }
}
