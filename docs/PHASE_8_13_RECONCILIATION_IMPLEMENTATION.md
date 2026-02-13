# Phase 8-13: Daily reconciliation — implementation

**Scope:** Tito_UI_Client repo `docs/PHASE_8_13_DAILY_RECONCILIATION_SCOPE.md`.

## Model

- **Inputs:** Server: transactions (and balances) for a date range from DB. Device: from POS messages (8-14) or device sync API (payloads with transaction id, amount, time).
- **Match rule:** Same transaction id or (device_id, amount, time window). Flag: missing_on_device, missing_on_server, amount_mismatch, status_mismatch.
- **Output:** Persisted reconciliation run with summary (e.g. run_id, business_date, device_id, total_server, total_device, matched_count, mismatch_count, details as JSON or child table).

## Database

- **Table `reconciliation_run`:** id, business_date, device_id (nullable), started_at, finished_at, status (RUNNING|COMPLETED|FAILED), server_count, device_count, matched_count, mismatch_count, detail_json (or FK to reconciliation_detail table).
- Liquibase: `payments-reconciliation-run.xml`.

## Job

- **ReconciliationJob:** Scheduled daily after EOD (e.g. 03:00) or on-demand.
  - For each device (or all transactions if no device breakdown): load server transactions for date; load device-reported data (from sync store or API); compare; persist reconciliation_run row(s).
- **API:** GET `/api/v1/reconciliation?date=...` or GraphQL query for Admin/support to view report.

## Status

- [x] Design (this file).
- [ ] Liquibase `reconciliation_run` (and optional `reconciliation_detail`).
- [ ] `ReconciliationJob` and reconciliation service (match logic).
- [ ] API to read reconciliation results.
