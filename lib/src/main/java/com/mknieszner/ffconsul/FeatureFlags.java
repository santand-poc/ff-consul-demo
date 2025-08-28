package com.mknieszner.ffconsul;

public interface FeatureFlags {
    String getString(String key, String defaultValue);

    boolean getBool(String key, boolean defaultValue);

    int getInt(String key, int defaultValue);

    long getLong(String key, long defaultValue);

    double getDouble(String key, double defaultValue);

    <T> T getJson(String key, Class<T> type, T defaultValue);

    /**
     * Wyczyść cache (np. w testach)
     */
    default void clearCache() {
    }
}