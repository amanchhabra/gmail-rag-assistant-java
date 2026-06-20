package com.gmailintelligence.gmail;

import org.springframework.stereotype.Component;

@Component
public class GmailLinkBuilder {

    public String threadUrl(String gmailThreadId) {
        return "https://mail.google.com/mail/u/0/#all/" + gmailThreadId;
    }
}
