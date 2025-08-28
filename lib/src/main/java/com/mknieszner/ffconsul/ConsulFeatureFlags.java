package com.mknieszner.ffconsul;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mknieszner.ffconsul.model.ConsulKvEntry;
import jakarta.annotation.PreDestroy;

import java.util.List;
import java.util.concurrent.*;

public class ConsulFeatureFlags implements FeatureFlags {

    private final FfConsulProperties cfg;
    private final ConsulKvClient client;
    private final ObjectMapper om = new ObjectMapper();

    private final ConcurrentMap<String, Cached> cache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "ff-consul-refresh");
        t.setDaemon(true);
        return t;
    });

    private volatile boolean running = true;
    private volatile Long lastIndex = null; // do blocking queries

    public ConsulFeatureFlags(FfConsulProperties cfg) {
        this.cfg = cfg;
        this.client = new ConsulKvClient(cfg.getUrl(), cfg.getTimeoutMs(), cfg.getToken());
        // Start background updater
        if (cfg.isUseBlockingQueries()) {
            scheduler.execute(this::watchLoop);
        } else {
            scheduler.scheduleAtFixedRate(this::refreshPrefix, 2, cfg.getRefreshSeconds(), TimeUnit.SECONDS);
        }
    }

    private String fullPath(String key) {
        String base = trimSlashes(cfg.getBasePath());
        String env = trimSlashes(cfg.getEnv());
        if (!env.isBlank()) {
            return base + "/" + env + "/" + trimSlashes(key);
        }
        return base + "/" + trimSlashes(key);
    }

    private static String trimSlashes(String s) {
        if (s == null) return "";
        return s.replaceAll("^/+", "").replaceAll("/+$", "");
    }

    /* === Background refresh (polling) === */
    private void refreshPrefix() {
        String prefix = effectivePrefix();
        ConsulKvClient.RecurseResponse rr = client.getRecurse(prefix, null, null);
        if (rr == null) return;
        updateCacheFromEntries(rr.entries);
    }

    /* === Background watch (blocking queries) === */
    private void watchLoop() {
        String prefix = effectivePrefix();
        while (running) {
            try {
                ConsulKvClient.RecurseResponse rr = client.getRecurse(prefix, lastIndex, cfg.getBlockingWaitSeconds());
                if (rr != null) {
                    if (lastIndex == null || rr.index != lastIndex) {
                        updateCacheFromEntries(rr.entries);
                        lastIndex = rr.index;
                    }
                }
            } catch (Exception ignore) {
            }
        }
    }

    private String effectivePrefix() {
        String base = trimSlashes(cfg.getBasePath());
        String env = trimSlashes(cfg.getEnv());
        return (env.isBlank()) ? base : (base + "/" + env);
    }

    private void updateCacheFromEntries(List<ConsulKvEntry> entries) {
        if (entries == null) return;
        long exp = System.currentTimeMillis() + cfg.getTtlSeconds() * 1000L;
        String prefix = effectivePrefix() + "/";
        for (ConsulKvEntry e : entries) {
            if (e.getKey() == null) continue;
            String relKey = e.getKey().startsWith(prefix) ? e.getKey().substring(prefix.length()) : e.getKey();
            String val = ConsulKvClient.decodeBase64(e.getValueBase64());
            if (val != null) {
                cache.put(relKey, new Cached(val, exp));
            }
        }
    }

    private String getOrFetch(String key, String defaultValue) {
        String relKey = trimSlashes(key);
        Cached c = cache.get(relKey);
        long now = System.currentTimeMillis();
        if (c != null && !c.isExpired(now)) return c.value;

        String fullPath = fullPath(relKey);
        String v = client.getRaw(fullPath);
        if (v != null) {
            cache.put(relKey, new Cached(v, now + cfg.getTtlSeconds() * 1000L));
            return v;
        }
        return (c != null) ? c.value : defaultValue;
    }

    /* === API === */

    @Override
    public String getString(String key, String defaultValue) {
        String v = getOrFetch(key, defaultValue);
        return v == null ? defaultValue : v;
    }

    @Override
    public boolean getBool(String key, boolean defaultValue) {
        String v = getOrFetch(key, String.valueOf(defaultValue));
        if (v == null) return defaultValue;
        String s = v.trim().toLowerCase();
        return s.equals("true") || s.equals("1") || s.equals("yes") || s.equals("on");
    }

    @Override
    public int getInt(String key, int defaultValue) {
        String v = getOrFetch(key, String.valueOf(defaultValue));
        if (v == null) return defaultValue;
        try {
            return Integer.parseInt(v.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    @Override
    public long getLong(String key, long defaultValue) {
        String v = getOrFetch(key, String.valueOf(defaultValue));
        if (v == null) return defaultValue;
        try {
            return Long.parseLong(v.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    @Override
    public double getDouble(String key, double defaultValue) {
        String v = getOrFetch(key, String.valueOf(defaultValue));
        if (v == null) return defaultValue;
        try {
            return Double.parseDouble(v.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    @Override
    public <T> T getJson(String key, Class<T> type, T defaultValue) {
        String v = getOrFetch(key, null);
        if (v == null || v.isBlank()) return defaultValue;
        try {
            return om.readValue(v, type);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    @Override
    public void clearCache() {
        cache.clear();
    }

    @PreDestroy
    public void shutdown() {
        running = false;
        scheduler.shutdownNow();
    }

    /* === Cache holder === */
    private static final class Cached {
        final String value;
        final long expiresAt;

        Cached(String value, long expiresAt) {
            this.value = value;
            this.expiresAt = expiresAt;
        }

        boolean isExpired(long now) {
            return now > expiresAt;
        }
    }
}
