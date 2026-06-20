package com.gmailintelligence.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String encryptionKey,
        Gmail gmail,
        Ai ai,
        Retrieval retrieval,
        Chunking chunking
) {
    public record Gmail(String clientId, String clientSecret, String redirectUri) {
        public boolean configured() {
            return hasText(clientId) && hasText(clientSecret) && hasText(redirectUri);
        }
    }

    public record Ai(String apiKey, String baseUrl, String chatModel, String embeddingModel, int embeddingDimensions) {
        public boolean configured() {
            return hasText(apiKey) && hasText(baseUrl) && hasText(chatModel) && hasText(embeddingModel);
        }
    }

    public record Retrieval(int topK, double minimumSimilarity) {
    }

    public record Chunking(int chunkSize, int overlap) {
    }

    public boolean encryptionConfigured() {
        return hasText(encryptionKey) && encryptionKey.length() >= 32;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
