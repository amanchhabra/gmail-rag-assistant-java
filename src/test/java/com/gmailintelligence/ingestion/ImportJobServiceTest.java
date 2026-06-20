package com.gmailintelligence.ingestion;

import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ImportJobServiceTest {

    @Test
    void exposesCompletedImportStatistics() {
        GmailImportService importService = mock(GmailImportService.class);
        when(importService.importMessages(eq("newer_than:30d"), eq(10), any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Consumer<ImportProgress> listener = invocation.getArgument(2);
            listener.accept(new ImportProgress(10, 1, 1, 0, 0));
            listener.accept(new ImportProgress(10, 2, 1, 1, 0));
            return new ImportResult(1, 1, 0);
        });
        ImportJobService jobService = new ImportJobService(importService, Runnable::run);

        UUID jobId = jobService.start("newer_than:30d", 10);
        ImportJobStatus status = jobService.status(jobId);

        assertThat(status.state()).isEqualTo(ImportJobStatus.State.COMPLETED);
        assertThat(status.processed()).isEqualTo(2);
        assertThat(status.imported()).isEqualTo(1);
        assertThat(status.skipped()).isEqualTo(1);
        assertThat(status.failed()).isZero();
    }
}
