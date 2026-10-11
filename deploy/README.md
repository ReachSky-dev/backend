# Deploy — ReachSky Backend

Wdrożenie przez Ansible + Cloudflare Tunnel na serwer domowy za NAT.
Żaden port (80/443) nie jest wystawiony na router — ruch wychodzi tylko
przez `cloudflared`.

Model: jednorazowy bootstrap z serwera → self-hosted runner → CI/CD wdraża.

---

## FAZA A — Bootstrap z serwera (jednorazowo)

Ansible działa lokalnie na serwerze (`ansible_connection: local`).
Nie potrzeba laptopa — wystarczy dostęp SSH i sudo.

### A.1 — Przygotuj zaszyfrowane vault.yml w repo

Na **dowolnej maszynie** (laptop, inny serwer) — przed klonowaniem:

```bash
cd deploy/

# prod
cp group_vars/prod/vault.example.yml group_vars/prod/vault.yml
nano group_vars/prod/vault.yml      # wypełnij wszystkie pola
ansible-vault encrypt group_vars/prod/vault.yml
git add --force group_vars/prod/vault.yml

# demo
cp group_vars/demo/vault.example.yml group_vars/demo/vault.yml
nano group_vars/demo/vault.yml
ansible-vault encrypt group_vars/demo/vault.yml
git add --force group_vars/demo/vault.yml

git commit -m "chore(deploy): add encrypted vaults"
git push
```

### A.2 — Na serwerze: zainstaluj zależności

SSH na serwer, następnie:

```bash
# Python i Ansible (Ubuntu 24.04)
sudo apt update
sudo apt install -y python3 python3-pip ansible git

# Kolekcja community.general (moduł UFW)
ansible-galaxy collection install community.general
```

### A.3 — Sklonuj repo i skonfiguruj vault password

```bash
git clone https://github.com/ReachSky-dev/backend.git /opt/reachsky-repo
cd /opt/reachsky-repo

# Hasło do odszyfrowania vault.yml — trzymaj poza repo
echo "TWOJE_HASLO_VAULTA" > ~/.vault_pass_reachsky
chmod 600 ~/.vault_pass_reachsky
```

### A.4 — Uruchom site.yml lokalnie

```bash
cd /opt/reachsky-repo/deploy

# Dry run:
ansible-playbook -i inventory.local.yml site.yml --check --diff

# Właściwe uruchomienie — konfiguruje oba środowiska:
ansible-playbook -i inventory.local.yml site.yml

# Lub tylko jedno środowisko:
ansible-playbook -i inventory.local.yml site.yml -l prod
ansible-playbook -i inventory.local.yml site.yml -l demo
```

`site.yml` zainstaluje Ansible i kolekcje system-wide (potrzebne dla runnera),
stworzy użytkownika `reachsky`, skonfiguruje katalogi i UFW.

Weryfikacja UFW działa poprawnie w trybie lokalnym — `SSH_CLIENT` jest puste,
sprawdzenie CIDR jest pomijane.

### A.5 — Pierwsze wdrożenie (ręczne, przed uruchomieniem CI/CD)

```bash
cd /opt/reachsky-repo/deploy

ansible-playbook -i inventory.local.yml deploy.yml -l demo \
  -e "backend_tag=<SHA> frontend_tag=<SHA>"

ansible-playbook -i inventory.local.yml deploy.yml -l prod \
  -e "backend_tag=<SHA> frontend_tag=<SHA>"
```

SHA znajdziesz: `git -C /opt/reachsky-repo log --oneline -5`

---

## FAZA B — Self-hosted runner (jednorazowo po bootstrapie)

### B.1 — Pobierz token rejestracyjny z GitHub

GitHub → repo → **Settings → Actions → Runners → New self-hosted runner**
→ Linux → skopiuj wartość po `--token` z wyświetlonej komendy konfiguracyjnej.

Token jest jednorazowy, wygasa po 1h.

### B.2 — Uruchom runner.yml

```bash
cd /opt/reachsky-repo/deploy

ansible-playbook -i inventory.local.yml runner.yml \
  -e "runner_token=<TOKEN-Z-GITHUB>"
```

