---
name: note
description: Dopisuje ustalenie, pułapkę albo decyzję do wspólnego KNOWLEDGE.md nad oboma repozytoriami. Używaj, gdy coś zostało ustalone eksperymentalnie, coś zaskoczyło, albo gdy zapadła decyzja niewidoczna w kodzie.
allowed-tools: Read Edit
---

# Zapis do KNOWLEDGE.md

Plik: `../KNOWLEDGE.md` względem katalogu repozytorium (workspace nad oboma repo).

## Kroki

1. Przeczytaj `../KNOWLEDGE.md`.
2. Ustal właściwą sekcję. Nie twórz nowych sekcji bez potrzeby.
3. Dopisz wpis w formacie właściwym dla sekcji.
4. Jeśli wpis rozstrzyga coś z „Otwartych pytań" — usuń stamtąd i przenieś
   do „Rozstrzygnięte — archiwum" z datą.
5. Pokaż użytkownikowi dopisany fragment. Nie streszczaj całego pliku.

## Format wpisów

**Pułapki i zaskoczenia** — trzy części, zawsze:
```
- {data} {objaw} → {przyczyna} → {rozwiązanie}
```

**Decyzje spoza ADR-ów** — decyzja plus powód w jednym zdaniu:
```
- {data} {co zdecydowano}, bo {powód}
```

**Komendy** — tylko komendy, które faktycznie zostały uruchomione i zadziałały
w tym projekcie. Nie wpisuj komend z dokumentacji ani zgadywanych.

## Zasady

- Jedna linia na wpis, o ile to możliwe. To jest indeks, nie dziennik.
- Zapisuj **fakt**, nie narrację. „Testcontainers wymaga `testcontainers.reuse.enable=true`
  w `~/.testcontainers.properties`, inaczej każda klasa testowa startuje nowy kontener"
  — nie „mieliśmy problem z testami i go rozwiązaliśmy".
- Nie duplikuj tego, co jest w `PROJECT.md`, w ADR-ach albo wynika wprost z kodu.
- Nie zapisuj sekretów, tokenów, haseł, kluczy AWS ani identyfikatorów zasobów,
  które mogą posłużyć do dostępu. Nazwy zasobów tak, poświadczenia nigdy.
- Jeśli wpis pasowałby do ADR-a (wybór między opcjami z konsekwencjami) —
  powiedz to użytkownikowi i zaproponuj `/adr` zamiast dopisywania tutaj.

## Argument

`$ARGUMENTS` zawiera treść do zapisania. Jeśli jest pusta, zaproponuj wpis na
podstawie tego, co wydarzyło się w bieżącej sesji, i poczekaj na potwierdzenie
przed zapisem.
