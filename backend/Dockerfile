FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY mvnw mvnw.cmd ./
COPY .mvn ./.mvn
RUN ./mvnw -B dependency:go-offline
COPY src ./src
RUN ./mvnw -B clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN groupadd --system ourgalaxy && useradd --system --gid ourgalaxy ourgalaxy
RUN mkdir -p /app/uploads && chown ourgalaxy:ourgalaxy /app/uploads
COPY --from=build /workspace/target/our-galaxy-backend-0.1.0-SNAPSHOT.jar app.jar
USER ourgalaxy
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
