```markdown
---
name: module
description: Tworzy szkielet nowego modułu domenowego zgodny ze strukturą warstw projektu. Używaj przy dodawaniu nowego obszaru domeny.
---

# Nowy moduł

## Struktura

```
pl.lastcall.{modul}
├── domain/         encje, obiekty wartości, wyjątki domenowe — zero Springa
├── application/    przypadki użycia, porty (interfejsy), @Transactional
├── adapter/in/     kontrolery REST/WS, scheduler
└── adapter/out/    repozytoria JPA, klienci zewnętrzni
```

## Reguły

- `domain/` nie importuje niczego ze Springa, JPA ani Jacksona. Sprawdź to.
- Encja JPA nie jest encją domenową. Jeśli to trudne — na start dopuszczamy
  wspólną klasę, ale zapisz to jako świadomy kompromis w ADR.
- Port to interfejs w `application/`, implementacja w `adapter/out/`.
- Moduł wystawia na zewnątrz **tylko** klasy z `application/`. Reszta
  package-private tam, gdzie się da.
- Dodaj regułę ArchUnit pilnującą granic tego modułu w tym samym zadaniu.

## Definicja ukończenia

- [ ] struktura katalogów
- [ ] przynajmniej jedna encja domenowa z realną regułą biznesową
- [ ] port i adapter wyjściowy
- [ ] test jednostkowy domeny bez kontekstu Springa
- [ ] test integracyjny adaptera z Testcontainers
- [ ] reguła ArchUnit
- [ ] migracja Flyway, jeśli moduł ma własne tabele
```