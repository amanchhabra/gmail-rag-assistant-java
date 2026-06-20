package com.gmailintelligence.ingestion;

public record ImportResult(int imported, int skipped, int failed) {
}
