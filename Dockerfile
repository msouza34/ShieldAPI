# syntax=docker/dockerfile:1

FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -q -DskipTests clean package

FROM eclipse-temurin:17-jre
WORKDIR /app

RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser

COPY --from=build /app/target/shieldapi-1.0.0.jar app.jar
RUN chown appuser:appgroup /app/app.jar
EXPOSE 8080

USER appuser
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
