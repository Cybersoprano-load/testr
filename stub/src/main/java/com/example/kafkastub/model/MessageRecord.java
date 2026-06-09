package com.example.kafkastub.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * Строка таблицы messages — то, что заглушка сохраняет в PostgreSQL.
 * Имена столбцов в БД snake_case; соответствие из задания:
 * msg_uuid → msgUuid, head → head, time_rq → timeRq.
 */
@Entity
@Table(name = "messages")
public class MessageRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "msg_uuid", nullable = false)
    private UUID msgUuid;

    @Column(name = "head", nullable = false)
    private boolean head;

    /** Время вычитки сообщения из Kafka в формате UNIX (секунды). */
    @Column(name = "time_rq", nullable = false)
    private long timeRq;

    /** Конструктор без аргументов нужен JPA. */
    protected MessageRecord() {
    }

    public MessageRecord(UUID msgUuid, boolean head, long timeRq) {
        this.msgUuid = msgUuid;
        this.head = head;
        this.timeRq = timeRq;
    }

    public Long getId() {
        return id;
    }

    public UUID getMsgUuid() {
        return msgUuid;
    }

    public boolean isHead() {
        return head;
    }

    public long getTimeRq() {
        return timeRq;
    }
}
