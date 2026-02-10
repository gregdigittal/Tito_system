# Render: TiDB and MongoDB (both required)

The **api-service** uses **both** TiDB (MySQL) and MongoDB. Both must be configured and reachable for the service to start and for login to work.

---

## TiDB (relational: entity, accounts, users, Liquibase)

- **Config:** `common-sqldb/src/main/resources/sqldb.yml` (profile **dev-k8s** uses `tidb.url`, `tidb.username`, `tidb.password`).
- **Render env:** **TIDB_URL**, **TIDB_USERNAME**, **TIDB_PASSWORD**.
- **Profiles:** **dev-k8s,tidb-cloud** (see [TIDB_CLOUD_SETUP.md](../TIDB_CLOUD_SETUP.md)).

---

## MongoDB (collections: PaymentRequest, PaymentResponse, OtpData, MozLinkTagData, etc.)

- **Config:** `common/src/main/resources/mongodb.yml`. Either:
  - **Default:** Builds `spring.data.mongodb.uri` from **mongodb.host**, **mongodb.username**, **mongodb.password**, **mongodb.auth-db**, **mongodb.database**.
  - **Profile `mongodb-uri`:** Uses **mongodb.uri** and **mongodb.database** directly (for Atlas or any full connection string). Set **SPRING_PROFILES_ACTIVE** to include `mongodb-uri`, and set **MONGODB_URI** (and **MONGODB_DATABASE**) on Render.
- **Application:** `ApiApplication` loads config names: `application,security,kafka,mongodb,sqldb,cache,logging` — so `mongodb.yml` is always loaded.
- **Render env (default):** **MONGODB_HOST**, **MONGODB_USERNAME**, **MONGODB_PASSWORD**, **MONGODB_DATABASE**, **MONGODB_AUTH_DB** (for Atlas, use **MONGODB_AUTH_DB** = `admin?ssl=true`).
- **Render env (Atlas recommended):** Use profile **mongodb-uri** and set **MONGODB_URI** = full Atlas connection string, **MONGODB_DATABASE** = your DB name. Atlas → Network Access: allow **0.0.0.0/0** so Render can connect.

---

## If login times out with a MongoDB error

The UI may show an error like: *"Timed out after 30000 ms while waiting for a server... MongoClientDelegate... mongodb.net"*. That means the **MongoDB** connection from Render is failing (e.g. Atlas unreachable or SSL failure). Fix MongoDB connectivity (Atlas network access, correct URI/auth-db, or use a MongoDB instance Render can reach); keep TiDB and MongoDB env vars set and redeploy.
