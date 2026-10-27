---
name: idempiere-add-field-to-tab
description: Add a new field to an existing iDempiere window tab. Validates the AD_Window, AD_Tab, and AD_Column (or optionally adds a missing column to the table), confirms the AD_Field values, then creates the AD_Field record.
---

# Add Field to iDempiere Window Tab

This skill adds a new field (`AD_Field`) to an existing tab on an iDempiere window using the `Window, Tab & Field` window (`window-tab-and-field`).

## Mandatory Inputs

| Input | Description |
|---|---|
| `WindowName` | Window name (`AD_Window.Name`), e.g. `Sales Order`. |
| `TabName` or `TabIndex` | Target tab name (`AD_Tab.Name`) or 0-based index in the tab list sorted by `SeqNo`. |
| `ColumnName` | Database column name (`AD_Column.ColumnName`). |

All three parameters (or `TabIndex` in place of `TabName`) are **mandatory**. If `WindowName`, `ColumnName`, or both `TabName` and `TabIndex` are missing, ask the user for the missing input and **stop**. Do not guess or derive them.

`TabIndex` is **0-based** and refers to the position in the tab list sorted by `SeqNo` (see Step 2). If the user gives a 1-based ordinal such as "the third tab", subtract one and confirm the resolved tab name before continuing. If both `TabName` and `TabIndex` are provided and they resolve to different tabs, show both and ask the user which one to use.

## Default Field Inputs

The following default values must be applied for `AD_Field` unless explicitly overridden by the user:

| Input | Default Value | Description |
|---|---|---|
| `XPosition` | `1` | Column position: `1` = left column, `2` = right column |
| `ColumnSpan` | `2` | Number of layout columns the field spans. `XPosition: 1` + `ColumnSpan: 2` gives a full-width field. |

> Use `XPosition: 2` with `ColumnSpan: 1` for a right-column field. Avoid `XPosition: 2` with `ColumnSpan: 2`, which overflows the two-column tab layout.

## Other Optional Inputs

- Any valid `AD_Field` column name provided by the user (for example `Name`, `Description`, `Help`, `IsDisplayed`, `IsDisplayedGrid`, `IsReadOnly`, `IsSameLine`, `NumLines`, `DisplayLogic`, `ReadOnlyLogic`, `MandatoryLogic`, `EntityType`).
- Migration and dictionary flags:
  - `logMigrationScript`: Log migration script (`true` or `false`).
  - `adempiereSys`: Dictionary maintenance (i.e. `EntityType=D`). `true` or `false`. Default to `false`.
  - `migrationScriptComment`: Migration script comment. Usually ticket number and summary text.

Use `_authorization_bearer` with the raw `IDEMPIERE_BEARER_TOKEN` (without the `Bearer ` prefix) on every tool call (use the `idempiere-authenticate-system` skill to obtain a token if not already authenticated).

---

## Workflow

### 1. Check Existence of `AD_Window` Record

Search for the window using `idempiere_model_search_records` on `AD_Window`:

```json
{
  "model": "AD_Window",
  "filter": "Name eq '<WindowName>' and IsActive eq true",
  "_authorization_bearer": "<IDEMPIERE_BEARER_TOKEN>"
}
```

> **Note**: If `WindowName` contains single quotes, escape them (e.g. `''`) in the OData filter string.

- If no record exists: re-run the search without `IsActive eq true` to check whether only an inactive window exists. Report the result (`AD_Window` with `Name = '<WindowName>'` does not exist, or exists but is inactive) and **stop**.
- If multiple records exist: display them and ask the user which one to use.
- If found: retain `AD_Window_ID` (`id` of the record).

### 2. Validate `TabName` or `TabIndex`

Search for tabs under the window using `idempiere_model_search_records` on `AD_Tab`:

```json
{
  "model": "AD_Tab",
  "filter": "AD_Window_ID eq <AD_Window_ID> and IsActive eq true",
  "limit": 100,
  "_authorization_bearer": "<IDEMPIERE_BEARER_TOKEN>"
}
```

> **Notes**:
> - Set `"limit": 100` to prevent pagination truncation on large windows with more than 10 tabs.
> - The order of records returned by the search is **not guaranteed**. Sort the returned tabs by `SeqNo` ascending (then `Name`) and use that order when resolving `TabIndex`.

Validate against the sorted list of `AD_Tab` records:
- If `TabName` is provided: find records whose trimmed `Name` matches the trimmed `TabName` (case-insensitive). If multiple tabs match, display them and ask the user which one to use.
- If `TabIndex` is provided: check that it is a valid 0-based index (`0 <= TabIndex < records.length`) in the sorted list, and resolve its `Name` and `SeqNo`.
- If both are provided and they resolve to different tabs, display both and ask the user which one to use.
- If validation fails (empty list, `TabName` not found, or `TabIndex` out of bounds): if the unfiltered search returns only inactive tabs, report that; otherwise report the failure. **Stop** in either case.
- If valid: display the resolved tab (`Name`, `SeqNo`) to the user in the confirmation, then retain `AD_Tab_ID`, `AD_Table_ID` (extract the numeric `.id` if returned as an object `{ "id": ..., "identifier": ... }`), and the tab's `SeqNo`.

### 3. Validate `ColumnName`

Search for the column in the parent table using `idempiere_model_search_records` on `AD_Column`:

```json
{
  "model": "AD_Column",
  "filter": "AD_Table_ID eq <AD_Table_ID> and ColumnName eq '<ColumnName>'",
  "_authorization_bearer": "<IDEMPIERE_BEARER_TOKEN>"
}
```

