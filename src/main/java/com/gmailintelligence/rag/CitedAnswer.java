package com.gmailintelligence.rag;

import com.gmailintelligence.retrieval.RetrievedEmailChunk;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

record CitedAnswer(String text, List<String> citedMessageIds) {
    private static final Pattern CITATION_LINE = Pattern.compile(
            "(?im)^\\s*CITED_MESSAGE_IDS\\s*:\\s*(.*?)\\s*$");

    static CitedAnswer parse(String response, List<RetrievedEmailChunk> chunks) {
        String safeResponse = response == null ? "" : response.trim();
        Matcher matcher = CITATION_LINE.matcher(safeResponse);
        Set<String> availableIds = chunks.stream()
                .map(RetrievedEmailChunk::gmailMessageId)
                .collect(java.util.stream.Collectors.toSet());
        LinkedHashSet<String> citedIds = new LinkedHashSet<>();
        boolean citationMetadataPresent = false;

        while (matcher.find()) {
            citationMetadataPresent = true;
            String value = matcher.group(1).trim();
            if (!value.equalsIgnoreCase("NONE")) {
                Arrays.stream(value.split(","))
                        .map(String::trim)
                        .filter(availableIds::contains)
                        .forEach(citedIds::add);
            }
        }

        String answerText = matcher.reset().replaceAll("").trim();
        if (citedIds.isEmpty() && !citationMetadataPresent) {
            chunks.stream()
                    .map(RetrievedEmailChunk::gmailMessageId)
                    .distinct()
                    .filter(safeResponse::contains)
                    .forEach(citedIds::add);
        }
        return new CitedAnswer(answerText, List.copyOf(citedIds));
    }
}
