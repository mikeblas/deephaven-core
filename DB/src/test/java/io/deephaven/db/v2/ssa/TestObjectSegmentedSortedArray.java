/* ---------------------------------------------------------------------------------------------------------------------
 * AUTO-GENERATED CLASS - DO NOT EDIT MANUALLY - for any changes edit TestCharSegmentedSortedArray and regenerate
 * ------------------------------------------------------------------------------------------------------------------ */
package io.deephaven.db.v2.ssa;

import io.deephaven.db.tables.Table;
import io.deephaven.db.tables.live.LiveTableMonitor;
import io.deephaven.db.v2.*;
import io.deephaven.db.v2.sources.ColumnSource;
import io.deephaven.db.v2.sources.chunk.Attributes;
import io.deephaven.db.v2.sources.chunk.Attributes.KeyIndices;
import io.deephaven.db.v2.sources.chunk.Attributes.Values;
import io.deephaven.db.v2.sources.chunk.ObjectChunk;
import io.deephaven.db.v2.sources.chunk.LongChunk;
import io.deephaven.db.v2.utils.Index;
import io.deephaven.db.v2.utils.IndexShiftData;
import io.deephaven.test.types.ParallelTest;
import junit.framework.TestCase;
import org.junit.experimental.categories.Category;

import java.util.Random;

import static io.deephaven.db.v2.TstUtils.getTable;
import static io.deephaven.db.v2.TstUtils.initColumnInfos;

@Category(ParallelTest.class)
public class TestObjectSegmentedSortedArray extends LiveTableTestCase {
    public void testInsertion() {
        for (int seed = 0; seed < 10; ++seed) {
            for (int tableSize = 100; tableSize <= 1000; tableSize *= 10) {
                for (int nodeSize = 8; nodeSize <= 2048; nodeSize *= 2) {
                    testUpdates(seed, tableSize, nodeSize, true, false);
                }
            }
        }
    }

    public void testRemove() {
        for (int seed = 0; seed < 20; ++seed) {
            for (int tableSize = 100; tableSize <= 10000; tableSize *= 10) {
                for (int nodeSize = 16; nodeSize <= 2048; nodeSize *= 2) {
                    testUpdates(seed, tableSize, nodeSize, false, true);
                }
            }
        }
    }

    public void testInsertAndRemove() {
        for (int seed = 0; seed < 10; ++seed) {
            for (int tableSize = 100; tableSize <= 10000; tableSize *= 10) {
                for (int nodeSize = 16; nodeSize <= 2048; nodeSize *= 2) {
                    testUpdates(seed, tableSize, nodeSize, true, true);
                }
            }
        }
    }

    public void testShifts() {
        for (int seed = 0; seed < 20; ++seed) {
            for (int tableSize = 10; tableSize <= 10000; tableSize *= 10) {
                for (int nodeSize = 16; nodeSize <= 2048; nodeSize *= 2) {
                    testShifts(seed, tableSize, nodeSize);
                }
            }
        }
    }

