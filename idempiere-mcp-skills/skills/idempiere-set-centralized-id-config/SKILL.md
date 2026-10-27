---
name: idempiere-set-centralized-id-config
description: Configure iDempiere centralized ID settings (DICTIONARY_ID_USER and DICTIONARY_ID_PASSWORD in AD_SysConfig). Retrieves record IDs from AD_SysConfig and updates their values from environment variables or user input.
---

# Configure iDempiere Centralized ID Settings

This skill configures the centralized dictionary ID credentials (`DICTIONARY_ID_USER` and `DICTIONARY_ID_PASSWORD`) in the `AD_SysConfig` table using iDempiere MCP tools.

## Prerequisites

- **System Tenant Authentication**: Updating `AD_SysConfig` records at System configuration level requires authentication to the **System** tenant (Client `0`).
- Ensure `IDEMPIERE_BEARER_TOKEN` is available. If not authenticated or if an authentication error occurs (401), execute the `idempiere-authenticate-system` skill first to obtain a valid token.
- Pass `_authorization_bearer` with the raw JWT token on every tool call.

## Environment Variables & Inputs

| Setting | Environment Variable | Fallback Variable | Description |
|---|---|---|---|
| Centralized ID User | `IDEMPIERE-CENTRALIZED-ID-USER` | `IDEMPIERE_CENTRALIZED_ID_USER` | Developer username registered for centralized ID sequence service. |
| Centralized ID Password | `IDEMPIERE-CENTRALIZED-ID-PASS` | `IDEMPIERE_CENTRALIZED_ID_PASS` | Password for the centralized ID sequence service. |

> **Note**: If either environment variable does not exist or is empty, **prompt the user** to supply the value before proceeding. Never proceed with empty or guessed credentials. Never log or print the password in plain text in summary outputs.

## Workflow

### 1. Resolve Credentials

1. Check for `IDEMPIERE-CENTRALIZED-ID-USER` (or `IDEMPIERE_CENTRALIZED_ID_USER`). If unset or blank, ask the user to provide the centralized ID username.
2. Check for `IDEMPIERE-CENTRALIZED-ID-PASS` (or `IDEMPIERE_CENTRALIZED_ID_PASS`). If unset or blank, ask the user to provide the centralized ID password.

### 2. Search AD_SysConfig Records

Query the `AD_SysConfig` table using `idempiere_model_search_records` to find the record IDs for both `DICTIONARY_ID_USER` and `DICTIONARY_ID_PASSWORD`:

```json
{
  "_authorization_bearer": "<IDEMPIERE_BEARER_TOKEN>",
  "model": "AD_SysConfig",
  "filter": "Name eq 'DICTIONARY_ID_USER' or Name eq 'DICTIONARY_ID_PASSWORD'"
}
```

- Extract `id` (or `AD_SysConfig_ID`) for the record where `Name == 'DICTIONARY_ID_USER'`.
- Extract `id` (or `AD_SysConfig_ID`) for the record where `Name == 'DICTIONARY_ID_PASSWORD'`.
- If either record is not found, report an error to the user and halt.

### 3. Update DICTIONARY_ID_USER

Update the `Value` field for the `DICTIONARY_ID_USER` record using `idempiere_model_update_record`:

```json
{
  "_authorization_bearer": "<IDEMPIERE_BEARER_TOKEN>",
  "model": "AD_SysConfig",
  "id": "<AD_SysConfig_ID_FOR_USER>",
  "data": {
    "Value": "<RESOLVED_USER_VALUE>"
  }
}
```

Verify that the update succeeds.

### 4. Update DICTIONARY_ID_PASSWORD

Update the `Value` field for the `DICTIONARY_ID_PASSWORD` record using `idempiere_model_update_record`:

```json
{
  "_authorization_bearer": "<IDEMPIERE_BEARER_TOKEN>",
  "model": "AD_SysConfig",
  "id": "<AD_SysConfig_ID_FOR_PASSWORD>",
  "data": {
    "Value": "<RESOLVED_PASSWORD_VALUE>"
  }
}
```

Verify that the update succeeds.

### 5. Confirm and Report

Report the result to the user:
- Confirmation that `DICTIONARY_ID_USER` (Record ID: `<id>`) was updated with the configured username.
- Confirmation that `DICTIONARY_ID_PASSWORD` (Record ID: `<id>`) was updated (do not output the actual password text).
- If any tool returns an error, display the error message as-is and stop.