Playbook:
- tworzy użytkownika `github-runner` (bez dostępu do danych aplikacji)
- pobiera i instaluje runner binary
- rejestruje runner z etykietą `self-hosted,linux,reachsky`
- instaluje jako usługę systemd

### B.3 — Weryfikacja

```bash
# Czy usługa działa:
systemctl status "actions.runner.*"

# Czy widoczny w GitHub:
# GitHub → repo → Settings → Actions → Runners
# Status: Idle (zielona kropka)
```

---

## FAZA C — GitHub: sekrety i środowiska (jednorazowo)

### C.1 — Sekret repozytorium

| Sekret | Wartość | Gdzie |
|--------|---------|-------|
| `VAULT_PASSWORD` | hasło do ansible-vault | repo → Settings → Secrets → Actions |

Już istniejące sekrety (z release.yml) — zostawiasz bez zmian:
`DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN`

### C.2 — Zmienna repozytorium

| Zmienna | Wartość | Gdzie |
|---------|---------|-------|
| `FRONTEND_TAG` | SHA ostatniego obrazu frontendu | repo → Settings → Variables → Actions |

Aktualizowana przez CI frontendu lub ręcznie przy każdym deploy frontendu.

### C.3 — Środowiska GitHub

W repo → **Settings → Environments** utwórz dwa środowiska:

**`demo`** — bez ochrony (wdraża automatycznie):
- Create environment → nazwa: `demo`
- Nie dodawaj Required reviewers

**`production`** — z ręczną akceptacją:
- Create environment → nazwa: `production`
- Required reviewers → dodaj siebie
- (opcjonalnie) Deployment branches → Only selected branches → `main`

Po skonfigurowaniu: każdy push na `main` → Release → Deploy demo automatycznie
→ GitHub czeka na Twoją akceptację → Deploy prod.

---

## Pierwsze uruchomienie — krok po kroku (tryb z laptopa, alternatywny)

### Krok 1 — Klucz SSH

Jeśli nie masz klucza dedykowanego temu serwerowi, utwórz:

```bash
ssh-keygen -t ed25519 -f ~/.ssh/id_reachsky -C "reachsky-deploy"
ssh-copy-id -i ~/.ssh/id_reachsky.pub TWOJ_USER@192.168.1.XX
```

Sprawdź czy działa:
```bash
ssh -i ~/.ssh/id_reachsky TWOJ_USER@192.168.1.XX
```

### Krok 2 — Inventory

```bash
cd deploy/
cp inventory.example.yml inventory.yml
```

Edytuj `inventory.yml` — podstaw prawdziwy adres IP i nazwę użytkownika SSH.
Plik jest gitignorowany (nie trafi do repo).

### Krok 3 — Vault password file

Plik z hasłem Vaulta przechowujesz **poza repozytorium**:

```bash
# Wymyśl mocne hasło i zapisz je w pliku poza repo:
echo "TWOJE_HASLO_VAULTA" > ~/.vault_pass_reachsky
chmod 600 ~/.vault_pass_reachsky
```

`ansible.cfg` wskazuje na ten plik przez `vault_password_file = ~/.vault_pass_reachsky`.
Dzięki temu nie musisz podawać hasła przy każdym uruchomieniu playbooka.

**Gdzie trzymać hasło w CI/CD (self-hosted runner):**
Dodaj do GitHub Secrets jako `VAULT_PASSWORD`, a workflow zapisze je do pliku:
```yaml
- name: Prepare vault password
  run: echo "${{ secrets.VAULT_PASSWORD }}" > ~/.vault_pass_reachsky && chmod 600 ~/.vault_pass_reachsky
```

### Krok 4 — Zaszyfruj sekrety w Vault

```bash
cd deploy/

# Skopiuj przykład
cp group_vars/prod/vault.example.yml group_vars/prod/vault.yml

# Wypełnij wartości w edytorze:
#   vault_db_password:             silne hasło dla PostgreSQL
#   vault_kc_admin_password:       hasło do panelu Keycloak /admin
#   vault_cloudflare_tunnel_token: token z Zero Trust → Tunnels
nano group_vars/prod/vault.yml

# Zaszyfruj plik (używa hasła z ~/.vault_pass_reachsky):
ansible-vault encrypt group_vars/prod/vault.yml

# Zweryfikuj że plik jest zaszyfrowany:
head -1 group_vars/prod/vault.yml   # powinno być: $ANSIBLE_VAULT;1.1;AES256

# Commituj zaszyfrowany plik:
git add group_vars/prod/vault.yml
git commit -m "chore(deploy): add encrypted vault"
```

