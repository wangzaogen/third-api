package com.thirdapi.starter.config;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * 调用链共享的最新配置快照。
 *
 * <p>通过不可变 Map 保存配置，避免执行期间读到半更新状态；
 * 配置更新后通过监听器通知关注方。</p>
 */
public class ConfigStore {

    /** 当前生效的接口配置，key 为 provider.channel.endpoint。 */
    private volatile Map<String, ApiConfig> configs = Collections.emptyMap();
    /** 当前配置版本号。 */
    private volatile long version;
    /** 配置变更监听器，使用写时复制列表保证并发遍历安全。 */
    private final CopyOnWriteArrayList<Consumer<ConfigSnapshot>> listeners = new CopyOnWriteArrayList<Consumer<ConfigSnapshot>>();

    /**
     * 用新快照原子替换当前配置，并通知所有监听器。
     */
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

    /**
     * 按 provider.channel.endpoint 查询接口配置，未配置时返回 null。
     */
    public ApiConfig get(String key) {
        return configs.get(key);
    }

    /**
     * 返回全部接口配置。
     */
    public Collection<ApiConfig> all() {
        return configs.values();
    }

    /**
     * 返回当前配置版本号。
     */
    public long getVersion() {
        return version;
    }

    /**
     * 注册配置变更监听器，每次配置更新后回调。
     */
    public void addListener(Consumer<ConfigSnapshot> listener) {
        listeners.add(listener);
    }
}
