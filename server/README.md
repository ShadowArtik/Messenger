# MessengerServer — backend

The backend for the Messenger app: a Spring Boot server that is the **single owner of the
database** and the **point every message passes through**. Clients
([Messenger](../client), JavaFX) talk to it over REST
(JSON) for CRUD and over WebSocket (a custom **XML** protocol) for realtime.

## Responsibilities

- **Owns PostgreSQL** — the only component with database access; it creates/owns the schema (`schema.sql`).
- **Routes all realtime traffic** — private/group messages, typing, presence, edits, deletes, group events.
- **Persists everything** — messages, chats, members, attachments.
- **Enforces rules** — bcrypt password hashing (the hash never leaves the server), group membership checks, owner-only group deletion.
- **Serves files** — image upload/download via REST (stored as `BYTEA`).
- **Admin dashboard** — a web page at `/admin` with active sessions, conversations and history (images as thumbnails).

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.0.6 (Spring Web MVC) |
| Realtime | Spring WebSocket (Tomcat) — custom **XML** protocol on `/ws/chat` |
| Database access | Plain JDBC over a `DataSource` + `schema.sql` (no ORM entities) |
| Database | PostgreSQL 16 |
| Password hashing | jBCrypt 0.4 |
| Build | Maven (wrapper: `./mvnw`) |
| Tests | JUnit 5 |
| Dev database | Docker + Docker Compose |

> `spring-boot-starter-data-jpa` is on the classpath only for the JDBC `DataSource`; the app
> uses raw SQL and owns its schema via `schema.sql` (Hibernate DDL is disabled, `ddl-auto=none`).

## Project Structure

```
MessengerServer/
├── src/main/java/org/example/messengerserver/
│   ├── MessengerServerApplication.java    # Spring Boot entry point
│   ├── controller/
│   │   ├── UserController.java             # /api/users/*  (register, login, profile, search)
│   │   ├── ChatController.java             # /api/chats/*  (list, create, members, roles, rename)
│   │   ├── FileController.java             # /api/files    (POST upload, GET /{id})
│   │   ├── SessionController.java          # /api/sessions (active online users)
│   │   └── AdminDashboardController.java   # /admin + /api/conversations*
│   ├── websocket/
│   │   ├── WebSocketConfig.java            # registers the handler on /ws/chat
│   │   ├── ChatWebSocketHandler.java       # routes ALL realtime messages, saves to DB
│   │   ├── XmlProtocol.java                # build/parse realtime XML
│   │   └── XmlMessage.java                 # a parsed incoming frame
│   ├── repository/                         # JDBC: User / Chat / Message / Attachment / AdminDashboard
│   ├── service/PasswordHasher.java         # bcrypt
│   ├── storage/ConnectedUserStorage.java   # userId → WebSocket session
│   └── dto/                                # JSON response objects
│   └── src/main/resources/
│       ├── schema.sql                      # full schema, applied on every startup (idempotent)
│       └── application.properties
├── src/test/java/org/example/messengerserver/
├── docker-compose.yml                      # PostgreSQL 16 for development
├── mvnw · mvnw.cmd
└── pom.xml
```

## Quick Start

### 1. Database (Docker)
`docker-compose.yml` starts **only PostgreSQL** (the schema is created by the server, not Docker):

```bash
docker compose up -d
```
Starts PostgreSQL 16 on `localhost:5433` — database `messenger`, user/password `postgres` / `postgres`
(port 5433 avoids clashing with a local Postgres on 5432).

### 2. Server
```bash
./mvnw spring-boot:run
```
- WebSocket: `ws://localhost:8080/ws/chat`
- REST: `http://localhost:8080/api`
- Admin dashboard: `http://localhost:8080/admin`

On startup it runs `schema.sql` (idempotent) to create/upgrade all tables.

Then run the JavaFX client from
[Messenger](../client) (`mvn javafx:run`).

> **Without Docker:** create a PostgreSQL database `messenger` (user/password `postgres`/`postgres`)
> reachable at `localhost:5433`, or edit `src/main/resources/application.properties`.

## Tests

```bash
./mvnw test -Dtest=XmlProtocolTest,PasswordHasherTest
```
8 pure-logic unit tests (no database or Spring context required):

| Suite | Tests | Scope |
|---|---|---|
| `XmlProtocolTest` | 4 | parse + outgoing builders, XML escaping |
| `PasswordHasherTest` | 4 | bcrypt hash / verify / salting |

(`./mvnw test` also runs the `@SpringBootTest` context-load test, which needs a running PostgreSQL.)

## Protocols

Two channels, by purpose:

- **REST (JSON)** — request/response CRUD: auth, chats, files, sessions, dashboard.
- **WebSocket (XML)** — realtime: messages, presence, typing, edits, group events.

### Realtime message types (XML over `ws://localhost:8080/ws/chat`)

| Type | Direction | Description |
|---|---|---|
| `CONNECT` | client → server | register this session as online |
| `CONNECT_SUCCESS` / `ONLINE_USERS` | server → client | handshake + current online users |
| `USER_ONLINE` / `USER_OFFLINE` | server → clients | presence changes |
| `PRIVATE_MESSAGE` | client → server → recipient | private text/image (carries a client `msgId`) |
| `GROUP_MESSAGE` | client → server → members | group message (sender must be a member) |
| `LOAD_HISTORY` → `HISTORY` | client → server → client | load a chat's stored messages |
| `MESSAGE_READ` → `MESSAGES_READ` | client → server → others | mark a chat read; update ✓✓ |
| `EDIT_MESSAGE` → `MESSAGE_EDITED` | client → server → all | edit text, broadcast |
| `DELETE_MESSAGE` → `MESSAGE_DELETED` | client → server → all | delete a message, broadcast |
| `CLEAR_CHAT` | client → server → members | wipe a conversation's history |
| `TYPING` | client → server → recipients | typing indicator relay |
| `GROUP_CREATED` | client → server → members | a new group was created |
| `GROUP_MEMBERS_UPDATED` / `GROUP_RENAMED` | client → server → members | membership / name changes (+ system message) |
| `DELETE_GROUP` → `GROUP_DELETED` | client → server → all | owner deletes the whole group |
| `USER_PROFILE_UPDATED` | client → server → others | display-name change propagation |
| `SAVE_MESSAGE` | client → server | persist a bot/system message |

### Key REST endpoints

| Endpoint | Purpose |
|---|---|
| `POST /api/users/register`, `POST /api/users/login` | auth (bcrypt on the server) |
| `GET /api/chats?userId=…`, `POST /api/chats/private`, `POST /api/chats/group` | chats |
| `POST /api/files` (returns id), `GET /api/files/{id}` | image upload / download (`BYTEA`) |
| `GET /api/sessions` | active online sessions |
| `GET /admin`, `GET /api/conversations` | admin dashboard |

## How a message flows (everything through the server)

```
Lol (client)            Server (Spring Boot)            Bob (client)
   |  PRIVATE_MESSAGE (XML) |                                |
   |──────────────────────>│  save to PostgreSQL            |
   |                        │  relay ───────────────────────>│  renders message
   |<──────── echo ─────────│                                |
```

Clients hold only in-memory state — they never open the database, write SQL, or know the DB
password. All persistence and routing happen here.

## Requirements

- Java 21+
- Maven (wrapper included: `./mvnw`)
- PostgreSQL 14+ — or Docker, to run the bundled `docker-compose.yml`
