package com.example.router;

import com.mknieszner.ffconsul.FeatureFlags;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class RouterController {
    private final FeatureFlags ff;
    private final ProviderClient provider;

    public RouterController(FeatureFlags ff, ProviderClient provider) {
        this.ff = ff;
        this.provider = provider;
    }

    @GetMapping("/api/data")
    public Map<String, Object> data() {
        // Klucz w Consulu: CLP/ff/prod/pricing/demo/version -> "v1" | "v2"
        String version = ff.getString("pricing/demo/version", "v1");

        Map<String, Object> upstream = switch (version) {
            case "v2" -> provider.getV2();
            default   -> provider.getV1();
        };

        return Map.of(
                "routedVersion", version,
                "upstream", upstream
        );
    }

    @GetMapping("/api/ff/version")
    public Map<String, Object> current() {
        return Map.of("pricing.demo.version", ff.getString("pricing/demo/version", "v1"));
    }
}
