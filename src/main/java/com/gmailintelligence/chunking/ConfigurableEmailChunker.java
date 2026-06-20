package com.gmailintelligence.chunking;

import com.gmailintelligence.configuration.AppProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
class ConfigurableEmailChunker implements EmailChunker {
    static final int MAX_INDEXED_BODY_CHARACTERS = 100_000;
    static final int MAX_CHUNKS_PER_EMAIL = 64;

    private final AppProperties properties;

    ConfigurableEmailChunker(AppProperties properties) {
        this.properties = properties;
    }

    @Override
    public List<EmailChunk> chunk(EmailDocument document) {
        String metadata = """
                Subject: %s
                Sender: %s
                Recipients: %s
                Sent: %s

                """.formatted(document.subject(), document.sender(), document.recipients(), document.sentAt());
        String body = document.body() == null ? "" : document.body();
        if (body.length() > MAX_INDEXED_BODY_CHARACTERS) {
            body = body.substring(0, MAX_INDEXED_BODY_CHARACTERS);
        }
        int chunkSize = Math.max(300, properties.chunking().chunkSize());
        int overlap = Math.max(0, Math.min(properties.chunking().overlap(), chunkSize / 2));
        if (metadata.length() + body.length() <= chunkSize) {
            return List.of(toChunk(document, 0, metadata + body));
        }
        List<EmailChunk> chunks = new ArrayList<>();
        int start = 0;
        int index = 0;
        while (start < body.length() && index < MAX_CHUNKS_PER_EMAIL) {
            int available = Math.max(100, chunkSize - metadata.length());
            int end = Math.min(body.length(), start + available);
            end = adjustToSentenceBoundary(body, start, end);
            chunks.add(toChunk(document, index++, metadata + body.substring(start, end).trim()));
            if (end >= body.length()) {
                break;
            }
            start = Math.max(0, end - overlap);
        }
        return chunks;
    }

    private int adjustToSentenceBoundary(String body, int start, int proposedEnd) {
        if (proposedEnd >= body.length()) {
            return body.length();
        }
        int sentence = Math.max(body.lastIndexOf(". ", proposedEnd), Math.max(body.lastIndexOf("? ", proposedEnd), body.lastIndexOf("! ", proposedEnd)));
        if (sentence > start + 100) {
            return sentence + 1;
        }
        return proposedEnd;
    }

    private EmailChunk toChunk(EmailDocument document, int index, String content) {
        return new EmailChunk(
                UUID.randomUUID(),
                document.emailId(),
                document.gmailMessageId(),
                document.gmailThreadId(),
                document.subject(),
                document.sender(),
                document.recipients(),
                document.sentAt(),
                document.labels(),
                index,
                content);
    }
}
