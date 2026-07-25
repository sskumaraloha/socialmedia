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
| `user-service` | **Fully implemented** — see below |
| `chat-service` | **Fully implemented** — see below |
| `message-service` | **Fully implemented** — see below |
| `presence-service` | **Fully implemented** — see below |
| `media-service` | **Fully implemented** — see below |
| `notification-service` | **Fully implemented** — see below |
| `search-service` | **Fully implemented** — see below |
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

### `user-service`

Profile management (display name, bio, avatar, custom status), per-field privacy controls
(who can see online status / last-seen, who can message you, who can add you to groups —
`EVERYONE` / `CONTACTS` / `NOBODY`), username search and typeahead, follow/unfollow with
follower/following counts, contacts, block (which severs any existing follow relationship
in both directions) and mute. Consumes `auth-service`'s `user.registered` Kafka event to
provision a profile (auto-generated, de-duplicated username) the moment an account is
created, and consumes presence-service's presence-changed event to track last-known-online.
Same clean-architecture layering as `auth-service`, Flyway-managed schema, unit tests
(`./gradlew :user-service:test`) and a Testcontainers integration test
(`./gradlew :user-service:integrationTest`) that signs its own JWT with the shared secret
to simulate a token issued by `auth-service`, so the two services can be tested
independently of each other.

Building this alongside `auth-service` motivated several platform-wide additions to
`common-library` so every remaining service can reuse them instead of re-implementing:
a `TimestampedEntity` (createdAt/updatedAt only) split out from `BaseEntity` for entities
whose ID must be assigned rather than generated (a user profile's ID is the auth-service
user ID, not a fresh UUID), a `JpaAuditingAutoConfiguration` so `@EnableJpaAuditing` doesn't
need repeating in every service, and a shared JWT resource-server stack (`JwtValidator`,
`JwtAuthenticationFilter`, `AuthenticatedPrincipal`, `CurrentUser`, the REST entry
point/access-denied handler) so any downstream service can validate an `auth-service`-issued
token with a two-line `SecurityConfig` and no OAuth2/login machinery of its own.

Real end-to-end testing (native Postgres/Redis, and a natively-run Apache Kafka 3.8.0
broker in KRaft mode — no Docker daemon available in this environment) surfaced one
platform-wide bug that mocked unit tests could never have caught: `spring.kafka.producer
.value-serializer` was never actually configured anywhere, so Spring Kafka silently fell
back to `StringSerializer`. Every earlier test had run against an unreachable broker, where
the `max.block.ms` timeout fired before serialization was ever attempted — making it look
like a connectivity issue. Only running a real broker exposed the real
`ClassCastException`-style serializer error. Fixed by adding explicit
`key-serializer: StringSerializer` / `value-serializer: JsonSerializer` to every service's
producer config, then re-verifying the full `auth-service → Kafka → user-service` profile-
creation flow end-to-end.

### `chat-service`

Private chats, group chats and broadcast channels, with owner/admin/member roles and
permission checks (only admins rename a group, add/remove members, or pin messages;
private chats have no admin distinction — either participant can pin/delete). Per-member
mute (indefinite or until a timestamp), archive, unread counts, a chat-list search by
name, and pinned messages (referenced by an opaque message ID — chat-service never reads
message content, that's message-service's job once built). REST APIs plus a STOMP-over-
WebSocket endpoint (`/ws/chat`, SockJS-fallback) for real-time chat-metadata notifications
(member added/removed, chat renamed, new message, pinned message) — the STOMP `CONNECT`
frame carries the same bearer JWT as a native header (`Authorization: Bearer <token>`)
since browsers can't set arbitrary headers on the WebSocket upgrade itself, and a
`ChannelInterceptor` validates it before the session is admitted. This is a single-node
in-memory broker; fanning delivery out across horizontally-scaled chat-service instances
(a client connected to node A must see an event produced on node B) is the platform-wide
WebSocket-scaling architecture — task on the backlog, likely landing alongside
message-service.

