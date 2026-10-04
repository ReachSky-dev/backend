# ADR 0015: Transakcyjny outbox zamiast brokera komunikatów

- **Status:** Zaakceptowany
- **Data:** 2026-10-04
- **Dotyczy:** moduł `platform/outbox`, `auction/application/SettleAuctionService`, `ordering/application/CreateOrderOnAuctionSoldListener`

---

## Kontekst

Rozstrzygnięcie aukcji wymaga, żeby zmiana stanu agregatu (`Auction → SOLD`) i
publikacja zdarzenia (`AUCTION_SOLD`) były atomowe: albo jedno i drugie trafia
do systemu, albo żadne z nich. Bez tej gwarancji możliwe są scenariusze, w których
aukcja jest `SOLD` w bazie, ale `Order` nigdy nie powstaje (utracone zdarzenie),
albo `Order` powstaje mimo braku zmiany stanu (fantomowe zamówienie).

Musimy wybrać mechanizm, zanim `SettleAuctionService` trafi na produkcję.

## Rozważane opcje

### Opcja A — Outbox z pollingiem po bazie

Zdarzenie jest zapisywane w tabeli `outbox_events` w tej samej transakcji co
zmiana stanu agregatu. Osobny komponent (`OutboxPoller`) odpytuje tabelę
co 2 sekundy i publikuje nowe zdarzenia jako Spring `ApplicationEvent`.
Konsument (`CreateOrderOnAuctionSoldListener`) tworzy zamówienie w tej samej
transakcji co oznaczenie zdarzenia jako opublikowanego — atomowość przez MVCC.

- Za: zerowa dodatkowa infrastruktura; atomowość „za darmo" przez lokalną
  transakcję bazodanową; opóźnienie publikacji p99 ≈ 2–4 s przy cyklu 2 s;
  implementacja w ~200 liniach kodu; testowalna w Testcontainers bez mockowania
- Przeciw: polling co 2 s to dodatkowe zapytanie do bazy co cykl; maksymalne
  opóźnienie równe długości cyklu; działa poprawnie tylko w obrębie jednego
  procesu JVM (Spring `ApplicationEvent` nie przekracza granicy procesu);
  przy wielu instancjach dwa pollery mogą podnieść to samo zdarzenie —
  idempotencja po stronie konsumenta jest konieczna (`UNIQUE auction_id` w `orders`)

### Opcja B — Broker komunikatów (Kafka / RabbitMQ)

Zdarzenie jest publikowane do topiku Kafka (lub kolejki RabbitMQ) zamiast do
bazy. Producent wywołuje `producer.send()` wewnątrz transakcji bazodanowej
albo korzysta z transakcji Kafka (exactly-once semantics).

- Za: gotowy mechanizm exactly-once z Kafką; zdarzenia przekraczają granicę
  procesu — naturalne dla mikroserwisów; bardzo duża przepustowość (miliony
  zdarzeń/s); ekosystem narzędzi (Kafka UI, ksqlDB, Flink)
- Przeciw: Kafka lub RabbitMQ to osobny klaster do utrzymania (Zookeeper /
  broker / consumer groups); konfiguracja transakcji Kafka (idempotent producer,
  exactly-once) jest nietrywialnie skomplikowana; testowanie wymaga
  `EmbeddedKafka` lub Testcontainers z Kafka — wolniejsze testy;
  PROJECT.md wprost wyklucza brokera przed fazą 7 (`Kafka / RabbitMQ przed fazą 7`);
  na tym etapie projektu byłby to sygnał złożoności bez uzasadnienia operacyjnego

### Opcja C — Bezpośrednie wywołanie w transakcji

`SettleAuctionService` wywołuje `CreateOrderOnAuctionSoldListener` bezpośrednio
wewnątrz tej samej transakcji, bez outboxa i brokera.

- Za: najprostsza implementacja; brak dodatkowych tabel i polerów
- Przeciw: łamie granicę modułów (`auction` → `ordering` przez domenę, nie przez
  port); narusza zasadę jednej odpowiedzialności — serwis rozstrzygający aukcję
  nie powinien wiedzieć o zamówieniach; niemożliwe do przetestowania izolowanie
  — nie da się sprawdzić rozstrzygnięcia bez uruchomienia całego stosu
  zamówień; przy wzroście liczby konsumentów serwis staje się „bogiem"
  z dziesiątkami zależności

