package com.zuvo.web;

import java.util.LinkedHashMap;
import java.util.Map;

/** Tiny helper to build JSON objects: J.of("key", value, "key2", value2). */
public final class J {
    private J() {}

    public static Map<String, Object> of(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }
}
