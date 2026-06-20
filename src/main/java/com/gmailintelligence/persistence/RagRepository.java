package com.gmailintelligence.persistence;

import com.gmailintelligence.rag.GroundedAnswer;
import com.gmailintelligence.rag.SourceReference;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.UUID;

@Repository
public class RagRepository {
    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    public RagRepository(JdbcTemplate jdbcTemplate, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
    }

    public void save(String question, GroundedAnswer answer) {
        UUID queryId = UUID.randomUUID();
        UUID answerId = UUID.randomUUID();
        var now = Timestamp.from(clock.instant());
        jdbcTemplate.update("INSERT INTO rag_queries(id, question, created_at) VALUES (?, ?, ?)", queryId, question, now);
        jdbcTemplate.update("""
                INSERT INTO rag_answers(id, query_id, answer, evidence_status, created_at)
                VALUES (?, ?, ?, ?, ?)
                """, answerId, queryId, answer.answer(), answer.evidenceStatus(), now);
        for (SourceReference source : answer.sources()) {
            jdbcTemplate.update("""
                    INSERT INTO rag_sources(id, answer_id, gmail_message_id, gmail_thread_id, subject, sender, sent_at, excerpt, retrieval_score, gmail_url)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    UUID.randomUUID(),
                    answerId,
                    source.gmailMessageId(),
                    source.gmailThreadId(),
                    source.subject(),
                    source.sender(),
                    source.sentAt() == null ? null : Timestamp.from(source.sentAt()),
                    source.excerpt(),
                    source.retrievalScore(),
                    source.gmailUrl());
        }
    }
}
