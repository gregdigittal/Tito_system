# Phase 8-10: TiTo revenue account & EOD fee — implementation

**Scope:** Tito_UI_Client repo `docs/PHASE_8_10_TITO_REVENUE_EOD_SCOPE.md`.

## Backend design

### TiTo revenue account
- **Option A:** Dedicated system entity + account with account type **TiTo Revenue** (new account type in `account_type` table).
- **Option B:** Single platform account per deployment/currency; identified by account_type name "TiTo Revenue" or by entity type.
- Liquibase: idempotent insert of account type `TiTo Revenue` (e.g. per currency MZN/KES). See `payments-tito-revenue-account-type.xml`.

### Fee / device-rental rules
- **Table (optional):** e.g. `tito_fee_rule` (id, deployment/country, rule_type FEE|DEVICE_RENTAL, source_account_type, percent_or_fixed, amount, currency_id, active). Or use deployment config JSON.
- **EOD fee job:** For a given date window: determine applicable accounts (e.g. Sacco, owner), compute fee/rental per rule, debit source account, credit TiTo revenue account, record transaction lines.

### Integration
- **EOD settlement (8-11):** Runs after settlement (8-5); before sweep (8-6). Fee service is called from `EodSettlementJob`.
- **Idempotency:** Key by (account_id, date, rule_id) so re-runs do not double-deduct.

## Implementation status

- [x] Liquibase: TiTo Revenue account type (see `payments-tito-revenue-account-type.xml`).
- [ ] Resolve or create TiTo revenue account (entity + account) per deployment (bootstrap or migration).
- [ ] Fee rule model and persistence (or use config).
- [ ] `TitoRevenueFeeService`: compute and post fees for date range; called by EOD job.
- [ ] Admin (8-8): optional UI for fee rules and revenue view.
