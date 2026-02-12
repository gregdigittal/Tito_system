# Entity login returns 401 / errorCode 101-IC1146-0010

When the app calls **loginEntity** and gets:

```json
{
  "errors": [{ "message": "HTTP 401 Unauthorized", "extensions": { "errorCode": "101-IC1146-0010", "classification": "UNAUTHORIZED" } }],
  "data": { "loginEntity": null }
}
```

the backend is reporting that **Keycloak rejected the credentials**. The API found the entity in TiDB (by id_number / account number) but the **resource owner password** call to Keycloak failed.

---

## What 101-IC1146-0010 means

- **Unknown ID** – no entity in TiDB for that id_number/account (would usually be a different “user does not exist” style error).
- **Wrong password** – Keycloak user exists but password does not match.
- **Keycloak user missing or disabled** – no user in realm `payments-local` for the entity’s Keycloak username.
- **Client / realm misconfiguration** – wrong client, secret, or Direct access grants disabled.

---

## Keycloak username used for entities

The backend sends this **username** to Keycloak (realm **payments-local**):

- **`entity_` + (entity’s `legacy_account_id` if set, else `30000000 + entity.id`)**

Examples:

- Entity `id = 1`, no `legacy_account_id` → username **`entity_30000001`**.
- Entity with `legacy_account_id = 42` → username **`entity_42`**.

So in Keycloak (Phase Two, realm **payments-local**) there must be a user with that **exact username** and the **same password** the user types in the app.

---

## Checklist (in order)

1. **Render env (entity login)**  
   See **docs/RENDER_ENTITY_LOGIN_ENV.md**. In particular:
   - **AUTH_KEYCLOAK_URL**, **AUTH_KEYCLOAK_REALM** = `payments-local`
   - **AUTH_KEYCLOAK_ENTITIES_CLIENT_ID** (e.g. `tito-api`), **AUTH_KEYCLOAK_ENTITIES_CLIENT_SECRET**
   - **AUTH_TRUSTED_ISSUERS** = `https://<your-keycloak>/realms/payments-local`  
   If you’re not using a k8s profile, use **ICE_CASH_KEYCLOAK_ENTITIES_DEFAULT_CLIENT_ID** and **ICE_CASH_KEYCLOAK_ENTITIES_DEFAULT_CLIENT_SECRET** instead.

2. **Phase Two client**
   - Client (e.g. **tito-api**) has **Direct access grants** (Resource owner password) **ON**.
   - Client secret in Render matches **Credentials** in Keycloak.

3. **Keycloak user for the entity**
   - User exists in realm **payments-local** with username **`entity_30000001`** (or `entity_<legacyAccountId>` for your test entity).
   - User is **Enabled**.
   - **Password** in Keycloak matches what you type in the app (the backend sends the **plain** password to Keycloak).

4. **TiDB entity**
   - Entity exists and **login_status = ACTIVE**.
   - For id_number `3333333333333333`, seed script / docs say to set **id_number** and ensure **keycloak_id** matches the user in Keycloak if you use it elsewhere; login itself uses **username** = `entity_30000001` (or legacy id), not id_number, for the Keycloak call.

5. **Render logs**
   - Search for **KEYCLOAK_LOGIN_ATTEMPT** (shows username, realm, clientId, URL).
   - Search for **KEYCLOAK_LOGIN_FAILED** (exact reason: missing config, 401, invalid_client, etc.).

---

## Quick test (Phase Two)

1. In Keycloak → **payments-local** → **Users** → open user **entity_30000001** (or create it).
2. Set **Credentials** to the same password you use in the app (e.g. **333a!**).
3. Ensure user is **Enabled**.
4. In **Clients** → **tito-api** → **Settings** → **Direct access grants** = ON; **Credentials** tab → copy secret into Render **AUTH_KEYCLOAK_ENTITIES_CLIENT_SECRET** (or ICE_CASH_… equivalent).
5. Redeploy api-service on Render and try login again.

---

## See also

- **RENDER_ENTITY_LOGIN_ENV.md** – all Render env vars for entity login.
- **LOGIN_FIX_ATLAS_RENDER_DEFINITIVE.md** – MongoDB/Atlas issues (different from 401).
- Tito_UI_Client **LOGIN_NO_TOKEN_RECEIVED.md**, **PHASE_TWO_CLIENT_SETUP.md**.
