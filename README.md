# Messenger

A desktop messenger built with Java — real-time private & group chat over a custom
**XML-over-WebSocket** protocol. The project is **two independent applications** that talk only
over the network:

| Folder | Module | Stack |
|---|---|---|
| [`client/`](client) | JavaFX desktop client — UI + in-memory state | Java 21, JavaFX 21 |
| [`server/`](server) | Spring Boot backend — REST + WebSocket, owns the database | Java 21, Spring Boot 4 |

The client is a **thin client**: no SQL, no DB password — everything goes through the server,
which persists to PostgreSQL and routes all messages.

## Features

- Accounts (registration / login, **bcrypt on the server**).
- Real-time private & group chats with message history.
- Group roles (owner / admin / member): add, kick, transfer ownership, rename, leave, delete group.
- Edit & delete messages (broadcast to everyone, with an *edited* mark).
- Read receipts (✓ / ✓✓), unread counters, online + last seen, typing indicator.
- Image attachments (rendered inline), clear chat, date separators, chat search, helper bot.
- Admin web dashboard at `/admin` (active sessions, conversations, history with image thumbnails).

## Quick Start

### 1. Database + server (`server/`)
```bash
cd server
docker compose up -d        # PostgreSQL 16 on localhost:5433
./mvnw spring-boot:run      # server on http://localhost:8080
```
The server creates the schema (`schema.sql`) on startup.
- WebSocket: `ws://localhost:8080/ws/chat` · REST: `http://localhost:8080/api` · Dashboard: `http://localhost:8080/admin`

### 2. Client (`client/`)
```bash
cd client
mvn javafx:run
```
Run it again in another terminal to open more windows / log in as different users.

## Tests
```bash
cd client && mvn test                                          # 24 tests
cd server && ./mvnw test -Dtest=XmlProtocolTest,PasswordHasherTest   # 8 tests
```
32 pure-logic unit tests — no database or running server required.

## Architecture in one line

```
client (JavaFX)  ──REST (JSON, CRUD)──▶  server (Spring Boot)  ──▶  PostgreSQL
                 ──WebSocket (XML, realtime)──▶  (routes & broadcasts to everyone)
```

See [`client/README.md`](client/README.md) and [`server/README.md`](server/README.md) for the
full code map, message protocol and details.

## Requirements

- Java 21+
- Maven 3.8+ (the server bundles a wrapper: `./mvnw`)
- PostgreSQL 14+ — or Docker, to run the bundled `server/docker-compose.yml`
