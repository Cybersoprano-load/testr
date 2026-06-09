# ---------- Сборка (Java 21, как в задании) ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Сначала только pom.xml — чтобы слой с зависимостями кэшировался отдельно от кода.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Теперь исходники и сборка jar.
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---------- Запуск ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# curl нужен только для healthcheck'а в docker-compose.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
