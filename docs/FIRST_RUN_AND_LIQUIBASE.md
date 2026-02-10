# Tito_system (api-service): First run and Liquibase runbook

Concise runbook for the first run of api-service and for fixing Liquibase lock. For TiDB Cloud or Render, see [TIDB_CLOUD_SETUP.md](../TIDB_CLOUD_SETUP.md) and [RENDER_DATABASES.md](./RENDER_DATABASES.md) / [RENDER_ENTITY_LOGIN_ENV.md](./RENDER_ENTITY_LOGIN_ENV.md).

---

## First run order (local)

1. **Start dependencies**
   - **MySQL/MariaDB** (or TiDB): schema will be created by Liquibase.
   - **MongoDB**: required for api-service (collections created on use).
   - **Keycloak**: required for login; realm must be imported (see below).

2. **Keycloak realm (local)**
   - Open Keycloak admin (e.g. http://localhost:8180/) → Admin Console → add realm.
   - **Import** the `payments-local-realm.json` from the project root (name: **payments-local**).
   - For Tito test user (entity login), ensure the realm has a user **entity_30000001** with password **333a!** and direct access grants ON for the client used by the backend (see [RENDER_ENTITY_LOGIN_ENV.md](./RENDER_ENTITY_LOGIN_ENV.md) for hosted).

3. **Run api-service first**
   - **Liquibase** runs inside api-service on startup and creates/updates the relational schema.
   - Use Spring profile **local** (or **dev-k8s,tidb-cloud** for TiDB Cloud; see [TIDB_CLOUD_SETUP.md](../TIDB_CLOUD_SETUP.md)).
   - Example (TiDB Cloud):  
     `./mvnw -pl api-service -am -DskipTests spring-boot:run -Dspring-boot.run.profiles=dev-k8s,tidb-cloud`

4. **After first run (local)**
   - A test user may be created (see main [README.md](../README.md)); for Tito UI, seed the entity with `id_number = '3333333333333333'` and use Keycloak user **entity_30000001** with password **333a!**.

---

## Liquibase lock: how to unlock

If api-service is killed or errors during Liquibase run, the database can stay **locked** and the next startup will wait on the lock.

**Unlock manually:**

1. Connect to the **same database** api-service uses (MySQL, MariaDB, or TiDB).
2. Open the table **DATABASECHANGELOGLOCK** (in the same database/schema as the app).
3. Set **LOCKED** = `0` (or `false`) and set **LOCKGRANTED** and **LOCKEDBY** to `NULL` (if applicable).
   - Example (SQL):  
     `UPDATE DATABASECHANGELOGLOCK SET LOCKED = 0, LOCKGRANTED = NULL, LOCKEDBY = NULL;`
4. Restart api-service; Liquibase will run again (or skip already-applied changesets).

---

## Where Liquibase is configured

| Item | Location |
|------|----------|
| Changelog file (local test data) | `api-service/src/main/resources/db/changelog/liquibase-db-changelog-test.xml` |
| Changelog (schema only) | `api-service/src/main/resources/db/changelog/liquibase-db-changelog.xml` |
| Property (profile) | `common-sqldb/src/main/resources/sqldb.yml` → `liquibase.change-log-file` per profile |

---

## Hosted (Render)

- **TiDB:** Set **TIDB_URL**, **TIDB_USERNAME**, **TIDB_PASSWORD**; use profiles **dev-k8s,tidb-cloud** ([TIDB_CLOUD_SETUP.md](../TIDB_CLOUD_SETUP.md)).
- **MongoDB:** Set **SPRING_DATA_MONGODB_URI** (or **MONGODB_URI** with profile **mongodb-uri**) and **SPRING_DATA_MONGODB_DATABASE** ([RENDER_DATABASES.md](./RENDER_DATABASES.md)).
- **Keycloak:** [RENDER_ENTITY_LOGIN_ENV.md](./RENDER_ENTITY_LOGIN_ENV.md).

Liquibase runs on first deploy against TiDB; ensure the DB user has rights to create/alter tables.
