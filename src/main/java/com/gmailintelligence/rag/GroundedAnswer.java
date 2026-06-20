package com.gmailintelligence.rag;

import java.util.List;

public record GroundedAnswer(String answer, String evidenceStatus, List<SourceReference> sources) {
}
