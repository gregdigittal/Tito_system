# TiDB Cloud (and datasource) setup for api-service

This doc describes where the datasource / TiDB is configured and how to point your backend copy at **TiDB Cloud** (or any TiDB/MySQL) for local runs and for hosted runs (e.g. Render).

---

## Where datasource / TiDB is configured

| Location | Purpose |
|----------|--------|
| **common-sqldb/src/main/resources/sqldb.yml** | Defines Spring datasource and Liquibase for all profiles. Profile **dev-k8s** uses `${tidb.url}`, `${tidb.username}`, `${tidb.password}`. Profile **local** uses fixed MySQL localhost. |
| **api-service/src/main/resources/application-tidb-cloud.yml** | Optional file (gitignored) that sets `tidb.url`, `tidb.username`, `tidb.password` for TiDB Cloud. Used when you run with profiles **dev-k8s,tidb-cloud**. |
| **api-service/src/main/resources/application-tidb-cloud.yml.example** | Template. Copy to `application-tidb-cloud.yml` and fill in your TiDB Cloud host, port, database, username, password. **Do not commit** the real file. |

**Flow:** When you run api-service with **dev-k8s,tidb-cloud**:

1. **dev-k8s** (in `common-sqldb/sqldb.yml`) sets `spring.datasource.url=${tidb.url}`, `username=${tidb.username}`, `password=${tidb.password}`.
2. **tidb-cloud** loads `application-tidb-cloud.yml`, which provides those `tidb.*` values (or they come from environment variables).

So TiDB Cloud URL and credentials are set either in **application-tidb-cloud.yml** (local) or via **environment variables** (e.g. Render).

---

## Environment variables

Spring Boot maps these to `tidb.url`, `tidb.username`, `tidb.password` (relaxed binding):

| Env var | Maps to | Example |
|---------|--------|--------|
| **TIDB_URL** | tidb.url | `jdbc:mysql://xxx.tidbcloud.com:4000/icecash?createDatabaseIfNotExist=true&useSSL=true&serverTimezone=Africa/Harare` |
| **TIDB_USERNAME** | tidb.username | Your TiDB Cloud username (e.g. `prefix.root`) |
| **TIDB_PASSWORD** | tidb.password | Your TiDB Cloud password |

Use these when you **don’t** have a local `application-tidb-cloud.yml` (e.g. on Render, or in CI).

---

## Local run with TiDB Cloud

1. **Get TiDB Cloud connection details**  
   TiDB Cloud Console → your cluster → **Connect** → host, port (often 4000), database name, username, password.

2. **Option A – config file (gitignored)**  
   - Copy `api-service/src/main/resources/application-tidb-cloud.yml.example` to `application-tidb-cloud.yml` (same directory).  
   - Edit `application-tidb-cloud.yml`: set `tidb.url` (full JDBC URL), `tidb.username`, `tidb.password`.  
   - For TiDB Cloud use `useSSL=true` in the URL; add `sslMode=VERIFY_CA&certificateAuthorityPath=...` if you use a CA cert.

3. **Option B – env vars**  
   Set `TIDB_URL`, `TIDB_USERNAME`, `TIDB_PASSWORD` in your shell or IDE run config.

4. **Run api-service** with profiles **dev-k8s,tidb-cloud**:
   ```bash
   ./mvnw -pl api-service -am -DskipTests spring-boot:run -Dspring-boot.run.profiles=dev-k8s,tidb-cloud
   ```
   On first run, Liquibase creates/updates the schema (and test data if `liquibase.change-log-file` is `liquibase-db-changelog-test.xml`).

5. **Keycloak** must be running and the **payments-local** realm imported (see README) for login to work.

---

## Hosted run (e.g. Render)

Do **not** put credentials in a committed file. Use environment variables only:

1. In Render (or your host), set:
   - **TIDB_URL** = full JDBC URL (e.g. `jdbc:mysql://xxx.tidbcloud.com:4000/icecash?createDatabaseIfNotExist=true&useSSL=true&serverTimezone=Africa/Harare`)
   - **TIDB_USERNAME** = TiDB Cloud username  
   - **TIDB_PASSWORD** = TiDB Cloud password  

2. Set **Spring profiles** to **dev-k8s,tidb-cloud** (e.g. in Render: `SPRING_PROFILES_ACTIVE=dev-k8s,tidb-cloud` or in Start Command: `java -jar ... --spring.profiles.active=dev-k8s,tidb-cloud`).

3. In TiDB Cloud **Authorized Networks**, allow your host’s outbound IPs (or “allow all” for a demo).

No `application-tidb-cloud.yml` file is needed on the server; env vars supply `tidb.url`, `tidb.username`, `tidb.password`.

---

## Summary

| Scenario | How to set TiDB Cloud |
|----------|------------------------|
| **Local (file)** | Copy `application-tidb-cloud.yml.example` → `application-tidb-cloud.yml`, fill in tidb.url/username/password. Run with `dev-k8s,tidb-cloud`. |
| **Local (env)** | Set `TIDB_URL`, `TIDB_USERNAME`, `TIDB_PASSWORD`. Run with `dev-k8s,tidb-cloud`. |
| **Render / hosted** | Set env vars `TIDB_URL`, `TIDB_USERNAME`, `TIDB_PASSWORD` and profile `dev-k8s,tidb-cloud`. |

**Config chain:** `common-sqldb/sqldb.yml` (dev-k8s) → reads `tidb.url` / `tidb.username` / `tidb.password` → from `application-tidb-cloud.yml` or from env vars.