Żeby edytować zaszyfrowany vault w przyszłości:
```bash
ansible-vault edit group_vars/prod/vault.yml
```

### Krok 5 — Realm Keycloak

Zanim uruchomisz `site.yml`, musisz mieć `deploy/keycloak/realm.json`.
Eksportuj z lokalnej instancji Keycloak:

```bash
# Wyeksportuj realm (bez użytkowników — bezpieczniejsze dla produkcji)
docker exec <lokalny-keycloak-kontener> \
  /opt/keycloak/bin/kc.sh export \
  --dir /tmp/export \
  --realm reachsky \
  --users skip

docker cp <lokalny-keycloak-kontener>:/tmp/export/reachsky-realm.json \
  deploy/keycloak/realm.json
```

Przed commitem sprawdź plik (patrz `deploy/README.md` → sekcja "Realm — checklistа").

### Krok 6 — Konfiguracja serwera (jednorazowo)

```bash
cd deploy/

# Dry run — sprawdź co zostanie zmienione:
ansible-playbook site.yml --check --diff

# Właściwe uruchomienie:
ansible-playbook site.yml
```

`site.yml` wykonuje:
- Tworzy użytkownika `reachsky` (docker group)
- Tworzy `/opt/reachsky/` z podkatalogami
- Kopiuje `docker-compose.prod.yml`, init SQL, realm.json
- Konfiguruje UFW: deny inbound, allow SSH z LAN

Bezpieczny do ponownego uruchomienia — wszystkie zadania są idempotentne.

### Krok 7 — Pierwsze wdrożenie

```bash
# Pobierz aktualne SHA tagów z Docker Hub lub z GitHub Actions:
BACKEND_SHA=$(git rev-parse HEAD)   # jeśli właśnie pushujesz

ansible-playbook deploy.yml \
  -e "backend_tag=${BACKEND_SHA} frontend_tag=${FRONTEND_SHA}"
```

Playbook czeka do 5 minut na `healthy` status backendu.
Jeśli się nie uda — automatycznie wraca do poprzedniego tagu.

---

## Zwykłe wdrożenie (kolejne deploy)

```bash
cd deploy/

ansible-playbook deploy.yml \
  -e "backend_tag=<nowy-sha-backendu> frontend_tag=<nowy-sha-frontendu>"
```

SHA commitów znajdziesz w zakładce "Actions" → job "Release" → sekcja "Job summary".

---

## Rollback

### Automatyczny (w deploy.yml)

Jeśli health check nie przejdzie w ciągu 5 minut, `deploy.yml` sam wraca
do poprzednich tagów i kończy się błędem. Nie musisz nic robić.

Wyjątek: **pierwszy deploy** — nie ma poprzedniej wersji. Playbook kończy się błędem
z komunikatem "brak poprzedniej wersji do przywrócenia". Napraw problem i uruchom
`deploy.yml` ponownie.

### Ręczny — rollback do poprzedniego tagu

Kiedy problem wyszedł godzinę po wdrożeniu i health był zielony:

```bash
cd deploy/

# Opcja 1: wróć do tagu zapisanego w .deployed_tags (poprzedni deploy)
ansible-playbook rollback.yml

# Opcja 2: wskaż konkretny SHA z historii git
ansible-playbook rollback.yml \
  -e "backend_tag=<stary-sha> frontend_tag=<stary-sha>"

# Opcja 3: cofnij tylko backend, frontend zostaje
ansible-playbook rollback.yml -e "backend_tag=<stary-sha>"
```

Plik `.deployed_tags` na serwerze przechowuje:
- `current` — aktualnie uruchomione tagi
- `previous` — tagi przed ostatnim deploy