    private void testShifts(final int seed, final int tableSize, final int nodeSize) {
        final Random random = new Random(seed);
        final TstUtils.ColumnInfo[] columnInfo;
        final QueryTable table = getTable(tableSize, random, columnInfo = initColumnInfos(new String[]{"Value"},
                SsaTestHelpers.getGeneratorForObject()));

        final Table asObject = SsaTestHelpers.prepareTestTableForObject(table);

        final ObjectSegmentedSortedArray ssa = new ObjectSegmentedSortedArray(nodeSize);

        //noinspection unchecked
        final ColumnSource<Object> valueSource = asObject.getColumnSource("Value");

        System.out.println("Creation seed=" + seed + ", tableSize=" + tableSize + ", nodeSize=" + nodeSize);
        checkSsaInitial(asObject, ssa, valueSource);

        ((DynamicTable)asObject).listenForUpdates(new InstrumentedShiftAwareListenerAdapter((DynamicTable) asObject) {
            @Override
            public void onUpdate(Update upstream) {
                try (final ColumnSource.GetContext checkContext = valueSource.makeGetContext(asObject.getIndex().getPrevIndex().intSize())) {
                    final Index relevantIndices = asObject.getIndex().getPrevIndex();
                    checkSsa(ssa, valueSource.getPrevChunk(checkContext, relevantIndices).asObjectChunk(), relevantIndices.asKeyIndicesChunk());
                }

                final int size = Math.max(upstream.modified.intSize() +  Math.max(upstream.added.intSize(), upstream.removed.intSize()), (int)upstream.shifted.getEffectiveSize());
                try (final ColumnSource.GetContext getContext = valueSource.makeGetContext(size)) {
                    ssa.validate();

                    final Index takeout = upstream.removed.union(upstream.getModifiedPreShift());
                    if (takeout.nonempty()) {
                        final ObjectChunk<Object, ? extends Values> valuesToRemove = valueSource.getPrevChunk(getContext, takeout).asObjectChunk();
                        ssa.remove(valuesToRemove, takeout.asKeyIndicesChunk());
                    }

                    ssa.validate();

                    try (final ColumnSource.GetContext checkContext = valueSource.makeGetContext(asObject.getIndex().getPrevIndex().intSize())) {
                        final Index relevantIndices = asObject.getIndex().getPrevIndex().minus(takeout);
                        checkSsa(ssa, valueSource.getPrevChunk(checkContext, relevantIndices).asObjectChunk(), relevantIndices.asKeyIndicesChunk());
                    }

                    if (upstream.shifted.nonempty()) {
                        final IndexShiftData.Iterator sit = upstream.shifted.applyIterator();
                        while (sit.hasNext()) {
                            sit.next();
                            final Index indexToShift = table.getIndex().getPrevIndex().subindexByKey(sit.beginRange(), sit.endRange()).minus(upstream.getModifiedPreShift()).minus(upstream.removed);
                            if (indexToShift.empty()) {
                                continue;
                            }

                            final ObjectChunk<Object, ? extends Values> shiftValues = valueSource.getPrevChunk(getContext, indexToShift).asObjectChunk();

                            if (sit.polarityReversed()) {
                                ssa.applyShiftReverse(shiftValues, indexToShift.asKeyIndicesChunk(), sit.shiftDelta());
                            } else {
                                ssa.applyShift(shiftValues, indexToShift.asKeyIndicesChunk(), sit.shiftDelta());
                            }
                        }
                    }

                    ssa.validate();

                    final Index putin = upstream.added.union(upstream.modified);

                    try (final ColumnSource.GetContext checkContext = valueSource.makeGetContext(asObject.intSize())) {
                        final Index relevantIndices = asObject.getIndex().minus(putin);
                        checkSsa(ssa, valueSource.getChunk(checkContext, relevantIndices).asObjectChunk(), relevantIndices.asKeyIndicesChunk());
                    }

                    if (putin.nonempty()) {
                        final ObjectChunk<Object, ? extends Values> valuesToInsert = valueSource.getChunk(getContext, putin).asObjectChunk();
                        ssa.insert(valuesToInsert, putin.asKeyIndicesChunk());
                    }

                    ssa.validate();
                }
            }
        });

        for (int step = 0; step < 50; ++step) {
            System.out.println("Seed = " + seed + ", tableSize=" + tableSize + ", nodeSize=" + nodeSize + ", step = " + step);
            LiveTableMonitor.DEFAULT.runWithinUnitTestCycle(() ->
                    GenerateTableUpdates.generateShiftAwareTableUpdates(GenerateTableUpdates.DEFAULT_PROFILE, tableSize, random, table, columnInfo));

            try (final ColumnSource.GetContext getContext = valueSource.makeGetContext(asObject.intSize())) {
                checkSsa(ssa, valueSource.getChunk(getContext, asObject.getIndex()).asObjectChunk(), asObject.getIndex().asKeyIndicesChunk());
            }
        }
    }

