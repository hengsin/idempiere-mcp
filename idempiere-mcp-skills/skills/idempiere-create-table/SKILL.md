---
name: idempiere-create-table
description: Create a new table in iDempiere (AD_Table) and database. Validates table name syntax, convention, and case-insensitive uniqueness, creates the AD_Table record with dry-run confirmation, runs CreateTable to generate standard columns, synchronizes the physical database schema (including optional _Trl table), and validates key indexes plus standard unique indexes on Value (and on DocumentNo only when C_DocType_ID is present).
---

# Create iDempiere Table

This skill creates a new physical table definition in iDempiere's `Table and Column` window (`table-and-column`), generates standard columns using the `CreateTable` process, synchronizes the physical database schema (`CREATE TABLE`), and creates/validates the key indexes and standard unique indexes on `Value` and (conditionally) `DocumentNo`.

> **Scope Note**: This skill is for physical database tables only (`IsView = false`). If the user asks to create a database view (`IsView = true`), **stop** and inform them that views do not use `CreateTable`, `ad_column-sync`, or `table-index`.

## Mandatory Inputs

| Input | JSON Field | Description | Example |
|---|---|---|---|
| `TableName` | `TableName` | Database table name (max 60 chars, `Prefix_CamelCase` convention). | `XX_CustomerFeedback` |
| `AccessLevel` | `AccessLevel` | Data Access Level 1-character code (or display name mapped to code below). | `"3"` (`Client+Organization`), `"4"` (`System only`), `"7"` (`All`) |
| `Name` | `Name` | Human-readable table name shown to users. | `Customer Feedback` |

All three inputs are **mandatory**. If any is missing, ask the user for it and **stop** until provided. Do not guess or derive them.

### `AccessLevel` Reference & Normalization

`AD_Table.AccessLevel` (`AD_Reference_ID = 5`, `AD_Table Access Levels`) is stored as a 1-character string code. Always normalize user-provided names or aliases to the exact single-character string code below before calling any MCP tool:

| Code | Official `AD_Ref_List` Name | Common Aliases | Scope |
|---|---|---|---|
| `"4"` | **System only** | `System` | Client `0` only |
| `"6"` | **System+Client** | `System + Client` | System or tenant data (`Org 0`) |
| `"3"` | **Client+Organization** | `Client + Organization`, `Client+Org` | Tenant data partitioned by Organization |
| `"2"` | **Client only** | `Client` | Tenant data (`Org 0` across tenant) |
| `"1"` | **Organization** | `Org` | Specific non-zero Organization only |
| `"7"` | **All** | — | System, Client, and Organization |

- **Never** pass a display label (e.g. `"Client+Organization"`) or `{"identifier": "..."}` in `data.AccessLevel` — iDempiere REST treats raw strings as the 1-char `Value` code and throws HTTP 500 on ambiguous `AD_Ref_List.Name` identifier lookups.
- If the user's `AccessLevel` value does not match one of the codes or names above, report the valid options and **stop**.

## Other Inputs

| Input | Default | Description |
|---|---|---|
| `TableKeyType` | `ID Key Table` | Primary key type: `ID Key Table` (numeric `<TableName>_ID` PK + `<TableName>_UU` UUID) or `UUID Key Table` (`<TableName>_UU` PK only). |
| `logMigrationScript` | `false` | When `true`, logs SQL migration scripts on the server. Omit when not requested. |
| `adempiereSys` | `false` | Dictionary maintenance mode (`EntityType = 'D'`, centralized IDs). Requires centralized ID credentials (`DICTIONARY_ID_USER` / `DICTIONARY_ID_PASSWORD` in `AD_SysConfig`; configure via `idempiere-set-centralized-id-config` if missing). |
| `migrationScriptComment` | *(omitted)* | Comment logged with the migration script (e.g. ticket number and summary). Only pass when `logMigrationScript` is `true` and a non-empty comment is provided; never send `""`. |