Podgląd bez SSH:
```bash
ansible prod -m ansible.builtin.slurp \
  -a "src=/opt/reachsky/.deployed_tags" | \
  python3 -c "import sys,base64,json; d=json.load(sys.stdin); \
  print(json.dumps(json.loads(base64.b64decode(d['reachsky-prod']['content'])), indent=2))"
```

### Rollback o dwie wersje wstecz (lub dowolny SHA)

`.deployed_tags` trzyma tylko jeden poziom historii (poprzedni deploy).
Jeśli chcesz cofnąć się dalej, musisz podać SHA ręcznie.

**Skąd wziąć SHA:**

```bash
# 1. Z historii git:
git log --oneline -10

# 2. Z Docker Hub (obrazy tagowane SHA commita):
#    https://hub.docker.com/r/reachsky-dev/backend/tags
#    Kliknij "Tags", szukaj SHA z git log

# 3. Z GitHub Actions — Actions → Release → Job summary → "Tag SHA"
```

Gdy masz SHA:
```bash
ansible-playbook rollback.yml \
  -e "backend_tag=<sha-sprzed-dwoch-wersji> frontend_tag=<sha-sprzed-dwoch-wersji>"
```

---

## Co zrobić gdy deploy padnie w połowie

Ansible jest deterministyczny — każde zadanie wiesz po nazwie, które zawiodło.
Możliwe scenariusze:

### Po `docker compose pull`, przed `up -d`

Obrazy są pobrane, kontenery wciąż na starej wersji. Stan serwera jest **spójny** —
stare kontenery dalej działają. Możesz bezpiecznie uruchomić `deploy.yml` ponownie.

### Po `up -d`, health check nie przechodzi

`deploy.yml` automatycznie cofa do poprzedniego tagu (rescue block).
Sprawdź logi wypisane przez playbook, lub:

```bash
ansible prod -m ansible.builtin.command \
  -a "docker logs reachsky-backend-1 --tail 100"
```

### Rescue block (auto-rollback) też padł

Serwer jest w niespójnym stanie. Wejdź przez SSH i napraw ręcznie:

```bash
ssh -i ~/.ssh/id_reachsky TWOJ_USER@192.168.1.XX

# Sprawdź stan kontenerów
docker ps -a

# Przywróć poprzedni tag ręcznie — edytuj BACKEND_TAG w .env:
nano /opt/reachsky/.env

# Uruchom kontenery ze starymi tagami:
docker compose -f /opt/reachsky/docker-compose.prod.yml \
  --env-file /opt/reachsky/.env up -d

# Sprawdź czy backend wstał:
docker inspect --format='{{.State.Health.Status}}' reachsky-backend-1
```

Po naprawie ręcznej zaktualizuj `.deployed_tags` ręcznie lub uruchom `rollback.yml -e "backend_tag=..."`.

---

## Weryfikacja po wdrożeniu

Po udanym `deploy.yml` sprawdź z laptopa:

```bash
# 1. Backend przez tunel:
curl -s https://api.reachsky.pl/actuator/health | python3 -m json.tool
# Oczekiwane: {"status": "UP", ...}

# 2. Frontend:
curl -I https://reachsky.pl
# Oczekiwane: HTTP/2 200

# 3. Keycloak realm endpoint:
curl -s https://auth.reachsky.pl/realms/reachsky | python3 -m json.tool
# Oczekiwane: {"realm": "reachsky", ...}

# 4. Kontenery na serwerze (przez Ansible):
ansible prod -m ansible.builtin.command \
  -b -a "docker ps --format 'table {{.Names}}\t{{.Status}}'"
# Oczekiwane: wszystkie kontenery "Up X minutes (healthy)"

# 5. Aktualne tagi wdrożone na serwerze:
ansible prod -m ansible.builtin.slurp \
  -a "src=/opt/reachsky/.deployed_tags" | \
  python3 -c "import sys,base64,json; \
  d=json.load(sys.stdin)['reachsky-prod']; \
  print(json.dumps(json.loads(base64.b64decode(d['content'])), indent=2))"
```

### Weryfikacja przez tunel w deploy.yml (opcjonalnie)

Gdy Cloudflare Tunnel jest skonfigurowany, włącz automatyczną weryfikację publicznego URL:

