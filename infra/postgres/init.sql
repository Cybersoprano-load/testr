CREATE TABLE IF NOT EXISTS messages (
    id        BIGSERIAL PRIMARY KEY,
    msg_uuid  UUID    NOT NULL,
    head      BOOLEAN NOT NULL,
    time_rq   BIGINT  NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_messages_msg_uuid ON messages (msg_uuid);
