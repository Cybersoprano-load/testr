package com.example.kafkastub.repository;

import com.example.kafkastub.model.MessageRecord;
import org.springframework.data.jpa.repository.JpaRepository;

/** Доступ к таблице messages через Spring Data JPA (save, findAll и т.д. — из коробки). */
public interface MessageRecordRepository extends JpaRepository<MessageRecord, Long> {
}