Other optional `AD_Table` fields supplied by the user (e.g. `Description`, `Help`, `EntityType`, `IsSecurityEnabled`, `IsDeleteable`, `IsChangeLog`, `IsHighVolume`, `IsCentrallyMaintained`) should be passed in `data` using the corresponding `AD_Table` column name (the `AD_Column.ColumnName` of the Table tab field). Do **not** allow `"IsView": true`.

## Authentication & Security

- Creating `AD_Table` metadata and running `CreateTable`, `ad_column-sync`, and `ad_tableindex-validate` (`AccessLevel = 4 System only`) strictly require a **System Administrator** session on the **System tenant (`Client 0`)**.
- If no `IDEMPIERE_BEARER_TOKEN` is held, if the current token was issued for a non-System tenant (e.g. via `idempiere-authenticate-tenant`), or if any tool call returns HTTP `401` or `403`, run the `idempiere-authenticate-system` skill to obtain a System tenant token.
- Pass `_authorization_bearer` with the raw `IDEMPIERE_BEARER_TOKEN` (without the `Bearer ` prefix) on every MCP tool call.
- **Never** echo or display the raw `IDEMPIERE_BEARER_TOKEN` in user-facing tables, summaries, or error messages.

---

## Workflow

### 1. Validate `TableName`

Validate syntax and length **before** interpolating `TableName` into any OData filter expression:

#### A. Identifier Safety, Length, and Naming Convention
1. **Character Safety (Hard Stop)**:
   - `TableName` must start with an ASCII letter and contain only ASCII letters, digits, and underscores (`^[A-Za-z][A-Za-z0-9_]*$`).
   - If it contains spaces, quotes, punctuation, or other characters, report the error and **stop** immediately.
2. **Length Check**:
   - `AD_Table.TableName` is `VARCHAR(60)` (see IDEMPIERE-6064). If `TableName.length > 60`, report the limit and **stop**.
   - iDempiere caps database object identifiers at **63 characters** (`AdempiereDatabase.MAX_OBJECT_NAME_LENGTH = 63`). PostgreSQL silently truncates longer identifiers, which can cause silent collisions, so derived identifiers must be checked before they are created:

     | Derived identifier | Suffix length | Max `TableName` |
     |---|---|---|
     | `<TableName>_ID`, `<TableName>_UU` | +3 | 60 |
     | `<TableName>_Value` (index) | +6 | 57 |
     | `<TableName>_UU_idx`, `<TableName>_Trl_UU` | +7 | 56 |
     | `<TableName>_Trl_pkey` | +9 | 54 |
     | `<TableName>_DocumentNo` (index) | +11 | 52 |

   - If `TableName.length > 52`, warn the user that some optional derived identifiers may exceed the 63-character limit. The specific feature must then be blocked at the point of use (see Steps 3 and 5); never let PostgreSQL truncate an identifier.
   - `MColumn.getConstraint()` and `MTable.getUUIDIndexName()` truncate `<TableName>_Key` and `<TableName>_UU_idx` automatically, but CreateTable's `<TableName>_Trl_pkey` is **not** truncated.
3. **`Prefix_CamelCase` Convention Check (Warning)**:
   - Standard iDempiere table names use an uppercase module/entity prefix (`1+` uppercase letters/digits), a single underscore `_`, and `CamelCase` words: `^[A-Z][A-Z0-9]*_[A-Z][A-Za-z0-9]*$` (e.g. `XX_CustomerFeedback`, `C_Order`, `AD_TableIndex`).
   - This is a guideline, not a hard rule: legitimate dictionary names can contain additional underscores or a digit-leading second segment (e.g. `C_BankAccount_Acct`, `Fact_Acct_Summary`, `T_1099Extract`, `REST_RefreshToken`). If `TableName` does not match `Prefix_CamelCase`, raise a clear warning explaining the convention and ask the user whether to proceed with the current name or supply a conforming name. Wait for confirmation before continuing.