Consumes a `message.sent.v1` event to keep the chat list's last-message preview and
per-member unread counts live — message-service doesn't exist yet, so this is a wire
contract agreed up front (the same forward-dependency pattern user-service used for
presence-service's not-yet-built presence-changed event). Verified against a real,
natively-run Postgres/Kafka instance: booted the service, drove the full private-chat →
group-chat → permission-denied → owner-override → mute/archive → pin/unpin → leave →
delete lifecycle over HTTP with locally-signed JWTs, then hand-produced a `message.sent.v1`
Kafka message and confirmed the unread counter incremented for every member except the
sender and the chat-list preview updated live.

### `message-service`

The only MongoDB-backed service in the platform (per the architecture doc's two-data-
plane split: Postgres for anything relational/identity-shaped, MongoDB for high-volume,
schema-flexible, append-mostly message documents). All six content types (text, image,
video, audio, document, GIF), reply, forward (carries the original `forwardedFromMessageId`
through even across a second forward, so it always points at the true original), edit,
delete-for-me vs delete-for-everyone, scheduled messages, self-destructing messages,
reactions (one active emoji per user, upsert-on-change), delivery/read receipts, starred
messages (personal bookmarks, separate from chat-service's chat-wide pinned messages),
per-chat-per-user drafts, and a typing indicator over WebSocket. Reactions and receipts
are embedded directly in the message document rather than modeled as separate
collections/tables - a natural fit for MongoDB that a relational schema wouldn't allow as
cleanly.

Message content is encrypted at rest with AES-256-GCM (`MessageEncryptionService`) - this
is application-level encryption, not end-to-end: the server holds the key and can decrypt,
since there's no client-side key-exchange infrastructure in this backend-only project. It
protects content if the MongoDB data files or backups are exfiltrated without the
application's secret store.

Every send/list/get/forward is gated by a **synchronous** authoritative check against
chat-service (`ChatMembershipClient`, using Spring's `RestClient`, forwarding the caller's
own bearer token to chat-service's existing `GET /chats/{chatId}` - reusing its
membership/permission logic instead of duplicating it, and getting the full member list
back in the same call for immediate WebSocket fan-out). Higher-frequency, lower-risk
actions (reactions, receipts) are instead gated against a local, eventually-consistent
`ChatMembership` mirror kept current by consuming chat-service's `chat.created.v1` /
`chat.member.added.v1` / `chat.member.removed.v1` / `chat.deleted.v1` events - the same
forward-declared-contract pattern used throughout this platform, avoiding a synchronous
call on every reaction or read receipt. That same mirror also drives the typing-indicator
WebSocket fan-out (`/app/chats/{chatId}/typing` → `/user/queue/message-events`), reusing
the STOMP-over-JWT infrastructure that was promoted from chat-service into
`common-library` (`JwtStompChannelInterceptor`, `StompUserPrincipal`) the moment a second
service needed it.

Publishes `message.sent.v1` matching the wire contract chat-service already declared it
consumes (see chat-service's section above) - scheduled messages publish it only once
they become due, via a lightweight `@Scheduled` poller (`MessageMaintenanceScheduler`),
which also purges self-destructing messages past their expiry.

Unlike the previous services, this one could not be verified against a real running
instance in this sandbox: PostgreSQL/Redis/Kafka were reachable as native installs, but
MongoDB was not (no package available, and downloading a binary was blocked by this
environment's egress policy). Verification here is unit tests only (13, covering
encryption round-tripping through the mock boundary, the membership-check failure path,
scheduled/self-destruct timing, reaction upsert, and delete-for-me vs delete-for-everyone)
plus a Testcontainers integration test that compiles but - like every `*IT` test in this
repo - needs a Docker daemon this sandbox doesn't have.

### `presence-service` and the scalable-WebSocket architecture

**presence-service**: tracks online/offline and last-seen entirely in Redis - a sorted set
(`presence:heartbeats`, member = userId, score = last-heartbeat epoch-millis) rather than
per-user TTL keys with Redis keyspace notifications, so it works against any Redis
deployment including managed ones that disallow `CONFIG SET` for keyspace events. Clients
call `POST /api/v1/presence/heartbeat` periodically; a small `@Scheduled` sweep
(`PresenceSweepScheduler`) marks anyone whose last heartbeat fell outside the timeout as
offline and publishes `presence.changed.v1` - the exact contract user-service's
`PresenceChangedConsumer` was already built against (see the user-service section above),
so that consumer, which had been idling harmlessly against a topic with no producer, now
actually receives events. Verified end-to-end against native Redis/Kafka: heartbeat marks
a user online, waiting past the (test-shortened) timeout has the sweep mark them offline
automatically with `lastSeenAt` populated, and both the online and offline events were
confirmed on the wire and consumed cleanly by a running user-service instance with no
deserialization errors.

**Scalable WebSocket architecture**: chat-service and message-service each run their own
single-node, in-memory STOMP broker (`enableSimpleBroker`), which has a hard limit -
`SimpMessagingTemplate.convertAndSendToUser()` only reaches a session connected to the
exact node that called it. A message produced by a REST call handled on node A would
never reach a client whose WebSocket happens to be connected to node B. Fixed with a
`RedisWebSocketRelay` / `RedisWebSocketRelayListener` pair added to `common-library`:
every notification is published as JSON to a per-service Redis Pub/Sub channel
(`ws:relay:chat-service`, `ws:relay:message-service`) instead of being delivered directly;
every node subscribes to that same channel and attempts local delivery on receipt - a
no-op on any node where the target user isn't connected, and successful delivery on
whichever one node they actually are connected to. `ChatWebSocketNotifier` and
`MessageWebSocketNotifier` were updated to publish through this relay instead of calling
`SimpMessagingTemplate` directly, with no change to their public API or to any calling
code. Verified against native Redis: relaunched chat-service, confirmed via
`PUBSUB CHANNELS` that it subscribed to its relay channel, then hand-published a raw
relay envelope over Redis and confirmed it was received and processed without error.

What's *not* implemented in code, because it's an infrastructure/deployment concern
rather than application logic (see `docs/ARCHITECTURE.md` for where these fit): sticky
sessions and the load balancer are an ingress/reverse-proxy configuration (e.g. an nginx
or Kubernetes Ingress `session-affinity` annotation) that keeps a client's WebSocket
reconnects landing on nodes that already have warm local state, not something a Spring
service configures itself; client-side reconnect/backoff is a frontend concern. The Redis
relay above is what makes those deployment choices *safe* - even without sticky sessions,
correctness no longer depends on which node a client lands on, only latency does.

### `media-service`

Chunked, resumable uploads and signed URLs come largely for free from S3's own
multipart-upload API (S3 and MinIO speak the same protocol) rather than being
reimplemented: `POST /media/uploads` calls `CreateMultipartUpload` and returns a
`mediaId`; `POST /media/uploads/{id}/parts/{n}` returns a presigned `UploadPart` URL so
the client PUTs each chunk directly to the object store, never through this service's own
memory; `GET /media/uploads/{id}/parts` calls `ListParts` so a reconnecting client knows
exactly which chunks it still needs to (re-)send - that's the "resume" half, and it needs
no extra bookkeeping since S3 already keeps an incomplete multipart upload addressable by
its uploadId until explicitly completed or aborted. `POST /media/uploads/{id}/complete`
finalizes it and kicks off processing.

Processing (`MediaProcessingService`) runs off the request thread (`@Async`) so completing
an upload doesn't block on however long scanning/thumbnailing/transcoding takes - the
asset sits in `PROCESSING` until it finishes:
- **Virus scan**: speaks clamd's INSTREAM protocol directly over a socket (no client
  library) against a `ClamAvVirusScanService`. If clamd is unreachable it reports
  `SCAN_UNAVAILABLE` rather than throwing; a `virus-scan.fail-closed` flag decides whether
  that blocks the upload (defaults to fail-open, logged).
- **Thumbnailing / image compression**: pure-Java (Thumbnailator) - no native dependency.
- **Video compression**: best-effort `ffmpeg` via `ProcessBuilder`; if ffmpeg isn't on
  `PATH` (true in this sandbox) the original file is kept and the asset still becomes
  `READY`, just without a transcoded variant.
- **CDN vs signed URLs**: `MediaUrlService` returns a CDN URL when this deployment has one
  fronting the bucket (`MEDIA_CDN_BASE_URL` set) or a time-limited presigned S3 `GetObject`
  URL otherwise - callers never need to know which.

Real end-to-end testing caught a genuine AWS SDK dependency bug: booting the service threw
`NoClassDefFoundError: TlsSocketStrategy` the moment the `S3Client` bean was constructed.
The SDK's default sync HTTP client (Apache5HttpClient) needs `httpclient5` on the
classpath, and while it's pulled in transitively, Spring Boot's own dependency-management
BOM (imported by the shared Gradle convention plugin for unrelated reasons) conflict-
resolves it down to `5.3.1` - a version that predates the `TlsSocketStrategy` class the
Apache5 client relies on. Fixed by switching to the SDK's URL Connection HTTP client
instead (`software.amazon.awssdk:url-connection-client`, wired explicitly via
`.httpClientBuilder(...)`), which has no third-party HTTP library in the build graph to
conflict over. Re-verified by booting the service for real: it now reaches all the way to
attempting a genuine `Connection refused` against a MinIO endpoint that doesn't exist in
this sandbox (rather than failing at bean construction) - proof the client itself is wired
correctly, with only the storage backend missing.

Like message-service (MongoDB) and the ClamAV/ffmpeg dependencies above, MinIO isn't
running in this sandbox (no Docker daemon, and downloading a MinIO binary directly was
blocked by this environment's egress policy the same way MongoDB's was) so the full
upload → scan → thumbnail pipeline couldn't be exercised against a real object store here.
Verification is unit tests (13, covering ownership checks, the complete/abort lifecycle,
and MediaProcessingService's branches: clean image → thumbnail, infected → quarantine +
delete, scan-unavailable fail-open vs fail-closed, and video-compression failure still
leaving the asset usable) plus the real boot-and-auth-and-S3-wiring check described above.

### `notification-service`

Email, SMS, and push all funnel through one `NotificationDispatchService`: render a
localized template, hand off to the channel-specific sender, log the outcome. Localization
is plain Java `ResourceBundle`s (`templates/notifications[_xx].properties`, English and
Spanish included) rather than a templating engine - `ResourceBundle` already gives locale
fallback for free (an unsupported locale silently falls back to the base bundle), and a
simple `{{name}}` substitution reads better for callers passing a named parameter map than
`MessageFormat`'s positional args. Push covers both Android and iOS from one integration:
Firebase Cloud Messaging forwards to APNs under the hood for tokens registered from an iOS
app, so there's no separate native APNs HTTP/2 client to maintain. SMS goes through Twilio;
email through any SMTP server (`JavaMailSender`) - defaults to MailHog in docker-compose so
sent mail can actually be viewed (`http://localhost:8025`) without real credentials.

**Retry and DLQ** are wired at the Kafka consumer-container level
(`KafkaConsumerConfig`'s `DefaultErrorHandler` + `ExponentialBackOff` +
`DeadLetterPublishingRecoverer`) rather than inside each listener: every `@KafkaListener`
in this service benefits automatically, and a failing send (SMTP unreachable, Firebase not
configured, a malformed payload) is retried with backoff and, once attempts are exhausted,
published to `{originalTopic}.DLT` instead of being silently dropped or retried forever.
Consumes auth-service's two pre-existing forward-declared events
(`auth.email-verification-requested.v1`, `auth.password.reset-requested.v1` - see
auth-service's section above, which said sending was deferred to this service) plus a
generic self-topic (`notification.send.v1`) any service - or this service's own
`POST /notifications/send` REST endpoint - can publish to, so future notification triggers
don't need a bespoke consumer per event type.

Verified for real against native Postgres/Kafka - and this is the one place in the platform
where the *failure* path was the point of the test, not a limitation of it: pointed the
SMTP host at an unreachable port and published a real `auth.email-verification-requested.v1`
event. Watched Spring Kafka's error handler retry it the configured number of times,
confirmed three `FAILED` rows landed in `notification_logs` with the real
`MailSendException` message, and confirmed the record was published to
`auth.email-verification-requested.v1.DLT` by consuming it directly. Repeated the same
proof through the REST-triggered path (register a device token, `POST /notifications/send`
with `channel: PUSH`, confirm `202 Accepted`, then confirm `GET /notifications/logs` shows
three `FAILED` entries citing "Firebase is not configured in this deployment") - the same
retry/DLQ machinery, exercised through the generic self-topic instead of a bespoke
consumer. Auth enforcement was verified the same way as every other service (401 without a
token). Actually landing a real email or push (MailHog, a real Firebase project, Twilio)
wasn't exercised, since none are running in this sandbox, but the failure-path proof above
is direct evidence the dispatch, logging, and retry/DLQ code paths are correct - a
successful send only differs in which branch of a try/catch executes.

### `search-service`

Indexes into OpenSearch purely by consuming events every other already-built service
already publishes - no synchronous calls, no shared database access. Four indices:
`users` (from user-service's `user.profile.created/updated.v1`), `chats` (GROUP/CHANNEL
only - a private chat has no name to search by, and chat-service already refuses
name/description edits on one, so a `chat.updated.v1` is guaranteed to only ever
reference an already-indexed chat), `messages` (from message-service's `message.sent.v1`
- indexing the same truncated, non-encrypted content preview that event already carries
for chat-service's benefit, never a separate read of message-service's encrypted store),
and `media` (from media-service's `media.uploaded.v1`, matched by filename for "file
search"). `media.quarantined.v1` and `media.deleted.v1` remove a document from the index
so infected or deleted content never stays searchable.

Building this surfaced two small gaps in already-completed services' event contracts,
fixed as part of this work: chat-service's `ChatCreatedEvent` didn't carry the chat's
name (only later renames would ever reach search-service via `chat.updated.v1`, leaving
every freshly created group unsearchable by its actual name), and media-service never
published a `media.deleted.v1` event at all, nor did `media.uploaded.v1` carry the
filename search needs. All three were added as straightforward, additive changes to
those services' existing outgoing-event shape.

Ranking is OpenSearch's own BM25 relevance scoring with field-boosting (e.g. a username
match ranks above a bio match); autocomplete uses `match_bool_prefix` rather than a
custom edge-ngram-analyzed field, trading a little precision for not needing custom index
mappings; highlighting uses OpenSearch's own `highlight` API on the matched fields. A
chat-metadata update uses OpenSearch's partial-document `update` API rather than a full
re-index, so an update to just `name`/`description` can't accidentally wipe the `type`
field a full re-index would otherwise clobber.

Like OpenSearch's sibling infra (MongoDB, MinIO, ClamAV) this couldn't be run for real in
this sandbox - no Docker daemon, and downloading the OpenSearch distribution directly was
blocked by the environment's egress policy. Verification: 5 unit tests against a mocked
`OpenSearchClient` (index/delete dispatch, IOException wrapped as a clean 503, and a hit
constructed via the client's own real builder classes correctly mapped to a `SearchHit`
with score and highlights) — and, unexpectedly, a much stronger real-world proof than
that: booting search-service against this session's live Kafka broker replayed the actual
`user.profile.created.v1` / `chat.created.v1` / `chat.deleted.v1` / `message.sent.v1`
events that auth-service, chat-service, and message-service had produced earlier in this
same session while their own sections above were being verified. Every one of them
deserialized correctly against search-service's independently-written local mirrors -
real proof the wire contracts match without a single hand-crafted test payload - then
each attempted a genuine OpenSearch call, got a clean `Connection refused`, and was wrapped
into `SearchUnavailableException` and logged without crashing the consumer or the JVM.
