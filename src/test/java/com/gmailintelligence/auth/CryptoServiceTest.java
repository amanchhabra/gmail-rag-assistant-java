package com.gmailintelligence.auth;

import com.gmailintelligence.configuration.AppProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CryptoServiceTest {

    @Test
    void encryptsAndDecryptsRefreshToken() {
        CryptoService service = new CryptoService(properties());

        String encrypted = service.encrypt("refresh-token");

        assertThat(encrypted).isNotEqualTo("refresh-token");
        assertThat(service.decrypt(encrypted)).isEqualTo("refresh-token");
    }

    private AppProperties properties() {
        return new AppProperties(
                "test-encryption-key-with-32-chars",
                new AppProperties.Gmail("client", "secret", "http://localhost/callback"),
                new AppProperties.Ai("key", "http://localhost", "chat", "embedding", 4),
                new AppProperties.Retrieval(6, 0.1),
                new AppProperties.Chunking(100, 10));
    }
}
