package com.example.kafkastub.service;

import com.example.kafkastub.model.IncomingMessage;
import com.example.kafkastub.model.MessageRecord;
import com.example.kafkastub.repository.MessageRecordRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class MessageProcessingService {

    private static final Logger log = LoggerFactory.getLogger(MessageProcessingService.class);

    private final MessageRecordRepository repository;
    private final ObjectMapper objectMapper;

    public MessageProcessingService(MessageRecordRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public void save(IncomingMessage message, long timeRq) {
        MessageRecord record = new MessageRecord(message.msgUuid(), message.head(), timeRq);
        repository.save(record);
        log.info("[Write to DB] {}", toJson(record));
    }

    private String toJson(MessageRecord record) {

        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("msgUuid", record.getMsgUuid());
        fields.put("head", record.isHead());
        fields.put("timeRq", record.getTimeRq());
        try {
            return objectMapper.writeValueAsString(fields);
        } catch (JsonProcessingException e) {
            return record.getMsgUuid().toString();
        }
    }
}
