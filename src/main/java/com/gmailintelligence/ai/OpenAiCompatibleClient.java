package com.gmailintelligence.ai;

import com.gmailintelligence.configuration.AppProperties;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Comparator;

@Component
class OpenAiCompatibleClient implements AiClient {
    private final AppProperties properties;
    private final RestClient restClient;

    OpenAiCompatibleClient(AppProperties properties, RestClient.Builder builder) {
        this.properties = properties;
        this.restClient = builder
                .baseUrl(properties.ai().baseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.ai().apiKey())
                .build();
    }

    @Override
    public List<Double> embed(String text) {
        return embedAll(List.of(text)).getFirst();
    }

    @Override
    public List<List<Double>> embedAll(List<String> texts) {
        if (texts.isEmpty()) {
            return List.of();
        }
        requireAiConfig();
        Map<String, Object> response = restClient.post()
                .uri("/v1/embeddings")
                .body(Map.of(
                        "model", properties.ai().embeddingModel(),
                        "input", texts))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        return ((List<?>) response.get("data")).stream()
                .map(Map.class::cast)
                .sorted(Comparator.comparingInt(item -> ((Number) item.get("index")).intValue()))
                .map(item -> toEmbedding(item.get("embedding")))
                .toList();
    }

    private List<Double> toEmbedding(Object embedding) {
        return ((List<?>) embedding).stream()
                .map(Number.class::cast)
                .map(Number::doubleValue)
                .toList();
    }

    @Override
    public String chat(String systemPrompt, String userPrompt) {
        requireAiConfig();
        Map<String, Object> response = restClient.post()
                .uri("/v1/chat/completions")
                .body(Map.of(
                        "model", properties.ai().chatModel(),
                        "temperature", 0.1,
                        "messages", List.of(
                                Map.of("role", "system", "content", systemPrompt),
                                Map.of("role", "user", "content", userPrompt))))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        Object choice = ((List<?>) response.get("choices")).getFirst();
        Object message = ((Map<?, ?>) choice).get("message");
        Object content = ((Map<?, ?>) message).get("content");
        return content == null ? "" : content.toString();
    }

    private void requireAiConfig() {
        if (!properties.ai().configured()) {
            throw new IllegalStateException("AI provider is not configured.");
        }
    }
}
