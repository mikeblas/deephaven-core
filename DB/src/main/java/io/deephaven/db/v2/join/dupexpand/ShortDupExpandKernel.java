/* ---------------------------------------------------------------------------------------------------------------------
 * AUTO-GENERATED CLASS - DO NOT EDIT MANUALLY - for any changes edit CharDupExpandKernel and regenerate
 * ------------------------------------------------------------------------------------------------------------------ */
package io.deephaven.db.v2.join.dupexpand;

import io.deephaven.db.v2.sources.chunk.*;

public class ShortDupExpandKernel implements DupExpandKernel {
    public static final ShortDupExpandKernel INSTANCE = new ShortDupExpandKernel();

    private ShortDupExpandKernel() {} // use through the instance

    @Override
    public void expandDuplicates(int expandedSize, WritableChunk<? extends Attributes.Any> chunkToExpand, IntChunk<Attributes.ChunkLengths> keyRunLengths) {
        expandDuplicates(expandedSize, chunkToExpand.asWritableShortChunk(), keyRunLengths);
    }

    public static void expandDuplicates(int expandedSize, WritableShortChunk<? extends Attributes.Any> chunkToExpand, IntChunk<Attributes.ChunkLengths> keyRunLengths) {
        if (expandedSize == 0) {
            return;
        }

        int wpos = expandedSize;
        int rpos = chunkToExpand.size() - 1;
        chunkToExpand.setSize(expandedSize);

        for (; rpos >= 0; --rpos) {
            final int len = keyRunLengths.get(rpos);
            chunkToExpand.fillWithValue(wpos - len, len, chunkToExpand.get(rpos));
            wpos -= len;
        }
    }
}
