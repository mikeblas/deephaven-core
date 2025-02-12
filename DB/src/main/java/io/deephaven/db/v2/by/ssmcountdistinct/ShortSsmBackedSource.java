/* ---------------------------------------------------------------------------------------------------------------------
 * AUTO-GENERATED CLASS - DO NOT EDIT MANUALLY - for any changes edit CharSsmBackedSource and regenerate
 * ------------------------------------------------------------------------------------------------------------------ */
package io.deephaven.db.v2.by.ssmcountdistinct;

import io.deephaven.db.tables.dbarrays.DbShortArray;
import io.deephaven.db.v2.sources.AbstractColumnSource;
import io.deephaven.db.v2.sources.ColumnSourceGetDefaults;
import io.deephaven.db.v2.sources.MutableColumnSourceGetDefaults;
import io.deephaven.db.v2.sources.ObjectArraySource;
import io.deephaven.db.v2.ssms.ShortSegmentedSortedMultiset;
import io.deephaven.db.v2.utils.Index;

/**
 * A {@link SsmBackedColumnSource} for Shorts.
 */
public class ShortSsmBackedSource extends AbstractColumnSource<DbShortArray>
                                 implements ColumnSourceGetDefaults.ForObject<DbShortArray>,
                                            MutableColumnSourceGetDefaults.ForObject<DbShortArray>,
                                            SsmBackedColumnSource<ShortSegmentedSortedMultiset, DbShortArray> {
    private final ObjectArraySource<ShortSegmentedSortedMultiset> underlying;
    private boolean trackingPrevious = false;

    //region Constructor
    public ShortSsmBackedSource() {
        super(DbShortArray.class, short.class);
        underlying = new ObjectArraySource<>(ShortSegmentedSortedMultiset.class, short.class);
    }
    //endregion Constructor

    //region SsmBackedColumnSource
    @Override
    public ShortSegmentedSortedMultiset getOrCreate(long key) {
        ShortSegmentedSortedMultiset ssm = underlying.getUnsafe(key);
        if(ssm == null) {
            //region CreateNew
            underlying.set(key, ssm = new ShortSegmentedSortedMultiset(DistinctOperatorFactory.NODE_SIZE));
            //endregion CreateNew
        }
        ssm.setTrackDeltas(trackingPrevious);
        return ssm;
    }

    @Override
    public ShortSegmentedSortedMultiset getCurrentSsm(long key) {
        return underlying.getUnsafe(key);
    }

    @Override
    public void clear(long key) {
        underlying.set(key, null);
    }

    @Override
    public void ensureCapacity(long capacity) {
        underlying.ensureCapacity(capacity);
    }

    @Override
    public ObjectArraySource<ShortSegmentedSortedMultiset> getUnderlyingSource() {
        return underlying;
    }
    //endregion

    @Override
    public boolean isImmutable() {
        return false;
    }

    @Override
    public DbShortArray get(long index) {
        return underlying.get(index);
    }

    @Override
    public DbShortArray getPrev(long index) {
        final ShortSegmentedSortedMultiset maybePrev = underlying.getPrev(index);
        return maybePrev == null ? null : maybePrev.getPrevValues();
    }

    @Override
    public void startTrackingPrevValues() {
        trackingPrevious = true;
        underlying.startTrackingPrevValues();
    }

    @Override
    public void clearDeltas(Index indices) {
        indices.iterator().forEachLong(key -> {
            final ShortSegmentedSortedMultiset ssm = getCurrentSsm(key);
            if(ssm != null) {
                ssm.clearDeltas();
            }
            return true;
        });
    }
}