```bash
# Jednorazowo — zmień w group_vars/prod/vars.yml:
#   verify_public: true

# Albo podaj jako parametr:
ansible-playbook deploy.yml \
  -e "backend_tag=<sha> frontend_tag=<sha> verify_public=true"
```

---

## Self-hosted runner (tryb lokalny)

Gdy GitHub Actions runner działa na serwerze, użyj `inventory.local.yml`:

```bash
# Ręczne uruchomienie w trybie local:
ansible-playbook -i inventory.local.yml deploy.yml \
  -e "backend_tag=${GITHUB_SHA} frontend_tag=${FRONTEND_SHA}"
```

W workflow GitHub Actions:
```yaml
- name: Deploy
  working-directory: deploy/
  run: |
    ansible-playbook -i inventory.local.yml deploy.yml \
      -e "backend_tag=${{ github.sha }} frontend_tag=${{ vars.FRONTEND_TAG }}"
  env:
    ANSIBLE_VAULT_PASSWORD_FILE: ~/.vault_pass_reachsky
```

---

## Diagnostyka

### Sprawdź stan kontenerów na serwerze

```bash
# Bezpośrednio przez SSH:
ssh -i ~/.ssh/id_reachsky USER@192.168.1.XX \
  "docker ps --format 'table {{.Names}}\t{{.Status}}'"

# Przez Ansible (bez wchodzenia na serwer):
ansible prod -m ansible.builtin.command \
  -a "docker compose -f /opt/reachsky/docker-compose.prod.yml ps"
```

### Logi kontenerów

```bash
ansible prod -m ansible.builtin.command \
  -a "docker logs reachsky-backend-1 --tail 50"

ansible prod -m ansible.builtin.command \
  -a "docker logs reachsky-keycloak-1 --tail 50"
```

### Weryfikacja UFW

```bash
ansible prod -m ansible.builtin.command -b -a "ufw status verbose"
```

---

## Realm — checklistа przed commitem

Po wyeksportowaniu `keycloak/realm.json`:

```bash
# 1. Znajdź localhost i zastąp adresami prod
grep -n "localhost" keycloak/realm.json

# Dla klienta reachsky-frontend dodaj prod URL obok localhost:
# redirectUris:  dodaj "https://reachsky.pl/*"
# webOrigins:    dodaj "https://reachsky.pl"
# rootUrl:       zmień na "https://reachsky.pl"

# 2. Sprawdź sekrety klientów
grep -n '"secret"' keycloak/realm.json
# Jeśli reachsky-frontend ma "publicClient": true — pole secret nie istnieje. OK.
# Każde niepuste "secret" przy publicClient: false = TAJNE, usuń przed commitem.

# 3. Sprawdź hasło SMTP
grep -n '"password"' keycloak/realm.json
# Niepuste smtpServer.password = TAJNE, usuń.
```

---

## Struktura plików

```
deploy/
├── ansible.cfg                        — konfiguracja Ansible (vault path, inventory)
├── inventory.yml                      — COMMITOWANY; ansible_host w vault
├── inventory.local.yml                — dla self-hosted runner (localhost)
├── group_vars/
│   ├── all/
│   │   └── vars.yml                   — wartości wspólne (app_user, deploy_base, itp.)
│   ├── prod/
│   │   ├── vars.yml                   — konfiguracja prod (env_name, URL, limity RAM)
│   │   ├── vault.example.yml          — struktura vaultu (puste wartości)
│   │   └── vault.yml                  — GITIGNOROWANY niezaszyfrowany;
│   │                                    po encrypt: git add --force ...
│   └── demo/
│       ├── vars.yml                   — konfiguracja demo (env_name, URL, limity RAM)
│       ├── vault.example.yml          — struktura vaultu (puste wartości)
│       └── vault.yml                  — GITIGNOROWANY niezaszyfrowany;
│                                        po encrypt: git add --force ...
├── templates/
│   ├── docker-compose.yml.j2          — szablon compose (generowany przez site.yml)
│   └── env.j2                         — szablon .env (generowany przez deploy.yml)
├── site.yml                           — jednorazowa konfiguracja serwera
├── deploy.yml                         — wdrożenie z auto-rollbackiem
├── rollback.yml                       — ręczny rollback
├── init/01-keycloak-db.sql            — tworzy bazę keycloak (raz)
└── keycloak/
    ├── prod/realm.json                — eksport realmu prod (do dodania przez Ciebie)
    └── demo/realm.json                — eksport realmu demo (do dodania przez Ciebie)
```

