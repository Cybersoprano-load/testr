# CHECKME — шпаргалка по модулю `stub`

Заглушка: читает сообщения из Kafka → ждёт управляемую задержку → пишет в PostgreSQL. Метрики отдаёт в Prometheus.

## Поток данных
```
Kafka topic → MessageListener → (задержка) → MessageProcessingService → repository.save → PostgreSQL
```

## Файлы (что и зачем)

| Файл | Зачем |
|---|---|
| [pom.xml](stub/pom.xml) | Maven: родитель Spring Boot, Java 21, зависимости |
| [Dockerfile](stub/Dockerfile) | Двухэтапная сборка: Maven+JDK21 собирает jar → кладём в JRE-образ |
| `.dockerignore` | Что не копировать в контекст сборки (`target/` и т.п.) |
| [KafkaStubApplication.java](stub/src/main/java/com/example/kafkastub/KafkaStubApplication.java) | Точка входа (`main`), запуск Spring-контекста |
| [listener/MessageListener.java](stub/src/main/java/com/example/kafkastub/listener/MessageListener.java) | `@KafkaListener`: лог `[Read from Kafka]`, парсинг JSON, задержка, вызов сервиса |
| [service/MessageProcessingService.java](stub/src/main/java/com/example/kafkastub/service/MessageProcessingService.java) | Собирает `MessageRecord`, сохраняет, лог `[Write to DB]` (через Jackson) |
| [model/IncomingMessage.java](stub/src/main/java/com/example/kafkastub/model/IncomingMessage.java) | DTO входящего сообщения (`record`), `@JsonProperty` snake_case → поля |
| [model/MessageRecord.java](stub/src/main/java/com/example/kafkastub/model/MessageRecord.java) | JPA-сущность строки таблицы `messages` |
| [repository/MessageRecordRepository.java](stub/src/main/java/com/example/kafkastub/repository/MessageRecordRepository.java) | Spring Data JPA `JpaRepository` — `save()` из коробки |
| [delay/DelayState.java](stub/src/main/java/com/example/kafkastub/delay/DelayState.java) | Текущая задержка в `AtomicLong` (меняется на лету) + метрика `stub_delay_ms` |
| [delay/DelayController.java](stub/src/main/java/com/example/kafkastub/delay/DelayController.java) | REST: `GET/PUT /api/delay`, валидация → 400 на отрицательную |
| [resources/application.yml](stub/src/main/resources/application.yml) | Конфиг: Kafka, datasource, `concurrency`, `ddl-auto: validate`, actuator |
| [resources/logback-spring.xml](stub/src/main/resources/logback-spring.xml) | Формат логов строго как в задании |

## Ключевые зависимости (зачем каждая)

| Зависимость | Зачем |
|---|---|
| `spring-boot-starter-web` | REST `/api/delay` + HTTP-эндпоинты actuator + встроенный Tomcat |
| `spring-kafka` | `@KafkaListener` — консьюмер топика, многопоточность |
| `spring-boot-starter-data-jpa` | Репозиторий + ORM (Hibernate) — запись в БД |
| `postgresql` (runtime) | JDBC-драйвер PostgreSQL |
| `spring-boot-starter-actuator` | Эндпоинты `/actuator/**` (health, metrics, prometheus) |
| `micrometer-registry-prometheus` (runtime) | Экспорт метрик в формате Prometheus на `/actuator/prometheus` |
| `spring-boot-maven-plugin` | Сборка исполняемого «толстого» jar |

## Эндпоинты
| Метод | URL | Что |
|---|---|---|
| GET | `/actuator/health` | состояние (его же дёргает healthcheck) |
| GET | `/actuator/prometheus` | метрики для Prometheus (вкл. `stub_delay_ms`) |
| GET | `/api/delay` | текущая задержка записи (мс) |
| PUT | `/api/delay` | сменить задержку на лету, тело `{"delayMs": 500}` |

> Схемой таблицы владеет [init.sql](infra/postgres/init.sql), а не Hibernate (`ddl-auto: validate` только проверяет соответствие). Полный разбор — в [HOWTO-BUILD.md](HOWTO-BUILD.md), как работает — в [GUIDELINE.md](GUIDELINE.md).
