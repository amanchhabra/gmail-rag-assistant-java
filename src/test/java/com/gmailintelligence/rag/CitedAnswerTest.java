package com.gmailintelligence.rag;

import com.gmailintelligence.retrieval.RetrievedEmailChunk;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CitedAnswerTest {

    @Test
    void parsesKnownCitationsAndRemovesMetadataFromAnswer() {
        CitedAnswer answer = CitedAnswer.parse("""
                The dividend was paid.
                CITED_MESSAGE_IDS: dividend-id,unknown-id
                """, List.of(chunk("dividend-id")));

        assertThat(answer.text()).isEqualTo("The dividend was paid.");
        assertThat(answer.citedMessageIds()).containsExactly("dividend-id");
    }

    @Test
    void explicitNoneDoesNotFallBackToIdsMentionedInAnswer() {
        CitedAnswer answer = CitedAnswer.parse("""
                Message dividend-id is related, but it does not answer the question.
                CITED_MESSAGE_IDS : NONE
                """, List.of(chunk("dividend-id")));

        assertThat(answer.citedMessageIds()).isEmpty();
    }

    private RetrievedEmailChunk chunk(String messageId) {
        return new RetrievedEmailChunk(
                messageId,
                "thread-" + messageId,
                "Subject",
                "sender@example.com",
                Instant.parse("2026-06-17T01:00:37Z"),
                "Content",
                0.45,
                "https://mail.google.com/" + messageId);
    }
}
