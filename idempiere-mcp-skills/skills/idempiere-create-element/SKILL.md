---
name: idempiere-create-element
description: Create a new iDempiere Element (AD_Element) for a column name after checking that it does not already exist. Use when adding dictionary metadata or when a new table column needs a reusable element.
---

# Create iDempiere Element

This skill creates an `AD_Element` record in the iDempiere **Element** window (slug: `element`) using the iDempiere MCP tools.

## Inputs

| Input | JSON property | Required | Notes |
|---|---|---|---|
| Name | `Name` | **Yes** | Label shown to users, e.g. `Customer Feedback`. |
| Column Name | `ColumnName` | **Yes** | Database column name, e.g. `XX_CustomerFeedback`. |
| Print Name | `PrintName` | No | Short label for lists and reports. Defaults to `Name` when omitted. |
| Description | `Description` | No | Short description of the element. |
| Help | `Help` | No | Longer help text. |
| Entity Type | `EntityType` | No | Module entity type, e.g. `U` for user-maintained metadata. |

`Name` and `ColumnName` **must come from the user**. Do not guess, derive, or silently rename them. If either is missing, ask the user for it and **stop** until it is provided.

Use `_authorization_bearer` with the raw `IDEMPIERE_BEARER_TOKEN` (without the `Bearer ` prefix) on every tool call (use the `idempiere-authenticate-system` skill to obtain a token if not already authenticated).

## Other options
| Input | Description |
|---|---|
| `logMigrationScript` | Log migration script. true or false. |
| `adempiereSys` | Dictionary maintenance (i.e EntityType=D). true of false. Default to false.|
| `migrationScriptComment` | Migration script comment. Usually ticket number and summary text. |

## Workflow

### 1. Check whether the element already exists

Use `idempiere_window_get_records` to search the **Element** window by `ColumnName`:

```json
{
  "_authorization_bearer": "<IDEMPIERE_BEARER_TOKEN>",
  "window_name": "element",
  "filter": "ColumnName eq '<ColumnName>'"
}
```

> `idempiere_window_get_record` retrieves a single record by `record_id` and cannot search by `ColumnName`; use its search counterpart `idempiere_window_get_records` with the `filter` above. If the server exposes search criteria directly on `idempiere_window_get_record`, pass the same filter there.

### 2. Stop if the element exists

If the search returns one or more records, show the existing record(s) to the user and **stop**. Do not update or create anything.

Display at least:

| Field | Value |
|---|---|
| `AD_Element_ID` | ... |
| `Name` | ... |
| `ColumnName` | ... |
| `PrintName` | ... |
| `Description` | ... |
| `Help` | ... |
| `EntityType` | ... |
| `IsActive` | ... |

Tell the user that the element already exists and that no new record was created.

### 3. Create the element

If no record is found, create it in the **Element** window. Map the user's inputs to the `data` object using the column names above:

```json
{
  "_authorization_bearer": "<IDEMPIERE_BEARER_TOKEN>",
  "window_name": "element",
  "data": {
    "Name": "<Name>",
    "ColumnName": "<ColumnName>",
    "PrintName": "<PrintName>",
    "Description": "<Description>",
    "Help": "<Help>",
    "EntityType": "<EntityType>"
  }
}
```

Rules:

- Always include `Name` and `ColumnName`.
- Only include optional properties the user supplied; omit empty ones rather than sending blank strings.
- Use the database column names (`PrintName`, `Description`, `Help`, `EntityType`), not the window field labels.
- Ask the user to confirm the field values before saving, unless they already asked you to proceed without confirmation.
- Pass `adempiereSys`, `logMigrationScript`, and `migrationScriptComment` if provided.

### 4. Report the result

On success, show the created record (at least `AD_Element_ID`, `Name`, and `ColumnName`) and confirm that the element was created.

If the tool returns an error, report it as-is and stop. Do not retry with modified values unless the user provides them.
