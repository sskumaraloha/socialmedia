# syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

COPY gradlew .
COPY gradle gradle
COPY buildSrc buildSrc
COPY settings.gradle.kts build.gradle.kts gradle.properties ./
COPY common-library common-library
COPY media-service media-service

RUN chmod +x gradlew
RUN ./gradlew :media-service:bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S spring && adduser -S spring -G spring
WORKDIR /app
COPY --from=build /workspace/media-service/build/libs/*.jar app.jar
USER spring:spring
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
