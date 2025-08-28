# Oto projekt demonstracyjny: **feature flagi z Consul KV** + **dwie apki**:

* `provider-service` – serwuje dwa warianty API: `/v1/data` i `/v2/data`.
* `router-service` – jedno API `/api/data`, które **przełącza** się pomiędzy v1↔v2 bazując na **kluczu w Consul KV** i korzysta z Twojej **libki** `ff-consul-starter`.

Całość to **multi-module Maven** z parentem, OpenFeign, autokonfiguracją Spring Boot 3.

---

## Spis treści

1. [Wymagania](#wymagania)
2. [Uruchom Consul (Docker)](#uruchom-consul-docker)
3. [Build wszystkich modułów](#build-wszystkich-modułów)
4. [Uruchomienie aplikacji](#uruchomienie-aplikacji)
5. [Klucz feature flag w Consul KV](#klucz-feature-flag-w-consul-kv)
6. [Szybki test przełączania v1 ↔ v2](#szybki-test-przełączania-v1--v2)
7. [Architektura i co gdzie jest](#architektura-i-co-gdzie-jest)
8. [Konfiguracja libki (ff-consul-starter)](#konfiguracja-libki-ff-consul-starter)
9. [Troubleshooting (typowe potknięcia)](#troubleshooting-typowe-potkniecia)
10. [Dalsze kroki](#dalsze-kroki)

---

## Wymagania

* **Java 17+**
* **Maven 3.8+**
* **Docker** (do lokalnego Consula)
* Porty lokalne wolne: `8500` (Consul), `8081` (provider), `8080` (router)

---

## Uruchom Consul (Docker)

W repo jest `docker-compose.yml` i `config/server.hcl`.

```bash
docker compose up -d
```

* UI: [http://localhost:8500](http://localhost:8500)
* KV API: [http://localhost:8500/v1/kv](http://localhost:8500/v1/kv)

> Domyślnie ACL jest **wyłączone** (zobacz `config/server.hcl`). Do dev to wystarczy. Jeśli chcesz ACL – patrz sekcja „Dalsze kroki”.

---

## Build wszystkich modułów

W katalogu głównym (tam gdzie `pom.xml` — **parent**):

```bash
mvn clean package -DskipTests
```

Zbuduje:

* `lib/ff-consul-starter-0.1.0.jar`
* `provider-service/target/provider-service-0.1.0.jar`
* `router-service/target/router-service-0.1.0.jar`

---

## Uruchomienie aplikacji

W osobnych terminalach:

**1) provider-service (8081):**

```bash
java -jar provider-service/target/provider-service-0.1.0.jar
```

**2) router-service (8080):**

```bash
java -jar router-service/target/router-service-0.1.0.jar
```

---

## Klucz feature flag w Consul KV

Router czyta **względny** klucz: `pricing/demo/version`
Libka dokleja prefix z konfiguracji:

```
basePath: CLP/ff
env:      prod
=> pełna ścieżka: CLP/ff/prod/pricing/demo/version
```

Ustaw klucz (v1 / v2):

```bash
# v1
curl -X PUT --data-binary "v1" \
  http://localhost:8500/v1/kv/CLP/ff/prod/pricing/demo/version

# v2
curl -X PUT --data-binary "v2" \
  http://localhost:8500/v1/kv/CLP/ff/prod/pricing/demo/version

# podgląd raw
curl http://localhost:8500/v1/kv/CLP/ff/prod/pricing/demo/version?raw
```

---

## Szybki test przełączania v1 ↔ v2

1. Sprawdź `provider` bezpośrednio:

```bash
curl http://localhost:8081/v1/data
curl http://localhost:8081/v2/data
```

2. Sprawdź `router`:

```bash
# odczyt bieżącej wartości FF
curl http://localhost:8080/api/ff/version

# ruch przez router (zobaczysz "routedVersion" i payload z providera)
curl http://localhost:8080/api/data | jq
```

3. Przełącz w Consulu `v1` → `v2` (patrz wyżej).
   Powtórz `curl http://localhost:8080/api/data` — **bez restartu** routera zobaczysz inną wersję.

---

## Architektura i co gdzie jest

**Repo modułowe:**

```
.
├─ pom.xml                               # parent + modules
├─ lib/                                   # ff-consul-starter (autokonfiguracja)
│  ├─ src/main/java/com/mknieszner/ffconsul/
│  │  ├─ FeatureFlags.java               # interfejs
│  │  ├─ FfConsulProperties.java         # @ConfigurationProperties
│  │  ├─ FfConsulAutoConfiguration.java  # @AutoConfiguration -> rejestruje bean FeatureFlags
│  │  ├─ ConsulFeatureFlags.java         # implementacja (cache + watch/poll)
│  │  ├─ ConsulKvClient.java             # HTTP do /v1/kv
│  │  └─ model/ConsulKvEntry.java
│  └─ src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
│       ↳ com.mknieszner.ffconsul.FfConsulAutoConfiguration
├─ provider-service/                      # demo upstream z v1/v2
│  └─ src/main/java/com/mknieszner/provider/{ProviderApplication,DataController}.java
└─ router-service/                        # router z Feign i flagami
   └─ src/main/java/com/example/router/
      ├─ RouterApplication.java           # @EnableFeignClients
      ├─ ProviderClient.java             # Feign do providera
      └─ RouterController.java           # używa FeatureFlags do wyboru v1/v2
```

**Endpointy:**

* Provider:

    * `GET /v1/data`
    * `GET /v2/data`
* Router:

    * `GET /api/ff/version` – podgląd wartości FF
    * `GET /api/data` – zwraca `{"routedVersion": "...", "upstream": {...}}`

---

## Konfiguracja libki (`ff-consul-starter`)

`router-service/src/main/resources/application.yml`:

```yaml
ff:
  consul:
    enabled: true
    url: http://localhost:8500
    basePath: CLP/ff     # bez końcowego slasha
    env: prod            # jeśli puste → brak segmentu /prod/
    ttlSeconds: 30       # TTL cache
    useBlockingQueries: true      # włączone watch (blocking queries)
    blockingWaitSeconds: 55
    timeoutMs: 500

provider:
  baseUrl: http://localhost:8081

spring:
  cloud:
    openfeign:
      compression.request.enabled: true
      compression.response.enabled: true
```

**Jak używać w kodzie:**

```java
// wstrzykujesz
private final FeatureFlags ff;

// czytasz względny klucz (bez basePath/env):
String version = ff.getString("pricing/demo/version", "v1");
```

> Uwaga: **nie podawaj** w kodzie pełnej ścieżki `CLP/ff/prod/...`. Libka sama dokleja `basePath` + `env`.

---

## Troubleshooting (typowe potknięcia)

**„Bean FeatureFlags nie został znaleziony”**
→ W libce musi istnieć plik:

```
lib/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

z zawartością:

```
com.mknieszner.ffconsul.FfConsulAutoConfiguration
```

oraz w YAML `ff.consul.enabled=true`.

**Czytamy z `CLP/ff/pricing/...` zamiast `CLP/ff/prod/...`**
→ Sprawdź `ff.consul.env: prod`.
Nie wstawiaj `prod` do `basePath` **i jednocześnie** do `env` (powstanie `.../prod/prod/...`).
Klucze w kodzie podawaj **względne** (bez `CLP/ff` oraz bez `prod`).

**Consul działa, ale router nie reaguje od razu na zmianę**

* Przy `useBlockingQueries=true` zmiany powinny być szybkie (long-poll).
* Przy `false` aktualizacja nastąpi co `refreshSeconds` (domyślnie 20s).
* Zawsze jest też cache TTL (`ttlSeconds`).

**W IntelliJ widzę `.idea/misc.xml` w Local Changes mimo `.gitignore`**
→ Pliki zostały kiedyś **zacommitowane**. Zrób:

```bash
git rm -r --cached .idea
git commit -m "remove IDE files; they are ignored"
```

**Feign wywala przy braku providera**
→ Dodaj fallback lub prosty retry (poza zakresem demo).

---

## Dalsze kroki

* **Stickiness per request/order** – zapisz wybraną wersję przy obiekcie domenowym (deterministyczny flow).
* **Tryb `mode`** (`ON/OFF/SHADOW/MIRROR`) + `mirror_pct` – rozbuduj router o kanarkowanie / shadow-run.
* **ACL Consula** – włącz w `server.hcl`, wygeneruj token (`consul acl bootstrap`), dodaj `ff.consul.token`.
* **Observability** – metryki (Micrometer) `ff.cache.hit/miss`, logowanie zmian wartości.
* **Backstage/README dla zespołu** – opis konwencji kluczy, RACI, „time-bomb” (`expires_at`) dla rolloutów.

---
