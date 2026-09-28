# ADR 0011: Spring Boot 4.0.x zamiast 3.5.x

- **Status:** Zaakceptowany
- **Data:** 2026-09-28
- **Dotyczy:** cały projekt, wszystkie moduły

---

## Kontekst

`PROJECT.md` zakładał Spring Boot 3.5.x jako wybór pod rynek bankowy: dojrzały,
sprawdzony w produkcji, łatwy do obronienia na rozmowie technicznej.
Spring Initializr wygenerował projekt z wersją 4.0.8, opartą na Spring Framework 7,
Testcontainers 2.x i Flyway 11. Faza 0 pokazała, że SB 4.x wprowadza realne
koszty adaptacji — trzeba zdecydować, czy zostajemy, czy cofamy.

Zmiana wersji po pierwszym commicie jest możliwa, ale zwiększa ryzyko
niespójności i kosztuje czas na re-test.

## Rozważane opcje

### Opcja A — Spring Boot 3.5.x (LTS-ready, standard bankowy)
- Za: de facto standard w polskiej bankowości i ubezpieczeniach w 2026 r.;
  tysiące gotowych odpowiedzi na Stack Overflow i w dokumentacji;
  rekruter / tech lead rozumie wybór bez tłumaczenia; długie komercyjne wsparcie VMware
- Przeciw: krótszy community support niż 4.x; trzeba cofnąć wersję i sprawdzić
  wszystkie zależności od nowa; SB 3.x nie wejdzie na nowe środowiska JDK LTS
  tak sprawnie jak 4.x

### Opcja B — Spring Boot 4.0.x (najnowszy, Spring Framework 7)
- Za: oparty na Spring Framework 7 (Jakarta EE 11, wirtualne wątki jako first-class);
  dłuższe wsparcie społecznościowe; demonstracja znajomości aktualnego ekosystemu;
  projekt jest portfolio — pokazuje orientację w nowościach, nie tylko konserwatyzm
- Przeciw: mało materiałów i gotowych przepisów w sieci; część bibliotek jeszcze
  nie nadąża; konkretne koszty adaptacji już wystąpiły w fazie 0 (patrz niżej)

## Decyzja

Zostajemy na **Spring Boot 4.0.x**, ponieważ cofnięcie wersji w fazie 0 kosztuje
więcej niż adaptacja, a projekt portfolio zyska na pokazaniu znajomości najnowszego
ekosystemu.

## Konsekwencje

**Zyskujemy:**
- Dłuższe wsparcie community dla SB 4.x
- Spring Framework 7: wirtualne wątki, ulepszone `RestClient`, czystsza modularyzacja
  auto-konfiguracji (każdy starter sam rejestruje swoje beany — łatwiej debugować)
- Sygnał dla rekrutera, że autor śledzi ekosystem, nie tylko powtarza przepisy

**Tracimy / płacimy:**
- `TestRestTemplate` usunięty w Spring Framework 7; zastąpiony przez `RestClient`
  — trzeba pisać testy HTTP inaczej niż pokazuje 90% tutoriali dla SB 3.x
- Testcontainers 2.x zmienił nazwy modułów: `org.testcontainers:postgresql`
  i `org.testcontainers:junit-jupiter` nie istnieją; poprawne nazwy to
  `testcontainers-postgresql` i `testcontainers-junit-jupiter` — błąd ujawni się
  dopiero przy próbie buildu z BOM SB 4.x
- Auto-konfiguracja Flyway wyprowadzona z `spring-boot-autoconfigure` do osobnego
  modułu `spring-boot-flyway`; bez `spring-boot-starter-flyway` Flyway jest na
  classpath, ale się nie uruchamia — aplikacja startuje, testy przechodzą, tylko
  migracje nie wykonują się
- `flyway-database-postgresql` wymagany osobno jako runtime dependency; bez niego
  Flyway 11 rzuca `Unsupported Database: PostgreSQL 17.11` mimo że Postgres działa

**Co musiałoby się zmienić, żeby wrócić do tej decyzji:**
- Jeśli w fazie 2 lub 3 okaże się, że biblioteka kluczowa dla projektu
  (np. klient Cognito, STOMP, konkretny driver) nie ma stabilnej wersji
  kompatybilnej z SB 4.x / Spring Framework 7 i nie ma obejścia — wtedy
  migracja na 3.5.x staje się uzasadniona.
- Konkretny sygnał: `./mvnw verify` czerwony z powodu konfliktu zależności,
  którego nie da się rozwiązać bez łamania któregoś z twardych zakazów z `CLAUDE.md`.

## Jak to weryfikujemy

`./mvnw -B verify` zielony na każdej gałęzi — to jedyna miara, że projekt
działa z wybraną wersją. Nowe zależności dodawane do `pom.xml` są sprawdzane
pod kątem obecności w BOM 4.0.x przed mergem.
