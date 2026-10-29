---
name: idempiere-generate-model
description: Generate or re-generate iDempiere Java model interface (I_*) and implementation class (X_*) for a database table using the generatemodel process.
---

# Generate iDempiere Model Class

This skill generates or re-generates Java model interfaces (`I_<TableName>.java`) and/or PO implementation classes (`X_<TableName>.java`) for one or more iDempiere tables by resolving `AD_Table` / `AD_EntityType` metadata and running the **Generate Model** process (`process_value`: `"generatemodel"`, `AD_Process_ID`: `200154`).

## Authentication & Prerequisites

- **System Administrator Required**: `GenerateModel` (`AD_Process_ID = 200154`) has `AccessLevel = 4` (System only). You **must** authenticate to the **System tenant (`Client 0`)** using the `idempiere-authenticate-system` skill before running this skill. If the current session holds a tenant token, switch or re-authenticate to System first.
- Pass `_authorization_bearer` with the raw `IDEMPIERE_BEARER_TOKEN` (without the `Bearer ` prefix) on every tool call. Never display `IDEMPIERE_BEARER_TOKEN` in user-facing output.

## Inputs

### Mandatory Inputs

| Input | Description |
|---|---|
| `TableName`, `AD_Table_ID`, or `TableLike` | Target table name (e.g. `C_Order`), numeric `AD_Table_ID`, or a SQL `LIKE` pattern / comma-separated table list (e.g. `XX_%` or `C_Order,C_OrderLine`). At least one is required. |
| `Folder` | Absolute path to the Java **source root** folder on the iDempiere server's filesystem (e.g. `/home/user/workspace/idempiere/org.adempiere.base/src`). Prompt the user if not provided. |

### Optional Inputs & Defaults

| Input | Description | Default |
|---|---|---|
| `PackageName` | Java package name for the generated interface/class. | `AD_EntityType.ModelPackage` if defined for the table's `EntityType`; otherwise `org.compiere.model`. Prompt user to confirm. |
| `TableEntityType` | Entity type code(s) of the target table(s) (`AD_Table.EntityType`, max 20 chars, e.g. `"D"`, `"U"`, or `"U,A"`). | Resolved automatically from `AD_Table.EntityType` for a single table. |
| `ColumnEntityType` | Optional entity type filter for columns (`AD_Column.EntityType`, max 20 chars). | Omitted (all active columns) unless specified; see **ColumnEntityType Guidance** below. |
| `IsGenerateInterface` | Generate Java interface (`I_<TableName>.java`). `"Y"` or `"N"`. | `"Y"` |
| `IsGenerateClass` | Generate Java implementation class (`X_<TableName>.java`). `"Y"` or `"N"`. | `"Y"` |
| `AD_ModelGeneratorTemplate_ID` / `TemplateName` | Optional `AD_ModelGeneratorTemplate` record to pre-populate `Folder`, `PackageName`, `TableLike`, `TableEntityType`, and `ColumnEntityType`. | (None) |

---

## Workflow

### 1. Resolve Table and `TableEntityType`

Use mutually exclusive branches so only the minimum necessary MCP call is made:

1. **If both canonical `TableName` and scalar `TableEntityType` are already known** (for example, passed directly from `idempiere-create-table`):
   - Reuse `TableName` and `TableEntityType` directly without querying `AD_Table` again.

2. **Else if `AD_Table_ID` is provided**:
   - Call `idempiere_model_get_record` with `model: "AD_Table"` and `id: "<AD_Table_ID>"` (pass `id` as a string):
     ```json
     {
       "model": "AD_Table",
       "id": "<AD_Table_ID>",
       "_authorization_bearer": "<IDEMPIERE_BEARER_TOKEN>"
     }
     ```
   - If not found, report it and **stop**.
   - Extract `TableName`, `IsActive`, `IsView`, and the scalar `EntityType` code (`response.EntityType.id` if `EntityType` is an object, or `response.EntityType` if a string — **never** use `response.EntityType.identifier` such as `"Dictionary"`).

3. **Else if `TableName` is provided** (single table name without `%` or `,`):
   - Validate that `TableName` matches `^[A-Za-z][A-Za-z0-9_]*$`. If it contains invalid characters, reject it and **stop** before building an OData filter.
   - Perform a single case-insensitive search on `AD_Table` using `idempiere_model_search_records`:
     ```json
     {
       "model": "AD_Table",
       "filter": "tolower(TableName) eq '<lowercase_TableName>'",
       "_authorization_bearer": "<IDEMPIERE_BEARER_TOKEN>"
     }
     ```
   - If no record is found, report that table `<TableName>` does not exist in `AD_Table` and **stop**.
   - If multiple records are returned, prefer the exact case match if unambiguous; otherwise display the candidates and ask the user to choose.
   - Retain `AD_Table_ID` (`record.id`), canonical `TableName` (`record.TableName`), `IsActive`, `IsView`, and scalar `EntityType` (`record.EntityType.id` or `record.EntityType`).

