package com.gmailintelligence.chunking;

import java.time.Instant;
import java.util.UUID;

public record EmailChunk(
        UUID id,
        UUID emailId,
        String gmailMessageId,
        String gmailThreadId,
        String subject,
        String sender,
        String recipients,
        Instant sentAt,
        String labels,
        int chunkIndex,
        String content
) {
}
