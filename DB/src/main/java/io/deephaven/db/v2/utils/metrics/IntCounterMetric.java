package io.deephaven.db.v2.utils.metrics;

public class IntCounterMetric implements IntMetric {
    private final int id;

    public IntCounterMetric(final String name) {
        id = MetricsManager.instance.registerIntCounterMetric(name);
    }

    @Override
    public void sample(final int n) {
        MetricsManager.instance.sampleIntCounter(id, n);
    }
}
