package com.example.kafkastub.repository;

import com.example.kafkastub.model.MessageRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRecordRepository extends JpaRepository<MessageRecord, Long> {
}