#### B. Check Uniqueness (Case-Insensitive)

PostgreSQL and Oracle fold unquoted table identifiers case-insensitively. Search `AD_Table` using `tolower(TableName)` via `idempiere_model_search_records`:

```json
{
  "model": "AD_Table",
  "filter": "tolower(TableName) eq '<lowercase_TableName>'",
  "_authorization_bearer": "<IDEMPIERE_BEARER_TOKEN>"
}
```

- If one or more records are returned, display the existing record(s) (`id` as `AD_Table_ID`, `TableName`, `Name`, `EntityType`, `IsActive`).
- **Resuming an interrupted run**: If the user is continuing a previous run of this skill for the same table (e.g. a later step failed), do **not** create a new `AD_Table`. Determine which parts are already complete (query `AD_Column` for the table, `AD_TableIndex` for key/index records) and continue from the first incomplete step (Step 3, 4, or 5).
- Otherwise **stop**. Do not modify or recreate existing tables.

---

### 2. Create `AD_Table` Record

Use `idempiere_window_create_record` on the `Table and Column` window (`window_name`: `"table-and-column"`).

#### A. Dry-Run Creation (`save: false`)
Call `idempiere_window_create_record` with `save: false` to evaluate window defaults and callouts without persisting:

```json
{
  "window_name": "table-and-column",
  "save": false,
  "data": {
    "TableName": "<TableName>",
    "Name": "<Name>",
    "AccessLevel": "<AccessLevel_Code>"
  },
  "_authorization_bearer": "<IDEMPIERE_BEARER_TOKEN>"
}
```

Rules for the dry-run payload:
- Pass `"AccessLevel"` as the normalized 1-character string code (e.g. `"3"`).
- Include any optional fields explicitly provided by the user (e.g. `Description`, `Help`, `EntityType`).
- **Do not hardcode `EntityType`**: omit `EntityType` unless the user explicitly supplied one. The **window** path evaluates the `AD_Table.EntityType` default (`@SQL=...` referencing `@P|AdempiereSys:N@`), so include `"adempiereSys": true` on the dry-run call when dictionary maintenance was requested. (This is different from the `CreateTable` process, whose defaults are **not** evaluated by REST — see Step 3.3.)
- Do **not** pass `logMigrationScript` or `migrationScriptComment` during `save: false`.

#### B. Show Evaluated Values & Confirm
From the dry-run response, display a table of the evaluated field values. Note that reference/foreign-key fields (`AccessLevel`, `EntityType`) are returned as compound objects `{"id": "...", "identifier": "..."}` — format them clearly (e.g. `3 (Client+Organization)` and `U (User maintained)`):

| Field | Evaluated Value |
|---|---|
| `TableName` | `<TableName>` |
| `Name` | `<Name>` |
| `AccessLevel` | `<AccessLevel.id> (<AccessLevel.identifier>)` |
| `EntityType` | `<EntityType.id> (<EntityType.identifier>)` |
| `IsView` | `<IsView>` |
| `IsActive` | `<IsActive>` |
| `IsChangeLog` | `<IsChangeLog>` |
| `IsDeleteable` | `<IsDeleteable>` |
| `IsHighVolume` | `<IsHighVolume>` |
| `IsSecurityEnabled` | `<IsSecurityEnabled>` |
| `IsCentrallyMaintained` | `<IsCentrallyMaintained>` |
| `Description` | `<Description>` |
| `Help` | `<Help>` |

`TableName`, `Name`, `AccessLevel`, `EntityType`, `IsView`, and `IsSecurityEnabled` are mandatory `AD_Table` columns — surface any that are still empty.

Prompt the user for:
1. Any missing mandatory fields or adjustments to optional fields.
2. Confirmation to save the `AD_Table` record.

