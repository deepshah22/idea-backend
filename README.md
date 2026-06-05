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

See `.env.example`. Key ones:

| Variable | Default | Notes |
|---|---|---|
| `DB_HOST` | localhost | RDS endpoint in prod |
| `DB_PASSWORD` | change-me | |
| `REDIS_HOST` | localhost | ElastiCache endpoint in prod |
| `REDIS_AUTH_TOKEN` | _(empty)_ | Only if ElastiCache auth is enabled |
| `JWT_SECRET` | — | Min 32 chars — generate: `openssl rand -base64 32` |

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

## Project layout

```
├── build.gradle.kts           Gradle build (Kotlin DSL)
├── settings.gradle.kts        Project name
├── gradlew / gradlew.bat      Gradle wrapper
├── Dockerfile                 Multi-stage build
├── docker-compose.yml         Local dev (postgres + redis + api)
├── infra/
│   ├── Caddyfile
│   └── docker-compose.prod.yml   prod (api + caddy only — RDS + ElastiCache are AWS managed)
└── src/main/java/com/idea/
    ├── config/        SecurityConfig, RedisConfig
    ├── controller/    HelloController, AuthController
    ├── dao/           UserRepository (Spring Data JPA)
    ├── entity/        User + enums
    ├── exception/     GlobalExceptionHandler
    └── security/      JwtService, JwtAuthFilter, UserDetailsServiceImpl
```
