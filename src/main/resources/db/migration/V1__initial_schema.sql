CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE gmail_accounts (
    id UUID PRIMARY KEY,
    email_address TEXT NOT NULL,
    connected_at TIMESTAMPTZ NOT NULL,
    disconnected_at TIMESTAMPTZ
);

CREATE TABLE oauth_tokens (
    account_id UUID PRIMARY KEY REFERENCES gmail_accounts(id) ON DELETE CASCADE,
    encrypted_refresh_token TEXT NOT NULL,
    access_token TEXT,
    access_token_expires_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE emails (
    id UUID PRIMARY KEY,
    gmail_message_id TEXT NOT NULL UNIQUE,
    gmail_thread_id TEXT NOT NULL,
    subject TEXT NOT NULL,
    sender TEXT NOT NULL,
    recipients TEXT NOT NULL,
    sent_at TIMESTAMPTZ,
    labels TEXT NOT NULL,
    cleaned_body TEXT NOT NULL,
    gmail_url TEXT NOT NULL,
    imported_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_emails_thread ON emails(gmail_thread_id);
CREATE INDEX idx_emails_sent_at ON emails(sent_at);

CREATE TABLE email_chunks (
    id UUID PRIMARY KEY,
    email_id UUID NOT NULL REFERENCES emails(id) ON DELETE CASCADE,
    gmail_message_id TEXT NOT NULL,
    gmail_thread_id TEXT NOT NULL,
    subject TEXT NOT NULL,
    sender TEXT NOT NULL,
    recipients TEXT NOT NULL,
    sent_at TIMESTAMPTZ,
    labels TEXT NOT NULL,
    chunk_index INTEGER NOT NULL,
    content TEXT NOT NULL,
    embedding vector,
    embedded_at TIMESTAMPTZ,
    UNIQUE(email_id, chunk_index)
);

CREATE INDEX idx_email_chunks_thread ON email_chunks(gmail_thread_id);

CREATE TABLE rag_queries (
    id UUID PRIMARY KEY,
    question TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE rag_answers (
    id UUID PRIMARY KEY,
    query_id UUID NOT NULL REFERENCES rag_queries(id) ON DELETE CASCADE,
    answer TEXT NOT NULL,
    evidence_status TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE rag_sources (
    id UUID PRIMARY KEY,
    answer_id UUID NOT NULL REFERENCES rag_answers(id) ON DELETE CASCADE,
    gmail_message_id TEXT NOT NULL,
    gmail_thread_id TEXT NOT NULL,
    subject TEXT NOT NULL,
    sender TEXT NOT NULL,
    sent_at TIMESTAMPTZ,
    excerpt TEXT NOT NULL,
    retrieval_score DOUBLE PRECISION NOT NULL,
    gmail_url TEXT NOT NULL
);