#### C. Persist Table Record (`save: true`)
Once confirmed, call `idempiere_window_create_record` with `save: true`:

```json
{
  "window_name": "table-and-column",
  "save": true,
  "data": {
    "TableName": "<TableName>",
    "Name": "<Name>",
    "AccessLevel": "<AccessLevel_Code>"
  },
  "_authorization_bearer": "<IDEMPIERE_BEARER_TOKEN>"
}
```

Rules for the `save: true` call:
- Merge any additional or adjusted field values confirmed by the user into `data`, using scalar codes/IDs (e.g. `"AccessLevel": "3"`, `"EntityType": "U"`) and omitting read-only tenant/org objects (`AD_Client_ID`, `AD_Org_ID`).
- Include `"adempiereSys": true`, `"logMigrationScript": true`, and `"migrationScriptComment": "<comment>"` **only** if requested by the user.
- If `adempiereSys: true` fails with a centralized ID reservation error, direct the user to run `idempiere-set-centralized-id-config` and **stop**.
- Retain the numeric `id` from the response as `AD_Table_ID`, and retain the resolved `EntityType` code (`response.EntityType.id` or `response.EntityType`) as `TableEntityType`.
- Retain `TableName`, `Name`, `AccessLevel`, and `Description` — they must be passed explicitly to the `CreateTable` process in Step 3.3.

---

### 3. Generate Standard Set of Columns

Run the `CreateTable` process (`process_value`: `"createtable"`, `AD_Process_ID`: `200134`) to generate standard audit and business columns in `AD_Column`.

1. **Determine `IsCreateKeyColumn` from `TableKeyType`**:
   - **ID Key Table** (default): `"IsCreateKeyColumn": "Y"` (creates numeric PK `<TableName>_ID` and UUID column `<TableName>_UU`).
   - **UUID Key Table**: `"IsCreateKeyColumn": "N"` (creates UUID column `<TableName>_UU` without `<TableName>_ID`).

2. **Collect Optional Column & Table Generation Flags**:
   Prompt the user (if not already specified) for standard `CreateTable` `Yes-No` flags (use `"Y"` / `"N"` strings consistently):
   - Common entity columns:
     - `IsCreateColValue`: Create `Value` search key column (default `"Y"`)
     - `IsCreateColName`: Create `Name` column (default `"Y"`)
     - `IsCreateColDescription`: Create `Description` column (default `"Y"`)
     - `IsCreateColHelp`: Create `Help` column (default `"Y"`)
     - `IsCreateColDocumentNo`: Create `DocumentNo` column (default `"N"`)
     - `IsCreateTranslationTable`: Create `<TableName>_Trl` translation table (default `"N"`)
   - Transactional / document / workflow options (default `"N"` — ask or enable when creating a document/transactional table; call `idempiere_process_get_info` with `process_value: "createtable"` if full parameter metadata is needed):
     - `IsCreateWorkflow`, `IsCreateColDocAction`, `IsCreateColDocStatus`, `IsCreateColProcessed`, `IsCreateColProcessedOn`, `IsCreateColProcessing`, `IsCreateColC_DocTypeTarget_ID`, `IsCreateColC_DocType_ID`, `IsCreateColC_Currency_ID`, `IsCreateColDateTrx`, `IsCreateColDateAcct`, `IsCreateColPosted`, `IsCreateColIsApproved`, `IsCreateColSalesRep_ID`, `IsCreateColAD_User_ID`.
   - **`IsCreateWorkflow = "Y"` has side effects**: besides the DocAction/DocStatus/Processed/ProcessedOn/Processing columns, `CreateTable` creates an `AD_Workflow` (document process) **and** an `AD_Process` (`"<TableName> Process"`), and links the workflow to the `DocAction` column. Confirm with the user before enabling it.
   - **If `IsCreateTranslationTable = "Y"`**, validate before running `CreateTable`:
     - Search `AD_Table` for `tolower(TableName) eq '<lowercase_TableName>_trl'`. If it exists, **stop** — `CreateTable` would silently reuse that table and add/reuse columns on it (it only raises "already exists" for the main table). Do not proceed until the conflict is resolved.
     - Verify `<TableName>_Trl_pkey` (or `<TableName>_Trl_UU`) length ≤ 63 (see Step 1.2); otherwise tell the user the table name is too long for a translation table and stop.