- If the record exists: retain `AD_Column_ID`. If more than one record is returned, display them and ask the user which one to use. If the matching column is inactive, report that and ask the user whether to continue before using it.
- If the record does **not** exist:
  - Ask the user whether to **stop** or **add `ColumnName` to the table**.
  - If the user chooses to stop: **stop**.
  - If the user chooses to add `ColumnName` to the table:
    1. Resolve `TableName` from the `AD_Table_ID.identifier` value on the tab record, or by calling `idempiere_model_get_record` with `model: "AD_Table"` and `id: <AD_Table_ID>`.
    2. Tell the user that the `idempiere-add-column-to-table` skill will prompt for `Reference` and other column details, and that it runs the **Synchronize Column** DDL process (plus optional index and model regeneration). If the user does not want that, **stop**.
    3. Execute the `idempiere-add-column-to-table` skill with `TableName` and `ColumnName`.
    4. If adding the column fails, **stop**.
    5. Otherwise, retain the newly created `AD_Column_ID`.

### 4. Add New Field (`AD_Field`)

#### 4.1 Check for Existing Field

Search for an existing field on the tab using `idempiere_model_search_records` on `AD_Field`:

```json
{
  "model": "AD_Field",
  "filter": "AD_Tab_ID eq <AD_Tab_ID> and AD_Column_ID eq <AD_Column_ID>",
  "_authorization_bearer": "<IDEMPIERE_BEARER_TOKEN>"
}
```

- If a record already exists (active or inactive): inform the user that the field already exists on the tab, including its `IsActive` value and `AD_Field_ID`, and **stop**. Do not create a duplicate.

#### 4.2 Dry-run Creation (`save: false`)

Call `idempiere_window_create_child_tab_record` with `save: false` to trigger server defaults and callouts without persisting:

- `window_name`: `window-tab-and-field`
- `tab_name`: `tab`
- `record_id`: the `<AD_Tab_ID>` from Step 2
- `child_tab_name`: `field`

```json
{
  "window_name": "window-tab-and-field",
  "tab_name": "tab",
  "record_id": "<AD_Tab_ID>",
  "child_tab_name": "field",
  "data": {
    "AD_Column_ID": <AD_Column_ID>,
    "XPosition": 1,
    "ColumnSpan": 2
  },
  "save": false,
  "_authorization_bearer": "<IDEMPIERE_BEARER_TOKEN>"
}
```

- If the user explicitly provided custom values for `SeqNo`, `SeqNoGrid`, `XPosition`, `ColumnSpan`, or any other optional `AD_Field` properties, include them in `data` to override the defaults.
- Display the resulting field values in a table for the user to confirm. At minimum include `Name`, `Description`, `Help`, `AD_Column_ID`, `SeqNo`, `SeqNoGrid`, `XPosition`, `ColumnSpan`, `IsDisplayed`, `IsDisplayedGrid`, `IsReadOnly`, `IsSameLine`, and `EntityType`.
- Check for and prompt the user for any missing mandatory fields.
- Keep the returned values from the dry run (including server defaults and callout results) so they can be sent to the save call in Step 4.3. Unless the user already asked to proceed without confirmation, wait for their confirmation before saving.

#### 4.3 Save Record (`save: true`)

After user confirmation (and collecting any missing mandatory fields):

1. Build the save `data` by merging the writable `AD_Field` values from the dry-run response with the user-confirmed overrides applied on top. This preserves server defaults and callout results while keeping any user-supplied values. Exclude read-only or derived properties such as `AD_Field_ID`, `AD_Client_ID`, `AD_Org_ID`, and `AD_Tab_ID` from `data`.
2. Call `idempiere_window_create_child_tab_record` with the merged `data` and `"save": true`.
   - Pass `adempiereSys`, `logMigrationScript`, and `migrationScriptComment` if provided.
3. If the tool returns an error, report it as-is and **stop**. Do not retry with modified values unless the user provides them.
4. Report the created `AD_Field` record details (including `AD_Field_ID`, `Name`, `AD_Column_ID`, `SeqNo`, `SeqNoGrid`, `XPosition`, `ColumnSpan`) and tell the user the field becomes visible after the iDempiere cache is reset or the user re-logs in.

---

## Notes & Edge Cases

- **Window & Tab Slugs**: Use `"window_name": "window-tab-and-field"`, `"tab_name": "tab"`, `"record_id": "<AD_Tab_ID>"`, and `"child_tab_name": "field"`.
- **Foreign Key Lookups**: In iDempiere REST responses, foreign keys such as `AD_Table_ID` or `AD_Column_ID` can be returned as objects (e.g. `{"id": 259, "identifier": "C_Order"}`). Always extract the numeric `.id` for filter queries.
- **Escape Quotes**: When constructing OData filter strings, escape any single quotes in window, tab, or column names (e.g., `'` becomes `''`).
- **Tab Ordering**: `TabIndex` is 0-based and resolved against tabs sorted by `SeqNo`, then `Name`. The search API does not guarantee result order, so never index into the raw response.
- **Layout**: `XPosition` is `1` (left) or `2` (right). `ColumnSpan` normally matches a full-width field only when starting at `XPosition: 1`; use `ColumnSpan: 1` for a right-column field.
- **Field Position**: `SeqNo: 0` places the new field before all existing fields on the tab. Use a higher `SeqNo` (for example the current maximum plus 10) to append it.
- **Visibility**: A new `AD_Field` appears in the UI only after the cache is reset or the user re-logs in.
- **Inactive Records**: Inactive `AD_Window` and `AD_Tab` records are excluded from searches. If a search returns nothing, re-run it without `IsActive eq true` to report whether only an inactive record exists. The `AD_Field` existence check intentionally includes inactive fields so a duplicate is never created.
