# Phase 8-2: Link mobile money wallet, top-up, cash-out — scope

**Backlog item:** Link mobile money wallet to Tito wallet; top-up; cash-out.  
**Owner:** Backend + Frontend.

## Definitions

- **Link:** Associate a user’s mobile money number (e.g. M-Pesa) with their Tito account so top-up and cash-out can use it. May be stored per account or per entity (msisdn already exists in backend).
- **Top-up:** User pays from mobile money into their Tito wallet. Backend already supports this via `topupAccountMoz(accountNumber, provider, mobile, amount)` (Phase 8-1: provider validated by country).
- **Cash-out:** User moves funds from Tito wallet to mobile money. No backend API specified yet; likely a new mutation or REST endpoint plus provider integration.

## What exists today

- **Backend:** GraphQL `topupAccountMoz`; `TopUpServiceSelector` + deployment `topUpProviderIds`; validation in `EntityMozServiceImpl.topupAccount` (EC1001 if provider not allowed for country). Msisdn on entity (e.g. `addOrUpdateMsisdnMoz`).
- **Frontend (Tito_UI_Client):** `TopUpAccountMozCall`, `InitiatePaymentCall` (GraphQL top-up when accountNumber+mobile provided). Deployment config exposes `topUpProviderIds`; provider dropdown uses it. Agent and general-user top-up pages currently show “Topup in progress” and “completed successfully” then update **local state only** (`updateAccountBalanceAction`) — they do **not** call the backend yet.

## Suggested next steps

1. **Frontend:** In the top-up flows (e.g. account_topup_agent, account_topup_general_user), call `PaymentsGroup.topUpAccountMozCall.call()` (or `InitiatePaymentCall`) with account number, mobile, amount, provider before showing success; on failure show `apiResponseErrorMessage(response)` in a snackbar/dialog. Then refresh user/accounts (e.g. GetUserDetailsCall) or update local state on success.
2. **Backend:** No change required for top-up itself. For **cash-out**, define an API (e.g. `cashOutToMobileMoney(accountNumber, provider, mobile, amount)`) and wire to provider (M-Pesa, etc.) when ready.
3. **Link:** If “link” means a dedicated step (e.g. “Add M-Pesa number”), reuse or extend msisdn (entity mobile) and ensure top-up/cash-out use that or an explicit field.

## References

- Phase 8-1: `docs/PHASE_8_1_REGIONAL_TOPUP_SERVICE_DESIGN.md`
- Backend top-up: `EntityMozController.topupAccountMoz`, `EntityMozServiceImpl.topupAccount`
- Flutter: `lib/backend/api_requests/api_calls.dart` (`TopUpAccountMozCall`, `InitiatePaymentCall`), `lib/custom_code/actions/update_account_balance_action.dart` (local-only today)
