package io.deephaven.db.v2.sources.regioned;

import io.deephaven.db.v2.sources.AbstractDeferredGroupingColumnSource;
import io.deephaven.db.v2.sources.chunk.Attributes;
import io.deephaven.db.v2.sources.chunk.WritableChunk;
import io.deephaven.db.v2.utils.OrderedKeys;
import io.deephaven.util.annotations.TestUseOnly;
import org.jetbrains.annotations.NotNull;

/**
 * Partial implementation of {@link RegionedColumnSource} for array-backed and delegating implementations to extend.
 */
abstract class RegionedColumnSourceBase<DATA_TYPE, ATTR extends Attributes.Values, REGION_TYPE extends ColumnRegion<ATTR>>
        extends AbstractDeferredGroupingColumnSource<DATA_TYPE>
        implements RegionedPageStore<Attributes.Values, ATTR, REGION_TYPE>, RegionedColumnSource<DATA_TYPE> {

    RegionedColumnSourceBase(Class<DATA_TYPE> type) {
        super(type);
    }

    /**
     * Use the more efficient fill chunk implementation, rather than the default which uses get().
     */
    @Override
    public void fillChunk(@NotNull FillContext context, @NotNull WritableChunk<? super Attributes.Values> destination, @NotNull OrderedKeys orderedKeys) {
        RegionedPageStore.super.fillChunk(context, destination, orderedKeys);
    }

    /**
     * We are immutable, so stick with the efficient fill chunk even when getting prev.
     */
    @Override
    public void fillPrevChunk(@NotNull FillContext context, @NotNull WritableChunk<? super Attributes.Values> destination, @NotNull OrderedKeys orderedKeys) {
        fillChunk(context, destination, orderedKeys);
    }

    /**
     * <p>Add a pre-constructed region without going through the abstract factory method.
     * <p><em>This method is for unit testing purposes only!</em>
     *
     * @param region The region to add
     * @return The index assigned to the added region
     */
    @SuppressWarnings("UnusedReturnValue")
    @TestUseOnly
    abstract <OTHER_REGION_TYPE> int addRegionForUnitTests(final OTHER_REGION_TYPE region);

    /**
     * @return the region which represents null for this column source.
     */
    @NotNull
    abstract REGION_TYPE getNullRegion();
}
