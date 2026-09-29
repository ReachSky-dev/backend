# ---- build stage --------------------------------------------------------
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

# pom.xml i wrapper kopiowane osobno — zmiana src nie unieważnia warstwy z zależnościami
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -B -q

COPY src/ src/
RUN ./mvnw package -B -q -DskipTests

# ---- runtime stage ------------------------------------------------------
FROM eclipse-temurin:21-jre-jammy AS runtime

# curl potrzebny do healthchecka definiowanego w docker-compose.yml
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

RUN groupadd -r appgroup && useradd -r -g appgroup appuser
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
USER appuser

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
