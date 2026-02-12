# Render: Entity login (Phase Two Keycloak)

When the api-service runs on Render and entity login uses **Phase Two** Keycloak (realm `payments-local`), set these environment variables so the backend can obtain a token with username/password (Direct access grants).

## Required for entity login

| Env var | Description | Example |
|--------|-------------|--------|
| **AUTH_KEYCLOAK_URL** | Keycloak base URL (trailing slash) | `https://euc1.auth.ac/auth/` |
| **AUTH_KEYCLOAK_REALM** | Realm for entity users | `payments-local` |
| **AUTH_KEYCLOAK_ENTITIES_CLIENT_ID** | Client ID used for password grant (Direct access grants ON) | `tito-api` |
| **AUTH_KEYCLOAK_ENTITIES_CLIENT_SECRET** | That client’s secret (Phase Two → Clients → tito-api → Credentials) | *(from Keycloak)* |
| **AUTH_KEYCLOAK_ADMIN_CLIENT_ID** | Admin client for user management | e.g. `admin-cli` |
| **AUTH_KEYCLOAK_ADMIN_CLIENT_SECRET** | Admin client secret | *(from Keycloak)* |
| **AUTH_TRUSTED_ISSUERS** | JWT issuer URL | `https://euc1.auth.ac/auth/realms/payments-local` |

The entity login flow uses **resource owner password** grant. The client identified by `AUTH_KEYCLOAK_ENTITIES_CLIENT_ID` must have **Direct access grants** enabled in Phase Two.

## If login fails (“No token received”)

1. In **Render logs**, search for:
   - **KEYCLOAK_LOGIN_ATTEMPT** — confirms a login was tried and shows realm, clientId, URL in use.
   - **KEYCLOAK_LOGIN_FAILED** — shows the exact reason (e.g. config missing, exception from Keycloak).
2. If you see “config missing” (clientId or clientSecret empty), set the env vars below and redeploy.
3. If you see an exception (e.g. 401, invalid_client, Direct access grants disabled), fix that in Phase Two or the env (URL, realm, client secret).
4. Confirm Phase Two: client has Direct access grants ON, user exists and password is correct, user is enabled.
5. **Redeploy** tito-api after changing any env var.

### Env vars when not using a k8s profile

If Render does **not** use a profile like `prod-k8s`, set these instead (Spring Boot prefix `ice.cash.keycloak.entities`):

| Env var | Value |
|--------|--------|
| **ICE_CASH_KEYCLOAK_ENTITIES_DEFAULT_CLIENT_ID** | `tito-api` |
| **ICE_CASH_KEYCLOAK_ENTITIES_DEFAULT_CLIENT_SECRET** | Client secret from Phase Two → tito-api → Credentials |

### Logging (optional)

To reduce Kafka and Spring Kafka log noise in Render logs, set **SPRING_PROFILES_ACTIVE** to include `render` (e.g. `render` or `no-mongodb,render`). The `render` profile sets `org.apache.kafka` and `org.springframework.kafka` to WARN. See `api-service/src/main/resources/application-render.yml`.

---

See also: Tito_UI_Client `docs/LOGIN_NO_TOKEN_RECEIVED.md` and `docs/PHASE_TWO_CLIENT_SETUP.md`.