4. **Else if `TableLike` is a wildcard pattern (`%`) or comma-separated list**:
   - Validate that `TableLike` contains only valid table identifier characters, commas, single quotes, or `%` (`^[A-Za-z0-9_% ,']+$`).
   - If `TableEntityType` was not supplied, prompt the user for the scalar entity type code(s) (e.g. `"U"`, `"D"`).

#### Pre-Execution Table Validation (Prevent Silent No-Ops)

`ModelInterfaceGenerator` executes:
```sql
SELECT AD_Table_ID FROM AD_Table
WHERE IsActive = 'Y' AND TableName NOT LIKE '%_Trl'
  AND UPPER(TableName) LIKE ...
  AND EntityType IN (...)
```
When 0 rows match a single-table `LIKE` query, `GenerateModel` still returns `"@ProcessOK@"` with `isError: false` while writing **no files**. Always check before proceeding:
- **Inactive Table**: If `IsActive === false`, report that `GenerateModel` skips inactive tables (`IsActive = 'N'`) and **stop**.
- **Translation Table (`*_Trl`)**: If `TableName` ends with `_Trl` (case-insensitive), report that `GenerateModel` explicitly excludes `*_Trl` tables (`TableName NOT LIKE '%_Trl'`) and **stop**.
- **Database View (`IsView === true`)**: If `IsView === true` and `TableLike` contains `%` (and the view is not `RV_WarehousePrice` or `RV_BPartner`), warn the user that wildcard generation skips views (`IsView = 'N'`); pass the exact view name without `%` if generating a model for a view.
- **EntityType Mismatch**: Always pass the scalar `EntityType` code (`"D"`, `"U"`, etc.), never the display name. If `TableEntityType` is omitted, `ModelInterfaceGenerator` defaults to `EntityType IN ('U','A')` and silently skips dictionary (`"D"`) or custom entity type tables.

---

### 2. Resolve Defaults & Confirm Parameters

#### A. Optional Template Lookup (`AD_ModelGeneratorTemplate`)
If the user specifies a template name or `AD_ModelGeneratorTemplate_ID`, query `AD_ModelGeneratorTemplate` via `idempiere_model_search_records` or `idempiere_model_get_record`:
- Substitute `@TableName@` with the resolved `<TableName>` and `@EntityType@` with `<TableEntityType>`.
- Use the template's `Folder`, `PackageName`, `TableEntityType`, and `ColumnEntityType` as defaults.

#### B. Resolve Default `PackageName` from `AD_EntityType`
If `PackageName` was not provided by the user or a template:
- If `TableEntityType == "D"`: default `PackageName` to `org.compiere.model`.
- If `TableEntityType` is a single non-`"D"` code (e.g. `"U"` or a custom plugin entity type):
  - Query `AD_EntityType` via `idempiere_model_search_records`:
    ```json
    {
      "model": "AD_EntityType",
      "filter": "EntityType eq '<TableEntityType>'",
      "_authorization_bearer": "<IDEMPIERE_BEARER_TOKEN>"
    }
    ```
  - If `records[0].ModelPackage` is non-empty, propose `records[0].ModelPackage` as the default `PackageName`; otherwise default to `org.compiere.model`.

#### C. `ColumnEntityType` Guidance
- **Full Table Model (Default)**: Leave `ColumnEntityType` empty/omitted to generate getters/setters for all active non-SQL columns (`ColumnSQL IS NULL OR ColumnSQL NOT LIKE '@SQL%'`) on the table.
- **Core Dictionary Table (`TableEntityType == "D"`, `PackageName == "org.compiere.model"`)**: Recommend `ColumnEntityType: "D"` if the database may contain custom (`"U"` or plugin) columns on that core table, so custom columns do not pollute core `I_*` / `X_*` sources. (Note: `ModelInterfaceGenerator` rule S4 refuses writing non-`"D"` `ColumnEntityType` into `org.compiere.*` or `org.adempiere.*` packages.)
- **Plugin Extending a Core Table** (custom columns with `EntityType = "<PluginEntityType>"` on a core `"D"` table): Set `TableEntityType: "D"`, `ColumnEntityType: "<PluginEntityType>"`, `PackageName: "<plugin.package>"`, `IsGenerateInterface: "Y"`, and `IsGenerateClass: "N"` to generate only the extension interface.

