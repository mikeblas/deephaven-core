/* ---------------------------------------------------------------------------------------------------------------------
 * AUTO-GENERATED CLASS - DO NOT EDIT MANUALLY - for any changes edit CharHelper and regenerate
 * ------------------------------------------------------------------------------------------------------------------ */
package io.deephaven.db.v2.sources;

import io.deephaven.db.v2.sources.chunk.Attributes.OrderedKeyIndices;
import io.deephaven.db.v2.sources.chunk.LongChunk;
import io.deephaven.db.v2.sources.chunk.WritableDoubleChunk;
import io.deephaven.db.v2.utils.OrderedKeys;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Random;

class DoubleHelper implements FillBenchmarkHelper {
    private final double[] doubleArray;
    private final DoubleArraySource doubleArraySource;
    private final WritableSource doubleSparseArraySource;

    private final ColumnSource.FillContext arrayContext;
    private final ColumnSource.FillContext sparseContext;

    DoubleHelper(Random random, int fullSize, int fetchSize) {
        doubleArray = new double[fullSize];
        for (int ii = 0; ii < doubleArray.length; ii++) {
            doubleArray[ii] = makeValue(random);
        }

        doubleSparseArraySource = new DoubleSparseArraySource();
        doubleArraySource = new DoubleArraySource();
        doubleArraySource.ensureCapacity(doubleArray.length);

        for (int ii = 0; ii < doubleArray.length; ii++) {
            doubleArraySource.set(ii, doubleArray[ii]);
            doubleSparseArraySource.set(ii, doubleArray[ii]);
        }

        arrayContext = doubleArraySource.makeFillContext(fetchSize);
        sparseContext = doubleSparseArraySource.makeFillContext(fetchSize);
    }

    @Override
    public void release() {
        arrayContext.close();
        sparseContext.close();
    }

    @Override
    public void getFromArray(Blackhole bh, int fetchSize, LongChunk<OrderedKeyIndices> keys) {
        final WritableDoubleChunk result = WritableDoubleChunk.makeWritableChunk(fetchSize);
        for (int ii = 0; ii < keys.size(); ++ii) {
            result.set(ii, doubleArray[(int)keys.get(ii)]);
        }
        bh.consume(result);
    }

    @Override
    public void fillFromArrayBacked(Blackhole bh, int fetchSize, OrderedKeys orderedKeys) {
        final WritableDoubleChunk result = WritableDoubleChunk.makeWritableChunk(fetchSize);

        doubleArraySource.fillChunk(arrayContext, result, orderedKeys);

        bh.consume(result);
    }

    @Override
    public void fillFromSparse(Blackhole bh, int fetchSize, OrderedKeys orderedKeys) {
        final WritableDoubleChunk result = WritableDoubleChunk.makeWritableChunk(fetchSize);

        doubleSparseArraySource.fillChunk(sparseContext, result, orderedKeys);

        bh.consume(result);
    }

    private double makeValue(Random random) {
        // region makeValue
        return (double)(random.nextInt('Z' - 'A') + 'A');
        // region makeValue
    }
}
