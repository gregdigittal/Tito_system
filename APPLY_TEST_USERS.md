# Apply Test Users (Keycloak + Liquibase)

How to apply the Tito test users and admin user so anyone can log in with the credentials in [TEST_USERS.md](./TEST_USERS.md).

---

## Which database is used?

- **Profile `local`:** Uses **MySQL** at `localhost:3306`, database **`icecash`**, user `root`, password `123456` (see `common-sqldb/src/main/resources/sqldb.yml`). This is the MySQL from `docker compose` (service `mysql`) or a local MySQL install.
- **Profile `dev-k8s,tidb-cloud`:** Uses **TiDB Cloud** (or any MySQL-compatible URL) via `api-service/src/main/resources/application-tidb-cloud.yml` (gitignored). Set `tidb.url`, `tidb.username`, `tidb.password` and ensure `liquibase.change-log-file: liquibase-db-changelog-test.xml` so the test users changelog runs.
- **Other profiles (e.g. `dev-k8s`):** Use `tidb.url` / `mariadb.url` from config (e.g. k8s ConfigMap). To create test users, the api-service must run with a changelog that includes the test users (e.g. set `liquibase.change-log-file` to `liquibase-db-changelog-test.xml` for that environment).

So: **DB = MySQL for local** (docker or local install); **DB = TiDB or your hosted MySQL** when you use the profile that points at your existing setup. If you have “all the DB’s set up” (e.g. TiDB Cloud), use the profile that connects to that DB and ensure the **test changelog** is used so Liquibase runs `payments-tito-test-users.xml` and `payments-tito-admin-staff.xml`.

---

## Steps to apply

### 1. Keycloak – realm import (admin user `gregm`)

The backoffice user **gregm** (password 123456) is defined in **`backoffice-local-realm.json`**. The payments realm is **`payments-local-realm.json`**.

**Option A – Docker (local)**  
- Realm files are copied into **`infrastructure/keycloak/realms/`** so Keycloak 24 can import them if the image supports it:
  - Start Keycloak:  
    `docker compose up -d postgres keycloak24`
  - If the Bitnami Keycloak 24 image does not auto-import from that folder, import manually: Keycloak Admin → Create realm → Import → select `payments-local-realm.json` and `backoffice-local-realm.json` (or import from `infrastructure/keycloak/realms/`).

**Option B – Hosted Keycloak**  
- In your hosted Keycloak admin UI, create realms and import the two JSON files (or the contents of `infrastructure/keycloak/realms/`).

**Result:** Realms `payments-local` and `backoffice-local` exist; **gregm** exists in `backoffice-local` with password **123456**.

---

### 2. Liquibase – test users and staff row

Liquibase runs when the **api-service** starts, using the changelog set for the active profile. The **test** changelog (`liquibase-db-changelog-test.xml`) includes:

- **Payments test users** (1111111111111111, 2222222222222222, 4444444444444444, 5555555555555555): created in the **DB** and in **Keycloak** by `RegisterUserDbMigrationTask` (so Keycloak must be reachable when the api-service starts).
- **Staff row for gregm:** `payments-tito-admin-staff.xml` inserts the `staff` row for gregm.

**2.1 Point the api-service at your DB**

- **Local MySQL:**  
  Use profile **`local`**. Ensure MySQL is running (e.g. `docker compose up -d mysql`) and that the DB **icecash** exists (created automatically if `createDatabaseIfNotExist=true`).
- **Your existing DB (e.g. TiDB Cloud):**  
  Use the profile that configures your JDBC URL (e.g. **`dev-k8s,tidb-cloud`**) and ensure that profile (or an overlay like `application-tidb-cloud.yml`) sets:
  - `spring.datasource.url` (or `tidb.url`) to your DB
  - `liquibase.change-log-file: liquibase-db-changelog-test.xml`

**2.2 Keycloak reachable**

- Set api-service so it can reach Keycloak (e.g. `auth.keycloak.url`, `auth.trusted-issuers`). For local: `http://localhost:8181` (Keycloak 24) or `8180` (jboss/keycloak).

**2.3 Run the api-service once**

From the j-payments repo root:

```bash
# Local MySQL + local Keycloak
docker compose up -d mysql postgres keycloak24
# Wait for Keycloak to be up, then:
./mvnw -pl api-service -am -DskipTests spring-boot:run -Dspring-boot.run.profiles=local
```

Or with your hosted DB (example TiDB Cloud):

