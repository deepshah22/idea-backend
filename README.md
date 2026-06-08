# idea-backend

Generic Spring Boot 3 boilerplate. Clone, rename, build.

## Stack

| Layer | Tech |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.3 |
| Build | Gradle 8.8 (Kotlin DSL) |
| Auth | Spring Security + JWT (jjwt) |
| DB | PostgreSQL 16 — AWS RDS |
| Cache | Redis-compatible — AWS ElastiCache |
| Migrations | Flyway |
| Container | Docker + Compose |
| Proxy | Caddy (auto SSL) |
| CI/CD | GitHub Actions → GHCR → VPS |

## Quick start (local)

```bash
# 1. start local dependencies
docker compose up postgres redis -d

# 2. run API on dev profile
./gradlew bootRun --args='--spring.profiles.active=dev'
```

```
GET  http://localhost:8080/api/v1/hello       # public — DB ping
POST http://localhost:8080/api/v1/auth/register
POST http://localhost:8080/api/v1/auth/login
GET  http://localhost:8080/api/v1/hello/me    # protected — needs Bearer token

Swagger: http://localhost:8080/api/v1/swagger-ui.html
```

## Common Gradle tasks

```bash
./gradlew bootRun              # run locally
./gradlew test                 # run tests (needs Docker for Testcontainers)
./gradlew bootJar              # build fat jar → build/libs/app.jar
./gradlew dependencies         # list dependency tree
./gradlew clean build          # full clean build
```

## Or run everything in Docker

```bash
docker compose up --build
```

## ElastiCache setup (production)

1. Create an ElastiCache Serverless cache (or cluster-mode-disabled replication group)
2. Enable **in-transit encryption (TLS)** — required
3. Optionally enable **AUTH token** — set `REDIS_AUTH_TOKEN` env var if you do
4. Place VPS in the **same VPC** as ElastiCache
5. Set `REDIS_HOST` to the ElastiCache endpoint in your `.env`

## Environment variables

| Variable | Required | Default | Description |
|---|---|---|---|
| `DB_HOST` | prod only | `localhost` | RDS PostgreSQL endpoint |
| `DB_PORT` | no | `5432` | PostgreSQL port |
| `DB_NAME` | no | `ideadb` | Database name |
| `DB_USER` | no | `app` | Database username |
| `DB_PASSWORD` | yes | — | Database password |
| `REDIS_HOST` | yes | — | ElastiCache endpoint (TLS required in prod) |
| `REDIS_PORT` | no | `6379` | Redis port |
| `REDIS_AUTH_TOKEN` | no | _(empty)_ | Redis AUTH password — set only if ElastiCache auth is enabled |
| `JWT_SECRET` | yes | — | Min 32 chars — generate: `openssl rand -base64 32` |
| `OPENAI_API_KEY` | yes | — | OpenAI API key for chat and embeddings |

## Tests

```bash
./gradlew test    # Testcontainers spins up real postgres automatically
```

## Production deploy

```bash
# on VPS
cp .env.example .env && vi .env
docker compose -f infra/docker-compose.prod.yml up -d
```

Edit `infra/Caddyfile` with your real domain — SSL is automatic via Let's Encrypt.

## GitHub Actions secrets needed

| Secret | Description |
|---|---|
| `VPS_HOST` | Server IP or hostname |
| `VPS_USER` | SSH user |
| `VPS_SSH_KEY` | Private SSH key |
| `OPENAI_API_KEY` | OpenAI API key — passed to the container at deploy time |

## Project layout

```
idea-backend/
├── Dockerfile                           Multi-stage, non-root
├── docker-compose.yml                   Local dev (postgres + redis + api)
├── .env.example
├── .gitignore
├── .github/workflows/ci.yml             Test → build → push → deploy
├── src/main/java/com/idea/
    ├── config/        SecurityConfig (JWT filter chain, BCrypt bean), RedisConfig (ElastiCache TLS)
    ├── controller/    HelloController, AuthController (POST /auth/register + /auth/login)
    ├── dao/           UserRepository (Spring Data JPA)
    ├── entity/        User + enums
    ├── exception/     GlobalExceptionHandler
    └── security/      JwtService (Generate + validate tokens), JwtAuthFilter (OncePerRequestFilter), UserDetailsServiceImpl (Loads user from DB)
├──src/main/resources/
    ├── application.yml          prod defaults — TLS on, REDIS_HOST required
    ├── application-dev.yml      local overrides — TLS off, localhost redis
    └── db/migration/V1__init.sql

├──infra/
    ├── Caddyfile      (yourdomain.com → api:8080 + auto SSL)
    └── docker-compose.prod.yml  api + caddy only (RDS + ElastiCache are AWS managed)

```
