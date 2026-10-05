# ADR 0016: Identyfikator tożsamości użytkownika w domenie

- **Status:** Zaakceptowany
- **Data:** 2026-10-05
- **Dotyczy:** cały projekt — moduły auction, catalog, ordering, identity

---

## Kontekst

Każdy użytkownik istnieje w dwóch miejscach jednocześnie: w Keycloak (dostawca
tożsamości, generuje JWT z polem `sub`) i w lokalnej tabeli `user_profiles`
(generuje własny UUID v7 jako klucz główny `id`). W rezultacie ten sam człowiek
ma dwa różne UUID w systemie.

Problem ujawnił się konkretnie: backend zapisywał `seller_id`, `bidder_id`
i `buyer_id` jako `user_profiles.id` (klucz profilu), a frontend odczytywał
`auth.user.profile.sub` z tokenu Keycloak i porównywał z tym, co przyszło
z API. Oba UUID wyglądają identycznie (format, długość), ale są różnymi
wartościami. Zakładka „Moje sprzedaże" była strukturalnie pusta u każdego
użytkownika — filtry po stronie klienta nigdy nie dawały trafień.

Trzeba zdecydować, który identyfikator jest kanonicznym identyfikatorem
użytkownika w całej domenie.

## Rozważane opcje

### Opcja A — `sub` z dostawcy tożsamości jako jedyny identyfikator

`CurrentUserProvider` wyciąga `sub` z JWT i traktuje go wprost jako `UserId`.
Tabela `user_profiles` pozostaje (przechowuje `display_name`, `created_at`,
indeksuje po `subject`), ale jej klucz główny nie wycieka poza moduł identity.

- Za: jeden UUID na użytkownika w całym systemie — brak mapowania na granicach
- Za: frontend i backend operują na tej samej wartości bez żadnej konwersji
- Za: brak N+1 zapytań przy budowaniu odpowiedzi API
- Za: usunięcie klasy błędów: nie można przypadkowo pomylić obu identyfikatorów
- Przeciw: domena jest przywiązana do formatu UUID Keycloak; inny dostawca
  może wydawać sub w innym formacie (np. Cognito wydaje ciąg jak
  `us-east-1:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx`)
- Przeciw: zmiana dostawcy tożsamości wymaga migracji danych historycznych
  lub warstwy mapowania — nie da się tego zrobić transparentnie

### Opcja B — wewnętrzny klucz profilu z mapowaniem na granicy

`CurrentUserProvider` pobiera profil z bazy po `sub`, zwraca `profile.getId()`
jako `UserId`. Domena nigdy nie widzi tokenu Keycloak.

- Za: domena jest odizolowana od formatu identyfikatora dostawcy tożsamości
- Za: zmiana dostawcy nie dotyka żadnych danych historycznych — wystarczy
  zaktualizować kolumnę `subject` w `user_profiles`
- Przeciw: dwa UUID w obiegu jednocześnie — każdy endpoint wymagający
  porównania z danymi frontendu jest potencjalnym źródłem błędu
- Przeciw: każde wywołanie `CurrentUserProvider.get()` wymaga SELECT do bazy
  (sprawdzenie / założenie profilu)
- Przeciw: to właśnie ten wariant spowodował bieżący błąd i przez kilka
  tygodni nie był wykryty testami

### Opcja C — oba identyfikatory, tłumaczenie w warstwie aplikacji

`AuctionResponse` i podobne DTO zawierają zarówno `sellerId` (profile PK)
jak i `sellerSub` (Keycloak sub). Warstwa aplikacji tłumaczy przy budowaniu
odpowiedzi przez JOIN z `user_profiles`.

- Za: nie wymaga migracji danych przy zmianie dostawcy
- Przeciw: N+1 przy każdym zapytaniu zwracającym kolekcję aukcji
- Przeciw: kontrakt API staje się bardziej skomplikowany — klient musi wiedzieć,
  którego pola użyć
- Przeciw: powiela problem opcji B, tylko drożej

## Decyzja

Wybieramy **Opcję A** — `sub` z tokenu JWT jest jedynym identyfikatorem
użytkownika w całej domenie, ponieważ eliminuje klasę błędów polegającą
na niezauważalnym porównaniu dwóch semantycznie różnych UUID.

Zmiana wdrożona w commicie `76e0682`:
- `SecurityContextCurrentUserProvider.get()` zwraca
  `new CurrentUser(new UserId(UUID.fromString(sub)), ...)` — klucz profilu
  nie jest już eksponowany poza modułem identity
- migracja `V10__user_ids_to_subject.sql` zaktualizowała siedem kolumn
  w pięciu tabelach (szczegóły w sekcji Konsekwencje)

## Konsekwencje

