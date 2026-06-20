package com.gmailintelligence.parsing;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmailCleanerTest {

    private final EmailCleaner cleaner = new EmailCleaner();

    @Test
    void convertsHtmlToReadableText() {
        String cleaned = cleaner.clean("<html><body><p>Hello <b>Priyanka</b></p></body></html>");

        assertThat(cleaned).isEqualTo("Hello Priyanka");
    }

    @Test
    void removesCommonQuotedReplyMarkers() {
        String cleaned = cleaner.clean("Can we meet tomorrow?\nOn Monday, someone wrote:\nOld text");

        assertThat(cleaned).isEqualTo("Can we meet tomorrow?");
    }
}
