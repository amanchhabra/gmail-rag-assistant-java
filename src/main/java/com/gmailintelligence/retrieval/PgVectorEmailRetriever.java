package com.gmailintelligence.retrieval;

import com.gmailintelligence.ai.AiClient;
import com.gmailintelligence.persistence.EmailChunkRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
class PgVectorEmailRetriever implements EmailRetriever {
    private final AiClient aiClient;
    private final EmailChunkRepository chunkRepository;

    PgVectorEmailRetriever(AiClient aiClient, EmailChunkRepository chunkRepository) {
        this.aiClient = aiClient;
        this.chunkRepository = chunkRepository;
    }

    @Override
    public List<RetrievedEmailChunk> retrieve(EmailSearchRequest request) {
        List<Double> embedding = aiClient.embed(request.question());
        Map<String, RetrievedEmailChunk> deduped = new LinkedHashMap<>();
        for (EmailChunkRepository.RetrievedChunkRow row : chunkRepository.search(embedding, request.topK(), request.minimumSimilarity())) {
            deduped.putIfAbsent(row.gmailMessageId() + ":" + row.chunkIndex(), new RetrievedEmailChunk(
                    row.gmailMessageId(),
                    row.gmailThreadId(),
                    row.subject(),
                    row.sender(),
                    row.sentAt(),
                    row.content(),
                    row.score(),
                    row.gmailUrl()));
        }
        return List.copyOf(deduped.values());
    }
}
