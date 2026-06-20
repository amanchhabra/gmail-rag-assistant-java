package com.gmailintelligence.ingestion;

import com.gmailintelligence.ai.AiClient;
import com.gmailintelligence.auth.CryptoService;
import com.gmailintelligence.chunking.EmailChunker;
import com.gmailintelligence.chunking.EmailDocument;
import com.gmailintelligence.gmail.GmailClient;
import com.gmailintelligence.gmail.GmailLinkBuilder;
import com.gmailintelligence.gmail.GmailMessageDocument;
import com.gmailintelligence.parsing.EmailCleaner;
import com.gmailintelligence.persistence.EmailChunkRecord;
import com.gmailintelligence.persistence.EmailChunkRepository;
import com.gmailintelligence.persistence.EmailRecord;
import com.gmailintelligence.persistence.EmailRepository;
import com.gmailintelligence.persistence.GmailAccountRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.List;
import java.util.function.Consumer;

@Service
public class GmailImportService {
    private final GmailAccountRepository accountRepository;
    private final EmailRepository emailRepository;
    private final EmailChunkRepository chunkRepository;
    private final GmailClient gmailClient;
    private final CryptoService cryptoService;
    private final EmailCleaner cleaner;
    private final EmailChunker chunker;
    private final AiClient aiClient;
    private final GmailLinkBuilder linkBuilder;

    public GmailImportService(
            GmailAccountRepository accountRepository,
            EmailRepository emailRepository,
            EmailChunkRepository chunkRepository,
            GmailClient gmailClient,
            CryptoService cryptoService,
            EmailCleaner cleaner,
            EmailChunker chunker,
            AiClient aiClient,
            GmailLinkBuilder linkBuilder) {
        this.accountRepository = accountRepository;
        this.emailRepository = emailRepository;
        this.chunkRepository = chunkRepository;
        this.gmailClient = gmailClient;
        this.cryptoService = cryptoService;
        this.cleaner = cleaner;
        this.chunker = chunker;
        this.aiClient = aiClient;
        this.linkBuilder = linkBuilder;
    }

    public ImportResult importMessages(String query, int maxMessages) {
        return importMessages(query, maxMessages, progress -> {
        });
    }

    public ImportResult importMessages(
            String query,
            int maxMessages,
            Consumer<ImportProgress> progressListener) {
        var account = accountRepository.connectedAccount()
                .orElseThrow(() -> new IllegalStateException("Connect Gmail before importing messages."));
        String refreshToken = cryptoService.decrypt(account.encryptedRefreshToken());
        ImportCounters counters = new ImportCounters();
        gmailClient.forEachMessage(refreshToken, query, maxMessages, message -> {
            processMessage(message, counters);
            progressListener.accept(counters.snapshot(maxMessages));
        });
        return new ImportResult(counters.imported, counters.skipped, counters.failed);
    }

    private void processMessage(GmailMessageDocument message, ImportCounters counters) {
        try {
            if (emailRepository.existsByGmailMessageId(message.gmailMessageId())) {
                counters.skipped++;
                return;
            }
            String cleaned = cleaner.clean(message.body());
            if (cleaned.isBlank()) {
                counters.skipped++;
                return;
            }
            UUID emailId = UUID.randomUUID();
            String labels = String.join(",", message.labels());
            EmailRecord saved = emailRepository.save(new EmailRecord(
                    emailId,
                    message.gmailMessageId(),
                    message.gmailThreadId(),
                    safe(message.subject(), "(no subject)"),
                    safe(message.sender(), "(unknown sender)"),
                    safe(message.recipients(), ""),
                    message.sentAt(),
                    labels,
                    cleaned,
                    linkBuilder.threadUrl(message.gmailThreadId())));
            EmailDocument document = new EmailDocument(
                    saved.id(),
                    saved.gmailMessageId(),
                    saved.gmailThreadId(),
                    saved.subject(),
                    saved.sender(),
                    saved.recipients(),
                    saved.sentAt(),
                    saved.labels(),
                    saved.cleanedBody());
            var chunks = chunker.chunk(document);
            List<List<Double>> embeddings = aiClient.embedAll(chunks.stream()
                    .map(chunk -> chunk.content())
                    .toList());
            if (embeddings.size() != chunks.size()) {
                throw new IllegalStateException("Embedding provider returned an unexpected result count.");
            }
            for (int index = 0; index < chunks.size(); index++) {
                var chunk = chunks.get(index);
                chunkRepository.saveWithEmbedding(new EmailChunkRecord(
                        chunk.id(),
                        chunk.emailId(),
                        chunk.gmailMessageId(),
                        chunk.gmailThreadId(),
                        chunk.subject(),
                        chunk.sender(),
                        chunk.recipients(),
                        chunk.sentAt(),
                        chunk.labels(),
                        chunk.chunkIndex(),
                        chunk.content()), embeddings.get(index));
            }
            counters.imported++;
        } catch (RuntimeException ex) {
            counters.failed++;
        }
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static final class ImportCounters {
        private int processed;
        private int imported;
        private int skipped;
        private int failed;

        private ImportProgress snapshot(int requested) {
            processed++;
            return new ImportProgress(requested, processed, imported, skipped, failed);
        }
    }
}
