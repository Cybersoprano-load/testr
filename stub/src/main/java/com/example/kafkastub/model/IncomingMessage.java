package com.example.kafkastub.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/**
 * Сообщение, которое приходит в топик Kafka.
 * Пример: {"msg_uuid":"e7b8f190-051a-11ee-9e98-63e09b591d3a","head":true,"method":"POST","uri":"/post-message"}
 *
 * @JsonProperty связывает snake_case-поля JSON с camelCase-полями record'а.
 */
public record IncomingMessage(
        @JsonProperty("msg_uuid") UUID msgUuid,
        @JsonProperty("head") boolean head,
        @JsonProperty("method") String method,
        @JsonProperty("uri") String uri
) {
}
