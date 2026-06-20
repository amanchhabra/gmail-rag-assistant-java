package com.gmailintelligence.rag;

import com.gmailintelligence.ai.AiClient;
import com.gmailintelligence.configuration.AppProperties;
import com.gmailintelligence.persistence.RagRepository;
import com.gmailintelligence.retrieval.EmailRetriever;
import com.gmailintelligence.retrieval.EmailSearchRequest;
import com.gmailintelligence.retrieval.RetrievedEmailChunk;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
class DefaultEmailAnswerService implements EmailAnswerService {
    private final EmailRetriever retriever;
    private final AiClient aiClient;
    private final PromptBuilder promptBuilder;
    private final AppProperties properties;
    private final RagRepository ragRepository;

    DefaultEmailAnswerService(
            EmailRetriever retriever,
            AiClient aiClient,
            PromptBuilder promptBuilder,
            AppProperties properties,
            RagRepository ragRepository) {
        this.retriever = retriever;
        this.aiClient = aiClient;
        this.promptBuilder = promptBuilder;
        this.properties = properties;
        this.ragRepository = ragRepository;
    }

    @Override
    public GroundedAnswer answer(EmailQuestion question) {
        List<RetrievedEmailChunk> chunks = retriever.retrieve(new EmailSearchRequest(
                question.question(),
                properties.retrieval().topK(),
                properties.retrieval().minimumSimilarity()));
        if (chunks.isEmpty()) {
            return new GroundedAnswer("I do not have enough imported email evidence to answer that.", "INSUFFICIENT_EVIDENCE", List.of());
        }
        String response = aiClient.chat(promptBuilder.systemPrompt(), promptBuilder.userPrompt(question.question(), chunks));
        CitedAnswer citedAnswer = CitedAnswer.parse(response, chunks);
        List<SourceReference> sources = citedSources(citedAnswer.citedMessageIds(), chunks);
        String evidenceStatus = sources.isEmpty() ? "INSUFFICIENT_EVIDENCE" : "GROUNDED";
        GroundedAnswer groundedAnswer = new GroundedAnswer(citedAnswer.text(), evidenceStatus, sources);
        ragRepository.save(question.question(), groundedAnswer);
        return groundedAnswer;
    }

    private List<SourceReference> citedSources(
            List<String> citedMessageIds,
            List<RetrievedEmailChunk> chunks) {
        Map<String, RetrievedEmailChunk> bestChunkByMessage = new LinkedHashMap<>();
        chunks.stream()
                .sorted(Comparator.comparingDouble(RetrievedEmailChunk::score).reversed())
                .forEach(chunk -> bestChunkByMessage.putIfAbsent(chunk.gmailMessageId(), chunk));

        return citedMessageIds.stream()
                .map(bestChunkByMessage::get)
                .filter(java.util.Objects::nonNull)
                .map(chunk -> new SourceReference(
                        chunk.subject(),
                        chunk.sender(),
                        chunk.sentAt(),
                        chunk.gmailThreadId(),
                        chunk.gmailMessageId(),
                        chunk.excerpt(),
                        chunk.score(),
                        chunk.gmailUrl()))
                .toList();
    }
}
