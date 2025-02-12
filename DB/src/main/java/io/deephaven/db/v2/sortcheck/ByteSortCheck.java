/* ---------------------------------------------------------------------------------------------------------------------
 * AUTO-GENERATED CLASS - DO NOT EDIT MANUALLY - for any changes edit CharSortCheck and regenerate
 * ------------------------------------------------------------------------------------------------------------------ */
package io.deephaven.db.v2.sortcheck;

import io.deephaven.db.util.DhByteComparisons;
import io.deephaven.db.v2.sources.chunk.Attributes;
import io.deephaven.db.v2.sources.chunk.ByteChunk;
import io.deephaven.db.v2.sources.chunk.Chunk;

public class ByteSortCheck implements SortCheck {
    static final SortCheck INSTANCE = new ByteSortCheck();

    @Override
    public int sortCheck(Chunk<? extends Attributes.Values> valuesToCheck) {
        return sortCheck(valuesToCheck.asByteChunk());
    }

    private int sortCheck(ByteChunk<? extends Attributes.Values> valuesToCheck) {
        if (valuesToCheck.size() == 0) {
            return -1;
        }
        byte last = valuesToCheck.get(0);
        for (int ii = 1; ii < valuesToCheck.size(); ++ii) {
            final byte current = valuesToCheck.get(ii);
            if (!leq(last, current)) {
                return ii - 1;
            }
            last = current;
        }
        return -1;
    }

    // region comparison functions
    private static int doComparison(byte lhs, byte rhs) {
        return DhByteComparisons.compare(lhs, rhs);
    }
    // endregion comparison functions

    private static boolean leq(byte lhs, byte rhs) {
        return doComparison(lhs, rhs) <= 0;
    }
}
