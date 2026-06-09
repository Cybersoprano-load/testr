package com.example.kafkastub.delay;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST для управления задержкой на лету (п.3 задания):
 *   GET  /api/delay                  → текущее значение задержки
 *   PUT  /api/delay {"delayMs": 500} → поменять задержку без перезапуска заглушки
 */
@RestController
@RequestMapping("/api/delay")
public class DelayController {

    private final DelayState delayState;

    public DelayController(DelayState delayState) {
        this.delayState = delayState;
    }

    @GetMapping
    public Map<String, Long> current() {
        return Map.of("delayMs", delayState.get());
    }

    @PutMapping
    public Map<String, Long> update(@RequestBody DelayRequest request) {
        delayState.set(request.delayMs());
        return Map.of("delayMs", delayState.get());
    }

    /** Тело PUT-запроса: новое значение задержки в миллисекундах. */
    public record DelayRequest(long delayMs) {
    }
}
