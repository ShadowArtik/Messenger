CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    helper_initialized BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS chats (
    id SERIAL PRIMARY KEY,
    chat_name VARCHAR(100) NOT NULL,
    chat_type VARCHAR(20) NOT NULL DEFAULT 'PRIVATE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS chat_members (
    chat_id INTEGER NOT NULL REFERENCES chats(id) ON DELETE CASCADE,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    custom_chat_name VARCHAR(100),
    PRIMARY KEY (chat_id, user_id)
);

CREATE TABLE IF NOT EXISTS messages (
    id SERIAL PRIMARY KEY,
    chat_id INTEGER NOT NULL REFERENCES chats(id) ON DELETE CASCADE,
    sender_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    text TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE chat_members
    ADD COLUMN IF NOT EXISTS member_role VARCHAR(20) NOT NULL DEFAULT 'MEMBER';

-- Soft-delete for private chats: hiding a chat keeps the membership (so the
-- other side is not orphaned and the chat can be reused), it just stops showing
-- in the hider's list until a new message or re-add un-hides it.
ALTER TABLE chat_members
    ADD COLUMN IF NOT EXISTS hidden BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS last_seen TIMESTAMP;

ALTER TABLE messages
    ADD COLUMN IF NOT EXISTS is_read BOOLEAN NOT NULL DEFAULT FALSE;

-- Client-generated UUID so a message can be referenced for edit/delete
-- (the DB id is never sent to the client).
ALTER TABLE messages
    ADD COLUMN IF NOT EXISTS client_id VARCHAR(64);

-- Whether the message text has been edited (so the "edited" mark survives reloads).
ALTER TABLE messages
    ADD COLUMN IF NOT EXISTS edited BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS attachments (
    id SERIAL PRIMARY KEY,
    filename VARCHAR(255),
    content_type VARCHAR(100),
    data BYTEA NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

UPDATE chat_members cm
SET member_role = 'OWNER'
FROM (
    SELECT cm2.chat_id,
           MIN(cm2.user_id) AS owner_id
    FROM chat_members cm2
    JOIN chats c ON c.id = cm2.chat_id
    WHERE c.chat_type = 'GROUP'
      AND NOT EXISTS (
          SELECT 1
          FROM chat_members owner_check
          WHERE owner_check.chat_id = cm2.chat_id
            AND owner_check.member_role = 'OWNER'
      )
    GROUP BY cm2.chat_id
) owners
WHERE cm.chat_id = owners.chat_id
  AND cm.user_id = owners.owner_id;
