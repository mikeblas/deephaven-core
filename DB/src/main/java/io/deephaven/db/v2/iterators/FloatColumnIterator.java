/*
 * Copyright (c) 2016-2021 Deephaven Data Labs and Patent Pending
 */

package io.deephaven.db.v2.iterators;

import io.deephaven.base.Procedure;
import io.deephaven.db.tables.Table;
import io.deephaven.db.v2.sources.ColumnSource;
import io.deephaven.db.v2.utils.Index;
import org.jetbrains.annotations.NotNull;

import java.util.PrimitiveIterator;

/**
 * Iteration support for boxed or primitive floats contained with a ColumnSource.
 */
public class FloatColumnIterator extends ColumnIterator<Float> implements PrimitiveIterator<Float, Procedure.UnaryFloat> {

    public FloatColumnIterator(@NotNull final Index index, @NotNull final ColumnSource<Float> columnSource) {
        super(index, columnSource);
    }

    public FloatColumnIterator(@NotNull final Table table, @NotNull final String columnName) {
        //noinspection unchecked
        this(table.getIndex(), table.getColumnSource(columnName));
    }

    @SuppressWarnings("WeakerAccess")
    public float nextFloat() {
        return columnSource.getFloat(indexIterator.nextLong());
    }

    @Override
    public void forEachRemaining(@NotNull final Procedure.UnaryFloat action) {
        while (hasNext()) {
            action.call(nextFloat());
        }
    }
}