    private void testUpdates(final int seed, final int tableSize, final int nodeSize, boolean allowAddition, boolean allowRemoval) {
        final Random random = new Random(seed);
        final TstUtils.ColumnInfo[] columnInfo;
        final QueryTable table = getTable(tableSize, random, columnInfo = initColumnInfos(new String[]{"Value"},
                SsaTestHelpers.getGeneratorForObject()));

        final Table asObject = SsaTestHelpers.prepareTestTableForObject(table);

        final ObjectSegmentedSortedArray ssa = new ObjectSegmentedSortedArray(nodeSize);

        //noinspection unchecked
        final ColumnSource<Object> valueSource = asObject.getColumnSource("Value");

        System.out.println("Creation seed=" + seed + ", tableSize=" + tableSize + ", nodeSize=" + nodeSize);
        checkSsaInitial(asObject, ssa, valueSource);

        ((DynamicTable)asObject).listenForUpdates(new InstrumentedListenerAdapter((DynamicTable) asObject) {
            @Override
            public void onUpdate(Index added, Index removed, Index modified) {
                try (final ColumnSource.GetContext getContext = valueSource.makeGetContext(Math.max(added.intSize(), removed.intSize()))) {
                    if (removed.nonempty()) {
                        final ObjectChunk<Object, ? extends Values> valuesToRemove = valueSource.getPrevChunk(getContext, removed).asObjectChunk();
                        ssa.remove(valuesToRemove, removed.asKeyIndicesChunk());
                    }
                    if (added.nonempty()) {
                        ssa.insert(valueSource.getChunk(getContext, added).asObjectChunk(), added.asKeyIndicesChunk());
                    }
                }
            }
        });

        for (int step = 0; step < 50; ++step) {
            System.out.println("Seed = " + seed + ", tableSize=" + tableSize + ", nodeSize=" + nodeSize + ", step = " + step);
            LiveTableMonitor.DEFAULT.runWithinUnitTestCycle(() -> {
                final Index [] notify = GenerateTableUpdates.computeTableUpdates(tableSize, random, table, columnInfo, allowAddition, allowRemoval, false);
                assertTrue(notify[2].empty());
                table.notifyListeners(notify[0], notify[1], notify[2]);
            });

            try (final ColumnSource.GetContext getContext = valueSource.makeGetContext(asObject.intSize())) {
                checkSsa(ssa, valueSource.getChunk(getContext, asObject.getIndex()).asObjectChunk(), asObject.getIndex().asKeyIndicesChunk());
            }

            if (!allowAddition && table.size() == 0) {
                System.out.println("All values removed.");
                break;
            }
        }

    }

    private void checkSsaInitial(Table asObject, ObjectSegmentedSortedArray ssa, ColumnSource<?> valueSource) {
        try (final ColumnSource.GetContext getContext = valueSource.makeGetContext(asObject.intSize())) {
            final ObjectChunk<Object, ? extends Values> valueChunk = valueSource.getChunk(getContext, asObject.getIndex()).asObjectChunk();
            final LongChunk<Attributes.OrderedKeyIndices> tableIndexChunk = asObject.getIndex().asKeyIndicesChunk();

            ssa.insert(valueChunk, tableIndexChunk);

            checkSsa(ssa, valueChunk, tableIndexChunk);
        }
    }

    private void checkSsa(ObjectSegmentedSortedArray ssa, ObjectChunk<Object, ? extends Values> valueChunk, LongChunk<? extends KeyIndices> tableIndexChunk) {
        ssa.validate();

        try {
            ObjectSsaChecker.checkSsa(ssa, valueChunk, tableIndexChunk);
        } catch (SsaChecker.SsaCheckException e) {
            TestCase.fail(e.getMessage());
        }
    }
}
