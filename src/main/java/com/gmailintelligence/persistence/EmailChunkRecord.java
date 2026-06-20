package com.gmailintelligence.persistence;

import java.time.Instant;
import java.util.UUID;

public record EmailChunkRecord(
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
