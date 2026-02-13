# Phase 8-11: EOD settlement job — implementation

**Scope:** Tito_UI_Client repo `docs/PHASE_8_11_EOD_SETTLEMENT_SCOPE.md`.

## Flow (order)

1. **Settlement (8-5):** Run settlement rules for the day’s activity; credit/debit accounts (including pool if 8-7 enabled).
2. **TiTo fee (8-10):** Deduct fees and device rental; credit TiTo revenue account.
3. **Sweep (8-6):** For accounts with active sweep rules, execute sweep (wallet → mobile money or bank).

## Implementation

- **Single job:** `EodSettlementJob` (e.g. `cash.ice.api.job.EodSettlementJob`).
  - Trigger: `@Scheduled(cron = "${eod.settlement.cron:0 0 2 * * ?}")` (default 02:00) or on-demand via REST/GraphQL for Admin.
  - Steps: call `SettlementService.runForDate(date)`, then `TitoRevenueFeeService.runForDate(date)`, then `SweepExecutionService.runScheduledForDate(date)`.
- **Idempotency:** Each step should be safe for a given business date (e.g. settlement run stored per date; fee keys by account+date; sweep run idempotent per rule+date).
- **API:** Optional `POST /api/v1/eod/run` or GraphQL mutation `runEodSettlement(businessDate: Date)` for Admin trigger; response includes status or run id.

## Status

- [x] Design doc (this file).
- [ ] `EodSettlementJob` with scheduled and optional REST trigger.
- [ ] Settlement service stub/implementation for date (8-5).
- [ ] `TitoRevenueFeeService` (8-10).
- [ ] `SweepExecutionService` (8-6) invoked after fee step.
