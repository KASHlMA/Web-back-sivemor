FROM eclipse-temurin:17-jdk-jammy AS build

WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends wget unzip \
    && rm -rf /var/lib/apt/lists/*

COPY gradlew gradlew
COPY gradlew.bat gradlew.bat
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts ./
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

COPY src src
RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends wget \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/build/libs/*.jar /app/app.jar

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --retries=10 CMD wget -qO- http://localhost:8080/actuator/health | grep -q "\"status\":\"UP\""

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
