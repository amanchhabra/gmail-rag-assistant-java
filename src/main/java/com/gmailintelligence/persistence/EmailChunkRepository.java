package com.gmailintelligence.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Repository
public class EmailChunkRepository {
    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    public EmailChunkRepository(JdbcTemplate jdbcTemplate, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
    }

    public void saveWithEmbedding(EmailChunkRecord chunk, List<Double> embedding) {
        jdbcTemplate.update("""
                INSERT INTO email_chunks(id, email_id, gmail_message_id, gmail_thread_id, subject, sender, recipients, sent_at, labels, chunk_index, content, embedding, embedded_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::vector, ?)
                """,
                chunk.id(),
                chunk.emailId(),
                chunk.gmailMessageId(),
                chunk.gmailThreadId(),
                chunk.subject(),
                chunk.sender(),
                chunk.recipients(),
                chunk.sentAt() == null ? null : Timestamp.from(chunk.sentAt()),
                chunk.labels(),
                chunk.chunkIndex(),
                chunk.content(),
                vectorLiteral(embedding),
                Timestamp.from(clock.instant()));
    }

    public List<RetrievedChunkRow> search(List<Double> embedding, int topK, double minimumSimilarity) {
        return jdbcTemplate.query("""
                        SELECT c.*, e.gmail_url, 1 - (c.embedding <=> ?::vector) AS score
                        FROM email_chunks c
                        JOIN emails e ON e.id = c.email_id
                        WHERE c.embedding IS NOT NULL AND 1 - (c.embedding <=> ?::vector) >= ?
                        ORDER BY c.embedding <=> ?::vector
                        LIMIT ?
                        """,
                this::mapRetrieved,
                vectorLiteral(embedding),
                vectorLiteral(embedding),
                minimumSimilarity,
                vectorLiteral(embedding),
                topK);
    }

    public int countChunks() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM email_chunks", Integer.class);
        return count == null ? 0 : count;
    }

    private RetrievedChunkRow mapRetrieved(ResultSet rs, int rowNum) throws java.sql.SQLException {
        Timestamp sentAt = rs.getTimestamp("sent_at");
        return new RetrievedChunkRow(
                UUID.fromString(rs.getString("id")),
                UUID.fromString(rs.getString("email_id")),
                rs.getString("gmail_message_id"),
                rs.getString("gmail_thread_id"),
                rs.getString("subject"),
                rs.getString("sender"),
                rs.getString("recipients"),
                sentAt == null ? null : sentAt.toInstant(),
                rs.getString("labels"),
                rs.getInt("chunk_index"),
                rs.getString("content"),
                rs.getDouble("score"),
                rs.getString("gmail_url"));
    }

    private String vectorLiteral(List<Double> embedding) {
        return embedding.toString();
    }

    public record RetrievedChunkRow(
            UUID id,
            UUID emailId,
            String gmailMessageId,
            String gmailThreadId,
            String subject,
            String sender,
            String recipients,
            java.time.Instant sentAt,
            String labels,
            int chunkIndex,
            String content,
            double score,
            String gmailUrl
    ) {
    }
}
