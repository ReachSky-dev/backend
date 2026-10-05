# ── build stage ───────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

# pom.xml i wrapper kopiowane osobno — zmiana src nie unieważnia warstwy z zależnościami
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -B -q

COPY src/ src/
RUN ./mvnw package -B -q -DskipTests

# ── layer extraction stage ─────────────────────────────────────────────────────
# Dzieli fat JAR na warstwy wg częstotliwości zmian: zależności (rzadko) → kod (często).
# Każde kolejne wdrożenie przenosi przez sieć tylko warstwę application (~2 MB),
# a nie cały 100 MB JAR.
FROM eclipse-temurin:21-jre-jammy AS layers
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted

# ── runtime stage ──────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy AS runtime

RUN groupadd -r appgroup && useradd -r -g appgroup appuser

WORKDIR /app

# Warstwy wklejane w kolejności od najstabilniejszej do najczęściej zmienianej,
# żeby cache Dockera był jak najbardziej skuteczny.
COPY --from=layers /app/extracted/dependencies/          ./
COPY --from=layers /app/extracted/spring-boot-loader/    ./
COPY --from=layers /app/extracted/snapshot-dependencies/ ./
COPY --from=layers /app/extracted/application/           ./

USER appuser

EXPOSE 8080

# wget jest dostępny w eclipse-temurin:21-jre-jammy bez dodatkowej instalacji.
# Sprawdza /actuator/health zamiast portu TCP, bo endpoint zawiera "status":"UP/DOWN".
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO /dev/null http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
