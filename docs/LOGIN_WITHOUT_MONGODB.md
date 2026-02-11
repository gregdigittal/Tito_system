# Login without MongoDB (temporary / testing)

If you **cannot fix the MongoDB Atlas connection** right now (e.g. SSL/timeout from Render) and need login to work for testing, you can run the api-service **without MongoDB**. Login will use **TiDB + Keycloak only**; login/MFA session data is kept **in memory** and is lost on restart.

---

## Limitations

- **Session data is in memory** – lost on restart; not shared across multiple instances.
- **Other Mongo-dependent features will not work** – e.g. payment requests, OTP storage, any flow that uses Mongo collections. Use only to verify **login + token** until Atlas is fixed.

---

## How to enable

On **Render** (or your deployment), set **SPRING_PROFILES_ACTIVE** to include **no-mongodb** and **remove** **mongodb-uri** and **MONGODB_URI** so the app does not try to connect to Atlas.

Example:

| Key | Value |
|-----|--------|
| **SPRING_PROFILES_ACTIVE** | `dev-k8s,tidb-cloud,no-mongodb` |
| **MONGODB_URI** | *(remove or leave empty)* |

Redeploy. The app will start without creating a Mongo client. Login uses TiDB (entity/account) and Keycloak (token); MFA/login state uses the in-memory store.

---

## Recommended: fix MongoDB instead

For a permanent fix, use **[LOGIN_FIX_ATLAS_RENDER_DEFINITIVE.md](./LOGIN_FIX_ATLAS_RENDER_DEFINITIVE.md)** (non-SRV URI, Java 21, replica set name, Network Access). Then remove **no-mongodb** from the profile and set **MONGODB_URI** again.
