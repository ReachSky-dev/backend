# ADR 0017: Cloudflare Tunnel zamiast przekierowania portów

- **Status:** Zaakceptowany
- **Data:** 2026-10-08
- **Dotyczy:** infrastruktura / dostęp publiczny

---

## Kontekst

Serwer produkcyjny stoi za domowym NAT. Opcje dostępu publicznego to:
przekierowanie portów 80/443 na routerze, VPS jako reverse proxy z tunelem SSH,
albo managed tunnel u dostawcy CDN. Zmiana ISP lub restart routera może zerwać
przekierowanie portów. Wystawianie SSH na zewnątrz wymaga hardeningu i monitoringu.
Projekt jest w fazie demonstracyjnej — priorytet to prostota operacyjna, nie
minimalizacja zależności od zewnętrznych dostawców.

## Rozważane opcje

### Opcja A — Przekierowanie portów na routerze
- Za: zero zależności zewnętrznych, pełna kontrola
- Przeciw: zależy od routera i ISP; dynamiczne IP wymaga DDNS; port 80/443
  musi być otwarty na świat; przy zmianie ISP konfiguracja się sypie

### Opcja B — VPS jako reverse proxy (SSH tunnel / WireGuard)
- Za: niezależność od Cloudflare; stałe IP na VPS
- Przeciw: dodatkowy koszt VPS; kolejny element do utrzymania; własny SSL;
  latencja przez dodatkowy skok; konfiguracja WireGuard/SSH tunnel jest
  nietrywialalna

### Opcja C — Cloudflare Tunnel (cloudflared)
- Za: nie wymaga otwartych portów; SSL z automatu; DDoS protection gratis;
  jeden kontener w compose; token rotowany bez restartu serwera; Cloudflare
  zarządza DNS dla reachsky.pl
- Przeciw: zależność od Cloudflare; ruch przechodzi przez ich sieć;
  konto Cloudflare wymagane; przy awarii Cloudflare serwis niedostępny
  (nawet jeśli serwer działa)

## Decyzja

Wybieramy **Cloudflare Tunnel**, ponieważ eliminuje potrzebę konfiguracji
routera i daje wbudowane SSL, CDN i ochronę DDoS za cenę jednej zależności
zewnętrznej akceptowalnej dla projektu w tej skali.

## Konsekwencje

**Zyskujemy:**
- Brak otwartych portów na routerze — powierzchnia ataku zredukowana
- SSL terminowany na edge Cloudflare — certyfikaty zarządzane automatycznie
- Zmiana ISP / restart routera nie zrywa dostępu publicznego
- Wbudowana ochrona DDoS i Web Application Firewall (plan Free)
- Jeden kontener `cloudflared` zastępuje całą konfigurację Traefik + Certbot

**Tracimy / płacimy:**
- **Zależność od Cloudflare:** awaria Cloudflare = niedostępność serwisu,
  nawet jeśli serwer lokalny działa poprawnie
- **Ruch przez sieć Cloudflare:** wszystkie żądania przechodzą przez
  infrastrukturę Cloudflare, w tym dane użytkowników; akceptowalne w fazie
  demo, wymaga oceny przed wdrożeniem produkcyjnym z danymi wrażliwymi
- **Zmiana konfiguracji w panelu Cloudflare:** DNS i reguły tunelu zarządzane
  przez dashboard / API Cloudflare, nie tylko przez repo
- **SSL nie jest end-to-end:** Cloudflare ↔ cloudflared ↔ backend to HTTP;
  musi być sieć Docker niepubliczna i zaufanie do Cloudflare jako terminatora

**Co musiałoby się zmienić, żeby wrócić do tej decyzji:**
- Wymóg end-to-end encryption bez wyjątku (regulacje, dane medyczne)
- Cennik Cloudflare zmienia się w sposób nieproporcjonalny do korzyści
- Projekt wychodzi z domowego serwera na dedykowany VPS z publicznym IP

## Jak to weryfikujemy

- `curl -I https://reachsky.pl` zwraca `200` i nagłówek `cf-ray`
- `curl https://api.reachsky.pl/actuator/health` zwraca `{"status":"UP"}`
- Po wyłączeniu przekierowania portów (lub jego braku od początku)
  oba endpointy wciąż działają
- `docker ps` na serwerze domowym pokazuje kontener `cloudflared` jako jedyny
  z dostępem na zewnątrz (brak opublikowanych portów 80/443)
