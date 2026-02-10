# Test Users (Tito)

Test users created by Liquibase (test changelog) and Keycloak realm imports.

## Payments realm (Flutter app – `payments-local`)

Login with **username** = id number, **password** = 4-digit PIN.

| User type           | Username            | Password (PIN) | Notes                          |
|---------------------|---------------------|----------------|--------------------------------|
| General User        | `1111111111111111`  | `1111`         | Individual (CommuterRegular)   |
| Matatu/Taxi Owner   | `2222222222222222`  | `2222`         | Individual (test)              |
| Agents              | `4444444444444444`  | `4444`         | Individual (test)              |
| SACCO               | `5555555555555555`  | `5555`         | Individual (test)              |
| (existing)          | `3333333333333333`  | `3333`         | John Doe (fake-user-insert)    |

**Realm:** `payments-local`  
**Client:** `icecash` (direct access grant for login)

## Backoffice realm (Admin – `backoffice-local`)

| Username | Password | Roles        | Notes                    |
|----------|----------|-------------|--------------------------|
| `gregm`  | `123456` | BACKOFFICE, TITO_ADMIN | Admin user; staff row with security_group_id 3001 (Tito Admin) |

**Realm:** `backoffice-local`  
**Staff:** `staff` table row with `email = gregm@tito.local`, `keycloak_id` set to Keycloak user id.

## Applying

1. **Liquibase:** Run api-service once with profile that includes the test changelog (e.g. `local` with `liquibase-db-changelog-test.xml`) so the test users and staff row are created.
2. **Keycloak:** Import `payments-local-realm.json` and `backoffice-local-realm.json` (or re-import after adding users). For `gregm`, the password is in the realm JSON; for payments users, Liquibase `RegisterUserDbMigrationTask` creates Keycloak users when the changelog runs.

**Note:** If Keycloak is imported before Liquibase runs, the payments test users (1111…, 2222…, etc.) are created by `RegisterUserDbMigrationTask` when the api-service starts with the test changelog. The existing user 3333333333333333 is created the same way. Re-importing the realm overwrites Keycloak users; ensure Liquibase runs after realm import if both define users.
