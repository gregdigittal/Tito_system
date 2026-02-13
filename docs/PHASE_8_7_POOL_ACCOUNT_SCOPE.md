# Phase 8-7: Pool account — scope (backend)

**Backlog item:** Pool account: country-specific model. **Owner:** Backend.

**Definitions:** Pool account = shared/aggregation account (route pool, Sacco pool). Model may vary by country; configurable per deployment.

**Implementation (this repo):**

- **Account type:** Add `Pool` account type (Liquibase idempotent) so accounts can be created with type Pool. Country-specific behaviour via deployment config (e.g. which currency/entity can have pool).
- **Settlement (TODO):** Wire tap-out/EOD (8-4, 8-11) to credit pool and distribute from pool per settlement rules (8-5). Pool account type exists; settlement service to use it when deployment config enables pool.

**References:** Phase 8-4, 8-5, 8-11, deployment config. Full scope: Tito_UI_Client `docs/PHASE_8_7_POOL_ACCOUNT_SCOPE.md`.
