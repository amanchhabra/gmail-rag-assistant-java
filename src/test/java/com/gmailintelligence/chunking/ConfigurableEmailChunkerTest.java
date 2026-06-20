package com.gmailintelligence.chunking;

import com.gmailintelligence.configuration.AppProperties;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigurableEmailChunkerTest {

    @Test
    void chunksLongEmailsWithMetadata() {
        ConfigurableEmailChunker chunker = new ConfigurableEmailChunker(properties());
        EmailDocument document = new EmailDocument(
                UUID.randomUUID(),
                "msg-1",
                "thread-1",
                "Subject",
                "sender@example.com",
                "to@example.com",
                Instant.parse("2026-01-01T00:00:00Z"),
                "INBOX",
                "Sentence one. ".repeat(80));

        var chunks = chunker.chunk(document);

        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks.getFirst().content()).contains("Subject: Subject", "Sender: sender@example.com");
    }

    @Test
    void capsChunksForExceptionallyLargeEmails() {
        ConfigurableEmailChunker chunker = new ConfigurableEmailChunker(properties());
        EmailDocument document = new EmailDocument(
                UUID.randomUUID(), "msg-1", "thread-1", "Subject", "sender@example.com", "to@example.com",
                Instant.parse("2026-01-01T00:00:00Z"), "INBOX", "x".repeat(2_000_000));

        var chunks = chunker.chunk(document);

        assertThat(chunks).hasSize(ConfigurableEmailChunker.MAX_CHUNKS_PER_EMAIL);
        assertThat(chunks).allSatisfy(chunk -> assertThat(chunk.content().length()).isLessThanOrEqualTo(400));
    }

    private AppProperties properties() {
        return new AppProperties(
                "test-encryption-key-with-32-chars",
                new AppProperties.Gmail("client", "secret", "http://localhost/callback"),
                new AppProperties.Ai("key", "http://localhost", "chat", "embedding", 4),
                new AppProperties.Retrieval(6, 0.1),
                new AppProperties.Chunking(300, 40));
    }
}
