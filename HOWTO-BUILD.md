# Как собрать это приложение с нуля — пошагово, с разбором каждого файла

Цель документа — чтобы ты мог повторить проект сам и понимал назначение **каждого файла** и
**в каком порядке** их писать. Принцип один: строим **снизу вверх маленькими шагами**, каждый
слой проверяем отдельным инструментом, прежде чем идти дальше.

Поток данных всей системы:
```
JMeter → Kafka (топик messages) → заглушка (Spring) → PostgreSQL
                  └─ метрики: Prometheus/InfluxDB → Grafana
```

---

## Часть A. Последовательность написания (что за чем)

| Шаг | Что делаем                            | Главные файлы                                             |
| :-: | ------------------------------------- | --------------------------------------------------------- |
|  1  | Kafka+Zookeeper и PostgreSQL в Docker | `docker-compose.yml`, `.env`, `init.sql`                  |
|  2  | Каркас Spring Boot                    | `pom.xml`, `KafkaStubApplication.java`, `application.yml` |
|  3  | Модель данных (таблица + сущность)    | `MessageRecord.java`, `MessageRecordRepository.java`      |
|  4  | Чтение из Kafka — сначала просто лог  | `MessageListener.java`                                    |
|  5  | Парсинг тела + запись в БД            | `IncomingMessage.java`, `MessageProcessingService.java`   |
|  6  | Формат логов как в задании            | `logback-spring.xml`                                      |
|  7  | Многопоточная вычитка                 | `application.yml` (`concurrency`)                         |
|  8  | Упаковать заглушку в Docker           | `Dockerfile`, `.dockerignore`                             |
|  9  | Нагрузочный сценарий JMeter           | `kafka-load-test.jmx`                                     |
|  10 | Мониторинг (Prometheus + Grafana)     | `prometheus.yml`, `grafana/provisioning/**`               |
|  11 | Динамическая задержка                 | `DelayState.java`, `DelayController.java`                 |
|  12 | Документация                          | `README.md`, `REPORT.md`, `GUIDELINE.md`                  |

Ключевая идея: к шагу 5 у тебя уже **работает сквозная цепочка** Kafka→заглушка→БД на одном
сообщении. Всё остальное (нагрузка, метрики, задержка) — надстройки поверх рабочего ядра.

---

## Часть B. Разбор каждого файла

### Инфраструктура / оркестрация

**`docker-compose.yml`** — описание всего стенда: какие контейнеры поднять, их порты, переменные,
зависимости (`depends_on` + healthcheck) и тома. Здесь же два listener'а Kafka (внутренний
`kafka:29092` для контейнеров и внешний `localhost:9092` для хоста), одноразовый `kafka-init`
(создаёт топик) и сервисы мониторинга. Пишется по мере добавления слоёв (шаги 1, 8, 10).

**`.env`** — общие настройки, которые docker compose подставляет в `docker-compose.yml`
(имя топика, число партиций, доступы к БД, начальная задержка, число consumer-потоков). Вынесены
сюда, чтобы не дублировать значения и менять их в одном месте.

**`.gitignore`** — что не коммитить: артефакты прогонов JMeter (`*.jtl`, `jmeter.log`) и каталог
сборки `stub/target/`.

**`infra/postgres/init.sql`** — SQL, создающий таблицу `messages` (`id, msg_uuid, head, time_rq`)
и индекс. Postgres выполняет его **один раз при первой инициализации** (файл монтируется в
`/docker-entrypoint-initdb.d/`). Схемой владеет SQL, а не Hibernate (заглушка только `validate`).

**`infra/prometheus/prometheus.yml`** — что Prometheus опрашивает (scrape): заглушку
(`/actuator/prometheus`), postgres-exporter, kafka-exporter и node-exporter. Интервал сбора — 10 c.

**`infra/grafana/provisioning/datasources/datasources.yml`** — автонастройка источников данных
Grafana при старте: Prometheus (метрики JVM/Postgres/Kafka) и InfluxDB (метрики JMeter).

**`infra/grafana/provisioning/dashboards/dashboards.yml`** — провайдер дашбордов: говорит Grafana
подхватить все JSON-дашборды из этой папки при старте.

**`infra/grafana/provisioning/dashboards/*.json`** — сами дашборды (в основном стандартные с
grafana.com, переработанные под наши датасорсы; node-exporter — собственный):
- `jvm-micrometer.json` — JVM-метрики заглушки (ID 4701);
- `postgres.json` — метрики PostgreSQL (ID 9628);
- `kafka-exporter.json` — метрики Kafka (ID 7589);
- `jmeter.json` — метрики прогона JMeter из InfluxDB (ID 5496);
- `node-exporter.json` — метрики хоста: CPU, RAM, диск, сеть (собственный компактный дашборд).

### Заглушка (Spring Boot) — `stub/`

