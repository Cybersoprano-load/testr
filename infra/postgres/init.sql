-- Таблица, куда заглушка пишет данные из Kafka (п.1.1).
-- Имена столбцов snake_case (best practice для PostgreSQL).
-- Соответствие из задания: msg_uuid → msgUuid, head → head, time_rq → timeRq.
CREATE TABLE IF NOT EXISTS messages (
    id        BIGSERIAL PRIMARY KEY,
    msg_uuid  UUID    NOT NULL,
    head      BOOLEAN NOT NULL,
    -- время вычитки сообщения из Kafka в формате UNIX (секунды)
    time_rq   BIGINT  NOT NULL
);

-- Индекс по msg_uuid удобен при ручной проверке записей.
CREATE INDEX IF NOT EXISTS idx_messages_msg_uuid ON messages (msg_uuid);
