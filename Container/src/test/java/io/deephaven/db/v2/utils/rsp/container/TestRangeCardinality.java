package io.deephaven.db.v2.utils.rsp.container;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;

@RunWith(Parameterized.class)
public class TestRangeCardinality {

    private final int[] elements;
    private final int begin;
    private final int end;
    private final int expected;

    @Parameterized.Parameters(name = "{index}: cardinalityInBitmapRange({0},{1},{2})={3}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {new int[]{1, 3, 5, 7, 9}, 3, 8, 3},
                {new int[]{1, 3, 5, 7, 9}, 2, 8, 3},
                {new int[]{1, 3, 5, 7, 9}, 3, 7, 2},
                {new int[]{1, 3, 5, 7, 9}, 0, 7, 3},
                {new int[]{1, 3, 5, 7, 9}, 0, 6, 3},
                {new int[]{1, 3, 5, 7, 9, Short.MAX_VALUE}, 0, Short.MAX_VALUE + 1, 6},
                {new int[]{1, 10000, 25000, Short.MAX_VALUE - 1}, 0, Short.MAX_VALUE, 4},
                {new int[]{1 << 3, 1 << 8, 511, 512, 513, 1 << 12, 1 << 14}, 0, Short.MAX_VALUE, 7}
        });
    }

    public TestRangeCardinality(int[] elements, int begin, int end, int expected) {
        this.elements = elements;
        this.begin = begin;
        this.end = end;
        this.expected = expected;
    }

    @Test
    public void testCardinalityInBitmapWordRange() {
        BitmapContainer bc = new BitmapContainer();
        for (int e : elements) {
            bc.iset((short) e);
        }
        Assert.assertEquals(expected, ContainerUtil.cardinalityInBitmapRange(bc.bitmap, begin, end));
    }
}
