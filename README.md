# Gmail Intelligence Assistant

A local-first Gmail RAG assistant built with Java 21, Spring Boot, Spring AI dependencies, PostgreSQL, pgvector, Flyway, Thymeleaf, and the Gmail API.

The MVP connects one Gmail account with read-only access, imports recent messages, cleans and chunks email text, stores embeddings locally, answers natural-language questions, and cites the Gmail messages used as evidence.

## Current MVP Features

- Google OAuth web flow with Gmail read-only scope only.
- Encrypted local refresh-token storage.
- Manual Gmail import using a Gmail search query and max message count.
- Basic plain-text and HTML email cleaning with quoted-reply trimming.
- Configurable chunk size and overlap.
- OpenAI-compatible chat and embedding calls.
- PostgreSQL + pgvector storage.
- Semantic retrieval with top-K and minimum similarity settings.
- Grounded answers with source citations and Gmail thread links.
- Local imported-data deletion.
- Minimal Thymeleaf UI.

Out of scope for this MVP: sending email, drafting replies, modifying Gmail labels, deleting Gmail messages, attachments, incremental sync, thread summaries, action/deadline/decision extraction, multiple Gmail accounts, and SaaS/multi-user deployment.

## Requirements

- Java 21+
- Docker
- Google Cloud OAuth credentials
- OpenAI-compatible chat and embedding API

## Local Setup

1. Configure environment variables:

   ```bash
   cp .env.example .env
   ```

   Fill in `.env`. The application imports this file automatically for local development.

2. Start PostgreSQL with pgvector:

   ```bash
   docker compose up -d
   ```

3. Run tests:

   ```bash
   ./mvnw test
   ```

4. Start the application:

   ```bash
   ./mvnw spring-boot:run
   ```

5. Open:

   ```text
   http://localhost:8080
   ```

## Google OAuth Setup

Create a Google Cloud project, enable the Gmail API, configure the OAuth consent screen, and create OAuth credentials for a web application.

Use this redirect URI unless you changed the app config:

```text
http://localhost:8080/oauth2/gmail/callback
```

The app requests only:

```text
https://www.googleapis.com/auth/gmail.readonly
```

It must never request send, delete, modify, or label-management scopes.

## Environment Variables

| Variable | Purpose |
| --- | --- |
| `GOOGLE_CLIENT_ID` | Google OAuth web client ID |
| `GOOGLE_CLIENT_SECRET` | Google OAuth web client secret |
| `GOOGLE_REDIRECT_URI` | OAuth callback URI |
| `APP_ENCRYPTION_KEY` | At least 32 characters; encrypts refresh tokens |
| `SPRING_AI_API_KEY` | OpenAI-compatible API key |
| `SPRING_AI_CHAT_MODEL` | Chat model name |
| `SPRING_AI_EMBEDDING_MODEL` | Embedding model name |
| `SPRING_AI_BASE_URL` | OpenAI-compatible base URL |
| `SPRING_AI_EMBEDDING_DIMENSIONS` | Embedding dimension count |
| `DATABASE_URL` | JDBC URL |
| `DATABASE_USERNAME` | Database username |
| `DATABASE_PASSWORD` | Database password |

## How To Get The Environment Variables

### Google OAuth Values

