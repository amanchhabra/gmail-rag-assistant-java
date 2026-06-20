package com.gmailintelligence.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class EmailRepository {
    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    public EmailRepository(JdbcTemplate jdbcTemplate, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
    }

    public boolean existsByGmailMessageId(String gmailMessageId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM emails WHERE gmail_message_id = ?",
                Integer.class,
                gmailMessageId);
        return count != null && count > 0;
    }

    public EmailRecord save(EmailRecord email) {
        jdbcTemplate.update("""
                INSERT INTO emails(id, gmail_message_id, gmail_thread_id, subject, sender, recipients, sent_at, labels, cleaned_body, gmail_url, imported_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                email.id(),
                email.gmailMessageId(),
                email.gmailThreadId(),
                email.subject(),
                email.sender(),
                email.recipients(),
                email.sentAt() == null ? null : Timestamp.from(email.sentAt()),
                email.labels(),
                email.cleanedBody(),
                email.gmailUrl(),
                Timestamp.from(clock.instant()));
        return email;
    }

    public Optional<EmailRecord> findById(UUID id) {
        return jdbcTemplate.query("SELECT * FROM emails WHERE id = ?", this::mapEmail, id).stream().findFirst();
    }

    public int countEmails() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM emails", Integer.class);
        return count == null ? 0 : count;
    }

    public void deleteAllImportedData() {
        jdbcTemplate.update("DELETE FROM rag_queries");
        jdbcTemplate.update("DELETE FROM email_chunks");
        jdbcTemplate.update("DELETE FROM emails");
    }

    private EmailRecord mapEmail(ResultSet rs, int rowNum) throws java.sql.SQLException {
        Timestamp sentAt = rs.getTimestamp("sent_at");
        return new EmailRecord(
                UUID.fromString(rs.getString("id")),
                rs.getString("gmail_message_id"),
                rs.getString("gmail_thread_id"),
                rs.getString("subject"),
                rs.getString("sender"),
                rs.getString("recipients"),
                sentAt == null ? null : sentAt.toInstant(),
                rs.getString("labels"),
                rs.getString("cleaned_body"),
                rs.getString("gmail_url"));
    }
}
