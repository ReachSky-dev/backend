# ReachSky — backend

Dom aukcyjny zasobów nietrwałych w czasie. Pokój hotelowy, miejsce w busie,
wycieczka last-minute — zamiast przepaść o północy, trafiają na aukcję.
Sprzedawca chroni markę ukrytą ceną minimalną. Platforma rozlicza wyłącznie
aukcje, które osiągnęły próg — dla sprzedawcy to darmowa opcja bez ryzyka.

Opis domeny, fazy projektu i niezmienniki: [`../PROJECT.md`](../PROJECT.md).

---

## Uruchomienie lokalne

**Wymagania:** Docker, Java 21, Maven (lub użyj `./mvnw`).

```bash
# 1. Baza danych
docker compose up -d          # z katalogu ../  (docker-compose.yml jest tam)

# 2. Aplikacja
cd backend
./mvnw spring-boot:run
```

Aplikacja startuje na `http://localhost:8080`.
Health check: `http://localhost:8080/actuator/health`

Zmienne środowiskowe (domyślne wartości dopasowane do docker-compose):

| Zmienna       | Domyślna                                      |
|---------------|-----------------------------------------------|
| `DB_URL`      | `jdbc:postgresql://localhost:5432/reachsky`   |
| `DB_USERNAME` | `reachsky`                                    |
| `DB_PASSWORD` | `reachsky`                                    |

---

## Obraz Docker

Produkowane przez workflow `release.yml` przy każdym pushu na `main`.

**Nazwa na Docker Hub:** `<DOCKERHUB_USERNAME>/backend`
*(podstaw nazwę użytkownika/organizacji ustawioną w sekrecie `DOCKERHUB_USERNAME`)*

### Schemat tagów

| Tag | Znaczenie |
|-----|-----------|
| `<sha>` | Pełny SHA commita (40 znaków) — jedyny tag zdatny do rollbacku |
| `latest` | Ostatni build z `main` — nigdy nie używać do rollbacku |

### Uruchomienie konkretnej wersji

```bash
# Konkretna wersja po SHA (zalecane w produkcji)
docker run -p 8080:8080 \
  -e DB_URL=jdbc:postgresql://host:5432/reachsky \
  -e DB_USERNAME=reachsky \
  -e DB_PASSWORD=secret \
  <DOCKERHUB_USERNAME>/backend:abc1234...

# Zawsze najnowsza (dla lokalnych testów)
docker run -p 8080:8080 \
  -e DB_URL=jdbc:postgresql://host:5432/reachsky \
  -e DB_USERNAME=reachsky \
  -e DB_PASSWORD=secret \
  <DOCKERHUB_USERNAME>/backend:latest
```

### Metryki obrazu

| | |
|---|---|
| Rozmiar (uncompressed) | ~543 MB |
| Podstawa | `eclipse-temurin:21-jre-jammy` |
| Użytkownik | `appuser` (non-root) |
| Health check | `GET /actuator/health` co 30 s, start-period 60 s |
| Warstwy aplikacji | 4 (layertools) — po pierwszym pushu kolejne wdrożenia przenoszą ~2 MB |

---

## Testy

```bash
./mvnw verify
```

Testy integracyjne używają Testcontainers (PostgreSQL 17). Docker musi być
dostępny. Kontener PostgreSQL jest singletonem — startuje raz na cały build,
nie per klasa testowa.

### Fragment testu I1 — jeden zwycięzca spośród 20 równoległych licytujących

*(test zostanie dodany w Fazie 2 — patrz `../PROJECT.md` sekcja 5)*

```java
int threads = 20;
var startGate = new CountDownLatch(1);
var done = new CountDownLatch(threads);
var results = Collections.synchronizedList(new ArrayList<Result>());

for (int i = 0; i < threads; i++) {
    int bidder = i;
    executor.submit(() -> {
        try {
            startGate.await();
            results.add(placeBid(auctionId, bidder));
        } catch (Exception e) {
            results.add(Result.failed(e));
        } finally {
            done.countDown();
        }
    });
}
startGate.countDown();
done.await(10, TimeUnit.SECONDS);

assertThat(results).filteredOn(Result::isAccepted).hasSize(1);
```

---

## Architektura

Monolit modularny. Pakiety dzielone po domenie, nie po warstwie:

```
pl.reachsky
├── shared/       Money, Ids, DomainEvent, wyjątki bazowe
├── identity/     profil użytkownika
├── catalog/      Listing
├── auction/      Auction, Bid, strategie wyceny
├── wallet/       Wallet, LedgerEntry
├── ordering/     Order, wygasanie
├── notification/ WebSocket, e-mail
└── platform/     konfiguracja, security, scheduler, outbox
```

Każdy moduł ma warstwy: `domain/` → `application/` → `adapter/in/` + `adapter/out/`.
Moduł komunikuje się z innym **wyłącznie przez jego `application/`**.

---

## Decyzje architektoniczne (ADR)

Katalog: [`docs/adr/`](docs/adr/)

| # | Decyzja |
|---|---------|
| 0000 | Szablon ADR |
| 0001 | Monolit modularny na start *(do uzupełnienia)* |
| 0002 | Maven zamiast Gradle *(do uzupełnienia)* |
| 0003 | Dlaczego nie Kubernetes *(do uzupełnienia)* |
| 0004 | Pieniądze jako `long` w groszach *(do uzupełnienia)* |
| 0005 | Cognito jako IdP *(do uzupełnienia)* |
| 0006 | Cena holenderska jako funkcja schodkowa *(do uzupełnienia)* |
| 0007 | Blokada optymistyczna vs pesymistyczna *(do uzupełnienia)* |
| 0008 | Outbox zamiast brokera *(do uzupełnienia)* |
| 0009 | Dwa repo zamiast monorepo *(do uzupełnienia)* |
| 0010 | Testcontainers zamiast H2 *(do uzupełnienia)* |

---

## Stack

- Java 21, Spring Boot 4.0.x
- PostgreSQL 17, Flyway, Spring Data JPA
- Spring Security (OAuth2 Resource Server — Faza 1)
- Micrometer + Actuator + Prometheus
- JUnit 5, AssertJ, Testcontainers, Awaitility