1. Open the [Google Cloud Console](https://console.cloud.google.com/).
2. Create or select a project.
3. Go to **APIs & Services** → **Library**.
4. Enable **Gmail API**.
5. Go to **APIs & Services** → **OAuth consent screen**.
6. Configure the consent screen for local development.
7. Go to **APIs & Services** → **Credentials**.
8. Create **OAuth client ID** credentials.
9. Choose **Web application**.
10. Add this authorized redirect URI:

    ```text
    http://localhost:8080/oauth2/gmail/callback
    ```

11. Copy the generated values:

    ```text
    GOOGLE_CLIENT_ID=<client id>
    GOOGLE_CLIENT_SECRET=<client secret>
    GOOGLE_REDIRECT_URI=http://localhost:8080/oauth2/gmail/callback
    ```

### App Encryption Key

Create a local-only secret with at least 32 characters:

```bash
openssl rand -base64 32
```

Use the generated value as:

```text
APP_ENCRYPTION_KEY=<generated value>
```

Keep this stable between app runs. If it changes, previously encrypted Gmail refresh tokens cannot be decrypted.

### AI Provider Values

For OpenAI, create an API key from the OpenAI dashboard and use:

```text
SPRING_AI_API_KEY=<your api key>
SPRING_AI_BASE_URL=https://api.openai.com
SPRING_AI_CHAT_MODEL=gpt-4.1-mini
SPRING_AI_EMBEDDING_MODEL=text-embedding-3-small
SPRING_AI_EMBEDDING_DIMENSIONS=1536
```

For another OpenAI-compatible provider, use that provider's base URL, model names, API key, and embedding dimensions.

### Database Values

If you use the provided Docker Compose file, use:

```text
DATABASE_URL=jdbc:postgresql://localhost:5433/gmail_intelligence
DATABASE_USERNAME=gmail
DATABASE_PASSWORD=gmail
```

The Docker Compose file maps the database to host port `5433` to avoid colliding with a local PostgreSQL server on `5432`.

If you see `FATAL: role "gmail" does not exist`, the app is probably connecting to another local PostgreSQL instance. Update `.env` to use port `5433`, then recreate the Compose container:

```bash
docker compose down
docker compose up -d
```

## Using The App

1. Go to **Gmail** and connect your account.
2. Go to **Import**.
3. Choose a friendly time range such as **Last 30 days**.

4. Import up to 100 messages for the first run.
5. Go to **Ask** and ask a question.
6. Review the answer and source cards.

Each source includes an **Open in Gmail** link using this format:

```text
https://mail.google.com/mail/u/0/#all/{gmailThreadId}
```

You must be signed into the same Gmail account in the browser. If multiple Google accounts are open, Gmail may route `/u/0/` to a different account.

## Privacy And Security

- All imported email data is stored locally.
- Gmail access is read-only.
- Refresh tokens are encrypted at rest using `APP_ENCRYPTION_KEY`.
- OAuth state is validated and consumed.
- Email content is treated as untrusted data in prompts.
- The app instructs the model to ignore instructions found inside emails.
- Secrets and local data paths are ignored by Git.

## Development Notes

The core interfaces are:

- `EmailChunker#chunk(EmailDocument document)`
- `EmailRetriever#retrieve(EmailSearchRequest request)`
- `EmailAnswerService#answer(EmailQuestion question)`

The current AI adapter calls OpenAI-compatible `/v1/embeddings` and `/v1/chat/completions` endpoints directly while keeping Spring AI dependencies in the project and AI concerns isolated behind `AiClient`.

## Current Limitations

- No incremental sync yet.
- No attachment indexing.
- No thread summary or extraction features yet.
- No advanced metadata filters in the UI yet.
- Basic MIME/body extraction only.
- Basic quoted-reply removal only.
- One local Gmail account at a time.

## Import Troubleshooting

Email import processes one full Gmail message at a time so large imports do not retain every message body in memory. Text MIME parts larger than 1 MB are skipped, cleaned bodies are capped at 100,000 characters, and each email is limited to 64 chunks. Chunk embeddings are submitted in batches to reduce memory usage and import time.

If an import still reports `OutOfMemoryError`, first retry with a smaller maximum message count. The Maven Spring Boot runner is configured with a 1 GB maximum heap. To override it temporarily, use:

```bash
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-Xms256m -Xmx1500m"
```

When running the packaged JAR directly, pass the heap setting to Java:

```bash
java -Xms256m -Xmx1g -jar target/gmail-intelligence-assistant-0.1.0-SNAPSHOT.jar
```
