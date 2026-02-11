# Phase 7: Admin backend seed (Tito Admin / Finance Admin)

**Purpose:** Document the backend seed for Phase 7 (security groups, Keycloak roles, staff row) used by the Admin UI.

## 7-1 Liquibase: security groups and rights

**Included in main changelog** (`api-service/src/main/resources/db/changelog/liquibase-db-changelog.xml`):

1. **payments-tito-backoffice-rights.xml**  
   Ensures backoffice `security_right` rows exist (ids 9, 15, 54, 60: BULKPAY, RELEASE_A, RELEASE_B, BULKPAY_LOAD). Idempotent (inserts only when missing). Required for production where test data is not loaded.

2. **payments-tito-finance-admin-groups.xml**  
   Inserts:
   - **security_group** 3001: "Tito Admin" (Tito product administration)
   - **security_group** 3002: "Finance Admin" (Finance and journal administration)  
   Both are linked to rights 9, 15, 54, 60 via **security_group_right**.

3. **payments-tito-admin-staff.xml**  
   Inserts a **staff** row for admin user (e.g. gregm): `security_group_id = 3001`, `keycloak_id` matching Keycloak user in backoffice-local realm.

**Test profile:** The test changelog (`liquibase-db-changelog-test.xml`) already includes `payments-test-data.xml` (which seeds the same security_right and other data) and the same Tito changelogs, so tests get consistent data.

## 7-2 Keycloak: realm roles

Realm **backoffice-local** already defines realm roles in `infrastructure/keycloak/realms/backoffice-local-realm.json`:

- **TITO_ADMIN** – Tito product administration  
- **FINANCE_ADMIN** – Finance and journal administration  
- **BACKOFFICE** – General backoffice user (staff)

To use them:

1. Import the realm (e.g. via Keycloak Admin UI or startup import) so the realm `backoffice-local` exists.
2. Create or update users in that realm and assign realm roles (TITO_ADMIN, FINANCE_ADMIN, BACKOFFICE) as needed.
3. Staff rows in the DB reference `keycloak_id` (UUID) of the Keycloak user; ensure the staff row’s `keycloak_id` matches the user’s Id in Keycloak.

No code changes are required for 7-2; the roles are defined in the realm JSON.

## Summary

| Item | Location | Notes |
|------|----------|--------|
| Backoffice rights (9, 15, 54, 60) | payments-tito-backoffice-rights.xml | Idempotent insert |
| security_group 3001, 3002 | payments-tito-finance-admin-groups.xml | Tito Admin, Finance Admin |
| security_group_right | payments-tito-finance-admin-groups.xml | Both groups → 9, 15, 54, 60 |
| Staff seed (e.g. gregm) | payments-tito-admin-staff.xml | security_group_id 3001, keycloak_id from realm |
| Keycloak roles | backoffice-local-realm.json | TITO_ADMIN, FINANCE_ADMIN, BACKOFFICE |

Next: Phase 7-3–7-5 (Admin UI: Tito_Admin_Client, staff login, staff list, journals, role-based visibility).