---

## Jak dodać trzecie środowisko

Zakładamy nowe środowisko o nazwie `staging`.

### Krok 1 — Skonfiguruj tunel w Cloudflare

W panelu Zero Trust → Tunnels utwórz nowy tunel `reachsky-staging`.
Dodaj trasy:
- `staging.reachsky.pl` → `frontend:80`
- `staging-api.reachsky.pl` → `backend:8080`
- `staging-auth.reachsky.pl` → `keycloak:8080`

Skopiuj token tunelu.

### Krok 2 — Utwórz group_vars

```bash
cd deploy/
mkdir -p group_vars/staging keycloak/staging

# Skopiuj vault.example jako punkt startowy
cp group_vars/demo/vault.example.yml group_vars/staging/vault.example.yml
cp group_vars/demo/vault.example.yml group_vars/staging/vault.yml
```

Edytuj `group_vars/staging/vault.yml` — wstaw prawdziwe wartości:

```yaml
vault_ansible_host:            "192.168.1.XX"   # ten sam serwer
vault_db_password:             "<wygeneruj: openssl rand -base64 32>"
vault_kc_admin_password:       "<wygeneruj: openssl rand -base64 32>"
vault_cloudflare_tunnel_token: "<token z kroku 1>"
```

Zaszyfruj:
```bash
ansible-vault encrypt group_vars/staging/vault.yml
git add --force group_vars/staging/vault.yml
```

### Krok 3 — Utwórz vars.yml

Utwórz `group_vars/staging/vars.yml`. Minimalna zawartość:

```yaml
env_name: staging

ansible_host:                 "{{ vault_ansible_host }}"
ansible_user:                 reachsky
ansible_ssh_private_key_file: ~/.ssh/id_reachsky

url_frontend: "https://staging.reachsky.pl"
url_api:      "https://staging-api.reachsky.pl"
url_auth:     "https://staging-auth.reachsky.pl"

verify_public:     false
public_health_url: "{{ url_api }}/actuator/health"

# Limity RAM — dostosuj do dostępnych zasobów
mem_postgres:    "256m"
mem_keycloak:    "512m"
mem_backend:     "384m"
mem_frontend:    "64m"
mem_cloudflared: "64m"

jvm_max_ram_pct: 75.0

db_password:             "{{ vault_db_password }}"
kc_admin_password:       "{{ vault_kc_admin_password }}"
cloudflare_tunnel_token: "{{ vault_cloudflare_tunnel_token }}"
```

### Krok 4 — Dodaj host do inventory.yml

```yaml
all:
  children:
    prod:
      hosts:
        reachsky-prod:
    demo:
      hosts:
        reachsky-demo:
    staging:                   # ← dodaj ten blok
      hosts:
        reachsky-staging:
```

### Krok 5 — Realm Keycloak

Wyeksportuj realm ze środowiska źródłowego i umieść w:
```
deploy/keycloak/staging/realm.json
```

Sprawdź checklistę „Realm — checklistа przed commitem" w tym pliku.

### Krok 6 — Skonfiguruj serwer

```bash
cd deploy/
ansible-playbook site.yml -l staging
```

### Krok 7 — Pierwsze wdrożenie

```bash
ansible-playbook deploy.yml -l staging \
  -e "backend_tag=<sha> frontend_tag=<sha>"
```

### Weryfikacja

```bash
# Sprawdź kontenery (przez Ansible):
ansible staging -m ansible.builtin.command \
  -b -a "docker ps --format 'table {{.Names}}\t{{.Status}}'"

# Oczekiwane nazwy kontenerów:
# reachsky-staging-postgres-1    Up X minutes (healthy)
# reachsky-staging-keycloak-1    Up X minutes (healthy)
# reachsky-staging-backend-1     Up X minutes (healthy)
# reachsky-staging-frontend-1    Up X minutes (healthy)
# reachsky-staging-cloudflared-1 Up X minutes
```
