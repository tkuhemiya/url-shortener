# syntax=docker/dockerfile:1

FROM maven:3.9.8-eclipse-temurin-21 AS builder
WORKDIR /app

COPY pom.xml ./
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app

RUN useradd -ms /bin/bash spring
USER spring

COPY --from=builder /app/target/url-shortener-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
