package com.example.router;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

/**
 * Prosty Feign bez discovery – używa url z application.yml (provider.baseUrl).
 */
@FeignClient(name = "providerClient", url = "${provider.baseUrl}")
public interface ProviderClient {

    @GetMapping("/v1/data")
    Map<String, Object> getV1();

    @GetMapping("/v2/data")
    Map<String, Object> getV2();
}
