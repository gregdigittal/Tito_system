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

1. Check **Render logs** at login time. The api-service now logs Keycloak login failures with realm, clientId, URL and exception message.
2. Confirm Phase Two: client has Direct access grants ON, user exists and password is correct, user is enabled.
3. Confirm env vars are set and **Redeploy** after changing them.

See also: Tito_UI_Client `docs/LOGIN_NO_TOKEN_RECEIVED.md` and `docs/PHASE_TWO_CLIENT_SETUP.md`.
