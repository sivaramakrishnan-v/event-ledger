# Build stage
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY event-gateway-service/pom.xml ./event-gateway-service/
RUN mvn -f event-gateway-service/pom.xml dependency:go-offline
COPY . .
RUN mvn -f pom.xml clean package

# Package stage
FROM eclipse-temurin:17-jre-jammy
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /app/event-gateway-service/target/event-gateway-service-0.0.1-SNAPSHOT.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
