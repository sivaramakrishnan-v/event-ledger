# Build stage
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY pom.xml .
COPY account-service/pom.xml ./account-service/
RUN mvn -f account-service/pom.xml dependency:go-offline
COPY . .
RUN mvn -f pom.xml clean package

# Package stage
FROM openjdk:17-jdk-slim
RUN apt-get update && apt-get install -y curl
WORKDIR /app
COPY --from=build /app/account-service/target/account-service-0.0.1-SNAPSHOT.jar /app/app.jar
EXPOSE 8081
ENTRYPOINT ["java","-jar","/app/app.jar"]
