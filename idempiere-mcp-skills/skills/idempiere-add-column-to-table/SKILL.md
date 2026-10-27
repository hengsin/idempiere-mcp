---
name: idempiere-add-column-to-table
description: Add one new column to an existing iDempiere table. Validates or creates the AD_Element, validates the AD_Reference, confirms the column values, synchronizes the database, and optionally adds a table index.
---

# Add Column to iDempiere Table

This skill adds a single column to an existing iDempiere table using the `Table and Column` window (`table-and-column`), then synchronizes the database.

## Mandatory Inputs

| Input | Description |
|---|---|
| `TableName` | Database table name, e.g. `C_Order`. |
| `ColumnName` | Database column name, e.g. `XX_CustomerFeedback`. |

All two are **mandatory**. If any is missing, ask the user for it and stop. Do not guess or derive them.

Other input provided by the user is optional and should be passed through to the column record using its `AD_Column` column name (for example `FieldLength`, `IsMandatory`, `IsIdentifier`, `IsSelectionColumn`, `AD_Reference_Value_ID`, `DefaultValue`, `Callout`, `EntityType`).

## Other Common Inputs
| Input | Description |
|---|---|
| `Reference` | Reference name (`AD_Reference.Name`), e.g. `String`, `TableDir`, `Table`. |
| `FieldLength` | Column length, e.g. 1, 10, 20. |
| `DefaultValue` | Default value of column, e.g. Y, 0, 1. |

## Other options
| Input | Description |
|---|---|
| `logMigrationScript` | Log migration script. true or false. |
| `adempiereSys` | Dictionary maintenance (i.e EntityType=D). true of false. Default to false.|
| `migrationScriptComment` | Migration script comment. Usually ticket number and summary text. |

Use `_authorization_bearer` with the raw `IDEMPIERE_BEARER_TOKEN` (without the `Bearer ` prefix) on every tool call (use the `idempiere-authenticate-system` skill to obtain a token if not already authenticated).
## Workflow

### 1. Check the Element

Use `idempiere_model_search_records` on `AD_Element` to check whether an element already exists for `ColumnName`:

```json
{
  "model": "AD_Element",
  "filter": "ColumnName eq '<ColumnName>'",
  "_authorization_bearer": "<IDEMPIERE_BEARER_TOKEN>"
}
```

- If exactly one record exists, keep its `AD_Element_ID`.
- If multiple records exist, show them and ask the user which one to use.
- If no record exists, use the `idempiere-create-element` skill to create a new `AD_Element` record for this `ColumnName`. That skill will ask the user for the element `Name` and any optional fields. Keep the returned `AD_Element_ID`.

### 2. Validate the Reference

Use `idempiere_model_search_records` on `AD_Reference` to resolve the reference name:

```json
{
  "model": "AD_Reference",
  "filter": "Name eq '<Reference>'",
  "_authorization_bearer": "<IDEMPIERE_BEARER_TOKEN>"
}
```

- If exactly one record is found, keep its `AD_Reference_ID`.
- If no record is found, report that the reference is invalid and **stop**. Do not create the column.
- If multiple records are found, show them and ask the user which one to use.

### 3. Find the Table

Use `idempiere_window_get_records` with the `Table and Column` window (`table-and-column`) to find the parent table record:

```json
{
  "window_name": "table-and-column",
  "filter": "TableName eq '<TableName>'",
  "_authorization_bearer": "<IDEMPIERE_BEARER_TOKEN>"
}
```

- If no table is found, report it and stop.
- If more than one record is returned, ask the user to choose.
- Keep the `AD_Table_ID`.

> `idempiere_window_get_record` retrieves one record by `record_id` and cannot search by `TableName`. Use its search counterpart `idempiere_window_get_records` with the filter above. If the server exposes search criteria directly on `idempiere_window_get_record`, pass the same filter there.

### 4. Add the Column

Use `idempiere_window_create_child_tab_record` with the `Column` child tab of the table record.

- `window_name`: `table-and-column`
- `tab_name`: `table`
- `record_id`: the `AD_Table_ID` from step 3
- `child_tab_name`: `column`

The window fields `Element` and `Reference` map to `AD_Element_ID` and `AD_Reference_ID`:

