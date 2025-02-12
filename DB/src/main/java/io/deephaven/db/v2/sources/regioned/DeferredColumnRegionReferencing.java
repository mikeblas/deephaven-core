package io.deephaven.db.v2.sources.regioned;

import io.deephaven.db.v2.sources.chunk.Attributes;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * {@link ColumnRegionReferencing} implementation for deferred regions, i.e. regions that will be properly constructed on
 * first access.
 */
public class DeferredColumnRegionReferencing<ATTR extends Attributes.Any, REFERENCED_COLUMN_REGION extends ColumnRegion<ATTR>>
        extends DeferredColumnRegionBase<ATTR, ColumnRegionReferencing<ATTR, REFERENCED_COLUMN_REGION>>
        implements ColumnRegionReferencing<ATTR, REFERENCED_COLUMN_REGION> {

    DeferredColumnRegionReferencing(@NotNull Supplier<ColumnRegionReferencing<ATTR, REFERENCED_COLUMN_REGION>> resultRegionFactory) {
        super(resultRegionFactory);
    }

    @NotNull
    @Override
    public REFERENCED_COLUMN_REGION getReferencedRegion() {
        return getResultRegion().getReferencedRegion();
    }
    
}
