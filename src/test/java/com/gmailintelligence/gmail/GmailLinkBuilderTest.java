package com.gmailintelligence.gmail;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GmailLinkBuilderTest {

    @Test
    void buildsThreadUrl() {
        assertThat(new GmailLinkBuilder().threadUrl("thread-123"))
                .isEqualTo("https://mail.google.com/mail/u/0/#all/thread-123");
    }
}
