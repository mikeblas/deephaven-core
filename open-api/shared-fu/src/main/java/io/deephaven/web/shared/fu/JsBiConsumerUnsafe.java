package io.deephaven.web.shared.fu;

import jsinterop.annotations.JsFunction;

/**
 * A js-friendly BiConsumer FunctionalInterface
 */
@JsFunction
@FunctionalInterface
public interface JsBiConsumerUnsafe<T1, T2, E extends Throwable> {

    @SuppressWarnings("unusable-by-js")
    void apply(T1 one, T2 two) throws E;

}
