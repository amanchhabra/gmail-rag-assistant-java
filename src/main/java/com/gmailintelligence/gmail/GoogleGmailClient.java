package com.gmailintelligence.gmail;

import com.gmailintelligence.configuration.AppProperties;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartHeader;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

@Component
class GoogleGmailClient implements GmailClient {
    private static final String USER = "me";
    private static final List<String> SCOPES = List.of(GmailScopes.GMAIL_READONLY);
    private static final int PAGE_SIZE = 25;
    private static final int MAX_TEXT_PART_BYTES = 1_000_000;

    private final AppProperties properties;
    private final GsonFactory jsonFactory = GsonFactory.getDefaultInstance();

    GoogleGmailClient(AppProperties properties) {
        this.properties = properties;
    }

    @Override
    public String authorizationUrl(String state) {
        requireGmailConfig();
        return new GoogleAuthorizationCodeRequestUrl(
                properties.gmail().clientId(),
                properties.gmail().redirectUri(),
                SCOPES)
                .setAccessType("offline")
                .setApprovalPrompt("force")
                .setState(state)
                .build();
    }

    @Override
    public ConnectedGmailAccount exchangeCode(String code) {
        try {
            requireGmailConfig();
            NetHttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
            var tokenResponse = new GoogleAuthorizationCodeTokenRequest(
                    transport,
                    jsonFactory,
                    properties.gmail().clientId(),
                    properties.gmail().clientSecret(),
                    code,
                    properties.gmail().redirectUri())
                    .execute();
            String refreshToken = tokenResponse.getRefreshToken();
            if (refreshToken == null || refreshToken.isBlank()) {
                throw new IllegalStateException("Google did not return a refresh token. Revoke app access and connect again.");
            }
            Gmail gmail = gmail(transport, new GoogleCredential.Builder()
                    .setTransport(transport)
                    .setJsonFactory(jsonFactory)
                    .setClientSecrets(properties.gmail().clientId(), properties.gmail().clientSecret())
                    .build()
                    .setFromTokenResponse(tokenResponse));
            String email = gmail.users().getProfile(USER).execute().getEmailAddress();
            return new ConnectedGmailAccount(email, refreshToken);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to complete Gmail OAuth connection.", ex);
        }
    }

    @Override
    public void forEachMessage(
            String refreshToken,
            String query,
            int maxMessages,
            Consumer<GmailMessageDocument> consumer) {
        try {
            requireGmailConfig();
            NetHttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
            Credential credential = new GoogleCredential.Builder()
                    .setTransport(transport)
                    .setJsonFactory(jsonFactory)
                    .setClientSecrets(properties.gmail().clientId(), properties.gmail().clientSecret())
                    .build()
                    .setRefreshToken(refreshToken);
            Gmail gmail = gmail(transport, credential);
            int processed = 0;
            String pageToken = null;
            while (processed < maxMessages) {
                Gmail.Users.Messages.List request = gmail.users().messages().list(USER)
                        .setMaxResults((long) Math.min(PAGE_SIZE, maxMessages - processed));
                if (query != null && !query.isBlank()) {
                    request.setQ(query);
                }
                if (pageToken != null) {
                    request.setPageToken(pageToken);
                }
                ListMessagesResponse response = request.execute();
                if (response.getMessages() == null || response.getMessages().isEmpty()) {
                    break;
                }
                for (Message listed : response.getMessages()) {
                    Message message = gmail.users().messages().get(USER, listed.getId()).setFormat("full").execute();
                    consumer.accept(toDocument(message));
                    processed++;
                    if (processed >= maxMessages) {
                        break;
                    }
                }
                pageToken = response.getNextPageToken();
                if (pageToken == null) {
                    break;
                }
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to import Gmail messages.", ex);
        }
    }

    private Gmail gmail(NetHttpTransport transport, Credential credential) {
        return new Gmail.Builder(transport, jsonFactory, credential)
                .setApplicationName("Gmail Intelligence Assistant")
                .build();
    }

    private GmailMessageDocument toDocument(Message message) {
        List<MessagePartHeader> headers = message.getPayload().getHeaders();
        String subject = header(headers, "Subject");
        String sender = header(headers, "From");
        String to = header(headers, "To");
        Instant sentAt = message.getInternalDate() == null ? null : Instant.ofEpochMilli(message.getInternalDate());
        String body = extractBody(message.getPayload());
        List<String> labels = message.getLabelIds() == null ? List.of() : message.getLabelIds();
        return new GmailMessageDocument(message.getId(), message.getThreadId(), subject, sender, to, sentAt, labels, body);
    }

    private String header(List<MessagePartHeader> headers, String name) {
        return headers.stream()
                .filter(header -> name.equalsIgnoreCase(header.getName()))
                .map(MessagePartHeader::getValue)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("");
    }

    private String extractBody(MessagePart part) {
        if (part == null) {
            return "";
        }
        String mimeType = part.getMimeType();
        if (("text/plain".equalsIgnoreCase(mimeType) || "text/html".equalsIgnoreCase(mimeType))
                && part.getBody() != null && part.getBody().getData() != null) {
            if (part.getBody().getSize() != null && part.getBody().getSize() > MAX_TEXT_PART_BYTES) {
                return "";
            }
            String encoded = part.getBody().getData();
            int maximumEncodedLength = ((MAX_TEXT_PART_BYTES + 2) / 3) * 4 + 4;
            if (encoded.length() > maximumEncodedLength) {
                return "";
            }
            byte[] bytes = Base64.getUrlDecoder().decode(encoded);
            return new String(bytes, StandardCharsets.UTF_8);
        }
        if (part.getParts() == null) {
            return "";
        }
        String html = "";
        for (MessagePart child : part.getParts()) {
            String value = extractBody(child);
            if ("text/plain".equalsIgnoreCase(child.getMimeType()) && !value.isBlank()) {
                return value;
            }
            if ("text/html".equalsIgnoreCase(child.getMimeType()) && !value.isBlank()) {
                html = value;
            }
        }
        return html;
    }

    private void requireGmailConfig() {
        if (!properties.gmail().configured()) {
            throw new IllegalStateException("Google OAuth is not configured.");
        }
    }
}
