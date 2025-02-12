package io.deephaven.web.shared.ide.lsp;

import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsType;
import jsinterop.base.JsPropertyMap;

import java.io.Serializable;

@JsType(namespace = "dh.lsp")
public class Location implements Serializable {
    public String uri;
    public DocumentRange range;

    public Location() {}

    @JsIgnore
    public Location(JsPropertyMap<Object> source) {
        this();

        if (source.has("uri")) {
            uri = source.getAny("uri").asString();
        }
        if (source.has("range")) {
            range = new DocumentRange(source.getAny("range").asPropertyMap());
        }
    }
}
