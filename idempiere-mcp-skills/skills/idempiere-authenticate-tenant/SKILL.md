---
name: idempiere-authenticate-tenant
description: Authenticate to an iDempiere tenant (default GardenWorld) and obtain a bearer token for MCP tool calls. Use when tenant data and window/model operations are needed, or when an iDempiere MCP tool fails with an authentication/authorization error.
---

# iDempiere Authentication to Tenant

Logs in to an iDempiere tenant with `idempiere_auth_create_token` and keeps the returned JWT for later tool calls.

## Instructions

1. If a valid token is already held for this session, reuse it. Re-run this skill when a tool call returns an authentication/authorization error (e.g. 401).
2. Resolve the login values. Treat an unset **or empty** environment variable as unset:
   - `userName`: `IDEMPIERE_TENANT_USER`, default `GardenAdmin`.
   - `password`: `IDEMPIERE_TENANT_PASS`, default `GardenAdmin`.
   - `clientId`: `IDEMPIERE_TENANT`, default `GardenWorld` (tenant id or search key).
   - `roleId`: `IDEMPIERE_TENANT_ROLE`, default `GardenWorld Admin` (role id or role name).
   - `organizationId`: `IDEMPIERE_TENANT_ORG`, default `0` (the `*` organization).
   - `warehouseId`: `IDEMPIERE_TENANT_WAREHOUSE`, default `0` (no specific warehouse).
3. Call `idempiere_auth_create_token` (no `_authorization_bearer` needed for login):

   ```json
   {
     "userName": "<userName>",
     "password": "<password>",
     "parameters": {
       "clientId": "<clientId>",
       "roleId": "<roleId>",
       "organizationId": "<organizationId>",
       "warehouseId": "<warehouseId>",
       "language": "en_US"
     }
   }
   ```

   - Keep all `parameters` values as JSON strings; ids and names are both accepted.
   - Omit `warehouseId` only when it is unset; `"0"` means no specific warehouse.
4. On success, take the **`token`** property of the JSON response (never `refresh_token`) and keep it as `IDEMPIERE_BEARER_TOKEN` for the rest of the session. Never display the password or the token.
5. On failure, report the returned error (e.g. `Invalid clientId`, `Role name not found`, `Role name not unique`, `Missing roleId parameter`, `Invalid organizationId`, `Invalid warehouseId`, `Authenticate error`) and stop. Do not retry with guessed credentials.
6. Pass the token in every later MCP tool call as `_authorization_bearer`, using the raw JWT without the `Bearer ` prefix:

   ```json
   { "_authorization_bearer": "<IDEMPIERE_BEARER_TOKEN>" }
   ```

   If the MCP client sends `Authorization: Bearer <token>`, the header wins and `_authorization_bearer` can be omitted.
7. To switch tenant/role/org/warehouse on the existing session, call `idempiere_auth_update_token` instead of logging in again. Call `idempiere_auth_logout` when finished.
