package com.gmailintelligence.auth;

import com.gmailintelligence.gmail.GmailClient;
import com.gmailintelligence.persistence.GmailAccountRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class GmailConnectionService {
    private final OAuthStateService stateService;
    private final GmailClient gmailClient;
    private final GmailAccountRepository accountRepository;
    private final CryptoService cryptoService;

    public GmailConnectionService(
            OAuthStateService stateService,
            GmailClient gmailClient,
            GmailAccountRepository accountRepository,
            CryptoService cryptoService) {
        this.stateService = stateService;
        this.gmailClient = gmailClient;
        this.accountRepository = accountRepository;
        this.cryptoService = cryptoService;
    }

    public String authorizationUrl(HttpSession session) {
        return gmailClient.authorizationUrl(stateService.createState(session));
    }

    public void completeConnection(HttpSession session, String state, String code) {
        if (!stateService.validateAndConsume(session, state)) {
            throw new IllegalArgumentException("Invalid OAuth state.");
        }
        GmailClient.ConnectedGmailAccount account = gmailClient.exchangeCode(code);
        accountRepository.connect(account.emailAddress(), cryptoService.encrypt(account.refreshToken()));
    }

    public Optional<String> connectedEmail() {
        return accountRepository.connectedAccount().map(GmailAccountRepository.ConnectedAccount::emailAddress);
    }

    public void disconnect() {
        accountRepository.disconnect();
    }
}
