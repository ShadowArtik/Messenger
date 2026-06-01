# Messenger — desktop client

A JavaFX desktop client for a real-time messenger: private & group chats, message
editing/deletion, read receipts, image attachments and more. It is a **thin client** — it
holds only in-memory UI state and talks to the backend over the network; it never touches the
database directly.

> Requires the backend to be running: **[MessengerServer](../server)**
> (Spring Boot + PostgreSQL). The client communicates with it over REST (JSON) and
> WebSocket (XML).

## Features

- **Accounts** — registration and login (passwords are hashed with bcrypt **on the server**).
- **Real-time messaging** — instant delivery over WebSocket.
- **Private & group chats** — one-to-one and multi-member groups.
- **Group roles** — owner / admin / member: add, kick, transfer ownership, rename, leave, and (owner) delete the whole group for everyone.
- **Edit & delete messages** — change or remove your own messages; updates appear on every client instantly, with an *edited* mark.
- **Read receipts** — ✓ sent, ✓✓ read (in groups, ✓✓ once anyone reads).
- **Unread counters** — per-chat badges (server-computed, correct even after being offline).
- **Online status & last seen**, **typing indicator** (auto-hides after ~2 s).
- **Image attachments** — PNG / JPG / JPEG / GIF / BMP, rendered inline.
- **Clear chat**, **date separators**, **chat search**, **profile editing**, **helper bot**.
- **Dark UI** — custom theme built with JavaFX CSS.

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| GUI | JavaFX 21 (FXML + CSS) |
| Server communication | `java.net.http` — REST client + WebSocket client |
| JSON | Jackson 2.18.2 |
| Realtime protocol | custom **XML** over WebSocket |
| Build | Maven (`javafx-maven-plugin`) |
| Tests | JUnit 5 |

## Project Structure

```
Messenger/
├── src/main/java/com/example/
│   ├── Main.java                          # JavaFX entry point
│   ├── controller/
│   │   ├── LoginController.java           # login / register screen
│   │   ├── MessengerController.java       # main screen coordinator (@FXML hub)
│   │   ├── WebSocketMessageHandler.java   # handles incoming realtime frames
│   │   ├── ChatActionsHandler.java        # send / edit / delete / clear / attach image
│   │   ├── GroupHandler.java              # group create / members / roles
│   │   ├── ProfileHandler.java            # current-user profile
│   │   ├── ChatHeaderView.java            # renders the open chat's header
│   │   └── TypingIndicators.java          # "is typing" header + chat-list previews
│   ├── model/
│   │   ├── MessengerModel.java            # in-memory UI state, reacts to server events
│   │   ├── MessageStore.java              # per-chat message lists
│   │   ├── PresenceTracker.java           # who is online
│   │   └── Chat.java · Message.java · User.java · Session.java
│   ├── network/
│   │   ├── ServerApi.java                 # HTTP gateway to the REST API (JSON)
│   │   ├── WebSocketClient.java           # realtime connection
│   │   └── XmlProtocol.java               # build/parse realtime XML
│   ├── repository/                        # REST clients (UserRepository, ChatRepository)
│   ├── service/                           # UserService, GroupService, result DTOs
│   └── view/                              # Avatars, LastSeen, ImageStore, MessengerCells, MessengerOverlays
│   └── src/main/resources/
│       ├── Login.fxml · chat-view.fxml
│       └── style.css
├── src/test/java/com/example/             # JUnit 5 unit tests
└── pom.xml
```

## Quick Start

1. **Start the backend first** — follow the README of
   [MessengerServer](../server)
   (`docker compose up -d` for PostgreSQL, then `./mvnw spring-boot:run`).
   It listens on `ws://localhost:8080/ws/chat` and `http://localhost:8080/api`.

2. **Run the client:**
   ```bash
   mvn javafx:run
   ```
   Run it again in another terminal to open more windows / log in as different users.

## Tests

```bash
mvn test
```

24 pure-logic unit tests — no server or database required:

| Suite | Tests | Scope |
|---|---|---|
| `MessageTest` | 5 | system-prefix stripping, `clientId`, edited/read flags |
| `ChatTest` | 5 | type flags, immutable "wither" methods |
| `MessageStoreTest` | 6 | add / edit / delete / mark-read / clear |
| `PresenceTrackerTest` | 3 | online / offline / online-users set |
| `XmlProtocolTest` | 5 | build/parse round-trip, escaping, history |

## Architecture

The client is intentionally thin: **no SQL, no JDBC, no DB password**. All persistence and
message routing happen on the server.

- **REST (JSON)** — request/response CRUD: auth, chats, file upload/download.
- **WebSocket (XML)** — realtime: messages, presence, typing, edits, group events.

See the [MessengerServer](../server) README for the
full message protocol and backend details.

## Requirements

- Java 21+
- Maven 3.8+
- A running [MessengerServer](../server) instance
