package com.gmailintelligence.retrieval;

import java.time.Instant;

public record RetrievedEmailChunk(
        String gmailMessageId,
        String gmailThreadId,
        String subject,
        String sender,
        Instant sentAt,
        String content,
        double score,
        String gmailUrl
) {
    public String excerpt() {
        String normalized = content.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 320 ? normalized : normalized.substring(0, 320) + "...";
    }
}
