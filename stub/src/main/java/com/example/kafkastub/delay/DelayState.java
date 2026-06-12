package com.example.kafkastub.delay;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class DelayState {

    private final AtomicLong delayMs;

    public DelayState(@Value("${stub.delay.initial-ms:1000}") long initialMs,
                      MeterRegistry registry) {
        this.delayMs = new AtomicLong(initialMs);
        registry.gauge("stub.delay.ms", delayMs);
    }

    public long get() {
        return delayMs.get();
    }

    public void set(long newDelayMs) {
        if (newDelayMs < 0) {
            throw new IllegalArgumentException("Задержка не может быть отрицательной");
        }
        delayMs.set(newDelayMs);
    }
}
