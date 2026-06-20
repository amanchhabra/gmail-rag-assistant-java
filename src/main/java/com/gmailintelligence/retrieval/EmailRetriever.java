package com.gmailintelligence.retrieval;

import java.util.List;

public interface EmailRetriever {
    List<RetrievedEmailChunk> retrieve(EmailSearchRequest request);
}
