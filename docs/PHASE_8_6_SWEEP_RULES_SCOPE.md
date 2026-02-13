# Phase 8-6: Sweep rules — scope (backend)

**Backlog item:** Sweep rules: Tito wallet → mPesa or bank. **Owner:** Backend.

**Definitions:** Sweep = transfer from Tito wallet to mobile money or bank on schedule/trigger. Sweep rules = configurable conditions and destinations.

**Implementation (this repo):**

- **Table `sweep_rule`:** account_id, destination_type (MOBILE_MONEY, BANK), destination_ref (msisdn or bank link id), trigger_type (SCHEDULE, THRESHOLD, MANUAL), schedule_expression (cron), threshold_amount, active, created_date. FK to account.
- **Entity:** `cash.ice.sqldb.entity.SweepRule`; **Repository:** `SweepRuleRepository`.
- **Execution:** `SweepExecutionService` (api-service) runs as part of EOD (8-11) via `EodSettlementService.runForDate()`; reads active rules, evaluates SCHEDULE/THRESHOLD; TODO: integrate cash-out (8-2) or bank transfer and post debit tx.
- **APIs (TODO):** GraphQL or REST to list/create/update sweep rules (Admin 8-8 or entity).

**References:** Phase 8-2 (cash-out), 8-3/8-9 (bank linkage), 8-8 (Admin UI), 8-11 (EOD). Full scope: Tito_UI_Client `docs/PHASE_8_6_SWEEP_RULES_SCOPE.md`.
