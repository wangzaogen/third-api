package com.thirdapi.starter.config;

import com.thirdapi.starter.autoconfigure.ThirdApiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Loads config on startup and polls the admin server in admin mode.
 */
public class ConfigSyncService {

    private static final Logger log = LoggerFactory.getLogger(ConfigSyncService.class);

    private final List<ConfigSource> sources;
    private final ConfigStore store;
    private final ThirdApiProperties properties;
    private final ScheduledExecutorService scheduler;

    public ConfigSyncService(List<ConfigSource> sources,
                             ConfigStore store,
                             ThirdApiProperties properties) {
        this.sources = sources;
        this.store = store;
        this.properties = properties;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "third-api-config-sync");
            thread.setDaemon(true);
            return thread;
        });
        start();
    }

    private void start() {
        refresh();
        boolean adminMode = "admin".equalsIgnoreCase(properties.getMode())
                && properties.getAdminUrl() != null
                && !properties.getAdminUrl().isEmpty();
        if (adminMode) {
            long interval = Math.max(1, properties.getPollIntervalSeconds());
            scheduler.scheduleWithFixedDelay(this::refresh, interval, interval, TimeUnit.SECONDS);
        }
    }

    private void refresh() {
        for (ConfigSource source : sources) {
            try {
                ConfigSnapshot snapshot = source.load();
                if (snapshot != null) {
                    store.update(snapshot);
                    log.info("Loaded {} endpoint configs from {} at version {}",
                            snapshot.getConfigs().size(), source.name(), snapshot.getVersion());
                }
            } catch (RuntimeException e) {
                log.warn("Config source {} failed: {}", source.name(), e.getMessage());
            }
        }
    }

    public void close() {
        scheduler.shutdownNow();
    }
}
