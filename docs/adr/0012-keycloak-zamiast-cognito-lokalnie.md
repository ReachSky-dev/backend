# ADR 0012: Keycloak lokalnie, Cognito dopiero w etapie chmurowym

- **Status:** Zaakceptowany
- **Data:** 2026-09-29
- **Dotyczy:** uwierzytelnianie, moduł identity, konfiguracja security

---

## Kontekst

Etap 3 wprowadza OIDC. `PROJECT.md` zakłada AWS Cognito jako IdP w środowisku
docelowym. Problem: Cognito nie ma lokalnego odpowiednika — każdy restart deweloperski
wymaga dostępu do internetu i do zasobów AWS. To spowalnia pętlę dev, komplikuje
testy integracyjne i uzależnia środowisko deweloperskie od konfiguracji konta AWS.

Potrzebny jest lokalny IdP, który można uruchomić jedną komendą i wyrzucić bez
śladów. Warunek konieczny: wymiana na Cognito musi być możliwa bez zmiany kodu —
jedynie zmiany konfiguracji.

## Rozważane opcje

### Opcja A — AWS Cognito od pierwszego dnia
- Za: spójne środowisko od razu; nie trzeba utrzymywać dwóch konfiguracji
- Przeciw: każdy `docker compose up` wymaga połączenia z AWS; testy integracyjne
  zależą od stanu zewnętrznego serwisu; cykl deweloperski rośnie z sekund do minut;
  konfigurace konta AWS (User Pool, App Client, PKCE) wymagana zanim powstanie
  pierwsza linijka kodu biznesowego

### Opcja B — Keycloak lokalnie, Cognito w etapie chmurowym
- Za: `docker compose up` startuje cały stack w 60–90 s bez dostępu do internetu;
  testy integracyjne nie zależą od zewnętrznych serwisów (MockMvc + `jwt()`, bez
  prawdziwego Keycloaka); realm eksportowany jako JSON w repozytorium — konfiguracja
  jest kodem; wymiana na Cognito = jedna zmienna środowiskowa (patrz Konsekwencje)
- Przeciw: dwa IdP do utrzymania; mapper ról w Cognito może różnić się od domyślnego
  Keycloak (do sprawdzenia w etapie chmurowym); trzeba pamiętać o zaktualizowaniu
  konfiguracji `realm-reachsky.json` przy dodawaniu ról/klientów

## Decyzja

Lokalnie **Keycloak 26.2** w trybie `start-dev --import-realm`.
Cognito wprowadzamy w etapie 5 (infrastruktura chmurowa), kiedy jest już co
deployować i kiedy konfiguracja AWS jest uzasadniona ekonomicznie.

## Konsekwencje

**Zyskujemy:**
- Pętla dev bez dostępu do internetu; `docker compose up` stawia kompletny stack
- Testy integracyjne całkowicie niezależne od IdP:
  `SecurityMockMvcRequestPostProcessors.jwt()` wstrzykuje mock JWT bez dekodera —
  żaden test nie wymaga działającego Keycloaka ani Cognito
- Realm jako kod: historia zmian konfiguracji IdP jest w git

**Warunek, który to umożliwia — zero SDK dostawcy:**

Kod aplikacji używa **wyłącznie standardu OIDC**. Żaden import nie pochodzi z AWS SDK,
Keycloak Admin Client ani żadnej biblioteki konkretnego dostawcy. Jedyna konfiguracja
zmieniająca się między Keycloakiem a Cognito to:

```
# Keycloak (dev)
OIDC_ISSUER_URI=http://localhost:8081/realms/reachsky

# Cognito (prod)
OIDC_ISSUER_URI=https://cognito-idp.{region}.amazonaws.com/{userPoolId}
```

Claim `realm_access.roles` w Cognito wymaga custom attribute mapper — to jedyna
zmiana po stronie IdP (nie kodu).

**Tracimy / płacimy:**
- Dwa zestawy konfiguracji IdP: `realm-reachsky.json` (Keycloak) i User Pool
  w Terraform (Cognito) do przygotowania w etapie 5
- Claim `realm_access.roles` jest domyślny w Keycloak; w Cognito wymagany custom
  Lambda trigger lub mapper atrybutów — do zweryfikowania przy migracji
- `docker compose up` jest wolniejszy o ~60 s (czas startu Keycloaka)

## Jak to weryfikujemy

1. `docker compose up -d` — wszystkie healthchecki zielone, Keycloak dostępny pod
   `http://localhost:8081/realms/reachsky`
2. `./mvnw -B verify` — wszystkie testy przechodzą **bez działającego Keycloaka**
   (testy używają `jwt()` post-processora, nie prawdziwego tokenu)
3. Przy dodaniu Cognito w etapie 5: zmiana `OIDC_ISSUER_URI` + `jwk-set-uri`
   i `./mvnw -B verify` musi pozostać zielony bez zmian w kodzie aplikacji
