package com.gmailintelligence.ingestion;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

@Service
public class ImportJobService {
    private final GmailImportService importService;
    private final Executor executor;
    private final Map<UUID, MutableJob> jobs = new ConcurrentHashMap<>();
    private UUID activeJobId;

    public ImportJobService(GmailImportService importService, @Qualifier("importExecutor") Executor executor) {
        this.importService = importService;
        this.executor = executor;
    }

    public synchronized UUID start(String query, int maxMessages) {
        if (activeJobId != null) {
            MutableJob active = jobs.get(activeJobId);
            if (active != null && active.snapshot().state() == ImportJobStatus.State.RUNNING) {
                throw new IllegalStateException("An email import is already running.");
            }
        }
        UUID id = UUID.randomUUID();
        MutableJob job = new MutableJob(id, maxMessages);
        jobs.put(id, job);
        activeJobId = id;
        executor.execute(() -> run(job, query, maxMessages));
        return id;
    }

    public ImportJobStatus status(UUID id) {
        MutableJob job = jobs.get(id);
        if (job == null) {
            throw new IllegalArgumentException("Import job was not found.");
        }
        return job.snapshot();
    }

    private void run(MutableJob job, String query, int maxMessages) {
        try {
            ImportResult result = importService.importMessages(query, maxMessages, job::update);
            job.complete(result);
        } catch (RuntimeException ex) {
            job.fail("The import could not be completed. Check the application logs and try again.");
        }
    }

    private static final class MutableJob {
        private final UUID id;
        private final int requested;
        private ImportJobStatus.State state = ImportJobStatus.State.RUNNING;
        private int processed;
        private int imported;
        private int skipped;
        private int failed;
        private String error;

        private MutableJob(UUID id, int requested) {
            this.id = id;
            this.requested = requested;
        }

        private synchronized void update(ImportProgress progress) {
            processed = progress.processed();
            imported = progress.imported();
            skipped = progress.skipped();
            failed = progress.failed();
        }

        private synchronized void complete(ImportResult result) {
            imported = result.imported();
            skipped = result.skipped();
            failed = result.failed();
            processed = imported + skipped + failed;
            state = ImportJobStatus.State.COMPLETED;
        }

        private synchronized void fail(String message) {
            state = ImportJobStatus.State.FAILED;
            error = message;
        }

        private synchronized ImportJobStatus snapshot() {
            return new ImportJobStatus(id, state, requested, processed, imported, skipped, failed, error);
        }
    }
}
