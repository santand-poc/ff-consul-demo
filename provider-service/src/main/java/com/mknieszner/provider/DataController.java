package com.mknieszner.provider;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Map;

@RestController
public class DataController {

    @GetMapping("/v1/data")
    public Map<String, Object> v1() {
        // prostsza/“stara” odpowiedź
        return Map.of(
                "version", "v1",
                "message", "Hello from v1",
                "timestamp", OffsetDateTime.now().toString()
        );
    }

    @GetMapping("/v2/data")
    public Map<String, Object> v2() {
        // “nowa” odpowiedź, ciut inna struktura/treść
        return Map.of(
                "version", "v2",
                "message", "Hello from v2 🚀",
                "timestamp", OffsetDateTime.now().toString(),
                "extra", Map.of("poweredBy", "provider-service")
        );
    }
}
