# Kafka-заглушка + нагрузка JMeter + мониторинг

Учебный стенд по заданию: Kafka+Zookeeper и PostgreSQL в Docker, нагрузочный скрипт JMeter,
Spring-заглушка (читает Kafka → пишет в БД), мониторинг в Grafana и управляемая задержка записи.

Подробное пошаговое объяснение «как это работает» и разбор метрик — в [GUIDELINE.md](GUIDELINE.md).
Отчёт о тестировании и выводы — в [REPORT.md](REPORT.md).

## Что нужно
- Docker + Docker Compose
- JMeter (для нагрузки) с установленным плагином `di-kafkameter` — он уже стоит в системе
- (опционально) Kafka Tool / Offset Explorer и DBeaver для ручного просмотра

## Быстрый старт
```bash
docker compose up -d --build      # поднять весь стек (Kafka, Postgres, заглушка, мониторинг)
docker compose ps                 # убедиться, что всё healthy
```

Топик `messages` (6 партиций) и таблица `messages` создаются автоматически при старте.

### Прогнать нагрузку (п.1.2)
Открыть `jmeter/kafka-load-test.jmx` в JMeter и нажать Start — или из консоли:
```bash
jmeter -n -t jmeter/kafka-load-test.jmx -l results.jtl
```
Сценарий: 4 ступени по 5 минут — 5/10/12/14 оп/с (≈20 минут).

### Остановить / очистить
```bash
docker compose down               # остановить
docker compose down -v            # остановить и удалить данные (том БД и т.п.)
```

## Карта сервисов (что и где)

| Сервис | Адрес с хоста | Внутри docker-сети | Назначение |
|---|---|---|---|
| Kafka (брокер) | `localhost:9092` | `kafka:29092` | очередь сообщений (п.1.1) |
| Zookeeper | — | `zookeeper:2181` | координация Kafka |
| Kafka UI | http://localhost:8081 | — | просмотр топика в браузере |
| PostgreSQL | `localhost:5432` | `postgres:5432` | БД, таблица `messages` (п.1.1) |
| Заглушка (Spring) | http://localhost:8080 | `stub:8080` | консьюмер Kafka → запись в БД (п.1.3) |
| Prometheus | http://localhost:9090 | `prometheus:9090` | сбор метрик JVM/Postgres/Kafka/хоста |
| Grafana | http://localhost:3000 | — | дашборды (admin / admin) |
| InfluxDB | `localhost:8086` | `influxdb:8086` | метрики JMeter (db `jmeter`) |
| postgres-exporter | http://localhost:9187/metrics | `postgres-exporter:9187` | метрики PostgreSQL |
| kafka-exporter | http://localhost:9308/metrics | `kafka-exporter:9308` | метрики Kafka (lag, offset'ы) |
| node-exporter | http://localhost:9100/metrics | `node-exporter:9100` | метрики хоста (CPU, RAM, диск, сеть) |

Доступы к БД: база `kafka_stub`, пользователь `kafka_stub`, пароль `kafka_stub`.

## Полезные эндпоинты заглушки
- `GET  http://localhost:8080/actuator/health` — состояние
- `GET  http://localhost:8080/actuator/prometheus` — метрики для Prometheus
- `GET  http://localhost:8080/api/delay` — текущая задержка записи (мс)
- `PUT  http://localhost:8080/api/delay` — сменить задержку на лету, тело `{"delayMs": 500}`

## Структура репозитория
```
docker-compose.yml      весь стенд
.env                    общие настройки (топик, доступы к БД, задержка)
stub/                   Spring-заглушка (код + Dockerfile)
jmeter/                 сценарий нагрузки (.jmx) + разбор JMETER-GUIDE.md
infra/                  init.sql, конфиг Prometheus, provisioning Grafana (датасорсы + дашборды)
GUIDELINE.md            пошаговое объяснение + подробный разбор метрик
HOWTO-BUILD.md          как собрать проект с нуля + разбор каждого файла
REPORT.md               отчёт о тестировании и выводы
```
