# Deploy — ReachSky Backend

Wdrożenie przez Ansible + Cloudflare Tunnel na serwer domowy za NAT.
Żaden port (80/443) nie jest wystawiony na router — ruch wychodzi tylko
przez `cloudflared`.

---

## Wymagania wstępne

### Na laptopie (maszyna z której uruchamiasz Ansible)

```bash
# 1. Python 3.10+
python3 --version

# 2. Ansible (core 2.15+)
pip3 install --user ansible

# Weryfikacja:
ansible --version   # powinno pokazać ansible [core 2.15.x]

# 3. Kolekcja community.general (zawiera moduł ufw)
ansible-galaxy collection install community.general
```

### Na serwerze

- Ubuntu 24.04 LTS (x86_64)
- Docker Engine + Docker Compose plugin (już zainstalowane)
- Użytkownik SSH z `sudo` (bez hasła lub z hasłem — patrz niżej)
- Serwer w tej samej sieci lokalnej co laptop (SSH przez LAN)

---

## Pierwsze uruchomienie — krok po kroku

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
├── ansible.cfg                    — konfiguracja Ansible (vault path, inventory)
├── inventory.example.yml          — szablon; skopiuj jako inventory.yml
├── inventory.yml                  — GITIGNOROWANY (zawiera IP serwera)
├── inventory.local.yml            — dla self-hosted runner (localhost)
├── group_vars/prod/
│   ├── vars.yml                   — konfiguracja jawna
│   ├── vault.example.yml          — struktura vaultu (puste wartości)
│   └── vault.yml                  — GITIGNOROWANY niezaszyfrowany;
│                                    po encrypt trafia do repo
├── templates/
│   └── env.j2                     — szablon .env generowanego przez Ansible
├── site.yml                       — jednorazowa konfiguracja serwera
├── deploy.yml                     — wdrożenie z auto-rollbackiem
├── rollback.yml                   — ręczny rollback
├── docker-compose.prod.yml        — definicja kontenerów
├── init/01-keycloak-db.sql        — tworzy bazę keycloak (raz)
└── keycloak/realm.json            — eksport realmu (do dodania przez Ciebie)
```
# test
