# Отчёт о тестировании

Дата: 2026-06-07. Стенд развёрнут локально в Docker, проверка проведена end-to-end
(короткий smoke). Полный нагрузочный сценарий (4 ступени по 5 минут) подготовлен и
запускается отдельной командой.

## 1. Что развёрнуто и на каких хостах

| Сервис | Адрес с хоста | Внутри docker-сети | Назначение |
|---|---|---|---|
| Kafka (брокер) | `localhost:9092` | `kafka:29092` | очередь сообщений (п.1.1) |
| Zookeeper | — | `zookeeper:2181` | координация Kafka |
| Kafka UI | http://localhost:8081 | — | просмотр топика |
| PostgreSQL | `localhost:5432` | `postgres:5432` | БД, таблица `messages` (п.1.1) |
| Заглушка (Spring) | http://localhost:8080 | `stub:8080` | консьюмер Kafka → запись в БД (п.1.3) |
| Prometheus | http://localhost:9090 | `prometheus:9090` | сбор метрик JVM/Postgres/Kafka |
| Grafana | http://localhost:3000 | — | дашборды (admin / admin) |
| InfluxDB | `localhost:8086` | `influxdb:8086` | метрики JMeter (db `jmeter`) |
| postgres-exporter | http://localhost:9187 | `postgres-exporter:9187` | метрики PostgreSQL |
| kafka-exporter | http://localhost:9308 | `kafka-exporter:9308` | метрики Kafka |

БД: база/логин/пароль = `kafka_stub`. Заглушка собирается и запускается на **Java 21** в контейнере.

## 2. Методика проверки
Стек поднят `docker compose up -d --build`; дождались статуса `healthy` у kafka/postgres/stub.
Сообщения подавались двумя путями: вручную через `kafka-console-producer` (контроль логики) и
через JMeter-сценарий (smoke). Результат проверялся в БД (`psql`/DBeaver), в логах заглушки и в
метриках (Prometheus targets, экспортеры, Grafana).

## 3. Результаты (smoke)

**Kafka / БД (п.1.1, 1.3):**
- Топик `messages` создан, **6 партиций** (для многопоточной вычитки).
- Таблица `messages(id, msg_uuid, head, time_rq)` создана через `init.sql`.
- 12 тестовых сообщений → **12 строк** в БД (потерь нет).
- Логика «каждое 10-е сообщение `head=false`» подтверждена: 10-я запись имела `head=false`.
- `time_rq` пишется в формате UNIX (секунды), напр. `1780847539`.

**Логи заглушки (п.1.3 #4)** — формат точно по заданию:
```
2026-06-07 15:52:19.434 – [Read from Kafka] {"msg_uuid": "...", "head": true, "method": "POST", "uri": "/post-message"}
2026-06-07 15:52:19.937 – [Write to DB] { "msgUuid": "...", "head": true, "timeRq": "1780847539" }
```

**Многопоточность (п.1.3 #3):** заглушка работает с `concurrency=3` (3 consumer-потока в группе);
топик из 6 партиций распределяется между ними.

**Задержка (п.3):** между `[Read from Kafka]` и `[Write to DB]` выдерживается заданная задержка.
Динамическая смена без перезапуска подтверждена: `GET /api/delay` → 1000, `PUT {"delayMs":500}` →
последующие записи шли с интервалом ~500 мс. Текущая задержка видна в метрике `stub_delay_ms`.

**JMeter (п.1.2):** короткий smoke-прогон сценария — **206 запросов, 0 ошибок (0.00%)**;
в БД добавилось 205 записей (одна в момент замера была в обработке), метрики прогона ушли в
InfluxDB (измерения `jmeter`, `events`) и доступны на дашборде JMeter. Плагин di-kafkameter
(Kafka Producer Config/Sampler) отрабатывает корректно. Полный сценарий со ступенями 5/10/12/14
оп/с по 5 минут запускается отдельно (см. ниже).

**Метрики/мониторинг (п.2):**
- Prometheus targets `kafka-stub`, `postgres`, `kafka` — все `up`.
- `postgres-exporter`: `pg_up = 1`. `kafka-exporter`: топик `messages` = 6 партиций.
- Grafana: provisioning отработал — датасорсы **Prometheus** и **InfluxDB**, дашборды
  **JVM (Micrometer)**, **PostgreSQL Database**, **Kafka Exporter Overview**,
  **Apache JMeter Dashboard (Core InfluxdbBackendListenerClient)**.
- JVM-метрики заглушки отдаются на `/actuator/prometheus` (память, GC, потоки, CPU + `stub_delay_ms`).

## 4. Полный нагрузочный сценарий (запускать отдельно)
Профиль: **4 ступени по 5 минут**, темпы **5 / 10 / 12 / 14 оп/с**. Реализован через
Ultimate Thread Group (ступени потоков 5/10/12/14) + Constant Throughput Timer (1 оп/с на поток).
Запуск:
```bash
cd jmeter && jmeter -n -t kafka-load-test.jmx -l results.jtl   # ~20 минут; метрики в Grafana
```
Во время прогона смотреть: дашборд JMeter (throughput ступенями 5→10→12→14, 0 ошибок),
Kafka Exporter (consumer lag), JVM (память/GC), PostgreSQL (TPS, соединения).

## 5. Выводы
- Связка Kafka → заглушка → PostgreSQL работает корректно: сообщения не теряются, формат логов
  и поля БД соответствуют заданию, многопоточная вычитка включена.
- Управляемая задержка и её динамическое изменение на лету работают; влияние на пропускную
  способность наблюдается через consumer lag в Grafana.
- Мониторинг закрывает все четыре группы метрик из задания (JVM, PostgreSQL, JMeter, Kafka)
  стандартными дашбордами Grafana Labs.
- Нагрузочный сценарий с точными ступенями 5/10/12/14 оп/с подготовлен и проверен smoke-прогоном;
  полный 20-минутный прогон запускается одной командой.
