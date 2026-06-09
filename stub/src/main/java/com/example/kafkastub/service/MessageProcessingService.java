package com.example.kafkastub.service;

import com.example.kafkastub.model.IncomingMessage;
import com.example.kafkastub.model.MessageRecord;
import com.example.kafkastub.repository.MessageRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Бизнес-логика: из сообщения Kafka собирает запись для БД и сохраняет её.
 * После сохранения пишет лог [Write to DB].
 */
@Service
public class MessageProcessingService {

    private static final Logger log = LoggerFactory.getLogger(MessageProcessingService.class);

    private final MessageRecordRepository repository;

    public MessageProcessingService(MessageRecordRepository repository) {
        this.repository = repository;
    }

    /**
     * @param message сообщение, прочитанное из Kafka
     * @param timeRq  время вычитки из топика (UNIX, секунды)
     */
    public void save(IncomingMessage message, long timeRq) {
        MessageRecord record = new MessageRecord(message.msgUuid(), message.head(), timeRq);
        repository.save(record);

        // Лог в формате задания: { "msgUuid": "...", "head": true, "timeRq": "..." }
        String json = String.format("{ \"msgUuid\": \"%s\", \"head\": %b, \"timeRq\": \"%d\" }",
                record.getMsgUuid(), record.isHead(), record.getTimeRq());
        log.info("[Write to DB] {}", json);
    }
}