**`stub/pom.xml`** — Maven-проект: родитель Spring Boot, Java 21 и зависимости (web, spring-kafka,
data-jpa, драйвер postgresql, actuator, micrometer-prometheus). Отсюда начинается код (шаг 2).

**`stub/Dockerfile`** — двухэтапная сборка: на образе с Maven+JDK 21 собираем jar, затем кладём
его в лёгкий JRE-образ. Так фиксируем Java 21 и не зависим от того, что стоит на хосте.

**`stub/.dockerignore`** — что не копировать в контекст сборки образа (`target/`, `.idea` и т.п.).

**`stub/src/main/resources/application.yml`** — конфигурация заглушки: адрес Kafka, группа и
десериализаторы consumer'а, `listener.concurrency` (многопоточность), datasource PostgreSQL,
`ddl-auto: validate`, имя топика, начальная задержка и какие actuator-эндпоинты открыть
(`health`, `prometheus`, ...). Значения берутся из переменных окружения (12-factor).

**`stub/src/main/resources/logback-spring.xml`** — формат логов строго как в задании:
`yyyy-MM-dd HH:mm:ss.SSS – сообщение`. Нужен, чтобы строки `[Read from Kafka]`/`[Write to DB]`
выглядели точь-в-точь как требуется.

**`KafkaStubApplication.java`** — точка входа (`main`), запускает Spring-контекст. С неё начинается
приложение; больше в ней ничего нет.

**`model/IncomingMessage.java`** — DTO входящего сообщения (`record`). `@JsonProperty` связывает
snake_case-поля JSON (`msg_uuid`) с camelCase-полями. Сюда Jackson разбирает тело из Kafka.

**`model/MessageRecord.java`** — JPA-сущность строки таблицы `messages`. Поля размечены
`@Column(name=...)` под snake_case-столбцы; конструктор без аргументов нужен JPA.

**`repository/MessageRecordRepository.java`** — интерфейс Spring Data JPA (`extends JpaRepository`).
Даёт `save(...)` и прочее «из коробки», без реализации.

**`listener/MessageListener.java`** — сердце заглушки. `@KafkaListener` слушает топик; на каждое
сообщение: фиксирует время вычитки (UNIX-секунды), логирует `[Read from Kafka]`, разбирает JSON,
выдерживает текущую задержку и зовёт сервис записи. Многопоточность — через `concurrency` из конфига.

**`service/MessageProcessingService.java`** — бизнес-логика записи: собирает `MessageRecord`,
сохраняет через репозиторий и логирует `[Write to DB]`. Отделён от листенера, чтобы «что делаем»
(сохранить) не смешивалось с «откуда пришло» (Kafka).

**`delay/DelayState.java`** — хранит текущую задержку в `AtomicLong` (можно менять на лету,
потокобезопасно) и публикует её как Micrometer-метрику `stub_delay_ms` (видно в Grafana).

**`delay/DelayController.java`** — REST для управления задержкой без перезапуска:
`GET /api/delay` (узнать) и `PUT /api/delay {"delayMs": …}` (поменять).

### Нагрузка — `jmeter/`

**`jmeter/kafka-load-test.jmx`** — сценарий JMeter: Kafka Producer Config (продюсер), Ultimate
Thread Group (ступени потоков 5/10/12/14 по 5 мин), JSR223 PreProcessor (генерит `msg_uuid` и тело,
«каждое 10-е head=false»), Kafka Producer Sampler (отправка), Constant Throughput Timer (пейсинг
1 оп/с на поток) и Backend Listener (метрики → InfluxDB).

**`jmeter/JMETER-GUIDE.md`** — подробный разбор этого сценария: атомарность, скрипт построчно,
справочник всех переменных.

### Документация

**`README.md`** — как поднять и проверить стенд + карта хостов/портов.
**`REPORT.md`** — отчёт о тестировании, карта сервисов, выводы.
**`GUIDELINE.md`** — пошаговое «как это работает» и подробный разбор метрик по группам.
**`HOWTO-BUILD.md`** — этот файл: порядок сборки и назначение каждого файла.

---

## Часть C. Как проверять на каждом шаге (чтобы не отлаживать всё сразу)

- **Kafka**: `kafka-console-producer` / `kafka-console-consumer`, либо kafka-ui (`:8081`) / Offset Explorer.
- **PostgreSQL**: `psql` или DBeaver — смотри таблицу `messages`.
- **Заглушка**: `curl localhost:8080/actuator/health`, логи `docker compose logs -f stub`.
- **Метрики**: `curl localhost:8080/actuator/prometheus`, цели Prometheus (`:9090/targets`), дашборды Grafana (`:3000`).
- **Нагрузка**: View Results Tree в JMeter (0 ошибок), затем дашборд JMeter в Grafana.

Правило: новый слой добавляешь только когда предыдущий проверен. Тогда любой баг локализуется
сразу, а не «где-то в системе из десяти сервисов».
