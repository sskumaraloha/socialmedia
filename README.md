# Social Media Messaging Platform

A Java 21 / Spring Boot 3 multi-module microservices platform, built with Gradle. See
[`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for the full system design; this document
covers running and developing the codebase itself.

## Modules

| Module | Port | Datastore(s) | Purpose |
|---|---|---|---|
| `common-library` | — | — | Shared exception handling, correlation-id logging, OpenAPI & Actuator/Micrometer config, auto-wired into every service |
| `gateway-service` | 8080 | Redis | Spring Cloud Gateway edge — routing, rate limiting, circuit breaking |
| `auth-service` | 8081 | PostgreSQL, Redis | Registration/login, JWT issuance, sessions |
| `user-service` | 8082 | PostgreSQL | Profiles, contacts, privacy settings |
| `chat-service` | 8083 | PostgreSQL | Conversation/group/channel metadata & membership |
| `message-service` | 8084 | MongoDB | Message persistence, delivery, read receipts, WebSocket |
| `media-service` | 8085 | PostgreSQL, S3 (MinIO) | Upload orchestration, media metadata |
| `notification-service` | 8086 | PostgreSQL | Push notifications (FCM) |
| `presence-service` | 8087 | Redis | Online/offline, last-seen, typing indicators |
| `search-service` | 8088 | OpenSearch | Full-text search over messages/users/channels |
| `ai-service` | 8089 | Redis (cache) | Smart replies, summarization, moderation |
| `payment-service` | 8090 | PostgreSQL | Premium subscriptions, Stripe integration |
| `analytics-service` | 8091 | MongoDB | Usage analytics, audit trail |

## Prerequisites

- JDK 21
- Docker + Docker Compose v2
- Nothing else — the Gradle wrapper (`./gradlew`) downloads Gradle itself

## Quick start

```bash
cp .env.example .env        # adjust secrets as needed — defaults work for local dev
./gradlew build             # compiles + packages all 13 services and common-library
docker compose up --build   # builds each service's Docker image and starts the full stack
```

Once up:

| Tool | URL |
|---|---|
| API Gateway | http://localhost:8080 |
| Swagger UI (per service) | `http://localhost:<port>/swagger-ui.html` |
| Prometheus | http://localhost:9090 |
| Grafana (admin/admin by default) | http://localhost:3000 |
| Zipkin (distributed tracing) | http://localhost:9411 |
| MinIO console | http://localhost:9001 |
| Kafka broker | localhost:9092 |

To run a single service against the shared infra without rebuilding everything:

```bash
docker compose up -d postgres mongodb redis kafka opensearch minio zipkin
./gradlew :user-service:bootRun
```

## Project structure

```
.
├── build.gradle.kts, settings.gradle.kts, gradle.properties, gradle/
├── buildSrc/                     # Gradle convention plugins shared by every module
│   └── src/main/kotlin/
│       ├── socialmedia.java-conventions.gradle.kts          # Java 21 toolchain, Lombok, Spring Boot BOM
│       └── socialmedia.spring-service-conventions.gradle.kts # + actuator, micrometer, common-library
├── common-library/               # Cross-cutting concerns, see below
├── <13 service modules>/
│   ├── build.gradle.kts
│   ├── Dockerfile                # multi-stage: gradle build -> JRE-alpine runtime
│   └── src/main/
│       ├── java/com/socialmedia/<service>/<Service>ServiceApplication.java
│       └── resources/
│           ├── application.yml         # local/default profile (localhost datastores)
│           └── application-docker.yml  # docker profile (compose service-name hosts)
├── docker/
│   ├── postgres/init-multi-db.sh
│   ├── prometheus/prometheus.yml
│   └── grafana/provisioning/…, dashboards/…
├── docker-compose.yml
├── .env.example
└── docs/ARCHITECTURE.md
```

### What every service gets for free from `common-library`

Depending on `common-library` (already wired into `socialmedia.spring-service-conventions`)
auto-configures, with zero per-service setup:

- **Exception handling** — throw `BusinessException` / `ResourceNotFoundException` /
  `ConflictException` / `UnauthorizedException` from `com.socialmedia.common.exception`;
  a `GlobalExceptionHandler` renders them (plus validation errors) as a consistent `ApiError`
  JSON body.
- **Correlation IDs** — every request gets an `X-Correlation-Id` (generated if absent),
  propagated into the log MDC as `traceId` and into every `ApiError` response.
- **Structured logging** — `logback-spring.xml` emits JSON (via `logstash-logback-encoder`)
  in every profile except `local`/`dev`/`test`, which get a human-readable console pattern.
- **OpenAPI/Swagger** — a base `OpenAPI` bean (bearer-JWT security scheme) is pre-registered;
  each service just needs its own `springdoc-openapi-starter-webmvc-ui` (or `-webflux-ui` for
  the reactive gateway), already declared in its `build.gradle.kts`.
- **Actuator + Micrometer + Prometheus** — `/actuator/prometheus` is exposed and every metric
  is tagged with `application=<service-name>` automatically.

### Adding a new module

1. Add it to `settings.gradle.kts`.
2. Create `<module>/build.gradle.kts` applying `id("socialmedia.spring-service-conventions")`
   (or `socialmedia.java-conventions` + `java-library` if it's a library, not a deployable).
3. Add `<module>/Dockerfile` (copy an existing one, swap the module name/port) and a service
   block to `docker-compose.yml`.
4. Add its scrape target to `docker/prometheus/prometheus.yml`.

## Observability stack

- **Metrics**: every service → Micrometer → Prometheus (`docker/prometheus/prometheus.yml`) →
  Grafana, pre-provisioned with a Prometheus datasource and a "Services Overview" dashboard
  (request rate, p95 latency, JVM heap, Kafka consumer lag, CPU) under the **Platform** folder.
- **Tracing**: Micrometer Tracing + Brave, exported to Zipkin (`management.zipkin.tracing.endpoint`
  in each `application-docker.yml`).
- **Logs**: structured JSON to stdout, correlated via `traceId` — ship them to your log
  aggregator of choice (Loki/ELK) via the container runtime; no code changes needed.

## Service implementation status

| Service | Status |
|---|---|
| `auth-service` | **Fully implemented** — see below |
| everything else | Scaffold only (build config, health/metrics wiring) — domain logic is the next phase, built service by service |

### `auth-service`

JWT access tokens + rotating opaque refresh tokens (with reuse detection — a replayed,
already-rotated refresh token revokes every session for that account), OAuth2 login
(Google/GitHub) with account linking, an alternative Keycloak resource-server profile
(`SPRING_PROFILES_ACTIVE=keycloak`), TOTP-based 2FA with QR enrollment, QR-code login
(scan-to-authenticate from an already-logged-in device), email verification and
forgot/reset password (token-generation + Kafka event only — actual sending is
notification-service's job once built), device and session management, login history,
audit logs, and a Redis-backed rate limiter on the sensitive endpoints. Clean-architecture
layering (`domain` / `dto` / `repository` / `service` / `controller` / `security` /
`event` / `config`), Flyway-managed schema, unit tests (`./gradlew :auth-service:test`,
no external services required) and Testcontainers integration tests
(`./gradlew :auth-service:integrationTest`, requires a Docker daemon).

This was built and then verified against a real, locally running instance (native
Postgres/Redis, not just unit tests) — that process caught and fixed several bugs unit
tests alone missed: a null `AuthProvider` on registration, `KafkaProducer.send()` blocking
the request thread for up to 60s when the broker is unreachable, missing
`@EnableJpaAuditing` (so `createdAt`/`updatedAt` were silently null), a Jackson
polymorphic-typing mismatch in the Redis cache layer, and a 2FA challenge token that never
actually reached the client because it was being routed through a generic exception
handler that didn't know about it.
