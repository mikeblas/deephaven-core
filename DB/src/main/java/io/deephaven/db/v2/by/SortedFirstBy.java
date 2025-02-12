/*
 * Copyright (c) 2016-2021 Deephaven Data Labs and Patent Pending
 */

package io.deephaven.db.v2.by;

public final class SortedFirstBy extends SortedFirstOrLastByFactoryImpl {
    public SortedFirstBy(String... sortColumnNames) {
        super(true, sortColumnNames);
    }
}
