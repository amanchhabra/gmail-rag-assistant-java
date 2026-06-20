package com.gmailintelligence.rag;

import java.time.Instant;

public record SourceReference(
        String subject,
        String sender,
        Instant sentAt,
        String gmailThreadId,
        String gmailMessageId,
        String excerpt,
        double retrievalScore,
        String gmailUrl
) {
}