3. **Execute `createtable`**:
   Pass `record_id` as an **integer**, and pass `TableName`, `Name`, `AccessLevel`, and `Description` explicitly, because the process parameter defaults (`@TableName@`, `@Name@`, `@AccessLevel@`, `@Description@`) are evaluated client-side by the window and are **not** evaluated by the REST/MCP process endpoint. Also propagate `"EntityType": "<TableEntityType>"` so generated `AD_Column` and `AD_Element` records match `AD_Table.EntityType`:

   ```json
   {
     "process_value": "createtable",
     "record_id": 1000001,
     "parameters": {
       "TableName": "<TableName>",
       "Name": "<Name>",
       "AccessLevel": "<AccessLevel_Code>",
       "Description": "<Description, only if supplied>",
       "EntityType": "<TableEntityType>",
       "IsCreateKeyColumn": "Y",
       "IsCreateColValue": "Y",
       "IsCreateColName": "Y",
       "IsCreateColDescription": "Y",
       "IsCreateColHelp": "Y",
       "IsCreateColDocumentNo": "N",
       "IsCreateWorkflow": "N",
       "IsCreateTranslationTable": "N"
     },
     "_authorization_bearer": "<IDEMPIERE_BEARER_TOKEN>"
   }
   ```

   - Include `adempiereSys`, `logMigrationScript`, and `migrationScriptComment` if requested.
   - **Check `isError`**: iDempiere's process endpoint returns HTTP 200 even on process failure. Inspect `response.isError`; if `response.isError === true`, report `summary` and `logs` to the user and **stop**.

