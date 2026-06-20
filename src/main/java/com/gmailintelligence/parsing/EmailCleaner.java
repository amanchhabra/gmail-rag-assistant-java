package com.gmailintelligence.parsing;

import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

@Component
public class EmailCleaner {

    public String clean(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String text = looksLikeHtml(raw) ? Jsoup.parse(raw).text() : raw;
        text = removeQuotedReply(text);
        return text.replace('\u00a0', ' ')
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    String removeQuotedReply(String text) {
        String[] markers = {
                "\nOn ",
                "\nFrom:",
                "\nSent:",
                "\n> ",
                "\n-----Original Message-----"
        };
        int cut = text.length();
        for (String marker : markers) {
            int index = text.indexOf(marker);
            if (index > 0 && index < cut) {
                cut = index;
            }
        }
        return text.substring(0, cut);
    }

    private boolean looksLikeHtml(String raw) {
        String lower = raw.toLowerCase();
        return lower.contains("<html") || lower.contains("<body") || lower.contains("<div") || lower.contains("<p");
    }
}