## Decyzja

Wybieramy **Opcję A — outbox z pollingiem**, ponieważ gwarantuje atomowość
zapisu stanu i zdarzenia przy zerowej infrastrukturze dodatkowej, a opóźnienie
2–4 s jest nieistotne dla domeny aukcji, w której rozstrzygnięcie
jest zdarzeniem jednorazowym.

Implementacja: tabela `outbox_events` z kolumną `published_at` (null = nieopublikowane),
częściowy indeks na `published_at IS NULL`, `OutboxPoller` z `TransactionTemplate`
co 2 s, batche 50 zdarzeń. Idempotencja konsumenta przez `UNIQUE auction_id`
w tabeli `orders` + sprawdzenie `existsByAuctionId` przed wstawieniem.

## Konsekwencje

**Zyskujemy:**
- Atomowość: `Auction.status = SOLD` i `OutboxEvent` zapisane w jednej transakcji —
  żadne z nich nie może zaginąć osobno
- Idempotencja I11: jeśli aplikacja padnie przed commitem, zdarzenie nie istnieje
  w outboxie — przy restarcie scheduler rozstrzyga aukcję ponownie i zapisuje
  zdarzenie od nowa; jeśli padnie po commicie, ale przed opublikowaniem —
  poller podniesie zdarzenie przy kolejnym cyklu, a `existsByAuctionId` zapobiegnie
  duplikatowi `Order`
- Zero nowej infrastruktury w fazach 1–6

**Tracimy / płacimy:**
- **Opóźnienie publikacji:** p50 ≈ 1 s, p99 ≈ 4 s (cykl 2 s + czas transakcji).
  Dla webhooka lub systemu zewnętrznego wymagającego natychmiastowej notyfikacji
  to za dużo
- **Polling:** 1 zapytanie `SELECT … WHERE published_at IS NULL LIMIT 50` co 2 s
  per instancja aplikacji — przy 10 instancjach to 5 req/s do bazy dla samego
  pollera; przy małej skali pomijalny, przy dużej — widoczny
- **Granica procesu:** `ApplicationEvent` działa wyłącznie w obrębie jednej JVM.
  Przy przejściu na mikroserwisy (faza 7) outbox musi zostać zastąpiony brokerem;
  architektura outboxa jest jednak identyczna — zmiana polega tylko na zamianie
  `applicationEventPublisher.publishEvent()` na `kafkaTemplate.send()` w `OutboxPoller`

**Co musiałoby się zmienić, żeby wrócić do tej decyzji:**
- **Opóźnienie:** pomiar p99 opóźnienia `occurred_at → published_at` przekracza
  **10 s** w oknie 15-minutowym na produkcji, co blokuje procesy biznesowe
  (np. bramka płatności oczekuje na notyfikację w czasie rzeczywistym)
- **Przepustowość:** liczba rozstrzygnięć aukcji przekracza **100/s** — poller
  nie nadąża z jednym batchem 50 zdarzeń co 2 s; alternatywa: zmniejszyć
  cykl i zwiększyć batch, albo uruchomić wiele pollerów z optymistycznym lockiem
  na `outbox_events`; jeśli i to nie wystarcza → Kafka
- **Granica procesu:** projekt wchodzi w fazę 7 i `ordering` staje się osobnym
  serwisem; wtedy `ApplicationEvent` przestaje działać i broker staje się koniecznością
- **Konkretny sygnał:** `./mvnw verify` pokazuje flaky `OutboxTest` z powodu
  race condition między pollerami (możliwe tylko przy wielu instancjach),
  a rozwiązanie przez optymistyczny lock na outboxie okazuje się droższe niż
  wdrożenie Kafki

## Jak to weryfikujemy

`OutboxTest`:
1. Zapis zdarzenia w transakcji → widoczne po commicie
2. Rollback transakcji → zdarzenie nie istnieje w bazie
3. `findUnpublished` zwraca tylko nieopublikowane
4. `markPublished` ustawia `published_at`
5. Błąd konsumenta → `attempts` rośnie, zdarzenie pozostaje nieopublikowane

`SettlementExactlyOnceTest` (I11):
- 10 wątków concurrently → dokładnie 1 zdarzenie w `outbox_events`
- Symulowany rollback → brak zdarzenia → reconciliation zapisuje dokładnie 1