- `Element` = the element whose `ColumnName` is `<ColumnName>` (from step 1)
- `Reference` = the reference whose `Name` is `<Reference>` (from step 2)

First create the record **without saving**:

```json
{
  "window_name": "table-and-column",
  "tab_name": "table",
  "record_id": "<AD_Table_ID>",
  "child_tab_name": "column",
  "data": {
    "AD_Element_ID": <AD_Element_ID>,
    "AD_Reference_ID": <AD_Reference_ID>
  },
  "save": false,
  "_authorization_bearer": "<IDEMPIERE_BEARER_TOKEN>"
}
```

Add any other user-supplied fields to `data`. MUST NOT include the `ColumnName` property in `data`. This dry run applies defaults and callouts without persisting the record.

From the response, show the user a table of the resulting field values. At minimum include `ColumnName`, `Name`, `AD_Element_ID`, `AD_Reference_ID`, `AD_Reference_Value_ID`, `FieldLength`, `IsMandatory`, `IsIdentifier`, and `IsSelectionColumn`. Prompt for any missing mandatory field or required value.

If a name is provided by user for AD_Reference_Value_ID, validate that it exists for `AD_Reference` and is applicable for the `AD_Reference_ID` value.


When the values are complete and the user confirms, call `idempiere_window_create_child_tab_record` again with the same `data` and `save: true`.
- Pass `adempiereSys`, `logMigrationScript`, and `migrationScriptComment` if provided.

Keep the new `AD_Column_ID` from the response.

> If the server expects lookup objects instead of ids, use `{"lookupColumn": "ColumnName", "lookupValue": "<ColumnName>"}` for `AD_Element_ID` and `{"identifier": "<Reference>"}` for `AD_Reference_ID`.

### 5. Update the Database

Run the **Synchronize Column** process to create or alter the physical column:

- Use `idempiere_process_run` with `ad_column-sync` as process_value and `AD_Column_ID` value as the `record-id` parameter.
- Report any error returned by the process. Do not continue to the index step after a synchronization failure.

### 6. Optional Table Index

Ask the user whether to add an index for the new column.

If yes, collect:

- Index name
- Columns or SQL expression that comes before
- Columns or SQL expression that comes after
- Whether the index is unique

Add the index with `idempiere_window_create_child_tab_record` on the `Table Index` child tab:

```json
{
  "window_name": "table-and-column",
  "tab_name": "table",
  "record_id": "<AD_Table_ID>",
  "child_tab_name": "table-index",
  "data": {
    "Name": "<IndexName>",
    "IsUnique": false,
    "index-column": [
      { "AD_Column_ID": <Before_AD_Column_ID> }
      { "AD_Column_ID": <AD_Column_ID> },
      { "AD_Column_ID": <After_AD_Column_ID> }
    ]
  },
  "_authorization_bearer": "<IDEMPIERE_BEARER_TOKEN>"
}
```

- The `index-column` array creates the `Index Column` (`AD_IndexColumn`) child records.
- Include the newly added column and every other column selected for the index.
- Set `IsUnique` according to the user's choice.
- Confirm the values with the user before saving.

Then run the **Index Validate** process to create or verify the physical index:

- Use `idempiere_process_run` with `ad_tableindex-validate` as process_value and the `AD_TableIndex_ID` value as `record-id` parameter.
- Report any error returned by the process.

### Re-Generate model class
Ask user whether to re-generate model class for table.
If yes:
- use `idempiere_model_get_record` tool, `AD_Table_ID` value as id and `AD_Table` as model to get the `TableName` and `EntityType` property.
- Use `idempiere_process_run` with `generatemodel` as process_value and parameters:
  - TableLike=<TableName>
  - TableEntityType=<EntityType>
  - Folder=Prompt for user entry
  - PackageName=Prompt for user entry, default is `org.compiere.model`
  - IsGenerateInterface=Y
  - IsGenerateClass=Y
- Report any error returned by the process.

## Notes

- `Is*` columns are `Y`/`N` boolean columns, e.g. `IsActive`, `IsIdentifier`, `IsSOTrx`.
- `*_ID` columns are primary or foreign keys, e.g. `C_BPartner_ID`, `AD_Reference_Value_ID`.
- Each table needs one UUID column named `$TableName_UU`.
- Use CamelCase for multi-word names, e.g. `WhereClause`, `OrderByClause`.
