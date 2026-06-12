package com.example.kafkastub.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record IncomingMessage(
        @JsonProperty("msg_uuid") UUID msgUuid,
        @JsonProperty("head") boolean head,
        @JsonProperty("method") String method,
        @JsonProperty("uri") String uri
) {
}
