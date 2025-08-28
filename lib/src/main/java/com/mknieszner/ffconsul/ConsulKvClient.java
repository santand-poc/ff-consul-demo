package com.mknieszner.ffconsul;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mknieszner.ffconsul.model.ConsulKvEntry;
import org.springframework.http.HttpHeaders;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

class ConsulKvClient {

    private final RestTemplate http;
    private final ObjectMapper om = new ObjectMapper();
    private final String baseUrl;  // np. http://localhost:8500
    private final String token;    // ACL (opcjonalnie)

    ConsulKvClient(String baseUrl, int timeoutMs, String token) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.token = token;

        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(timeoutMs);
        rf.setReadTimeout(timeoutMs);
        this.http = new RestTemplate(rf);
    }

    /**
     * GET /v1/kv/{path}?raw → zwraca String lub null
     */
    String getRaw(String path) {
        try {
            URI uri = URI.create(baseUrl + "/v1/kv/" + path + "?raw");
            RequestEntity<Void> req = RequestEntity
                    .get(uri)
                    .headers(headers())
                    .build();
            ResponseEntity<String> resp = http.exchange(req, String.class);
            return resp.getBody();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * GET /v1/kv/{prefix}/?recurse [ + index & wait ]
     * Zwraca: body (lista wpisów) + X-Consul-Index (nagłówek) albo null przy błędzie.
     */
    RecurseResponse getRecurse(String prefix, Long index, Integer waitSeconds) {
        try {
            StringBuilder sb = new StringBuilder(baseUrl)
                    .append("/v1/kv/").append(prefix).append("/?recurse");
            if (index != null) sb.append("&index=").append(index);
            if (waitSeconds != null && waitSeconds > 0) sb.append("&wait=").append(waitSeconds).append("s");

            RequestEntity<Void> req = RequestEntity
                    .get(URI.create(sb.toString()))
                    .headers(headers())
                    .build();

            ResponseEntity<String> resp = http.exchange(req, String.class);
            if (resp.getStatusCode().is2xxSuccessful()) {
                String body = resp.getBody();
                if (body == null || body.isBlank()) return new RecurseResponse(List.of(), headerIndex(resp));
                List<ConsulKvEntry> list = om.readValue(body, new TypeReference<List<ConsulKvEntry>>() {
                });
                return new RecurseResponse(list, headerIndex(resp));
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private long headerIndex(ResponseEntity<?> resp) {
        List<String> idx = resp.getHeaders().get("X-Consul-Index");
        if (idx == null || idx.isEmpty()) return 0L;
        try {
            return Long.parseLong(idx.get(0));
        } catch (Exception e) {
            return 0L;
        }
    }

    HttpHeaders headers() {
        HttpHeaders h = new HttpHeaders();
        if (token != null && !token.isBlank()) {
            h.set("X-Consul-Token", token);
        }
        return h;
    }

    static String decodeBase64(String b64) {
        if (b64 == null) return null;
        try {
            return new String(Base64.getDecoder().decode(b64), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    static final class RecurseResponse {
        final List<ConsulKvEntry> entries;
        final long index;

        RecurseResponse(List<ConsulKvEntry> entries, long index) {
            this.entries = entries;
            this.index = index;
        }
    }
}
