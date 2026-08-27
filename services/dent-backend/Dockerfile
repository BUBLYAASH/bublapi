FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle* .

RUN chmod +x gradlew

COPY src src

RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew clean bootJar --no-daemon


FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S spring \
    && adduser -S spring -G spring

COPY --from=builder /app/build/libs/*.jar app.jar

USER spring:spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]