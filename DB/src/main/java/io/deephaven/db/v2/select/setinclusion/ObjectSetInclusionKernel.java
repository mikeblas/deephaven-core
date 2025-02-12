package io.deephaven.db.v2.select.setinclusion;

import io.deephaven.db.v2.sources.chunk.Attributes;
import io.deephaven.db.v2.sources.chunk.Chunk;
import io.deephaven.db.v2.sources.chunk.ObjectChunk;
import io.deephaven.db.v2.sources.chunk.WritableBooleanChunk;

import java.util.Collection;

public class ObjectSetInclusionKernel implements SetInclusionKernel {
    private final Collection<Object> liveValues;
    private final boolean inclusion;

    public ObjectSetInclusionKernel(Collection<Object> liveValues, boolean inclusion) {
        this.liveValues = liveValues;
        this.inclusion = inclusion;
    }

    @Override
    public void matchValues(Chunk<Attributes.Values> values, WritableBooleanChunk matches) {
        matchValues(values.asObjectChunk(), matches);
    }

    private void matchValues(ObjectChunk<?, Attributes.Values> values, WritableBooleanChunk matches) {
        for (int ii = 0; ii < values.size(); ++ii) {
            matches.set(ii, liveValues.contains(values.get(ii)) == inclusion);
        }
        matches.setSize(values.size());
    }
}