```bash
# Keycloak must already be running and reachable
./mvnw -pl api-service -am -DskipTests spring-boot:run -Dspring-boot.run.profiles=dev-k8s,tidb-cloud
```

(Use your actual profile and config file that points at your DB and test changelog.)

- On first run, Liquibase applies the test changelog (including the four payments test users and the gregm staff row).  
- **RegisterUserDbMigrationTask** creates the four payments users in **Keycloak**; the **gregm** user is already in Keycloak from the realm import.

**2.4 Optional: run Liquibase only**

If you only want to run Liquibase without keeping the api-service up, start the api-service with the correct profile and DB + Keycloak reachable; once Liquibase has run, you can stop the service.

---

## Hosted Docker options (to make this work in the cloud)

You need (1) a **database** (MySQL-compatible), (2) **Keycloak**, and (3) the **api-service** to run at least once so Liquibase (and the migration task) run. Below are hosted options that can run Docker or connect to your existing DB.

### Database (you said you have these set up)

- **TiDB Cloud** – MySQL-compatible; use profile `dev-k8s,tidb-cloud` (or your overlay) with `tidb.url` / `tidb.username` / `tidb.password`.
- **PlanetScale, AWS RDS MySQL, Azure Database for MySQL, etc.** – Use a Spring profile that sets `spring.datasource.url` (and username/password) to your JDBC URL and `liquibase.change-log-file: liquibase-db-changelog-test.xml`.

No Docker required for the DB if you use a hosted DB.

### Keycloak (hosted options)

- **Keycloak on Render / Fly.io / Railway / Cloud Run** – Run the Keycloak Docker image as a service; mount or bake in the realm JSONs, or import them after first start.
- **Keycloak as a Service** – e.g. **Keycloak Cloud** (keycloak.ch), **Auth0** (different product but can replace Keycloak), **Cloud IAM** – then reconfigure api-service `auth.keycloak.url` and `auth.trusted-issuers` to match.
- **Self-hosted on a VPS** – DigitalOcean Droplet, AWS EC2, etc., with Docker; run Keycloak and import the two realm JSONs.

### Api-service (hosted Docker / runtime options)

- **Render** – Web Service; connect to your existing DB and Keycloak via env vars; run the api-service (e.g. JAR or Docker). Run at least once so Liquibase runs; you can trigger a deploy to do that.
- **Fly.io** – Run the api-service as an app; set env (DB URL, Keycloak URL, etc.); deploy once to run Liquibase.
- **Railway** – Deploy from repo or Dockerfile; add MySQL/Postgres/Keycloak from the catalog or use external DB + Keycloak; set env and deploy.
- **Google Cloud Run** – Run the api-service as a container; set env to your DB and Keycloak; one run executes Liquibase.
- **AWS ECS/Fargate** – Run api-service (and optionally Keycloak) as tasks; use RDS or TiDB for DB; run the service once so Liquibase applies.
- **DigitalOcean App Platform** – Deploy api-service; attach a managed DB or use your TiDB URL; point to hosted Keycloak.

### Minimal “make it work” with hosted Docker

1. **DB:** Use your existing TiDB (or other MySQL-compatible) – no Docker needed.
2. **Keycloak:** Use one of:
   - A small **Render / Fly.io / Railway** Web Service running the Keycloak Docker image, with the two realm JSONs imported, or
   - A **managed Keycloak** offering, then set api-service auth env to that URL.
3. **Api-service:** Deploy once on **Render / Fly.io / Railway** (or your current host) with:
   - Profile and env set so **JDBC URL** = your DB (TiDB or other),
   - **Keycloak URL** and **trusted issuers** set,
   - **Liquibase** using the **test** changelog (`liquibase.change-log-file: liquibase-db-changelog-test.xml`).

After the first successful startup, Liquibase will have created the test users and the gregm staff row; the four payments users will exist in Keycloak if the migration task could reach Keycloak.

---

## Summary

| What        | Local (Docker)                    | Hosted (you have DB set up)                    |
|------------|------------------------------------|-----------------------------------------------|
| **DB**     | MySQL `localhost:3306/icecash`     | Your TiDB / MySQL (profile + env)             |
| **Keycloak** | `docker compose` keycloak24       | Hosted Keycloak or Docker on Render/Fly/etc.   |
| **Liquibase** | Run api-service with profile `local` | Run api-service with profile that uses your DB + test changelog |
| **Test users** | Created by Liquibase + migration task | Same, once api-service starts with test changelog and Keycloak reachable |

See [TEST_USERS.md](./TEST_USERS.md) for the actual usernames and passwords.
