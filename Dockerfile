FROM maven:3.9-eclipse-temurin-21-alpine AS build

# Build and install shared library first
WORKDIR /common
COPY common/pom.xml .
COPY common/src ./src
RUN mvn install -q

# Build service_bus (common is now in local .m2)
WORKDIR /app
COPY service_bus/pom.xml .
RUN mvn dependency:go-offline -q
COPY service_bus/src ./src
RUN mvn clean package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
