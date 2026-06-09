package com.example.kafkastub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Точка входа: поднимает Spring-контекст и Kafka-листенеры. */
@SpringBootApplication
public class KafkaStubApplication {

    public static void main(String[] args) {
        SpringApplication.run(KafkaStubApplication.class, args);
    }
}
