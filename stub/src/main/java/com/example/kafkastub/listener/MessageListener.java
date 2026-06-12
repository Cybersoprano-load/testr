package com.example.kafkastub.listener;

import com.example.kafkastub.delay.DelayState;
import com.example.kafkastub.model.IncomingMessage;
import com.example.kafkastub.service.MessageProcessingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class MessageListener {

    private static final Logger log = LoggerFactory.getLogger(MessageListener.class);

    private final ObjectMapper objectMapper;
    private final MessageProcessingService processingService;
    private final DelayState delayState;

    public MessageListener(ObjectMapper objectMapper,
                           MessageProcessingService processingService,
                           DelayState delayState) {
        this.objectMapper = objectMapper;
        this.processingService = processingService;
        this.delayState = delayState;
    }

    @KafkaListener(topics = "${stub.kafka.topic}")
    public void onMessage(String rawJson) {
        long timeRq = Instant.now().getEpochSecond();
        log.info("[Read from Kafka] {}", rawJson);

        IncomingMessage message;
        try {
            message = objectMapper.readValue(rawJson, IncomingMessage.class);
        } catch (Exception e) {
            log.warn("Не удалось разобрать сообщение, пропускаю: {}", rawJson, e);
            return;
        }

        applyDelay(delayState.get());

        processingService.save(message, timeRq);
    }

    private void applyDelay(long delayMs) {
        if (delayMs <= 0) {
            return;
        }
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
