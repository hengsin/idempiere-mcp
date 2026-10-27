---
name: idempiere-authenticate-system
description: Authenticate to the iDempiere System tenant (client 0) and obtain a bearer token for MCP tool calls. Use when System Administrator access is needed (dictionary maintenance, centralized IDs, migration scripts, System tenant records), or when an iDempiere MCP tool fails with an authentication/authorization error.
---

# iDempiere Authentication to System Tenant

Logs in to the iDempiere System tenant with `idempiere_auth_create_token` and keeps the returned JWT for later tool calls.

## Instructions

1. If a valid token is already held for this session, reuse it. Re-run this skill when a tool call returns an authentication/authorization error (e.g. 401).
2. Resolve the login values. Treat an unset **or empty** environment variable as unset:
   - `userName`: `IDEMPIERE_ADMINISTRATOR_USER`, default `SuperUser`.
   - `password`: `IDEMPIERE_ADMINISTRATOR_PASS`, default `System`.
   - `roleId`: `IDEMPIERE_ADMINISTRATOR_ROLE`, default `0` (System Administrator). Accepts a role id or a role name.
3. Call `idempiere_auth_create_token` (no `_authorization_bearer` needed for login):

   ```json
   {
     "userName": "<userName>",
     "password": "<password>",
     "parameters": {
       "clientId": "0",
       "roleId": "<roleId>",
       "organizationId": "0",
       "language": "en_US"
     }
   }
   ```

   - `clientId` must be `"0"` — client 0 is the System tenant.
   - `organizationId` `"0"` is the `*` organization. Do not send `warehouseId` for the System tenant.
   - Keep all `parameters` values as JSON strings; ids and names are both accepted.
4. On success, take the **`token`** property of the JSON response (never `refresh_token`) and keep it as `IDEMPIERE_BEARER_TOKEN` for the rest of the session. Never display the password or the token.
5. On failure, report the returned error (e.g. `Invalid clientId`, `Role name not found`, `Role name not unique`, `Missing roleId parameter`, `Invalid organizationId`, `Authenticate error`) and stop. Do not retry with guessed credentials.
6. Pass the token in every later MCP tool call as `_authorization_bearer`, using the raw JWT without the `Bearer ` prefix:

   ```json
   { "_authorization_bearer": "<IDEMPIERE_BEARER_TOKEN>" }
   ```

   If the MCP client sends `Authorization: Bearer <token>`, the header wins and `_authorization_bearer` can be omitted.
7. To switch from the System session to a tenant/role/org/warehouse, call `idempiere_auth_update_token` instead of logging in again. Call `idempiere_auth_logout` when finished.
