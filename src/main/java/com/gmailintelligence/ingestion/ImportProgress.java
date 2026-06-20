package com.gmailintelligence.ingestion;

public record ImportProgress(int requested, int processed, int imported, int skipped, int failed) {
}
