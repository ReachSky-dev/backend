# ADR 0014: Blokada pesymistyczna (PESSIMISTIC_WRITE) przy składaniu stawek

- **Status:** Zaakceptowany
- **Data:** 2026-10-03
- **Dotyczy:** moduł `auction`, klasy `PlaceBidService`, `AuctionSpringDataRepository`

---

## Kontekst

Złożenie stawki to operacja Read-Modify-Write na agregacie `Auction`:
odczytaj `bidCount` i `currentPrice`, sprawdź minimum, zaktualizuj oba pola,
wstaw wiersz do `bids`. Przy wielu licytujących kilka wątków (lub instancji
aplikacji) wykonuje tę sekwencję jednocześnie na tym samym wierszu.

Musimy zdecydować, który mechanizm izolacji gwarantuje niezmiennik I1
(`BidConcurrencyTest`: dokładnie jeden winner przy równoczesnych próbach),
zanim `PlaceBidService` trafi na produkcję.

## Rozważane opcje

### Opcja A — Blokada optymistyczna z `@Version` i retry

Dodajemy pole `version` do `auctions`. Przy konflikcie Hibernate rzuca
`ObjectOptimisticLockingFailureException`; `PlaceBidService` łapie wyjątek
i ponawia całą transakcję.

- Za: brak blokowania wierszy; duża przepustowość przy niskiej contention
- Przeciw: na gorącej aukcji (20 wątków jednocześnie) 19 z 20 transakcji
  kończy się wyjątkiem i wymaga retry; bez backoff i limitu prób tworzy
  thundering herd; wyjątek infrastrukturalny trudno odróżnić od błędu
  domenowego; logika retry w serwisie aplikacji narusza zasadę jednej
  odpowiedzialności

### Opcja B — Blokada pesymistyczna `PESSIMISTIC_WRITE`

`SELECT ... FOR UPDATE` na wierszu `auctions` przed każdą modyfikacją.
Wątki kolejkują się w Postgresie; tylko jeden na raz widzi i modyfikuje
agregat.

- Za: dokładnie jeden wątek na raz — gwarancja bez retry; kod serwisu
  jest prosty (odczytaj-zmodyfikuj-zapisz, bez obsługi wyjątków konkurencji);
  `BidConcurrencyTest` z 20 wątkami daje 1 `Accepted` + 19 czystych
  `BidTooLow` z domeny; blokada działa transparentnie między instancjami
  aplikacji — Postgres serializuje na poziomie wiersza
- Przeciw: serializacja zapisów do jednej aukcji; ryzyko zakleszczenia
  przy niewłaściwej kolejności blokad; każda transakcja trzyma połączenie
  przez cały czas trwania blokady

### Opcja C — Ograniczenie unikalne `UNIQUE (auction_id, sequence)` jako jedyny mechanizm

Brak blokady w ogóle. Zaufanie, że unikalny indeks na `(auction_id, sequence)`
odrzuci drugi zapis z tym samym numerem sekwencji.

- Za: zerowy nakład po stronie aplikacji; baza robi całą robotę
- Przeciw: dwa wątki czytają `bidCount = 0`, oba wyliczają `sequence = 1`
  i oba aktualizują `current_price` w `auctions` — drugi `UPDATE` nadpisuje
  poprawną cenę wiersza, mimo że insert bidu się nie powiódł; zapis do `auctions`
  jest niespójny; wyjątek jest infrastrukturalny (`DataIntegrityViolationException`),
  nie domenowy — klient dostaje błąd 500 zamiast 409 z kodem `BID_TOO_LOW`

## Decyzja

Wybieramy **Opcję B — PESSIMISTIC_WRITE**, ponieważ gwarantuje spójność
agregatu `Auction` bez retry i bez wycieku błędów infrastrukturalnych do API.

Implementacja: `AuctionSpringDataRepository.findByIdWithLock` z adnotacją
`@Lock(LockModeType.PESSIMISTIC_WRITE)` + `@Query`; `PlaceBidService`
wywołuje `auctionRepository.findByIdForUpdate()` wewnątrz `@Transactional`.

## Konsekwencje

**Zyskujemy:**
- Niezmiennik I1 bez retry: dokładnie jeden bid wins, reszta dostaje
  czysty `BidTooLow` (HTTP 409 z `"code": "BID_TOO_LOW"`)
- `BidConcurrencyTest` jest wiarygodny: bez `@Lock` widać 9 komunikatów
  `duplicate key value violates unique constraint "bids_seq_unique"` w logach;
  z `@Lock` — 0 błędów DB, 19 błędów domenowych
- Blokada działa poprawnie przy wielu instancjach aplikacji, bo `SELECT FOR UPDATE`
  to lock Postgresa, nie pamięć JVM

**Tracimy / płacimy:**
- **Serializacja zapisów na aukcji:** w każdej chwili tylko jeden wątek może
  zapisać stawkę do danej aukcji. Transakcja trzyma lock przez cały swój czas
  (odczyt agregatu + logika domenowa + zapis bidu + zapis aukcji). Przy operacjach
  trwających 5–15 ms i puli połączeń 10 wątków daje to teorytyczny sufit ok.
  700–2 000 stawek/min na aukcję
- **Ryzyko zakleszczenia:** zakleszczenie jest możliwe, jeśli jedna transakcja
  blokuje aukcję A, potem B, a inna B potem A. W bieżącej implementacji każda
  transakcja blokuje dokładnie jedną aukcję, więc cykl nie może wystąpić.
  Ryzyko powróci, jeśli `PlaceBidService` zacznie blokować wiele aukcji
  (np. przy aukcjach powiązanych lub przy rozszerzeniu proxy bid na wiele aukcji)
- **Zachowanie przy wielu instancjach:** Postgres serializuje `FOR UPDATE`
  globalnie — poprawnie. Koszt to potencjalne oczekiwanie wątków z różnych
  instancji w kolejce na ten sam wiersz. Im więcej instancji, tym dłuższa kolejka
  i wyższe p99 latencji przy dużym ruchu

**Co musiałoby się zmienić, żeby wrócić do tej decyzji:**
- Obserwowalny próg: p99 czasu oczekiwania na lock (`pg_locks`, `pg_stat_activity`)
  przekracza **200 ms** dla stawek w produkcji, LUB liczba odrzuconych stawek
  (HTTP 503 / lock timeout) przekracza **1% żądań** w oknie 5-minutowym
- Alternatywny sygnał: **≥ 50 stawek/s na jedną aukcję** przy typowym czasie
  transakcji 10 ms — Postgres zacznie kolejkować połączenia i pula się wyczerpie
- Gdy zajdzie jeden z powyższych warunków, warto rozważyć: event sourcing
  z agregatem w Redis (SETNX jako atomowy lock), albo optymistyczny lock
  z ograniczonym retry i wykładniczym backoff na poziomie serwisu

## Jak to weryfikujemy

`BidConcurrencyTest` (20 wątków, `CountDownLatch`) — uruchomiony z `@Lock`:
1 `Accepted`, 19 `exceptions` (domain `BidTooLow`), 0 `duplicate key` w logach.

Uruchomiony bez `@Lock` (komentarz w `AuctionSpringDataRepository`):
nadal 1 wynik w puli, ale logi zawierają błędy `bids_seq_unique` z warstwy DB —
to dowód, że bez blokady spójność wiszą na ograniczeniu bazy, nie na domenie.
