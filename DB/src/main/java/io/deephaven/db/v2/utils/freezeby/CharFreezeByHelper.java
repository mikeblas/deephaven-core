/*
 * Copyright (c) 2020 Deephaven Data Labs and Patent Pending
 */

package io.deephaven.db.v2.utils.freezeby;

import io.deephaven.db.v2.sources.CharacterArraySource;
import io.deephaven.db.v2.sources.WritableSource;
import io.deephaven.db.v2.sources.chunk.*;
import io.deephaven.db.v2.utils.OrderedKeys;

class CharFreezeByHelper implements FreezeByOperator.FreezeByHelper {
    private final CharacterArraySource resultSource;
    private final FreezeByCountOperator rowCount;

    CharFreezeByHelper(WritableSource resultSource, FreezeByCountOperator rowCount) {
        this.resultSource = (CharacterArraySource)resultSource;
        this.rowCount = rowCount;
    }

    @Override
    public void addChunk(Chunk<? extends Attributes.Values> values, IntChunk<Attributes.ChunkPositions> startPositions, IntChunk<Attributes.KeyIndices> destinations, IntChunk<Attributes.ChunkLengths> length) {
        final CharChunk asChar = values.asCharChunk();
        for (int ii = 0; ii < startPositions.size(); ++ii) {
            final int position = startPositions.get(ii);
            final int destination = destinations.get(position);
            // if there is just row churn for a key, we don't want to re-snapshot the value (adds/removes just get ignored)
            if (rowCount.wasDestinationEmpty(destination)) {
                resultSource.set(destination, asChar.get(position));
            }
        }
    }


    @Override
    public void addChunk(Chunk<? extends Attributes.Values> values, long destination) {
        if (rowCount.wasDestinationEmpty(destination)) {
            final CharChunk asChar = values.asCharChunk();
            resultSource.set(destination, asChar.get(0));
        }
    }

    @Override
    public void clearIndex(OrderedKeys removed) {
        // region clearIndex
        // endregion clearIndex
    }
}
