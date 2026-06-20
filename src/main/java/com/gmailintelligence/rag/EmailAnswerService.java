package com.gmailintelligence.rag;

public interface EmailAnswerService {
    GroundedAnswer answer(EmailQuestion question);
}
