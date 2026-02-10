# Deployment config API (Phase 6)

The api-service exposes a **public** (no auth) endpoint for deployment/country configuration so the frontend can show correct country, terminology, currencies, and locale without code changes.

## Endpoint

- **GET** `/api/v1/config/deployment`
- **Auth:** None (permitAll).
- **Response:** JSON (see below).

## Configuration

Config is read from `ice.cash.deployment` in `application.yml`. Override via environment (relaxed binding), e.g.:

- `ICE_CASH_DEPLOYMENT_COUNTRY_CODE`
- `ICE_CASH_DEPLOYMENT_DEFAULT_CURRENCY_CODE`
- `ICE_CASH_DEPLOYMENT_DEFAULT_LOCALE`
- `ICE_CASH_DEPLOYMENT_VEHICLE_TERMINOLOGY`
- `ICE_CASH_DEPLOYMENT_TAXI_OWNER_LABEL`
- `ICE_CASH_DEPLOYMENT_USER_TYPE_TO_ACCOUNT_TYPE` (map: key=user type label, value=AccountTypeMoz)

Example (Render env):

| Key | Value |
|-----|--------|
| ICE_CASH_DEPLOYMENT_COUNTRY_CODE | KE |
| ICE_CASH_DEPLOYMENT_DEFAULT_CURRENCY_CODE | KES |
| ICE_CASH_DEPLOYMENT_DEFAULT_LOCALE | en_KE |
| ICE_CASH_DEPLOYMENT_VEHICLE_TERMINOLOGY | Matatu |
| ICE_CASH_DEPLOYMENT_TAXI_OWNER_LABEL | Matatu/Taxi Owner |

The **currencies** and **locales** lists in the response come from the database (`currency`, `language` tables). The rest come from config.

## Response shape

```json
{
  "countryCode": "KE",
  "defaultCurrencyCode": "KES",
  "defaultLocale": "en_KE",
  "vehicleTerminology": "Matatu",
  "taxiOwnerLabel": "Matatu/Taxi Owner",
  "currencies": [ { "isoCode": "KES", "active": true } ],
  "locales": [ { "languageKey": "en", "name": "English" } ],
  "userTypeToAccountType": {
    "General User": "CommuterRegular",
    "Matatu/Taxi Owner": "MatatuOwnerBusiness",
    "Agents": "AgentBusiness",
    "SACCO": "SaccoBusiness"
  }
}
```

## Code references

- **Properties:** `cash.ice.api.config.property.DeploymentConfigProperties`
- **Controller:** `cash.ice.api.controller.DeploymentConfigController`
- **DTO:** `cash.ice.api.dto.DeploymentConfigDto`
- **Config:** `api-service/src/main/resources/application.yml` → `ice.cash.deployment`
- **Security:** `ApiSecurityConfig` permits `/api/v1/config/**` without authentication.
