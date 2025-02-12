/* ---------------------------------------------------------------------------------------------------------------------
 * AUTO-GENERATED CLASS - DO NOT EDIT MANUALLY - for any changes edit CharPermuteKernel and regenerate
 * ------------------------------------------------------------------------------------------------------------------ */
package io.deephaven.db.v2.sort.permute;

import io.deephaven.db.v2.sources.chunk.*;

import static io.deephaven.db.v2.sources.chunk.Attributes.*;

public class BytePermuteKernel {
    public static <T extends Any> void permute(ByteChunk<? extends T> inputValues, IntChunk<ChunkPositions> outputPositions, WritableByteChunk<? super T> outputValues) {
        for (int ii = 0; ii < outputPositions.size(); ++ii) {
            outputValues.set(outputPositions.get(ii), inputValues.get(ii));
        }
    }

    public static <T extends Any> void permuteInput(ByteChunk<? extends T> inputValues, IntChunk<ChunkPositions> inputPositions, WritableByteChunk<? super T> outputValues) {
        for (int ii = 0; ii < inputPositions.size(); ++ii) {
            outputValues.set(ii, inputValues.get(inputPositions.get(ii)));
        }
    }

    public static <T extends Any> void permute(IntChunk<ChunkPositions> inputPositions, ByteChunk<? extends T> inputValues, IntChunk<ChunkPositions> outputPositions, WritableByteChunk<? super T> outputValues) {
        for (int ii = 0; ii < outputPositions.size(); ++ii) {
            outputValues.set(outputPositions.get(ii), inputValues.get(inputPositions.get(ii)));
        }
    }

    private static class BytePermuteKernelContext implements PermuteKernel {
        @Override
        public <T extends Any> void permute(Chunk<? extends T> inputValues, IntChunk<ChunkPositions> outputPositions, WritableChunk<? super T> outputValues) {
            BytePermuteKernel.permute(inputValues.asByteChunk(), outputPositions, outputValues.asWritableByteChunk());
        }

        @Override
        public <T extends Any> void permute(IntChunk<ChunkPositions> inputPositions, Chunk<? extends T> inputValues, IntChunk<ChunkPositions> outputPositions, WritableChunk<? super T> outputValues) {
            BytePermuteKernel.permute(inputPositions, inputValues.asByteChunk(), outputPositions, outputValues.asWritableByteChunk());
        }

        @Override
        public <T extends Any> void permuteInput(Chunk<? extends T> inputValues, IntChunk<ChunkPositions> inputPositions, WritableChunk<? super T> outputValues) {
            BytePermuteKernel.permuteInput(inputValues.asByteChunk(), inputPositions, outputValues.asWritableByteChunk());
        }
    }

    public final static PermuteKernel INSTANCE = new BytePermuteKernelContext();
}
