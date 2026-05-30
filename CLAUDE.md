# Messenger (JavaFX client)

Тонкий клиент мессенджера. Отвечает за **интерфейс, состояние UI в памяти и общение с сервером**.
Прямого доступа к БД у клиента нет — все данные идут через `MessengerServer` (REST + WebSocket).

## Запуск
- Maven + JavaFX 21, Java 21. Главный класс — `com.example.Main`.
- Требует запущенный `MessengerServer` на `http://localhost:8080` (REST) и `ws://localhost:8080/ws/chat` (realtime).

## Карта ответственности

### UI / контроллеры
- `Main` — точка входа, грузит `Login.fxml`.
- `controller/LoginController` — экран входа/регистрации.
- `controller/MessengerController` — главный экран: координатор UI. Держит `@FXML`-поля и тонкие
  `@FXML`-делегаты, проводку (`initialize`), навигацию и мелкие view-хелперы (шапка чата, скролл,
  typing, оверлеи). Тяжёлая логика вынесена в хендлеры ниже.
- `controller/GroupHandler` — логика групп (создание, участники, роли) для `MessengerController`.
- `controller/ChatActionsHandler` — действия с чатом: создание приватного, очистка, переименование,
  удаление, отправка сообщения, typing.
- `controller/WebSocketMessageHandler` — обработка входящих realtime-сообщений (новое сообщение, typing, онлайн, события групп, история).
- `view/Avatars` — построение круглых аватаров (база, онлайн-точка, бот-бейдж).
- `view/cell/MessengerCells` — фабрики ячеек списков (`contactCell`, `messageCell`, участники группы).
- `view/overlay/MessengerOverlays` — показ/скрытие модальных оверлеев.
- `resources/*.fxml`, `resources/style.css` — разметка и стили.

> Хендлеры (`GroupHandler`, `ChatActionsHandler`) держат ссылку на контроллер и работают с его
> package-private полями/методами. Это осознанный прагматичный компромисс под FXML, а не баг:
> логика разделена по фокусным классам, контроллер остаётся центральным хабом UI-состояния.

### Модели (in-memory состояние UI, НЕ БД)
- `model/MessengerModel` — кэш открытых чатов и сообщений; реагирует на события сервера.
- `model/Chat`, `model/Message`, `model/User` — неизменяемые презентационные модели.
- `model/Session` — текущий залогиненный пользователь.

### Сетевой слой (общение с сервером)
- `network/ServerApi` — HTTP-шлюз к REST API сервера (java.net.http + Jackson, JSON).
- `network/WebSocketClient` — realtime-соединение с сервером.
- `network/XmlProtocol` — сборка/разбор XML-сообщений realtime-протокола.
- `repository/UserRepository` — клиент REST-домена пользователей (логин, регистрация, профиль, поиск, участники).
- `repository/ChatRepository` — клиент REST-домена чатов (список, создание, переименование, удаление, роли).
- `service/UserService` — валидация и фасад над `UserRepository`.
- `service/GroupService` — логика ролей/участников групп (права, передача владения, кик, добавление).
  Без состояния: только репозитории + `Session`. `MessengerModel` делегирует сюда групповые методы.
- `service/result/*` — результаты операций (создание чата/группы).

## Принципы
- Клиент **не** хеширует пароли, **не** пишет SQL, **не** знает пароль от БД.
- `repository/*` — это «клиенты API», а не доступ к БД: имена методов сохранены, внутри — HTTP-вызовы к серверу.
- Realtime (сообщения, typing, онлайн) — по WebSocket в XML. Запрос/ответ (CRUD) — по REST в JSON.
