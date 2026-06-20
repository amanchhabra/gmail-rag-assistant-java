package com.gmailintelligence.gmail;

import java.time.Instant;
import java.util.List;

public record GmailMessageDocument(
        String gmailMessageId,
        String gmailThreadId,
        String subject,
        String sender,
        String recipients,
        Instant sentAt,
        List<String> labels,
        String body
) {
}
