/* ---------------------------------------------------------------------------------------------------------------------
 * AUTO-GENERATED CLASS - DO NOT EDIT MANUALLY - for any changes edit CharSetResult and regenerate
 * ------------------------------------------------------------------------------------------------------------------ */
package io.deephaven.db.v2.by.ssmminmax;

import io.deephaven.db.v2.sources.ArrayBackedColumnSource;
import io.deephaven.db.v2.sources.LongArraySource;
import io.deephaven.db.v2.ssms.LongSegmentedSortedMultiset;
import io.deephaven.db.v2.ssms.SegmentedSortedMultiSet;

import static io.deephaven.util.QueryConstants.NULL_LONG;

public class LongSetResult implements SsmChunkedMinMaxOperator.SetResult {
    private final boolean minimum;
    private final LongArraySource resultColumn;

    public LongSetResult(boolean minimum, ArrayBackedColumnSource resultColumn) {
        this.minimum = minimum;
        this.resultColumn = (LongArraySource) resultColumn;
    }

    @Override
    public boolean setResult(SegmentedSortedMultiSet ssm, long destination) {
        final long newResult;
        if (ssm.size() == 0) {
            newResult = NULL_LONG;
        } else {
            final LongSegmentedSortedMultiset longSsm = (LongSegmentedSortedMultiset) ssm;
            newResult = minimum ? longSsm.getMinLong() : longSsm.getMaxLong();
        }
        return setResult(destination, newResult);
    }

    @Override
    public boolean setResultNull(long destination) {
        return setResult(destination, NULL_LONG);
    }

    private boolean setResult(long destination, long newResult) {
        final long oldResult = resultColumn.getAndSetUnsafe(destination, newResult);
        return oldResult != newResult;
    }
}