**Zyskujemy:**
- Jeden UUID na użytkownika w całym systemie — frontend i backend operują
  na tej samej wartości bez żadnej konwersji ani mapowania
- `CurrentUserProvider.get()` nie musi wykonywać SELECT do bazy przy każdym
  żądaniu w celu translacji identyfikatora (profil zakładany jest nadal
  idempotentnie przy pierwszym logowaniu, ale jego klucz nie jest potrzebny)
- Brak możliwości niezauważalnego przemieszania obu UUID — kompilator nie
  wyłapie pomyłki (oba to `UUID`), ale istnieje już tylko jeden identyfikator
  w obiegu

**Tracimy / płacimy:**

- **Migracja V10 — backfill siedmiu kolumn w pięciu tabelach.** Wykonana
  jednorazowo przy deployu. Zaktualizowane kolumny:
  `listings.seller_id`,
  `auctions.seller_id`,
  `auctions.highest_bidder_id`,
  `auctions.winner_id`,
  `bids.bidder_id`,
  `proxy_bids.bidder_id`,
  `orders.buyer_id`.
  Każda z nich zamieniła `user_profiles.id` na `user_profiles.subject::uuid`.
  Na bazie testowej z kilkudziesięcioma wierszami czas był pomijalny; na dużej
  bazie produkcyjnej UPDATE na tabelach `bids` i `orders` może wymagać
  indeksów i okna serwisowego.

- **Domena jest przywiązana do formatu identyfikatora dostawcy tożsamości.**
  Zakładamy, że `sub` jest UUID-parseable. Keycloak spełnia ten warunek.
  AWS Cognito domyślnie tego nie spełnia — jego `sub` wygląda jak UUID, ale
  pełny identyfikator w JWT ma postać `us-east-1:xxxxxxxx-...`. Gdybyśmy
  dziś przechodzili na Cognito, `UUID.fromString(sub)` rzucałoby wyjątek
  przy starcie każdego żądania.

- **Migracja Keycloak → inny dostawca wymaga migracji danych historycznych.**
  Nowy dostawca tożsamości wyda inny `sub` dla tego samego użytkownika
  (nawet przy migracji kont). Nie ma możliwości transparentnej podmiany
  dostawcy: albo wszystkie dane historyczne (bidy, zamówienia, wyniki aukcji)
  zostają przypisane do martwych identyfikatorów, albo trzeba przeprowadzić
  kolejny backfill mapujący stary `sub` → nowy `sub` w każdej z siedmiu
  kolumn. Mapowanie jest możliwe tylko wtedy, gdy stary dostawca udostępni
  eksport par (stary_sub → e-mail) i nowy dostawca udostępni pary
  (e-mail → nowy_sub).

- **Zmiana formatu `sub` bez zmiany dostawcy** (np. włączenie niestandardowego
  claim w Keycloak) ma ten sam skutek co zmiana dostawcy — wszystkie
  dotychczasowe `sub` UUID przestają być aktualne.

**Co musiałoby się zmienić, żeby wrócić do wewnętrznego klucza profilu:**

Warunek powrotu do Opcji B: pojawia się realna potrzeba zmiany dostawcy
tożsamości (Keycloak → Cognito, Auth0, własne rozwiązanie) przy zachowaniu
ciągłości danych historycznych, a jednorazowy backfill danych jest niemożliwy
(brak eksportu par sub → e-mail ze starego dostawcy lub skala danych
uniemożliwia backfill bez długiego okna serwisowego).

W takim przypadku wewnętrzny klucz profilu staje się warstwą izolującą domenę
od zewnętrznego identyfikatora — domena nadal zna tylko jeden UUID per
użytkownik, ale jest to UUID wygenerowany lokalnie, a mapowanie sub → profil
odbywa się wyłącznie w module identity przy każdym żądaniu.

Powrót wymaga: nowej migracji odwracającej V10 (backfill siedmiu kolumn
z powrotem na profile.id), zmiany `SecurityContextCurrentUserProvider`
i dodania cache'a dla SELECT-a profilu (żeby nie wykonywać zapytania przy
każdym żądaniu).

## Jak to weryfikujemy

Test kontraktowy w `AuctionControllerIntegrationTest` sprawdza, że
`sellerId` zwrócony przez `GET /api/auctions/{id}` jest równy `sub` z JWT
użytego przy tworzeniu aukcji — nie `user_profiles.id`. Test powinien nie
przejść, gdyby ktoś przywrócił stary wariant opcji B.

Pośrednia weryfikacja: zakładka „Moje sprzedaże" i „Moje zakupy" na
frontendzie są zasilane danymi bez client-side filtrowania po ID — jeżeli
te zakładki pokazują dane, identyfikatory są spójne.
