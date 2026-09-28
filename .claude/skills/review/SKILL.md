```markdown
---
name: review
description: Przegląd własnej zmiany przed commitem. Uruchamiaj po każdym zadaniu, przed zgłoszeniem wykonania.
---

# Przegląd przed commitem

Przejdź listę i zaraportuj każdy punkt jako OK albo z opisem problemu.
Nie naprawiaj przy okazji rzeczy spoza zakresu — zgłoś je w UWAGI.

## Poprawność
- [ ] `./mvnw verify` zielony
- [ ] nowy kod ma test, który faktycznie go pokrywa
- [ ] żaden istniejący test nie został wyłączony, usunięty ani osłabiony
- [ ] brak `TODO`, `FIXME`, zakomentowanego kodu

## Domena
- [ ] pieniądze jako `long` w groszach, `BigDecimal` tylko na granicach
- [ ] momenty w czasie jako `Instant`, kolumny `timestamptz`
- [ ] żadne przejście stanu nie omija maszyny stanów
- [ ] ukryty próg ceny nie trafia do żadnego DTO, zdarzenia WS ani loga

## Warstwy
- [ ] `domain/` bez adnotacji frameworkowych
- [ ] `@Transactional` tylko w `application/`
- [ ] kontroler nie zawiera logiki biznesowej
- [ ] brak wywołania do obcego modułu z pominięciem jego `application/`

## Współbieżność i transakcje
- [ ] operacja zmieniająca stan jest idempotentna albo ma klucz idempotencji
- [ ] transakcja nie obejmuje wywołania HTTP na zewnątrz
- [ ] przy równoległym dostępie do agregatu jest blokada i test na nią

## Baza
- [ ] żadna istniejąca migracja nie została zmieniona
- [ ] nowa migracja ma komentarz z analizą bezpieczeństwa
- [ ] zapytanie w pętli? sprawdź N+1 — włącz logowanie SQL i policz

## Bezpieczeństwo
- [ ] brak sekretów, tokenów i danych osobowych w kodzie i logach
- [ ] endpoint ma sprawdzenie uprawnień, nie tylko uwierzytelnienie
- [ ] dane wejściowe walidowane na granicy

## Higiena
- [ ] zmienione tylko pliki z zadeklarowanego zakresu
- [ ] brak nowych zależności bez ADR-a
- [ ] komunikat commita w konwencji, opisuje *dlaczego*
```