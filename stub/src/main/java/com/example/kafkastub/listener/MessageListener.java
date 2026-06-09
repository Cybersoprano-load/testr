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

/**
 * Слушает топик Kafka. На каждое сообщение:
 *   1) фиксирует время вычитки (UNIX, секунды) и пишет лог [Read from Kafka];
 *   2) выдерживает текущую задержку (её можно менять на лету, см. DelayState);
 *   3) передаёт данные в сервис, который сохраняет запись в БД.
 *
 * Многопоточность (п.1.3 #3) задаётся свойством spring.kafka.listener.concurrency:
 * Spring поднимает несколько consumer-потоков в одной группе, и они параллельно
 * читают разные партиции топика.
 */
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
        // 1) Время вычитки из топика — момент, когда сообщение реально прочитано.
        long timeRq = Instant.now().getEpochSecond();
        log.info("[Read from Kafka] {}", rawJson);

        IncomingMessage message;
        try {
            message = objectMapper.readValue(rawJson, IncomingMessage.class);
        } catch (Exception e) {
            log.warn("Не удалось разобрать сообщение, пропускаю: {}", rawJson, e);
            return;
        }

        // 2) Управляемая задержка перед записью (п.3). Значение берём заново на каждое
        //    сообщение, поэтому изменение через REST применяется сразу.
        applyDelay(delayState.get());

        // 3) Сохранение записи в БД + лог [Write to DB].
        processingService.save(message, timeRq);
    }

    private void applyDelay(long delayMs) {
        if (delayMs <= 0) {
            return;
        }
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            // Корректно завершаем поток, если заглушку останавливают во время ожидания.
            Thread.currentThread().interrupt();
        }
    }
}
