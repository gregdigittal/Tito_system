# Phase 8-1: Regional Tito Wallet top-up service selection

**Goal:** Backend supports selecting which top-up service(s) are available per country so the Tito wallet top-up flow can show the correct options (e.g. M-Pesa for KE, M-Pesa/Emola for MZ) and avoid invalid provider choices.

## Current state

- **GraphQL:** `topupAccountMoz(accountNumber, provider, mobile, amount)` in `EntityMozController`; `provider` is enum `MoneyProviderMoz` (MPESA, EMOLA).
- **REST (UAT):** `POST /api/v1/moz/account/topup` with account, amount, reference (no provider selection).
- **Config:** Deployment config (`GET /api/v1/config/deployment`) already exposes `countryCode` and is used by the frontend. No top-up provider list yet.

## Proposed approach

1. **Backend: service selector**
   - Introduce a `TopUpServiceSelector` (or equivalent) that, given a country code, returns the list of allowed top-up provider identifiers for that country.
   - Default implementation can be config-driven (e.g. `ice.cash.deployment.top-up-providers-by-country`) or hardcoded map (KE → [MPESA], MZ → [MPESA, EMOLA]) until product confirms per-country matrix.
   - Use this in the GraphQL layer (or in `EntityMozServiceImpl.topupAccount`) to validate that the requested `provider` is allowed for the deployment country; return a clear error if not.

2. **Config API (optional extension)**
   - Extend `GET /api/v1/config/deployment` with an optional field `topUpProviderIds` (or `allowedTopUpProviders`) for the current deployment country, so the frontend can show only relevant options without hardcoding.
   - Alternatively, add `GET /api/v1/config/topup-providers?country=KE` returning `{ "countryCode": "KE", "providerIds": ["MPESA"] }` for flexibility.

3. **Implementation order**
   - Add `TopUpServiceSelector` interface and a default impl that uses deployment country (from `DeploymentConfigProperties`) and a config or map of country → provider list.
   - Wire the selector into the top-up flow so invalid provider is rejected with a clear message.
   - Add `topUpProviderIds` (or similar) to deployment config DTO and properties when product confirms the per-country matrix.

## Implemented (initial)

- **TopUpServiceSelector** interface and **DefaultTopUpServiceSelector** (stub map: KE → [MPESA], MZ → [MPESA, EMOLA], default → all).
- **Deployment config API:** `GET /api/v1/config/deployment` now includes **topUpProviderIds** (list of allowed provider names for the deployment country).
- **DeploymentConfigDto** and **DeploymentConfigController** wire `TopUpServiceSelector.getAllowedProviderIds(countryCode)` into the response.

## Files touched

- New: `api-service/.../service/TopUpServiceSelector.java` (interface).
- New: `api-service/.../service/impl/DefaultTopUpServiceSelector.java` (stub map by country).
- Updated: `DeploymentConfigDto` (+ topUpProviderIds), `DeploymentConfigController` (inject selector, set topUpProviderIds).
- Optional later: config in `application.yml` (e.g. `top-up-providers-by-country`); validation in `EntityMozServiceImpl.topupAccount` to reject provider not in allowed list.

## Out of scope for 8-1

- Actual integration with mobile money providers (handled in existing payment flow).
- Phase 8-2 (link mobile money wallet, cash-out) and later items.

---

*Design agreed as starting point for Phase 8-1. Update this doc when config shape or API is finalized.*
