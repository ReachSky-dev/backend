# CLAUDE.md — backend (`lastcall-backend`)

Przeczytaj ten plik w całości przed każdym zadaniem. Jeśli polecenie użytkownika
jest sprzeczne z tym plikiem, **zatrzymaj się i zapytaj** — nie zgaduj, która
instrukcja wygrywa.

Kontekst domenowy, fazy i niezmienniki: `PROJECT.md`. Nie duplikuj ich tutaj.

---

## 1. Protokół pracy

Każde zadanie przebiega w pięciu krokach. Nie pomijaj żadnego.

1. **Zakres.** Jednym zdaniem: co zmieniamy i czego NIE ruszamy. Wypisz listę
   plików, które zamierzasz zmodyfikować. Jeśli lista przekracza 10 plików,
   zadanie jest za duże — zaproponuj podział i czekaj.
2. **Rozpoznanie.** Przeczytaj istniejący kod, którego dotyczy zmiana. Nie pisz
   kodu na podstawie założeń o tym, jak coś wygląda.
3. **Plan.** Krótki, punktowany. Przy zmianach dotykających niezmienników
   z `PROJECT.md` — najpierw test, potem implementacja.
4. **Implementacja.** Tylko pliki z zadeklarowanej listy.
5. **Weryfikacja.** Uruchom `./mvnw verify`. Dopiero potem raportuj wykonanie.
   Nigdy nie mów, że zadanie zrobione, bez uruchomienia testów.

### Format raportu po zadaniu

```
ZMIENIONE: lista plików
TESTY:     wynik ./mvnw verify
DECYZJE:   co wybrałem i dlaczego, jeśli była alternatywa
UWAGI:     problemy zauważone poza zakresem — NIE naprawiaj ich
```

---

## 2. Twarde zakazy

Złamanie któregokolwiek = zadanie do cofnięcia.

- **Nie modyfikuj istniejących migracji Flyway.** Nigdy. Zawsze nowa migracja
  z kolejnym numerem. Checksum się nie zgadza → problem na produkcji.
- **Nie wyłączaj ani nie usuwaj testu, żeby build przeszedł.** Czerwony test to
  informacja, nie przeszkoda. Nie dodawaj `@Disabled`, nie komentuj asercji.
- **Nie dodawaj zależności bez ADR-a.** Każda nowa biblioteka w `pom.xml` to
  decyzja architektoniczna.
- **Nie refaktoruj kodu poza zakresem zadania.** Zauważone problemy trafiają do
  sekcji UWAGI, nie do commita.
- **Nie zmieniaj publicznego API bez jawnej zgody.** To dotyczy też kształtu
  odpowiedzi JSON i nazw pól.
- **Nie generuj komentarzy opisujących oczywistości.** `// zapisz użytkownika`
  nad `save(user)` to szum. Komentarz tłumaczy *dlaczego*, nigdy *co*.
- **Nie używaj `double` ani `float` do pieniędzy.** `long` w groszach wewnątrz,
  `BigDecimal` na granicach.
- **Nie używaj `LocalDateTime` do momentów w czasie.** `Instant` wszędzie,
  kolumna `timestamptz`. Strefa czasowa wyłącznie w warstwie prezentacji.
- **Nie obejmuj `@Transactional` wywołań HTTP do systemów zewnętrznych.**
  Transakcja bazodanowa nie rozciąga się na cudzy serwer.
- **Nie ufaj czasowi z klienta.** Zegar serwera jest jedynym źródłem prawdy dla
  zamknięcia aukcji i wyceny holenderskiej.
- **Nie serializuj ukrytego progu ceny** do żadnego DTO, zdarzenia WS ani logu.
- **Nie loguj danych osobowych ani tokenów.** Maskuj e-mail, nigdy nie loguj JWT.
- **Nie używaj H2 w testach.** Testcontainers z Postgresem.
- **Nie zgaduj wersji bibliotek.** Sprawdź, co jest w `pom.xml`. Jeśli czegoś
  nie ma — zapytaj, nie dobieraj samodzielnie.

---

## 3. Zasady kodu

### Warstwy
- `domain/` — czysta Java. Zero `@Entity`, zero `@Service`, zero Springa.
  Encje domenowe mają logikę i pilnują własnych niezmienników.
- `application/` — przypadki użycia, granice transakcji, porty (interfejsy).
  Tu i tylko tu żyje `@Transactional`.
- `adapter/in/` — kontrolery REST i WS, scheduler. Cienkie. Zero logiki
  biznesowej — mapowanie żądania, wywołanie przypadku użycia, mapowanie odpowiedzi.
- `adapter/out/` — implementacje portów: repozytoria JPA, klienci HTTP, S3.

Moduł woła inny moduł **wyłącznie przez jego `application`**. Nigdy przez
`domain` ani repozytorium obcego modułu.

### Konwencje
- Rekordy (`record`) na DTO i obiekty wartości.
- `sealed interface` + pattern matching na wyniki operacji i stany domenowe.
- Konstruktor wstrzykujący zależności, nigdy `@Autowired` na polu.
- Wyjątki domenowe dziedziczą po `DomainException`, mapowane na HTTP w jednym
  `@RestControllerAdvice`. Kontroler nie łapie wyjątków.
