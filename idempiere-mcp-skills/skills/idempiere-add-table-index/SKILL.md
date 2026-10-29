---
name: idempiere-add-table-index
description: Create and validate a database table index (AD_TableIndex) with index columns (AD_IndexColumn) on an iDempiere table, and synchronize the physical database index using the ad_tableindex-validate process.
---

# Add Table Index

This skill creates a table index record (`AD_TableIndex`) with its associated index columns (`AD_IndexColumn`) on an iDempiere table using the `Table and Column` window (`table-and-column`), and synchronizes the physical database index using the **Index Validate** process (`ad_tableindex-validate`).

## Prerequisites & Authentication

- **System Administrator Required**: Index maintenance is performed via the Application Dictionary in the `Table and Column` window (`table-and-column`). You must authenticate to the System tenant (`Client 0`) using the `idempiere-authenticate-system` skill.
- **Physical Table Must Exist**: The physical table and its columns must already exist in the database (e.g. after running `ad_column-sync` or `CreateTable`) before running index validation.
- Pass `_authorization_bearer` with the raw `IDEMPIERE_BEARER_TOKEN` (without the `Bearer ` prefix) on every tool call. Never display credentials or bearer tokens in output.

## Inputs

### Mandatory Inputs

| Input | Description |
|---|---|
| `TableName` or `AD_Table_ID` | Target database table name (e.g. `C_Order`) or numeric table ID. |
| `Name` | Name of the index (e.g. `C_Order_DocumentNo`). Must be a valid DB identifier: starts with a letter `[a-zA-Z]`, contains only letters, digits, and underscores (`^[a-zA-Z0-9_]*$`), contains no spaces, and length is ≤ 63 characters. |
| `Columns` | Ordered list of columns in the index. Each element can be a column name string (e.g. `["AD_Client_ID", "DocumentNo"]`), an integer `AD_Column_ID` (e.g. `[1000011, 1000018]`), or an object specifying custom attributes (`{"ColumnName": "Value", "SeqNo": 10}` or `{"ColumnSQL": "UPPER(Value)", "SeqNo": 20}`). |

### Optional Inputs & Defaults

| Input | Description | Default |
|---|---|---|
| `IsUnique` | Whether the index enforces uniqueness. `true` or `false`. | `false` |
| `IsCreateConstraint` | Whether to create a UNIQUE constraint instead of an index. **Rule**: In iDempiere (`MTableIndex`), `IsCreateConstraint = true` requires `IsUnique = true`. | `false` |
| `adempiereSys` | Dictionary maintenance mode (requires System Admin on System client). | `false` |
| `logMigrationScript` | Log SQL migration script on server. | `false` |
| `migrationScriptComment` | Migration script comment, e.g. `'IDEMPIERE-#### Short description'`. | (None) |

---

## Workflow

### 1. Validate Table, Index Name, and Constraint Rules

1. **Enforce Constraint Rules**:
   - Check `IsCreateConstraint`: If `IsCreateConstraint === true` and `IsUnique !== true`, inform the user that a database constraint on index columns must be unique (`IsUnique = true`). Set `IsUnique = true` or request confirmation before continuing.

2. **Resolve `AD_Table_ID`**:
   - If `AD_Table_ID` is provided, retain it.
   - If only `TableName` is provided, search `AD_Table` using `idempiere_model_search_records`:
     ```json
     {
       "model": "AD_Table",
       "filter": "tolower(TableName) eq '<lowercase_TableName>'",
       "_authorization_bearer": "<IDEMPIERE_BEARER_TOKEN>"
     }
     ```
     If not found, report that table `<TableName>` does not exist in `AD_Table` and **stop**. Retain `record.id` as `AD_Table_ID` and `record.TableName` as `TableName`.

3. **Validate Index Identifier Syntax & Length**:
   - In iDempiere, index names are validated by `Database.isValidIdentifier(Name)`:
     - Must not be empty.
     - Must not contain spaces.
     - First character must be alphabetic (`^[a-zA-Z]`).
     - Must contain only alphanumeric characters and underscores (`^[a-zA-Z0-9_]*$`).
     - Length must be ≤ 63 characters (PostgreSQL identifier byte limit).
   - If any validation fails, report the error to the user (e.g. "Index name contains spaces or exceeds 63 characters") and **stop** until a valid name is provided.

### 2. Resolve Index Columns (Single Batch Query)

To avoid N+1 OData round-trips when resolving column names:

1. **Batch Fetch Table Columns**:
   Query all columns belonging to the table once using `idempiere_model_search_records`:
   ```json
   {
     "model": "AD_Column",
     "filter": "AD_Table_ID eq <AD_Table_ID>",
     "_authorization_bearer": "<IDEMPIERE_BEARER_TOKEN>"
   }
   ```
   Build an in-memory lookup map of lowercase `ColumnName` → `AD_Column_ID`.

2. **Map Columns and Assign Sequential `SeqNo`**:
   Index column order in `MTableIndex` is determined by `SeqNo` (`ORDER BY SeqNo`). Explicitly assign sequential `SeqNo` values (`10, 20, 30...`) for every column in the index.
   - If an entry has `ColumnSQL` (function/expression index, e.g. `UPPER(Value)`):
     ```json
     { "ColumnSQL": "<Expression>", "SeqNo": <seq> }
     ```
     If `ColumnName` is also known, optionally include its `AD_Column_ID`.
   - If an entry is an integer `AD_Column_ID`:
     ```json
     { "AD_Column_ID": <AD_Column_ID>, "SeqNo": <seq> }
     ```
   - If an entry is a string `ColumnName`:
     Look up `ColumnName` (case-insensitively) in the cached column map:
     - If found: `{ "AD_Column_ID": <AD_Column_ID>, "SeqNo": <seq> }`
     - If not found: report that column `<ColumnName>` does not exist on table `<TableName>` and **stop**.

