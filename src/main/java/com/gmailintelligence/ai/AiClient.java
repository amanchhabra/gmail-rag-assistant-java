package com.gmailintelligence.ai;

import java.util.List;

public interface AiClient {
    List<Double> embed(String text);

    List<List<Double>> embedAll(List<String> texts);

    String chat(String systemPrompt, String userPrompt);
}
