package com.thirdapi.starter.config;

/**
 * SPI for loading endpoint configuration snapshots.
 */
public interface ConfigSource {

    String name();

    /**
     * Returns a new snapshot, or null when nothing changed.
     */
    ConfigSnapshot load();
}
