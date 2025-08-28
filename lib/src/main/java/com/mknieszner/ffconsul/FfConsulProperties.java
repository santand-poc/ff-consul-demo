package com.mknieszner.ffconsul;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("ff.consul")
public class FfConsulProperties {

    /**
     * URL Consula, np. http://localhost:8500
     */
    private String url = "http://localhost:8500";

    /**
     * Ścieżka bazowa w KV bez slasha na końcu, np. "CLP/ff".
     * Końcowy path = basePath + (env != "" ? "/" + env : "") + "/" + klucz
     */
    private String basePath = "CLP/ff";

    /**
     * Nazwa środowiska w ścieżce (opcjonalnie), np. "prod", "uat", "dev1".
     * Jeśli pusty – klucze będą pod basePath/...
     */
    private String env = "";

    /**
     * ACL token Consula (opcjonalnie).
     */
    private String token;

    /**
     * TTL cache (sekundy).
     */
    private int ttlSeconds = 60;

    /**
     * Okres odświeżania prefiksu recurse (sekundy) – używane,
     * gdy useBlockingQueries=false (polling).
     */
    private int refreshSeconds = 20;

    /**
     * Czy używać blocking queries (watch) do natychmiastowej invalidacji?
     * Jeśli true – używa /v1/kv/{prefix}?recurse&index=...&wait=...s
     */
    private boolean useBlockingQueries = true;

    /**
     * Maks. czas oczekiwania na zmianę w blocking query (sekundy).
     */
    private int blockingWaitSeconds = 55;

    /**
     * Timeout HTTP (ms).
     */
    private int timeoutMs = 500;

    /**
     * Włącza/wyłącza starter.
     */
    private boolean enabled = true;

    // getters/setters:

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getBasePath() { return basePath; }
    public void setBasePath(String basePath) { this.basePath = basePath; }

    public String getEnv() { return env; }
    public void setEnv(String env) { this.env = env; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public int getTtlSeconds() { return ttlSeconds; }
    public void setTtlSeconds(int ttlSeconds) { this.ttlSeconds = ttlSeconds; }

    public int getRefreshSeconds() { return refreshSeconds; }
    public void setRefreshSeconds(int refreshSeconds) { this.refreshSeconds = refreshSeconds; }

    public boolean isUseBlockingQueries() { return useBlockingQueries; }
    public void setUseBlockingQueries(boolean useBlockingQueries) { this.useBlockingQueries = useBlockingQueries; }

    public int getBlockingWaitSeconds() { return blockingWaitSeconds; }
    public void setBlockingWaitSeconds(int blockingWaitSeconds) { this.blockingWaitSeconds = blockingWaitSeconds; }

    public int getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
