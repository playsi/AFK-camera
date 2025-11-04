package net.playsi.Afkcam.debug;

import java.util.LinkedHashMap;
import java.util.Map;

public class DebugInfoBuilder {
    private final Map<String, DebugEntry> entries = new LinkedHashMap<>();

    public DebugInfoBuilder add(String key, Object value) {
        entries.put(key, new DebugEntry(key, String.valueOf(value)));
        return this;
    }

    public DebugInfoBuilder addConditional(String key, Object value, boolean condition) {
        entries.put(key, new DebugEntry(key, String.valueOf(value)).setCondition(condition));
        return this;
    }

    public Map<String, DebugEntry> build() {
        return entries;
    }
}