4. **Fetch All Generated Columns (Single Query) & Verify Key Integrity**:
   Query `AD_Column` once with `"limit": 100` (avoiding the default `limit: 10` truncation, since `CreateTable` generates 13–25+ columns):

   ```json
   {
     "model": "AD_Column",
     "filter": "AD_Table_ID eq <AD_Table_ID>",
     "limit": 100,
     "_authorization_bearer": "<IDEMPIERE_BEARER_TOKEN>"
   }
   ```

   - If no columns are returned, report an error and **stop**.
   - Cache the returned `records` array (where each column's numeric `AD_Column_ID` is `record.id`, and its name is `record.ColumnName`) for Steps 4, 5, and 6.
   - **Never rely on array position** (`records[0]`) to choose a column — the query has no defined order. Always select columns by `ColumnName`.
   - If more than 100 columns are returned, page through the remaining records (or query the specific `Value`, `DocumentNo`, `C_DocType_ID`, `AD_Client_ID`, and key columns explicitly) so Step 5 does not silently miss columns.

---

### 4. Update Database (Synchronize Physical Table DDL)

> **Ordering Requirement**: Physical table synchronization (`ad_column-sync`) **must** execute *before* index validation (`ad_tableindex-validate`), because `CreateTable` only creates dictionary metadata and `CREATE INDEX`/`ALTER TABLE ... PRIMARY KEY` require the physical table to exist in PostgreSQL/Oracle.

#### A. Synchronize Main Table
Select the sync trigger column **by name** from the cached `AD_Column` list (Step 3.4):
- ID Key Table: `<TableName>_ID`
- UUID Key Table: `<TableName>_UU`
- Fallback (key column missing): any cached column, but tell the user the key column was not found.

Then run `ad_column-sync` via `idempiere_process_run` with that numeric `AD_Column_ID`:

```json
{
  "process_value": "ad_column-sync",
  "record_id": 1000010,
  "_authorization_bearer": "<IDEMPIERE_BEARER_TOKEN>"
}
```

- Pass `record_id` as an **integer** (`<AD_Column_ID>`), and include `adempiereSys`, `logMigrationScript`, and `migrationScriptComment` if requested.
- `ColumnSync` only syncs the given column (`AD_Column_ID=?`) unless the physical table is missing, in which case it emits the full `CREATE TABLE` (all columns, PK, and FKs). To sync every column in one call when the table already exists, pass `"DateFrom": "1970-01-01"` in `parameters` (the process then also processes all columns with `AD_Table_ID = <AD_Table_ID> AND Updated > DateFrom`).
- **Check `isError`**: If `response.isError === true`, report `summary` and `logs` and **stop** (do not proceed to index creation).
- Verify that `response.summary` reflects table creation (`CREATE TABLE ...`). *(Note: If an orphaned physical table with the same name already existed in the database, `ColumnSync` emits `ALTER TABLE` for only that single column; in that edge case, warn the user and re-run with `DateFrom` (or across the remaining columns) or clean up the orphan table.)*

#### B. Synchronize Translation Table (If `IsCreateTranslationTable = "Y"`)
If `IsCreateTranslationTable` was set to `"Y"` in Step 3:
1. Search `AD_Table` for `filter: "tolower(TableName) eq '<lowercase_TableName>_trl'"` to obtain `<Trl_AD_Table_ID>` (`records[0].id`).
2. Search `AD_Column` for `filter: "AD_Table_ID eq <Trl_AD_Table_ID>"` and select the `AD_Language` column (or any column if `AD_Language` is missing) — do not rely on `limit: 1` order. Use its `id` as `<Trl_AD_Column_ID>`.
3. Run `idempiere_process_run` with `process_value: "ad_column-sync"` and `record_id: <Trl_AD_Column_ID>` (integer), passing any migration flags.
4. Check `response.isError`; if `true`, report `summary` and `logs` and **stop**.
5. The `_Trl` physical table has no primary key yet — it is created by validating `<TableName>_Trl_pkey` in Step 5.A.

#### C. Reconcile the UUID Key Index (UUID Key Table Only)
After the main table is synchronized, the physical PK is `<TableName>_Key` (from Step 3.4), but `CreateTable` created `AD_TableIndex <TableName>_UU_idx` with `IsKey = true`.
1. Search `AD_TableIndex` with `filter: "AD_Table_ID eq <AD_Table_ID> and Name eq '<TableName>_UU_idx'"` (or `IsKey eq true`).
2. Update that record's `IsKey` to `false` (leave `IsUnique`/`IsCreateConstraint` as created), after confirming with the user.
3. Step 5.A validates the index, creating a physical `UNIQUE` constraint named `<TableName>_UU_idx` so the dictionary and database agree. If the record is not updated first, any later Index Validate attempt would try to add a second primary key and fail.

---

### 5. Create and Validate Table Indexes

> **Ordering Requirement**: `ad_tableindex-validate` must run after Step 4 (the physical table must exist).

#### A. Validate Key / Primary-Key Indexes (Always)
Validate the key indexes created by `CreateTable` before creating business indexes:

1. **UUID Key Table**: validate `<TableName>_UU_idx` (after Step 4.C). Expect a `UNIQUE` constraint creation.
2. **Translation table** (`IsCreateTranslationTable = "Y"`): validate `<TableName>_Trl_pkey`. This creates the missing `PRIMARY KEY` on the `_Trl` table. *(Note: `CreateTable` builds this index on `AD_Language` + parent key only; most standard `_Trl` tables also include `AD_Client_ID`. Add it to the index columns if the user wants dictionary-standard keys.)*
3. **ID Key Table**: the physical `PRIMARY KEY` is already `<TableName>_Key` from `CREATE TABLE`, and `<TableName>_UU_idx` already exists as a unique constraint; validating it is optional (it reports "NoChangesToIndex").

Run each with:

```json
{
  "process_value": "ad_tableindex-validate",
  "record_id": 1000050,
  "_authorization_bearer": "<IDEMPIERE_BEARER_TOKEN>"
}
```

- Pass `record_id` as an **integer** (`<AD_TableIndex_ID>`).
- **Check `isError`**; if `true`, report `summary` and `logs` and **stop** (or ask the user whether to continue with the remaining indexes).

#### B. Create and Validate the `Value` Unique Index
If the cached column list contains `ColumnName == "Value"`:
- Propose the standard unique index definition and confirm with the user (unless they already asked to proceed without confirmation):
  - `Name`: `<TableName>_Value`
  - `IsUnique`: `true`
  - Columns:
    - `AccessLevel = "4"` (System only): `Value`
    - otherwise: `AD_Client_ID`, `Value`

#### C. Create and Validate the `DocumentNo` Unique Index (Conditional)
Only when **both** `ColumnName == "DocumentNo"` **and** `ColumnName == "C_DocType_ID"` exist in the cached column list. This follows the standard document-table pattern (`DocumentNo` scoped by `C_DocType_ID`) and avoids rejecting legitimate documents that share a number across document types.
- If `DocumentNo` exists but `C_DocType_ID` does not, **skip the index** and tell the user why (a unique index on `(AD_Client_ID, DocumentNo)` alone is not safe for document tables).
- Propose and confirm:
  - `Name`: `<TableName>_DocumentNo`
  - `IsUnique`: `true`
  - Columns (in this order): `AD_Client_ID` (omit for `AccessLevel = "4"`), `DocumentNo`, `C_DocType_ID`.

#### D. Create the Index Record and Validate It
For each proposed index (from Step 5.B and 5.C):
- Execute the `idempiere-add-table-index` skill with:
  - `AD_Table_ID`: `<AD_Table_ID>`
  - `Name`: `<IndexName>`
  - `IsUnique`: `true`
  - `Columns`: array of `AD_Column_ID`s (or column names)
  - Pass `adempiereSys`, `logMigrationScript`, and `migrationScriptComment` if requested.
- If the index creation or validation fails, report the error and **stop**.

If neither `Value` nor a valid `DocumentNo`/`C_DocType_ID` combination exists, skip Step 5.B/5.C (5.A still applies).

---

### 6. Summary & Next Steps

Display a completion report to the user (never include `IDEMPIERE_BEARER_TOKEN`):
- **Table Name**: `<TableName>` (plus `<TableName>_Trl` if created)
- **`AD_Table_ID`**: `<AD_Table_ID>` (and `<Trl_AD_Table_ID>` if created)
- **Access Level**: `<AccessLevel_Code>` (`<AccessLevel_Label>`)
- **Entity Type**: `<TableEntityType>`
- **Key Type**: `<TableKeyType>` (physical PK: `<TableName>_Key` on `<TableName>_ID` or `<TableName>_UU`)
- **Columns Generated**: Exact list of `ColumnName`s from the cached Step 3.4 `AD_Column` query (sorted by `SeqNo` or `ColumnName`)
- **Database DDL Status**: Physical `CREATE TABLE` synchronization summary (including `_Trl` table if applicable)
- **Indexes Created & Validated**: Key indexes, `Value` index, and (if applicable) the `DocumentNo` index — include name(s), `AD_TableIndex_ID`, and indexed columns. Explicitly note when the `DocumentNo` index was skipped because `C_DocType_ID` is absent, or when any index was blocked by the identifier-length rule.

Suggest next actions:
- Use `idempiere-add-column-to-table` to add custom business columns.
- Use `idempiere-generate-model` to generate Java `I_*` / `X_*` model classes (passing `TableName` and `TableEntityType`).
- Reset the Application Dictionary cache via `idempiere_cache_reset` if immediate UI access is needed.
