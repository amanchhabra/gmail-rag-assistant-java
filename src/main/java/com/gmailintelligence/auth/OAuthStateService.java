package com.gmailintelligence.auth;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;

@Service
public class OAuthStateService {
    static final String SESSION_KEY = "gmail_oauth_state";
    private final SecureRandom secureRandom = new SecureRandom();

    public String createState(HttpSession session) {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String state = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        session.setAttribute(SESSION_KEY, state);
        return state;
    }

    public boolean validateAndConsume(HttpSession session, String state) {
        Object expected = session.getAttribute(SESSION_KEY);
        session.removeAttribute(SESSION_KEY);
        return expected instanceof String value && value.equals(state);
    }
}
