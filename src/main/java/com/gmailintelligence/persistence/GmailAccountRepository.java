package com.gmailintelligence.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public class GmailAccountRepository {
    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    public GmailAccountRepository(JdbcTemplate jdbcTemplate, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
    }

    public UUID connect(String emailAddress, String encryptedRefreshToken) {
        disconnect();
        UUID accountId = UUID.randomUUID();
        Instant now = clock.instant();
        jdbcTemplate.update("INSERT INTO gmail_accounts(id, email_address, connected_at) VALUES (?, ?, ?)",
                accountId, emailAddress, Timestamp.from(now));
        jdbcTemplate.update("""
                INSERT INTO oauth_tokens(account_id, encrypted_refresh_token, updated_at)
                VALUES (?, ?, ?)
                """, accountId, encryptedRefreshToken, Timestamp.from(now));
        return accountId;
    }

    public Optional<ConnectedAccount> connectedAccount() {
        return jdbcTemplate.query("""
                        SELECT a.id, a.email_address, t.encrypted_refresh_token
                        FROM gmail_accounts a
                        JOIN oauth_tokens t ON t.account_id = a.id
                        WHERE a.disconnected_at IS NULL
                        ORDER BY a.connected_at DESC
                        LIMIT 1
                        """,
                (rs, rowNum) -> new ConnectedAccount(
                        UUID.fromString(rs.getString("id")),
                        rs.getString("email_address"),
                        rs.getString("encrypted_refresh_token")))
                .stream()
                .findFirst();
    }

    public void disconnect() {
        jdbcTemplate.update("UPDATE gmail_accounts SET disconnected_at = ? WHERE disconnected_at IS NULL",
                Timestamp.from(clock.instant()));
    }

    public record ConnectedAccount(UUID id, String emailAddress, String encryptedRefreshToken) {
    }
}