- Każdy endpoint zmieniający stan przyjmuje nagłówek `Idempotency-Key`.
- Identyfikatory: UUID v7 (sortowalny czasowo), nie autoinkrement.
- Nazwy w domenie po angielsku i zgodne z językiem domeny (`Bid`, nie `Offer`,
  jeśli `PROJECT.md` mówi `Bid`).

### Migracje
- Jedna migracja = jedna zmiana logiczna.
- Nazwa: `V{numer}__{opis_snake_case}.sql`.
- Przed napisaniem migracji sprawdź w tabeli poniżej, czy nie jest niebezpieczna:

| Operacja | Ryzyko | Zamiast tego |
|---|---|---|
| `ALTER TABLE ... ADD COLUMN NOT NULL` bez default | przepisanie tabeli, długi lock | dodaj nullable → backfill → ustaw NOT NULL |
| `CREATE INDEX` na dużej tabeli | lock na zapisy | `CREATE INDEX CONCURRENTLY` (poza transakcją) |
| `DROP COLUMN` | brak odwrotu, psuje starą wersję aplikacji | najpierw przestań używać, usuń w kolejnym wydaniu |
| zmiana typu kolumny | przepisanie tabeli | nowa kolumna → backfill → przełączenie |

Każda migracja musi mieć test, że wykonuje się na bazie **z danymi**, nie na pustej.

---

## 4. Testy

Test jest częścią zadania, nie osobnym etapem. Zadanie bez testu jest niezrobione.

### Piramida
- **Jednostkowe** — logika domenowa, bez Springa, bez bazy. Szybkie, liczne.
- **Integracyjne** — Testcontainers z Postgresem. Repozytoria, transakcje, migracje.
- **Współbieżnościowe** — dla każdego niezmiennika z sekcji 5 `PROJECT.md`.
- **Kontraktowe** — kształt odpowiedzi API, w szczególności brak wycieku progu.

### Reguły
- Nazwa testu opisuje zachowanie: `rejectsBidBelowMinimumIncrement`,
  nie `testBid2`.
- Jedna asercja logiczna na test. AssertJ.
- Zero `Thread.sleep` — `Awaitility` do oczekiwania na warunek.
- Test współbieżności: `CountDownLatch` do zsynchronizowania startu wątków,
  potem asercja na dokładnie jednym zwycięzcy i poprawnej liczbie odrzuceń.
- Testy nie zależą od kolejności wykonania ani od danych z innych testów.
- Kontener Postgresa współdzielony między klasami testowymi (singleton pattern),
  czyszczenie stanu między testami, nie restart kontenera.

### Szkielet testu współbieżności

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
startGate.countDown();          // start wszystkich naraz
done.await(10, TimeUnit.SECONDS);

assertThat(results).filteredOn(Result::isAccepted).hasSize(1);
```

---

## 5. Git

- Jeden commit = jedna logiczna zmiana. Nie „WIP", nie „fixes".
- Conventional commits: `feat(auction): add anti-sniping extension`.
- Gałąź na zadanie, PR do `main`, pipeline musi być zielony.
- **Historia commitów jest częścią portfolio.** Ma pokazywać regularną pracę
  przez miesiące, nie trzy commity po tysiąc plików.
- Nie commituj sekretów, plików IDE, `target/`.

---

## 6. Kiedy zatrzymać się i zapytać

Zapytaj zamiast decydować, gdy:

- zadanie wymaga zmiany schematu bazy, która nie wynika wprost z polecenia,
- trzeba dodać zależność,
- pojawia się wybór między blokadą optymistyczną a pesymistyczną,
- polecenie jest sprzeczne z `PROJECT.md` albo z tym plikiem,
- rozwiązanie wymaga złamania granicy modułu,
- nie wiadomo, czy coś należy do domeny, czy do aplikacji,
- test przechodzi, ale nie masz pewności, że testuje to, co powinien.

Jedno pytanie naraz, konkretne, z propozycją domyślnej odpowiedzi.

---

## 7. Oszczędność kontekstu

- Nie czytaj całego repo na starcie. Czytaj to, czego dotyczy zadanie.
- Nie wypisuj plików, których nie zmieniasz.
- Nie powtarzaj treści `PROJECT.md` w odpowiedziach — odsyłaj do sekcji.
- Nie streszczaj tego, co przed chwilą zrobiłeś, jeśli raport z sekcji 1 to pokrywa.
- Przy dużym zadaniu proponuj podział, zamiast wciągać pół repo do kontekstu.

---

## 8. Dziennik

Po każdej sesji, w której coś się zepsuło albo zaskoczyło, dopisz jeden akapit
do `docs/journal.md`: co się stało, dlaczego, jak naprawione.

Ten plik jest surowcem na rozmowy kwalifikacyjne i na postmortemy. Za trzy
miesiące nie będziesz pamiętał szczegółów, a to one robią różnicę w rozmowie.
