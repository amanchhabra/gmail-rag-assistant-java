package com.gmailintelligence.gmail;

import java.util.function.Consumer;

public interface GmailClient {
    String authorizationUrl(String state);

    ConnectedGmailAccount exchangeCode(String code);

    void forEachMessage(String refreshToken, String query, int maxMessages, Consumer<GmailMessageDocument> consumer);

    record ConnectedGmailAccount(String emailAddress, String refreshToken) {
    }
}
