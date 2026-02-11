# Login failing: MongoDB Atlas SSL timeout – definitive fix (Chief Architect + DBA)

When the app shows **"Login Error"** with **"Timed out after 30000 ms"**, **MongoClientDelegate**, and **SSLException: (internal_error) Received fatal alert: internal_error** for Atlas hosts `ac-om7smlj-shard-00-*.mongodb.net`, the backend **cannot complete the login flow** because it uses **MongoDB for login-session data** (LoginData). The connection from Render to Atlas is failing at the **TLS handshake**.

---

## End-to-end flow (why MongoDB is involved)

| Step | Component | Role |
|------|-----------|------|
| 1 | Flutter app | Sends login (id_number + password) to api-service |
| 2 | api-service | **EntityLoginServiceImpl** → TiDB (entity/account) + **Keycloak** (token) |
| 3 | api-service | **MfaServiceImpl** → **MongoDB** (LoginData: MFA state, token, OTP) |
| 4 | Response | Success or error back to app |

So **login depends on both TiDB and MongoDB**. The error you see is from step 3 when the Mongo client tries to talk to Atlas and the TLS handshake fails.

---

## Fix in order (do all that apply)

### 1. Use the **non-SRV** connection string (critical for SSL from Render)

The **SRV** string (`mongodb+srv://...`) often triggers TLS/SNI issues from cloud runtimes (e.g. Render). Use the **standard (non-SRV)** URI with explicit hosts.

**Your cluster hostnames** (from your error):  
`ac-om7smlj-shard-00-00.1yw8ckd.mongodb.net`, `ac-om7smlj-shard-00-01.1yw8ckd.mongodb.net`, `ac-om7smlj-shard-00-02.1yw8ckd.mongodb.net`

**Replica set name:** For Atlas cluster `cluster0.1yw8ckd.mongodb.net` it is usually **`atlas0-shard-0`**. Confirm in Atlas: **Database** → **Connect** → **Drivers** → **Java** → look for "Connection string only" or the `replicaSet=` value in the URI.

**Non-SRV URI template:**

```
mongodb://<username>:<password>@ac-om7smlj-shard-00-00.1yw8ckd.mongodb.net:27017,ac-om7smlj-shard-00-01.1yw8ckd.mongodb.net:27017,ac-om7smlj-shard-00-02.1yw8ckd.mongodb.net:27017/?ssl=true&authSource=admin&replicaSet=atlas0-shard-0&connectTimeoutMS=10000&serverSelectionTimeoutMS=10000
```

- Replace **`<username>`** with your Atlas DB user (e.g. `gregm_db_user`).
- Replace **`<password>`** with the **actual password**. If it contains `:`, `@`, `#`, `%`, `/` → **URL-encode** them (`:` → `%3A`, `@` → `%40`, etc.).
- If your replica set is different (e.g. from Atlas UI), replace **`atlas0-shard-0`** with that value.
- Optional: add database in the path, e.g. `/ice-cash` before `?`:  
  `...27017/ice-cash?ssl=true&...`

### 2. Render environment variables

In **Render** → your **Web Service** (api-service) → **Environment**:

| Key | Value |
|-----|--------|
| **SPRING_PROFILES_ACTIVE** | `dev-k8s,tidb-cloud,mongodb-uri` |
| **MONGODB_URI** | The **full non-SRV** string from step 1 (with real password and correct `replicaSet=`). |
| **MONGODB_DATABASE** | Database name (e.g. `ice-cash`). |
| **PORT** | `8281` (if not already set). |

The backend binds **MONGODB_URI** to `mongodb.uri` and uses it when profile **mongodb-uri** is active.

### 3. Java 21 on Render

The Atlas TLS handshake works best with **Java 17+**. Use **Java 21** in your **Dockerfile** (base image, e.g. `eclipse-temurin:21-jre-alpine`). Render does not expose a "Java version" in the UI; it comes from the image. See **docs/RENDER_JAVA_VERSION.md**.

In Render **Logs**, confirm: **"Starting ApiApplication using Java 21"**. If you see Java 8 or 11, the image in use is not the one with Java 21.

### 4. Atlas: Network Access and cluster state

- **Network Access:** Allow Render → **Allow access from anywhere** `0.0.0.0/0` (for testing) or add Render’s outbound IPs.
- **Cluster:** Not paused. Resume if needed.

### 5. Optional: TLS debug on Render

To see the exact TLS failure, temporarily set in Render **Environment**:

**JAVA_TOOL_OPTIONS** = `-Djavax.net.debug=ssl`

Redeploy and reproduce login; then check **Logs** for the SSL handshake trace. Remove this after debugging.

---

## Verification

1. Save Environment on Render and **Manual Deploy** (or push to trigger deploy).
2. Wait for the service to be **Live**.
3. In Render Logs: no "Timed out... MongoClientDelegate" or "SSLException: internal_error" on startup or first request.
4. Try login again from the app (same id_number/password as before).

---

## If you cannot fix MongoDB right now: login without MongoDB

If you need login to work **before** fixing Atlas (e.g. to test the rest of the app), you can run the api-service **without MongoDB** so that login uses **TiDB + Keycloak only** and login-session data is kept in memory. See **docs/LOGIN_WITHOUT_MONGODB.md**. Use this only for temporary/testing; session data is lost on restart and not shared across instances. Other Mongo-dependent features (e.g. payment/OTP persistence) will not work with this profile.

---

## Summary

| Cause | Fix |
|-------|-----|
| SRV URI + Render/Atlas TLS | Use **non-SRV** URI with explicit hosts and `ssl=true&replicaSet=atlas0-shard-0` (or your replica set name). |
| Wrong replica set | Set **replicaSet=** in the URI to the value shown in Atlas for your cluster. |
| Old Java | Use **Java 21** in the Dockerfile and confirm in Render logs. |
| Network / cluster | Atlas Network Access allow Render; cluster not paused. |
| Password special chars | URL-encode password in the URI or use a password without `:`, `@`, `#`, `%`, `/`. |
