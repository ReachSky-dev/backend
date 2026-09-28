```markdown
---
name: invariant-test
description: Pisze test współbieżnościowy dla niezmiennika domenowego z sekcji 5 PROJECT.md. Używaj przy każdym niezmienniku dotyczącym równoległych operacji, wyścigów i spójności salda.
---

# Test niezmiennika

## Zasady

- Test nazywa niezmiennik z `PROJECT.md` w komentarzu klasy: `// I1: ...`.
- Testcontainers z Postgresem. Nigdy H2 — zachowanie blokad jest inne.
- Start wszystkich wątków zsynchronizowany `CountDownLatch`, żeby realnie
  konkurowały, a nie wykonywały się po kolei.
- Asercja na **dokładnej liczbie** sukcesów i porażek, nie „przynajmniej jeden".
- Wyjątki z wątków zbierane i asertowane co do typu — odrzucenie ma być
  kontrolowanym wyjątkiem domenowym, nie `NullPointerException`.
- Zero `Thread.sleep`. `Awaitility` na warunki asynchroniczne.
- Liczba wątków minimum 20. Przy mniejszej wyścig często się nie zmaterializuje.

## Struktura

1. Arrange — stan początkowy przez API domenowe, nie przez wstawki SQL.
2. Act — N wątków wykonuje operację jednocześnie.
3. Assert — niezmiennik sprawdzony przez odczyt stanu z bazy, nie z pamięci.
4. Assert — stan księgi zgodny: suma zapisów równa saldu.

## Weryfikacja jakości testu

Po napisaniu **celowo zepsuj implementację** (usuń blokadę, usuń warunek)
i sprawdź, że test faktycznie czerwienieje. Test współbieżnościowy, który nigdy
nie był czerwony, zwykle niczego nie sprawdza. Wynik tej próby zaraportuj.
```