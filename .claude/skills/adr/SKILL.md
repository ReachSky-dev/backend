```markdown
---
name: adr
description: Tworzy nowy Architecture Decision Record z szablonu projektu. Używaj przy każdej decyzji dotyczącej wyboru biblioteki, wzorca, struktury bazy lub podejścia architektonicznego.
---

# Tworzenie ADR

1. Sprawdź `docs/adr/`, ustal kolejny wolny numer (czterocyfrowy).
2. Skopiuj `docs/adr/0000-template.md` do `docs/adr/{numer}-{slug}.md`.
3. Wypełnij wszystkie sekcje. Sekcje puste są niedopuszczalne.

## Wymagania jakościowe

- Minimum dwie realnie rozważane opcje. Jedna opcja to nie decyzja, to notatka.
- Sekcja „Tracimy / płacimy" nie może być pusta. Każda decyzja ma koszt.
  Jeśli go nie widzisz, nie rozumiesz decyzji.
- Sekcja „Co musiałoby się zmienić" musi zawierać konkretny, obserwowalny
  warunek — liczbę użytkowników, rozmiar tabeli, czas builda. Nie „gdyby projekt urósł".
- Kontekst pisz w czasie teraźniejszym, decyzję w dokonanym.
- Maksymalnie jedna strona.

## Czego nie robić

- Nie uzasadniaj decyzji popularnością technologii.
- Nie pisz ADR-a po fakcie tak, jakby wybór był oczywisty — jeśli wahałeś się
  między opcjami, to ma być widoczne.
- Nie zmieniaj ADR-a, który ma status Zaakceptowany. Pisz nowy ze statusem
  zastępującym i zaktualizuj status starego.
```