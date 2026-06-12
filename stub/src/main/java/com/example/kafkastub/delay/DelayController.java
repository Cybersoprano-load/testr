package com.example.kafkastub.delay;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

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

    public record DelayRequest(long delayMs) {
    }

    // Отрицательная задержка — ошибка клиента, отдаём 400 вместо 500
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> onInvalidDelay(IllegalArgumentException e) {
        return Map.of("error", e.getMessage());
    }
}
