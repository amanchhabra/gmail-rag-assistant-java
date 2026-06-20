package com.gmailintelligence.ingestion;

import com.gmailintelligence.ai.AiClient;
import com.gmailintelligence.auth.CryptoService;
import com.gmailintelligence.chunking.EmailChunk;
import com.gmailintelligence.chunking.EmailChunker;
import com.gmailintelligence.gmail.GmailClient;
import com.gmailintelligence.gmail.GmailLinkBuilder;
import com.gmailintelligence.gmail.GmailMessageDocument;
import com.gmailintelligence.parsing.EmailCleaner;
import com.gmailintelligence.persistence.EmailChunkRepository;
import com.gmailintelligence.persistence.EmailRecord;
import com.gmailintelligence.persistence.EmailRepository;
import com.gmailintelligence.persistence.GmailAccountRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GmailImportServiceTest {

    @Test
    void processesMessagesAsTheGmailClientDeliversThem() {
        GmailAccountRepository accountRepository = mock(GmailAccountRepository.class);
        EmailRepository emailRepository = mock(EmailRepository.class);
        EmailChunkRepository chunkRepository = mock(EmailChunkRepository.class);
        GmailClient gmailClient = mock(GmailClient.class);
        CryptoService cryptoService = mock(CryptoService.class);
        EmailChunker chunker = mock(EmailChunker.class);
        AiClient aiClient = mock(AiClient.class);
        UUID accountId = UUID.randomUUID();
        UUID emailId = UUID.randomUUID();
        GmailMessageDocument message = new GmailMessageDocument(
                "message-1", "thread-1", "Subject", "sender@example.com", "to@example.com",
                Instant.parse("2026-06-18T00:00:00Z"), List.of("INBOX"), "Email body");
        EmailRecord saved = new EmailRecord(
                emailId, "message-1", "thread-1", "Subject", "sender@example.com", "to@example.com",
                message.sentAt(), "INBOX", "Email body", "https://mail.google.com/mail/u/0/#all/thread-1");

        when(accountRepository.connectedAccount()).thenReturn(java.util.Optional.of(
                new GmailAccountRepository.ConnectedAccount(accountId, "user@example.com", "encrypted")));
        when(cryptoService.decrypt("encrypted")).thenReturn("refresh-token");
        when(emailRepository.existsByGmailMessageId("message-1")).thenReturn(false);
        when(emailRepository.save(any())).thenReturn(saved);
        when(chunker.chunk(any())).thenReturn(List.of(new EmailChunk(
                UUID.randomUUID(), emailId, "message-1", "thread-1", "Subject", "sender@example.com",
                "to@example.com", message.sentAt(), "INBOX", 0, "Email body")));
        when(aiClient.embedAll(any())).thenReturn(List.of(List.of(0.1, 0.2)));
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Consumer<GmailMessageDocument> consumer = invocation.getArgument(3);
            consumer.accept(message);
            return null;
        }).when(gmailClient).forEachMessage(
                eq("refresh-token"), eq("newer_than:30d"), eq(100), any());

        GmailImportService service = new GmailImportService(
                accountRepository, emailRepository, chunkRepository, gmailClient, cryptoService,
                new EmailCleaner(), chunker, aiClient, new GmailLinkBuilder());

        AtomicReference<ImportProgress> progress = new AtomicReference<>();
        ImportResult result = service.importMessages("newer_than:30d", 100, progress::set);

        assertThat(result).isEqualTo(new ImportResult(1, 0, 0));
        assertThat(progress.get()).isEqualTo(new ImportProgress(100, 1, 1, 0, 0));
        verify(chunkRepository).saveWithEmbedding(any(), any());
    }
}
