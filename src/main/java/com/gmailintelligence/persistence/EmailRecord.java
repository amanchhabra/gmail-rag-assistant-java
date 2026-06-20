package com.gmailintelligence.persistence;

import java.time.Instant;
import java.util.UUID;

public record EmailRecord(
        UUID id,
        String gmailMessageId,
        String gmailThreadId,
        String subject,
        String sender,
        String recipients,
        Instant sentAt,
        String labels,
        String cleanedBody,
        String gmailUrl
) {
}
