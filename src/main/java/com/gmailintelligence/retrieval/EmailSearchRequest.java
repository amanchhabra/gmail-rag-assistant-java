package com.gmailintelligence.retrieval;

public record EmailSearchRequest(String question, int topK, double minimumSimilarity) {
}
