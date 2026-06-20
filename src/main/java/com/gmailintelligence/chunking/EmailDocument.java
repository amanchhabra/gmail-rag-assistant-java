package com.gmailintelligence.chunking;

import java.time.Instant;
import java.util.UUID;

public record EmailDocument(
        UUID emailId,
        String gmailMessageId,
        String gmailThreadId,
        String subject,
        String sender,
        String recipients,
        Instant sentAt,
        String labels,
        String body
) {
}
