# Admin UI: Laravel + Livewire build plan

**Purpose:** Build plan for the Tito Admin UI using **Laravel + Livewire**, consuming the existing **j-payments (Spring Boot) GraphQL API**. The backend is unchanged; Laravel is a separate frontend that talks to the same api-service.

**CTO decision:** Admin UI in Laravel + Livewire (web-only, no mobile).

---

## 1. Backend recap (current state)

### 1.1 API surface

All admin features are exposed via **GraphQL** on the same api-service (e.g. `https://tito-api.onrender.com/graphql` or your Render URL). There is **no REST API** for staff or journals.

| Area | Operations | Auth |
|------|------------|------|
| **Staff auth** | `loginStaffMember`, `enterLoginMfaCode`, `enterLoginBackupCode`, `refreshStaffMemberAccessToken`, `invalidateStaffMemberRefreshToken` | Unauthenticated for login/refresh; others use Bearer |
| **Staff CRUD** | `staffMember`, `searchStaffMembers`, `newStaffMember`, `updateStaffMember`, `deleteStaffMember`, `updateStaffMemberLoginStatus`, MFA/password/backup codes | `ROLE_BACKOFFICE` |
| **Journals** | `journals` (query), `createJournal`, `acceptJournal`, `rejectJournal` | `ROLE_BACKOFFICE` |

### 1.2 Auth model

- **Keycloak realm:** `backoffice-local` (configured in backend `security.yml` / env: `AUTH_KEYCLOAK_*`, backoffice realm).
- **Staff login:** Client sends `loginStaffMember(request: LoginUserRequest!)` with `{ username, password }`. Backend validates against Keycloak (backoffice-local) and returns `LoginResponse` with `accessToken` (and optionally MFA flow).
- **Token usage:** For protected operations, client sends `Authorization: Bearer <access_token>`. Backend maps JWT realm roles to Spring roles: `BACKOFFICE` → `ROLE_BACKOFFICE`, `TITO_ADMIN` → `ROLE_TITO_ADMIN`, `FINANCE_ADMIN` → `ROLE_FINANCE_ADMIN`.
- **Refresh:** `refreshStaffMemberAccessToken(refreshToken: String!)` returns a new `LoginResponse` with new tokens.

### 1.3 GraphQL schema (relevant parts)

- **Login:** `LoginUserRequest { username: String!, password: String! }` → `LoginResponse { status, mfaType, msisdn, accessToken: AccessTokenResponse }`. `AccessTokenResponse` has `token`, `refreshToken`, `expiresIn`, etc.
- **Staff:** `StaffMember` (id, email, firstName, lastName, msisdn, loginStatus, securityGroupId, securityGroup, mfaType, mfaBackupCodes, …). Queries: `staffMember(id, config)`, `searchStaffMembers(searchText, status, page, size, sort, config)`. Mutations: as in schema.graphqls (create, update, delete, login, refresh, etc.).
- **Journals:** `Journal` (id, status, amount, drAccountId, crAccountId, transactionCodeId, details, notes, fees, documents, …). Query: `journals(journalStatus!, days!, page, size, sort)` → `JournalPageable { total, content }`. Mutations: `createJournal`, `acceptJournal`, `rejectJournal`.
- **Types:** `SortInput`, `JournalInput`, `JournalFeeInput`, `NewStaffMemberInput`, `StaffMemberUpdate`, etc. (see `api-service/src/main/resources/graphql/staff.graphqls` and `schema.graphqls`).

### 1.4 Network / security

- **GraphQL endpoint:** `/graphql` (POST). Protected by **IP whitelist** (`IpWhitelist.check()`): if `ice.cash.ip-whitelist.disable-check` is false, only whitelisted IPs can call GraphQL. For Laravel server-side calls, either (a) add the Laravel server’s outbound IP to the backend whitelist, or (b) in dev use `ICE_CASH_IP_WHITELIST_DISABLE_CHECK=true`.
- **CORS:** Backend CORS is configured for Flutter web origins; if the Laravel app is served from a different origin and you later call GraphQL from the browser, backend CORS may need to allow that origin. For **server-side** Laravel → api-service calls, CORS does not apply.

---

## 2. Architecture: Laravel + Livewire → api-service

