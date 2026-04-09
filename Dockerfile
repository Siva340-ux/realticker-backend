# Render detects Java via pom.xml + Maven
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Render binds to $PORT automatically
ENV PORT=${PORT:-10000}
EXPOSE $PORT
ENTRYPOINT ["sh", "-c", "java -Dserver.port=$PORT -jar /app/app.jar"]