```markdown
---
name: migration
description: Tworzy migrację Flyway z obowiązkową analizą bezpieczeństwa. Używaj przy każdej zmianie schematu bazy danych.
---

# Migracja Flyway

## Zanim napiszesz SQL

1. Sprawdź najwyższy numer w `src/main/resources/db/migration/`.
2. **Nigdy nie modyfikuj istniejącej migracji.** Zawsze nowy plik.
3. Nazwa: `V{numer}__{opis_snake_case}.sql`.

## Analiza bezpieczeństwa — obowiązkowa

Dla każdej operacji w migracji odpowiedz na trzy pytania i umieść odpowiedzi
jako komentarz na górze pliku:

1. Jaki lock zakłada ta operacja i na jak długo przy tabeli z milionem wierszy?
2. Czy stara wersja aplikacji będzie działać po tej migracji? (deploy nie jest atomowy)
3. Jak to cofnąć, jeśli deploy trzeba wycofać?

## Wzorce bezpieczne

| Zamiast | Zrób |
|---|---|
| `ADD COLUMN NOT NULL` | `ADD COLUMN` nullable → migracja backfill → `SET NOT NULL` |
| `CREATE INDEX` | `CREATE INDEX CONCURRENTLY` w migracji z `-- flyway:executeInTransaction=false` |
| `DROP COLUMN` | najpierw przestań czytać i pisać w kodzie, usuń kolumnę w następnym wydaniu |
| zmiana typu | nowa kolumna → backfill → przełączenie kodu → usunięcie starej |
| `RENAME COLUMN` | jak wyżej — rename psuje starą wersję aplikacji natychmiast |

## Test

Każda migracja zmieniająca istniejące tabele wymaga testu integracyjnego,
który wypełnia tabelę danymi reprezentatywnymi, uruchamia migrację i sprawdza
integralność po niej. Migracja przetestowana wyłącznie na pustej bazie jest
nieprzetestowana.
```