- **Laravel app:** New project (e.g. `Tito_Admin` or `tito-admin-ui`). **No** Laravel-backed database for “users”; auth state is **tokens from the api-service** (session or encrypted cookies).
- **Flow:** Browser → Laravel (Livewire) → Laravel server HTTP client → api-service GraphQL (Bearer token). Optionally, for a SPA-like flow, browser could call api-service GraphQL directly with Bearer token (then CORS and token handling in JS); the plan below assumes **server-side** GraphQL from Laravel for simplicity and to avoid exposing tokens to the client.
- **Same api-service URL:** Use the same base URL as the Flutter app (e.g. from env `API_URL` or `TITO_API_URL`).

---

## 3. Build plan (phased)

### Phase A: Project and auth (7-3, 7-4)

| # | Task | Details |
|---|------|--------|
| A1 | **Laravel project** | New Laravel 11 app; PHP 8.2+; Livewire 3; Tailwind. Repo e.g. `tito-admin` or under existing monorepo. |
| A2 | **Config** | Env: `TITO_API_URL` (api-service base, e.g. `https://tito-api.onrender.com`), `TITO_GRAPHQL_ENDPOINT` (default `$TITO_API_URL/graphql`). |
| A3 | **GraphQL client** | HTTP client (Guzzle/Laravel HTTP) to POST to GraphQL with JSON body `{ "query": "...", "variables": {...} }`. Helper or service: `TitoGraphQL::query($operation, $variables)` that adds `Authorization: Bearer <token>` when token exists. |
| A4 | **Login flow** | Login page (Livewire or Blade): form username + password. On submit, Laravel calls `loginStaffMember(request: { username, password })`. On success: store `access_token` and `refresh_token` in session (or encrypted cookie). If response is `MFA_REQUIRED`, show MFA step and call `enterLoginMfaCode` or `enterLoginBackupCode`. Redirect to dashboard on success. |
| A5 | **Token storage** | Store tokens in session; optionally persist refresh token in encrypted cookie for “remember me”. Middleware or Livewire: before each GraphQL call, attach Bearer from session. |
| A6 | **401 and refresh** | On 401 from api-service: call `refreshStaffMemberAccessToken(refreshToken)`; if success, update session and retry; if failure, clear session and redirect to login. Centralize in HTTP client or middleware. |
| A7 | **Logout** | Call `invalidateStaffMemberRefreshToken(refreshToken)` if desired; clear session; redirect to login. |

### Phase B: Staff list and create (7-5 part)

| # | Task | Details |
|---|------|--------|
| B1 | **Staff list page** | Livewire component: table of staff (email, name, status, security group, etc.). Query `searchStaffMembers(searchText, status, page, size, sort)` with pagination and optional search/status filter. |
| B2 | **Create staff** | Form (Livewire): `newStaffMember(staffMember: NewStaffMemberInput!, url, sendEmail)`. Input: email, firstName, lastName, idNumber, idNumberType, msisdn, department, securityGroupId, loginStatus, locale. After create, redirect or refresh list. |
| B3 | **Staff detail / edit** | Page or modal: load `staffMember(id)`; form for `updateStaffMember(id, staffMember: StaffMemberUpdate!, …)`. Optional: update login status, MFA, backup codes (use existing mutations). |

### Phase C: Journals (7-5 part)

| # | Task | Details |
|---|------|--------|
| C1 | **Journals list** | Livewire: query `journals(journalStatus, days, page, size, sort)`. Tabs or filter by `JournalStatus` (PENDING, ACCEPTED, REJECTED). Display table: id, status, amount, accounts, transaction code, details, dates. |
| C2 | **Accept / Reject** | Buttons per row (or detail page): call `acceptJournal(journalId)` or `rejectJournal(journalId)`. Refresh list or update row. |
| C3 | **Create journal** | Form: `createJournal(journal: JournalInput!, fees, url, sendEmail)`. Inputs: currencyId, transactionCodeId, amount, drAccountId, crAccountId, details, notes; optional fees. May require dropdowns for accounts/transaction codes (from existing queries if available). |

### Phase D: Role-based visibility (7-5 part)

