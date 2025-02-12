package io.deephaven.web.client.api.widget.calendar;

import io.deephaven.web.shared.data.BusinessPeriod;
import jsinterop.annotations.JsProperty;

public class JsBusinessPeriod {
    private final BusinessPeriod businessPeriod;

    public JsBusinessPeriod(BusinessPeriod businessPeriod) {
        this.businessPeriod = businessPeriod;
    }

    @JsProperty
    public String getOpen() {
        return businessPeriod.getOpen();
    }

    @JsProperty
    public String getClose() {
        return businessPeriod.getClose();
    }
}
