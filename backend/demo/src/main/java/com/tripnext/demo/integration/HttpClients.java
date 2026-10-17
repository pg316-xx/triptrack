package com.tripnext.demo.integration;

import java.time.Duration;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.json.JsonMapper;

/** Shared bits for calling external APIs: short timeouts and a JSON mapper. */
final class HttpClients {

    static final JsonMapper JSON = JsonMapper.builder().build();

    private HttpClients() {
    }

    // External APIs must never hold a request thread for long (SRS CON-EXT-005).
    static RestClient restClient(String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(5));
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }
}
