package com.thirdapi.starter.config;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Latest configuration snapshot shared by the invocation pipeline.
 */
public class ConfigStore {

    private volatile Map<String, ApiConfig> configs = Collections.emptyMap();
    private volatile long version;
    private final CopyOnWriteArrayList<Consumer<ConfigSnapshot>> listeners = new CopyOnWriteArrayList<Consumer<ConfigSnapshot>>();

    public void update(ConfigSnapshot snapshot) {
        Map<String, ApiConfig> next = new HashMap<String, ApiConfig>();
        for (ApiConfig config : snapshot.getConfigs()) {
            next.put(config.key(), config);
        }
        this.configs = Collections.unmodifiableMap(next);
        this.version = snapshot.getVersion();
        for (Consumer<ConfigSnapshot> listener : listeners) {
            listener.accept(snapshot);
        }
    }

    public ApiConfig get(String key) {
        return configs.get(key);
    }

    public Collection<ApiConfig> all() {
        return configs.values();
    }

    public long getVersion() {
        return version;
    }

    public void addListener(Consumer<ConfigSnapshot> listener) {
        listeners.add(listener);
    }
}
