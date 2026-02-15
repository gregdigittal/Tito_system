# CORS and Vercel origins (Tito UI Client)

The **Tito UI Client** (Flutter web) is deployed on Vercel. The browser sends requests to the api-service (tito-api) with an `Origin` header (e.g. `https://tito-ui-client.vercel.app` or a preview URL like `https://tito-ui-client-3j859rdqw-digittal.vercel.app`). The api-service must allow that origin in CORS or the browser blocks the response (403).

## Config (application.yml)

Under **`ice.cash.cors`**:

- **`allowed-origins`**: Exact origins (e.g. `https://tito-ui-client.vercel.app`, localhost).
- **`allowed-origin-patterns`**: Patterns for dynamic origins. We use **`https://*.vercel.app`** so all Vercel deployment URLs (production and preview/branch) are allowed.

Override via env if needed (e.g. `ICE_CASH_CORS_ALLOWED_ORIGINS` for the list property).

## If you see 403 / "Origin not allowed"

1. Ensure the api-service (tito-api) has been redeployed with the latest config that includes `allowed-origin-patterns: https://*.vercel.app`.
2. Or add the specific frontend origin to `allowed-origins` (or set the corresponding env override) and redeploy.