Construct the final `index-column` child array preserving the requested column order:
```json
[
  { "AD_Column_ID": <AD_Column_ID_1>, "SeqNo": 10 },
  { "AD_Column_ID": <AD_Column_ID_2>, "SeqNo": 20 }
]
```

### 3. Check for Existing Index Record and Child Columns

Search `AD_TableIndex` for an existing index record with the same `AD_Table_ID` and `Name` (case-insensitive) using `idempiere_model_search_records`:

```json
{
  "model": "AD_TableIndex",
  "filter": "AD_Table_ID eq <AD_Table_ID> and tolower(Name) eq '<lowercase_Name>'",
  "_authorization_bearer": "<IDEMPIERE_BEARER_TOKEN>"
}
```

- **If an existing record is found**:
  - Retain `records[0].id` as `AD_TableIndex_ID`.
  - Check existing child columns using `idempiere_model_search_records`:
    ```json
    {
      "model": "AD_IndexColumn",
      "filter": "AD_TableIndex_ID eq <AD_TableIndex_ID>",
      "_authorization_bearer": "<IDEMPIERE_BEARER_TOKEN>"
    }
    ```
  - **Verify Existing Columns**:
    - If 0 child records exist: warn the user that `AD_TableIndex` exists without column definitions. Add the missing `index-column` child records before proceeding.
    - If child records exist: compare the ordered column IDs/expressions with the requested `Columns`.
      - If they match: display that `AD_TableIndex` already exists with matching column definitions; proceed directly to **Step 5 (Index Validate)**.
      - If they differ: inform the user of the discrepancy between the requested columns and the existing definition. Ask whether to keep the existing definition or update it.
  - **Verify Uniqueness Flags**:
    - If `IsUnique` or `IsCreateConstraint` in the existing record differs from requested values, update the record using `idempiere_model_update_record` before proceeding to validation.

### 4. Create the Index Record (`save: true`)

If no existing record was found, create the index record and nested column records using `idempiere_window_create_child_tab_record` on the `table-index` child tab:

- `window_name`: `table-and-column`
- `tab_name`: `table`
- `record_id`: `"<AD_Table_ID>"` (must be passed as a **string**)
- `child_tab_name`: `table-index`

```json
{
  "window_name": "table-and-column",
  "tab_name": "table",
  "record_id": "<AD_Table_ID>",
  "child_tab_name": "table-index",
  "data": {
    "Name": "<IndexName>",
    "IsUnique": <IsUnique>,
    "IsCreateConstraint": <IsCreateConstraint>,
    "index-column": [
      { "AD_Column_ID": <AD_Column_ID_1>, "SeqNo": 10 },
      { "AD_Column_ID": <AD_Column_ID_2>, "SeqNo": 20 }
    ]
  },
  "save": true,
  "_authorization_bearer": "<IDEMPIERE_BEARER_TOKEN>"
}
```

Rules:
- Pass `record_id` as a **string** (`"<AD_Table_ID>"`).
- Pass `AD_Column_ID` inside `index-column` as an **integer**.
- Always include explicit `SeqNo` (`10, 20, 30...`) on every `index-column` record to guarantee deterministic physical index DDL column ordering.
- Include `IsCreateConstraint` if requested (must be accompanied by `IsUnique: true`).
- Include `adempiereSys`, `logMigrationScript`, and `migrationScriptComment` if requested.
- Retain the numeric `id` from the response as `AD_TableIndex_ID`.

### 5. Validate the Database Index (`ad_tableindex-validate`)

Run the **Index Validate** process to create or update the physical index in the database:

- Use `idempiere_process_run` with `process_value: "ad_tableindex-validate"`.
- Pass `record_id` as an **integer** (`<AD_TableIndex_ID>`):

```json
{
  "process_value": "ad_tableindex-validate",
  "record_id": <AD_TableIndex_ID>,
  "_authorization_bearer": "<IDEMPIERE_BEARER_TOKEN>"
}
```

- Include `adempiereSys`, `logMigrationScript`, and `migrationScriptComment` if requested.
- **Check `response.isError`**:
  - **If `response.isError === true`**:
    - Report `response.summary` and `response.logs` and **stop**.
    - *Common Cause for Unique Indexes*: Duplicate data values already exist in the table (e.g. `could not create unique index ... key (...) is duplicated` or Oracle `ORA-01452: cannot CREATE UNIQUE INDEX; duplicate keys found`). Advise the user to clean up or deduplicate data before re-validating.
  - **If `response.isError === false`**:
    - Interpret `response.summary`:
      - `CreatedIndexSuccess`: The physical index was newly created in the database.
      - `ModifiedIndexSuccess`: The physical index was dropped and recreated to match changes in column order or definition.
      - `NoChangesToIndex`: The physical index in the database already matches the Application Dictionary definition (normal idempotent success).

### 6. Report Outcome

Report the index status clearly to the user:
- **Table**: `<TableName>` (`AD_Table_ID`)
- **Index Name**: `<IndexName>` (`AD_TableIndex_ID`)
- **Index Type**: `Unique Index` (`IsUnique=true`), `Unique Constraint` (`IsCreateConstraint=true`), or `Standard Index`
- **Indexed Columns**: Ordered list of indexed columns with their respective `SeqNo`
- **Validation Outcome**: `response.summary` (e.g. `CreatedIndexSuccess`, `ModifiedIndexSuccess`, or `NoChangesToIndex`)
