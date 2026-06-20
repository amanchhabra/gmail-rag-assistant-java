package com.gmailintelligence.chunking;

import java.util.List;

public interface EmailChunker {
    List<EmailChunk> chunk(EmailDocument document);
}
