/*
 * Copyright (c) 2016-2021 Deephaven Data Labs and Patent Pending
 */

package io.deephaven.db.v2.sources;

import io.deephaven.db.tables.utils.DBDateTime;
import io.deephaven.db.tables.utils.DBTimeUtils;
import io.deephaven.db.v2.sources.chunk.*;
import io.deephaven.db.v2.sources.chunk.Attributes.Values;
import io.deephaven.db.v2.utils.OrderedKeys;
import org.jetbrains.annotations.NotNull;

/**
 * Reinterpret result {@link ColumnSource} implementations that translates {@link Boolean} to {@code byte} values.
 */
@AbstractColumnSource.IsSerializable(value = true)
public class DatetimeAsLongColumnSource extends AbstractColumnSource<Long> implements MutableColumnSourceGetDefaults.ForLong {

    private final ColumnSource<DBDateTime> alternateColumnSource;

    public DatetimeAsLongColumnSource(ColumnSource<DBDateTime> alternateColumnSource) {
        super(long.class);
        this.alternateColumnSource = alternateColumnSource;
    }

    @Override
    public long getLong(long index) {
        return DBTimeUtils.nanos(alternateColumnSource.get(index));
    }

    @Override
    public long getPrevLong(long index) {
        return DBTimeUtils.nanos(alternateColumnSource.getPrev(index));
    }

    @Override
    public boolean isImmutable() {
        return alternateColumnSource.isImmutable();
    }

    @Override
    public <ALTERNATE_DATA_TYPE> boolean allowsReinterpret(@NotNull final Class<ALTERNATE_DATA_TYPE> alternateDataType) {
        return alternateDataType == DBDateTime.class;
    }

    @Override
    public <ALTERNATE_DATA_TYPE> ColumnSource<ALTERNATE_DATA_TYPE> doReinterpret(@NotNull final Class<ALTERNATE_DATA_TYPE> alternateDataType) throws IllegalArgumentException {
        //noinspection unchecked
        return (ColumnSource<ALTERNATE_DATA_TYPE>) alternateColumnSource;
    }

    private class UnboxingFillContext implements FillContext {
        final FillContext alternateFillContext;
        final WritableObjectChunk<DBDateTime, Values> dbdatetimeChunk;

        private UnboxingFillContext(final int chunkCapacity, final SharedContext sharedContext) {
            alternateFillContext = alternateColumnSource.makeFillContext(chunkCapacity, sharedContext);
            dbdatetimeChunk = WritableObjectChunk.makeWritableChunk(chunkCapacity);
        }

        @Override
        public void close() {
            alternateFillContext.close();
            dbdatetimeChunk.close();
        }
    }

    @Override
    public FillContext makeFillContext(final int chunkCapacity, final SharedContext sharedContext) {
        return new UnboxingFillContext(chunkCapacity, sharedContext);
    }

    @Override
    public void fillChunk(@NotNull FillContext context, @NotNull WritableChunk<? super Values> destination, @NotNull OrderedKeys orderedKeys) {
        final UnboxingFillContext unboxingFillContext = (UnboxingFillContext) context;
        final WritableObjectChunk<DBDateTime, Values> dbdatetimeChunk = unboxingFillContext.dbdatetimeChunk;
        alternateColumnSource.fillChunk(unboxingFillContext.alternateFillContext, dbdatetimeChunk, orderedKeys);
        convertToLong(destination, dbdatetimeChunk);
        dbdatetimeChunk.fillWithNullValue(0, dbdatetimeChunk.size());
    }

    @Override
    public void fillPrevChunk(@NotNull FillContext context, @NotNull WritableChunk<? super Values> destination, @NotNull OrderedKeys orderedKeys) {
        final UnboxingFillContext unboxingFillContext = (UnboxingFillContext) context;
        final WritableObjectChunk<DBDateTime, Values> dbdatetimeChunk = unboxingFillContext.dbdatetimeChunk;
        alternateColumnSource.fillPrevChunk(unboxingFillContext.alternateFillContext, dbdatetimeChunk, orderedKeys);
        convertToLong(destination, dbdatetimeChunk);
        dbdatetimeChunk.fillWithNullValue(0, dbdatetimeChunk.size());
    }

    private static void convertToLong(@NotNull WritableChunk<? super Values> destination, ObjectChunk<DBDateTime, Values> dbdatetimeChunk) {
        final WritableLongChunk<? super Values> longDestination = destination.asWritableLongChunk();
        for (int ii = 0; ii < dbdatetimeChunk.size(); ++ii) {
            final DBDateTime dbDateTime = dbdatetimeChunk.get(ii);
            longDestination.set(ii, DBTimeUtils.nanos(dbDateTime));
        }
        longDestination.setSize(dbdatetimeChunk.size());
    }
}
