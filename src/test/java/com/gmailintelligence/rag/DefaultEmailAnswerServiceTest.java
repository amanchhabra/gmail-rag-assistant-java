package com.gmailintelligence.rag;

import com.gmailintelligence.ai.AiClient;
import com.gmailintelligence.configuration.AppProperties;
import com.gmailintelligence.persistence.RagRepository;
import com.gmailintelligence.retrieval.EmailRetriever;
import com.gmailintelligence.retrieval.EmailSearchRequest;
import com.gmailintelligence.retrieval.RetrievedEmailChunk;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DefaultEmailAnswerServiceTest {

    @Test
    void returnsInsufficientEvidenceWithoutCallingChatModel() {
        EmailRetriever retriever = mock(EmailRetriever.class);
        AiClient aiClient = mock(AiClient.class);
        RagRepository ragRepository = mock(RagRepository.class);
        AppProperties properties = properties();
        when(retriever.retrieve(new EmailSearchRequest("question", 6, 0.1))).thenReturn(List.of());
        DefaultEmailAnswerService service = new DefaultEmailAnswerService(
                retriever,
                aiClient,
                new PromptBuilder(),
                properties,
                ragRepository);

        GroundedAnswer answer = service.answer(new EmailQuestion("question"));

        assertThat(answer.evidenceStatus()).isEqualTo("INSUFFICIENT_EVIDENCE");
        assertThat(answer.sources()).isEmpty();
        verifyNoInteractions(aiClient, ragRepository);
    }

    @Test
    void includesOnlyCitedMessagesAndDeduplicatesTheirChunks() {
        EmailRetriever retriever = mock(EmailRetriever.class);
        AiClient aiClient = mock(AiClient.class);
        RagRepository ragRepository = mock(RagRepository.class);
        List<RetrievedEmailChunk> chunks = List.of(
                chunk("dividend-id", "Dividend paid", "Payment confirmation", 0.455),
                chunk("spacex-id", "SpaceX has listed", "Market newsletter", 0.399),
                chunk("dividend-id", "Dividend paid", "Statement link", 0.410));
        when(retriever.retrieve(new EmailSearchRequest("Was a dividend paid?", 6, 0.1))).thenReturn(chunks);
        when(aiClient.chat(any(), any())).thenReturn("""
                Yes, the distribution was paid on 17 June 2026.

                CITED_MESSAGE_IDS: dividend-id
                """);
        DefaultEmailAnswerService service = new DefaultEmailAnswerService(
                retriever,
                aiClient,
                new PromptBuilder(),
                properties(),
                ragRepository);

        GroundedAnswer answer = service.answer(new EmailQuestion("Was a dividend paid?"));

        assertThat(answer.answer()).isEqualTo("Yes, the distribution was paid on 17 June 2026.");
        assertThat(answer.evidenceStatus()).isEqualTo("GROUNDED");
        assertThat(answer.sources())
                .extracting(SourceReference::gmailMessageId)
                .containsExactly("dividend-id");
        assertThat(answer.sources().getFirst().retrievalScore()).isEqualTo(0.455);
        verify(ragRepository).save("Was a dividend paid?", answer);
    }

    @Test
    void returnsNoSourcesWhenModelSaysEvidenceIsInsufficient() {
        EmailRetriever retriever = mock(EmailRetriever.class);
        AiClient aiClient = mock(AiClient.class);
        RagRepository ragRepository = mock(RagRepository.class);
        when(retriever.retrieve(new EmailSearchRequest("question", 6, 0.1)))
                .thenReturn(List.of(chunk("related-id", "Related only", "Not evidence", 0.4)));
        when(aiClient.chat(any(), any())).thenReturn("""
                The imported emails do not establish that.
                CITED_MESSAGE_IDS: NONE
                """);
        DefaultEmailAnswerService service = new DefaultEmailAnswerService(
                retriever,
                aiClient,
                new PromptBuilder(),
                properties(),
                ragRepository);

        GroundedAnswer answer = service.answer(new EmailQuestion("question"));

        assertThat(answer.evidenceStatus()).isEqualTo("INSUFFICIENT_EVIDENCE");
        assertThat(answer.sources()).isEmpty();
    }

    private RetrievedEmailChunk chunk(String messageId, String subject, String content, double score) {
        return new RetrievedEmailChunk(
                messageId,
                "thread-" + messageId,
                subject,
                "sender@example.com",
                Instant.parse("2026-06-17T01:00:37Z"),
                content,
                score,
                "https://mail.google.com/" + messageId);
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
