/*
 * Copyright (c) 2016-2021 Deephaven Data Labs and Patent Pending
 */

package io.deephaven.libs.primitives;

/**
 * Algorithm used to resolve ties when performing a binary search.
 *
 * @IncludeAll
 */
public enum BinSearch {
    /**
     * Binary search algorithm returns any matching index.
     */
    BS_ANY,

    /**
     * Binary search algorithm returns the highest matching index.
     */
    BS_HIGHEST,

    /**
     * Binary search algorithm returns the lowest matching index.
     */
    BS_LOWEST
}
