package com.gmailintelligence.ingestion;

import java.util.UUID;

public record ImportJobStatus(
        UUID id,
        State state,
        int requested,
        int processed,
        int imported,
        int skipped,
        int failed,
        String error
) {
    public enum State {
        RUNNING,
        COMPLETED,
        FAILED
    }
}
