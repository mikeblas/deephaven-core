package io.deephaven.web.shared.ide.lsp;

import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsType;
import jsinterop.base.JsPropertyMap;

import java.io.Serializable;

@JsType(namespace = "dh.lsp")
public class CompletionParams extends TextDocumentPositionParams implements Serializable {
    public CompletionParams() {
        super();
    }

    @JsIgnore
    public CompletionParams(JsPropertyMap<Object> source) {
        this();

        this.updateFromJsPropertyMap(source);
    }

    protected void updateFromJsPropertyMap(JsPropertyMap<Object> source) {
        super.updateFromJsPropertyMap(source);

        if (source.has("context")) {
            context = new CompletionContext(source.getAny("context").asPropertyMap());
        }
    }

    public CompletionContext context;
}
