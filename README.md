# idea-backend

Generic Spring Boot 3 boilerplate. Clone, rename, build.

## Stack

| Layer | Tech |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.3 |
| Auth | Spring Security + JWT (jjwt) |
| DB | PostgreSQL 16 — AWS RDS |
| Cache | Redis-compatible — AWS ElastiCache |
| Migrations | Flyway |
| Build | Maven wrapper (`./mvnw`) |
| Container | Docker + Compose |
| Proxy | Caddy (auto SSL) |
| CI/CD | GitHub Actions → GHCR → VPS |

## Quick start (local)

```bash
# 1. start local dependencies (postgres + plain Redis — no TLS)
docker compose up postgres redis -d

# 2. run API on dev profile (TLS disabled for local Redis)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

```
GET  http://localhost:8080/api/v1/hello       # public — DB ping
POST http://localhost:8080/api/v1/auth/register
POST http://localhost:8080/api/v1/auth/login
GET  http://localhost:8080/api/v1/hello/me    # protected — needs Bearer token

Swagger: http://localhost:8080/api/v1/swagger-ui.html
```

## Or run everything in Docker

```bash
docker compose up --build
```

## ElastiCache setup (production)

1. Create an ElastiCache Serverless cache (or cluster-mode-disabled replication group)
2. Enable **in-transit encryption (TLS)** — required; the app always connects with TLS in prod
3. Optionally enable **AUTH token** — set `REDIS_AUTH_TOKEN` env var if you do
4. Place your VPS in the **same VPC** as ElastiCache, or use a VPC peering / endpoint
5. Set `REDIS_HOST` to the ElastiCache endpoint in your `.env`

## Environment variables

See `.env.example`. Key ones:

| Variable | Default | Notes |
|---|---|---|
| `DB_HOST` | localhost | RDS endpoint in prod |
| `DB_PASSWORD` | change-me | |
| `REDIS_HOST` | localhost | ElastiCache endpoint in prod |
| `REDIS_AUTH_TOKEN` | _(empty)_ | Only if ElastiCache auth is enabled |
| `JWT_SECRET` | — | Min 32 chars |

## Tests

```bash
./mvnw test    # Testcontainers spins up real postgres
```

## Production deploy

```bash
# on VPS
cp .env.example .env && vi .env   # set RDS host, ElastiCache host, JWT secret
docker compose -f infra/docker-compose.prod.yml up -d
```

Edit `infra/Caddyfile` with your real domain — SSL is automatic.

## GitHub Actions secrets needed

| Secret | Description |
|---|---|
| `VPS_HOST` | Server IP or hostname |
| `VPS_USER` | SSH user |
| `VPS_SSH_KEY` | Private SSH key |

## Project layout

```
idea-backend/
├── Dockerfile                           Multi-stage, non-root
├── docker-compose.yml                   Local dev (postgres + redis + api)
├── .env.example
├── .gitignore
├── .github/workflows/ci.yml             Test → build → push → deploy
        
src/main/java/com/idea/
├── config/        SecurityConfig (JWT filter chain, BCrypt bean), RedisConfig (ElastiCache TLS)
├── controller/    HelloController, AuthController (POST /auth/register + /auth/login)
├── dao/           UserRepository (Spring Data JPA)
├── entity/        User + enums
├── exception/     GlobalExceptionHandler
└── security/      JwtService (Generate + validate tokens), JwtAuthFilter (OncePerRequestFilter), UserDetailsServiceImpl (Loads user from DB)

src/main/resources/
├── application.yml          prod defaults — TLS on, REDIS_HOST required
├── application-dev.yml      local overrides — TLS off, localhost redis
└── db/migration/V1__init.sql

infra/
├── Caddyfile      (yourdomain.com → api:8080 + auto SSL)
└── docker-compose.prod.yml  api + caddy only (RDS + ElastiCache are AWS managed)
```
