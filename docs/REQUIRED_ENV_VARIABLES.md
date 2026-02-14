# Required environment variables (secrets and config)

These variables **must** be set in deployed environments. Do not commit real values to source control.

## Secrets (no defaults in code)

| Variable | Used in | Description |
|----------|---------|-------------|
| **PVV_CIPHER_KEY** | `security.yml` (ice.cash.pvv.hex-cipher-key) | Hex key for PVV cipher. **Required** — no default. Set in all environments. |
| **AUTH_KEYCLOAK_ENTITIES_ADMIN_CLIENT_SECRET** | `security.yml` (local profile, entities admin) | Keycloak admin client secret for entities realm (user management). |
| **AUTH_KEYCLOAK_BACKOFFICE_ADMIN_CLIENT_SECRET** | `security.yml` (local profile, backoffice admin) | Keycloak admin client secret for backoffice realm. |
| **MINIO_ACCESS_SECRET** | `application.yml` (local profile, minio.access-secret) | MinIO S3 access secret. **Required** when using MinIO. |

## Config with weak default (override in deployment)

| Variable | Used in | Description |
|----------|---------|-------------|
| **IP_WHITELIST_PASSWORD** | `application.yml` (ice.cash.ip-whitelist.password) | Password for IP whitelist check. Default `changeme` — **must** be overridden in production. |
| **MINIO_ACCESS_NAME** | `application.yml` (local profile, minio.access-name) | MinIO access key / username. Default `admin`. |

## K8s / Render profiles

When using `dev-k8s`, `uat-k8s`, `prod-k8s` (or similar), Keycloak and auth are typically configured via `auth.*` or `AUTH_*` variables; see [RENDER_ENTITY_LOGIN_ENV.md](RENDER_ENTITY_LOGIN_ENV.md).

## .env.example (for local development)

Create a `.env` file (or set in your IDE / shell) when running with default profiles:

```bash
# PVV cipher — get from secure store; never commit
PVV_CIPHER_KEY=

# Keycloak admin (local Keycloak)
AUTH_KEYCLOAK_ENTITIES_ADMIN_CLIENT_SECRET=
AUTH_KEYCLOAK_BACKOFFICE_ADMIN_CLIENT_SECRET=

# IP whitelist (use a strong value in production)
IP_WHITELIST_PASSWORD=changeme

# MinIO (local)
MINIO_ACCESS_NAME=admin
MINIO_ACCESS_SECRET=
```