| # | Task | Details |
|---|------|--------|
| D1 | **Roles in Laravel** | After login, decode JWT (or call `staffMember(id: null)` to get current staff): read realm roles (e.g. `BACKOFFICE`, `TITO_ADMIN`, `FINANCE_ADMIN`). Store in session or component state. |
| D2 | **Visibility rules** | Show/hide menu items or sections: e.g. “Staff” and “Journals” for BACKOFFICE; extra “Tito config” for TITO_ADMIN; “Finance / Journals” emphasis for FINANCE_ADMIN. Backend already enforces permissions; this is UI-only. |

### Phase E: Polish and deploy

| # | Task | Details |
|---|------|--------|
| E1 | **UI** | Tailwind; optional Laravel UI or custom components; consistent layout (sidebar/nav), tables, forms, flash messages. |
| E2 | **Deployment** | Deploy Laravel to shared hosting, VPS, or platform (e.g. Laravel Forge, Ploi, or Docker). Set `TITO_API_URL` and ensure server IP is whitelisted on api-service (or disable whitelist in dev). |
| E3 | **Docs** | Short README: how to run locally, env vars, that it talks to j-payments GraphQL only. |

---

## 4. Key implementation notes

### 4.1 GraphQL from Laravel

- Single endpoint: `POST $TITO_API_URL/graphql`, body `application/json`: `{ "query": "mutation LoginStaffMember(...) { loginStaffMember(request: $request) { ... } }", "variables": { "request": { "username": "...", "password": "..." } } }`.
- Send `Authorization: Bearer <access_token>` for all operations except login/refresh/MFA steps.
- Parse JSON response; handle `errors` array and `data` per GraphQL spec.

### 4.2 No Laravel auth driver for “users”

- Do **not** use Laravel’s built-in user provider (DB) for staff. Staff identity and auth are entirely determined by the api-service and Keycloak. Laravel only stores tokens and forwards them.

### 4.3 MFA (OTP / TOTP / backup code)

- If `loginStaffMember` returns `status: MFA_REQUIRED`, show a second step: OTP (sms) or TOTP or backup code. Call `enterLoginMfaCode` or `enterLoginBackupCode` with `{ username, code }`. Then store tokens and proceed as in A4.

### 4.4 Backend config (for admin UI to work)

- **Keycloak:** Realm `backoffice-local` must exist; staff users with roles BACKOFFICE (and optionally TITO_ADMIN, FINANCE_ADMIN). Same as Phase 7-2.
- **api-service:** `AUTH_TRUSTED_ISSUERS` must include the backoffice realm issuer (e.g. `https://your-keycloak/realms/backoffice-local`). Backoffice Keycloak URL/realm in env.
- **IP whitelist:** Either add Laravel server IP to whitelist or set `ICE_CASH_IP_WHITELIST_DISABLE_CHECK=true` in dev.

---

## 5. File / component sketch (Laravel)

- **Config:** `config/tito.php` → `tito.api_url`, `tito.graphql_endpoint`.
- **Service:** `App\Services\TitoGraphQLService` (or `App\Http\TitoGraphQL`) – build query, send with Bearer, handle 401 + refresh, return decoded response.
- **Auth:** `App\Http\Middleware\EnsureStaffToken` – if no token and not on login route, redirect to login. Optionally `RefreshTokenOn401` middleware or logic inside the GraphQL service.
- **Livewire:** `App\Livewire\LoginForm`, `App\Livewire\StaffList`, `App\Livewire\StaffForm`, `App\Livewire\JournalList`, `App\Livewire\JournalForm`, etc.
- **Routes:** `Route::get('/login', ...)`, `Route::middleware('staff')->group(...)` for dashboard, staff, journals.

---

## 6. Summary

| Phase | Scope |
|-------|--------|
| **A** | Laravel + Livewire project; env; GraphQL client; login (with MFA); token storage; 401 → refresh; logout. |
| **B** | Staff list (search, pagination); create staff; staff detail/edit. |
| **C** | Journals list (filter by status); accept/reject; create journal. |
| **D** | Role-based UI (menu/sections by TITO_ADMIN / FINANCE_ADMIN). |
| **E** | UI polish; deploy; docs. |

Backend (j-payments) remains the single source of truth for auth and data; no new backend APIs required for this plan. Laravel + Livewire is a web-only frontend that consumes the existing GraphQL API and respects the CTO’s technology choice.