#### D. Validate `Folder` and Prompt User
1. If `Folder` was not supplied, prompt the user for the target source root folder (if workspace context is available, propose `<workspace>/org.adempiere.base/src` when `TableEntityType == "D"` or `<bundle>/src` for a plugin bundle).
2. **Source Root Check (Prevent Double Package Nesting)**:
   - `ModelInterfaceGenerator` appends `<PackageName>` (converting `.` to `/`) onto `Folder` and creates the package subdirectories automatically.
   - Check whether the user-supplied `Folder` already ends with `<package_path>` (for example, `.../src/org/compiere/model` when `PackageName` is `org.compiere.model`). If so, strip the trailing `/<package_path>` so `Folder` points to `.../src` and inform the user.
3. **Existence Check**:
   - `ModelInterfaceGenerator` throws `IllegalArgumentException("Source folder doesn't exists. sourceFolder=...")` if the base `Folder` does not already exist on the iDempiere server's filesystem.
   - When the server runs locally on the same workspace filesystem, verify that `Folder` exists on disk before calling `idempiere_process_run`.
4. Display the resolved parameters and confirm with the user (unless they already asked to proceed without confirmation):

| Parameter | Value |
|---|---|
| `TableLike` | `<TableName_or_Pattern>` |
| `TableEntityType` | `<TableEntityType>` |
| `ColumnEntityType` | `<ColumnEntityType_or_(all)>` |
| `Folder` (source root) | `<Folder>` |
| `PackageName` | `<PackageName>` |
| `IsGenerateInterface` | `<Y/N>` |
| `IsGenerateClass` | `<Y/N>` |
| Output Directory | `<Folder>/<package_path>` |

---

### 3. Run the `generatemodel` Process

Call `idempiere_process_run` with `process_value: "generatemodel"`:

```json
{
  "process_value": "generatemodel",
  "parameters": {
    "TableLike": "<TableName>",
    "TableEntityType": "<TableEntityType>",
    "Folder": "<Folder>",
    "PackageName": "<PackageName>",
    "IsGenerateInterface": "Y",
    "IsGenerateClass": "Y"
  },
  "_authorization_bearer": "<IDEMPIERE_BEARER_TOKEN>"
}
```

Rules:
- Keep `"IsGenerateInterface"` and `"IsGenerateClass"` as `"Y"` or `"N"` strings.
- Include `"ColumnEntityType": "<ColumnEntityType>"` in `parameters` **only** when non-empty (omit it when generating all columns).
- Note: For a single table name containing `_` (e.g. `XX_Order`), `ModelInterfaceGenerator` uses `UPPER(TableName) LIKE 'XX_ORDER'` where `_` is a single-character SQL wildcard; if another active table with the same `EntityType` matches that pattern (e.g. `XXAOrder`), both will be generated.

---

### 4. Verify and Report Outcome

1. **Check Process Response**:
   - Inspect `response.isError`, `response.summary`, and `response.logs`.
   - If `response.isError === true`, report `response.summary` and `response.logs` and **stop**.

2. **Verify Generated Files on Disk**:
   - Compute `<package_path>` = `<PackageName>` with `.` replaced by `/`, and `<target_dir>` = `<Folder>/<package_path>`.
   - **Single Table (`<TableName>`)**:
     - Expected interface (if `IsGenerateInterface == "Y"`): `<target_dir>/I_<TableName>.java`
     - Expected class (if `IsGenerateClass == "Y"`): `<target_dir>/X_<TableName>.java`
     - When `<target_dir>` is on the local filesystem, verify that the expected file(s) exist on disk. If they are missing despite `response.isError === false`, report that `generatemodel` matched 0 tables/columns (verify `TableEntityType`, `ColumnEntityType`, and `AD_Table.IsActive`) instead of reporting success.
   - **Wildcard Pattern or Comma-Separated List (`TableLike`)**:
     - List the generated `I_*.java` and `X_*.java` files in `<target_dir>` corresponding to the matched tables.

3. **Report Summary**:
   - **Table(s)**: `<TableName>` (`AD_Table_ID: <AD_Table_ID>`) or `<TableLike>`
   - **Entity Type**: `<TableEntityType>` (and `ColumnEntityType` if used)
   - **Target Directory**: `<Folder>/<package_path>`
   - **Files Generated**:
     - `I_<TableName>.java` (when `IsGenerateInterface = "Y"`)
     - `X_<TableName>.java` (when `IsGenerateClass = "Y"`)
   - **Process Summary**: `response.summary`
