/*
 * Copyright (c) 2016-2021 Deephaven Data Labs and Patent Pending
 */

package io.deephaven.db.plot.example_plots;

import io.deephaven.db.plot.Figure;
import io.deephaven.db.plot.FigureFactory;
import io.deephaven.db.tables.utils.DBDateTime;
import io.deephaven.db.tables.utils.DBTimeUtils;


/**
 * Sample catErrorBar plot.
 */
public class SimpleCatError {

    public static void main(String[] args) {
        boolean testOHLC = false;
        final String[] x1 = {"A", "B", "C", "D"};
        final double[] y1 = {5.4, 2.3, 5.4, 4.4};

        final double[] xLow = {0.5, 1.5, 2.5, 3.5};
        final double[] xHigh = {1.5, 2.5, 3.5, 4.5};
        final double[] yLow = {4.9, -1.8, 4.9, 3.9};
        final double[] yHigh = {5.9, 2.8, 5.9, 4.9};

        final long time = 1491946585000000000L;
        DBDateTime[] date = {
                new DBDateTime(time + DBTimeUtils.DAY * 1),
                new DBDateTime(time + DBTimeUtils.DAY * 2),
                new DBDateTime(time + DBTimeUtils.DAY * 3),
                new DBDateTime(time + DBTimeUtils.DAY * 4)};

        final Number[] open = {3, 4, 3, 5};
        final Number[] high = {5, 6, 5, 7};
        final Number[] low = {2, 3, 1, 4};
        final Number[] close = {4, 5, 4, 6};

        Figure fig = FigureFactory.figure()
                .catErrorBar("S1", x1, y1, yLow, yHigh)
                .plotStyle("bar")
                .plotOrientation("H");

        ExamplePlotUtils.display(fig);
    }
}
