# Tito test users (TiDB + Keycloak)

Test users for the Tito Flutter app (id_number 1111111111111111, 2222222222222222, 3333333333333333, etc.) are **not** created by Liquibase in normal or test changelog runs. They are created via **script + SQL** so that TiDB entity IDs and Keycloak usernames stay in sync.

---

## Why not Liquibase?

The file **payments-tito-test-users.xml** defines four test users (General User, Matatu Owner, Agents, SACCO) via `RegisterUserDbMigrationTask` and follow-up SQL. Those changesets:

- Reference **account numbers 33333333336–33333333339** for `account_relationship` and `transaction_lines`.
- Those accounts are **not** created by **payments-db-structure.xml** or **payments-test-data.xml**, so the subsequent SQL fails (foreign key or missing row).
- The changelog uses **failOnError="false"**, so Liquibase marks the changeset as run but the entities/accounts are not actually created.

So **do not rely on payments-tito-test-users.xml** to create test users for TiDB Cloud or Render. Use the script-based approach below.

---

## How test users are created (recommended)

1. **TiDB**
   - Ensure schema is applied (Liquibase main changelog: **liquibase-db-changelog.xml**).
   - Insert entities (and accounts if needed) via **SQL** in TiDB Cloud Console or a MySQL client. Example for the seed user (3333333333333333) is in **Tito_UI_Client/scripts/seed_test_entity_tidb.sql** (use the Keycloak ID for your `entity_*` user).

2. **Keycloak**
   - Create users with username **entity_&lt;id&gt;** where `id` is the **TiDB entity.id** (e.g. 30002 → `entity_30030002`; TiDB auto-increment is not sequential, so always derive from actual `entity.id`).
   - Set passwords that meet app policy (e.g. 4+ chars, 1 letter, 1 special; e.g. `333a!`, `111a!`).

3. **Automation**
   - **Tito_UI_Client/scripts/setup_all_test_users_keycloak.sh** discovers entity IDs from TiDB (via pymysql), creates/resets Keycloak users with compliant passwords, and can verify login. Run from the Tito_UI_Client repo:  
     `./scripts/setup_all_test_users_keycloak.sh`

---

## Working test accounts (reference)

After running the script and SQL, you should have users like:

| id_number           | Password | Keycloak username   | Notes        |
|---------------------|----------|----------------------|--------------|
| 3333333333333333    | 333a!    | entity_30030002     | Seed user    |
| 1111111111111111    | 111a!    | entity_30540002     | General User |
| 2222222222222222    | 222a!    | entity_30540003     | Matatu Owner |
| 4444444444444444    | 444a!    | entity_30540004     | Agents       |
| 5555555555555555    | 555a!    | entity_30540005     | SACCO        |

Exact `entity_*` usernames depend on TiDB `entity.id` (backend uses `30000000 + entity.id`). See **EntityClass.keycloakUsername()** and **CONTEXT_SAVE_2026-02-12_LOGIN_401_RESOLVED.md** in Tito_UI_Client/docs.

---

## payments-tito-test-users.xml

The file remains in the repo for reference and for environments where the required accounts (33333333336–33333333339) are created by other means. It is **included only** in **liquibase-db-changelog-test.xml** (not in the main changelog). For TiDB Cloud and Render, use the script and SQL approach above instead of relying on these changesets.
