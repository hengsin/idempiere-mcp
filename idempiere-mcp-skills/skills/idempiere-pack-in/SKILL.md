---
name: idempiere-pack-in
description: Pack in a 2Pack archive zip file into iDempiere using MCP tools. Validates the archive format, creates an AD_Package_Imp_Proc record, uploads the archive as an attachment, and executes the PackIn process.
---

# Pack In 2Pack Archive

This skill imports a 2Pack package archive into iDempiere using the **Pack In** window (`pack-in`) and process (`PackIn`).

## Mandatory Inputs

| Input | Description |
|---|---|
| `ArchiveFile` | Path to the 2Pack archive zip file, e.g. `/path/to/2Pack_1.0.0.zip`. |

If `ArchiveFile` is not provided, prompt the user for the path to the 2Pack archive zip file and **stop** until provided.

## Prerequisites & Authentication

- `PackInHandler` enforces that the authenticated session matches the target client declared in the 2Pack's root `Client` attribute (e.g., `0-SYSTEM-System` vs. `11-GardenWorld-GardenWorld`):
  - If `Client` starts with `0-` (System), authenticate to the **System** tenant (Client `0`) using the `idempiere-authenticate-system` skill (`PackIn` throws `"Package is created for System, not Tenant"` if run in a tenant).
  - If `Client` starts with a non-zero tenant ID (`<id>-<clientValue>-<clientName>`), authenticate to that tenant using the `idempiere-authenticate-tenant` skill (`PackIn` throws `"Package is created for Tenant, not System"` if run in System).
- Ensure `IDEMPIERE_BEARER_TOKEN` is available. Re-run the appropriate authentication skill if not authenticated or if a tool call returns `401`.
- Pass `_authorization_bearer` with the raw JWT token (without `Bearer ` prefix) on every MCP tool call.

## Optional Execution Options

| Option | Description |
|---|---|
| `AD_Override_Dict` | Update System Maintained Application Dictionary on `AD_Package_Imp_Proc` (`true` or `false`, default `false`). |
| `logMigrationScript` | Log SQL migration script on server (`true` or `false`). |
| `adempiereSys` | Dictionary maintenance mode (centralized IDs). `true` or `false`. |
| `migrationScriptComment` | Comment for migration script logging (e.g., ticket number and summary). |

---

## Workflow

### 1. Validate Input Archive

1. **Check File Existence**: Ensure the specified file exists on disk and is readable.
2. **Validate 2Pack Format**:
   - Check that the file is a valid zip archive and contains a 2Pack descriptor file (`PackOut.xml`, `PackOut.json`, or `PackOut.yaml`) inside a single top-level package folder (`<package_name>/dict/PackOut.*`), as required by `PackInProcess`:
     ```bash
     unzip -Z1 "<ArchiveFile>" | grep -E "^[^/]+/dict/PackOut\.(xml|json|yaml)$"
     ```
   - Inspect the root `Client` attribute in `<package_name>/dict/PackOut.*` (or `<package_name>/doc/*Doc.*`) to verify whether the package targets the System tenant (`0-...`) or a specific tenant (`<id>-<value>-<name>`), and ensure the current `IDEMPIERE_BEARER_TOKEN` matches that tenant.
   - If the file does not exist, is not a valid zip archive, or does not contain `<package_name>/dict/PackOut.*`, report the error to the user and **halt**. Do not proceed.

### 2. Create 2Pack Record (`AD_Package_Imp_Proc`)

Extract the file name without directory path (e.g., `2Pack_1.0.0.zip` from `/path/to/2Pack_1.0.0.zip`).

Create a new record in `AD_Package_Imp_Proc` using `idempiere_model_create_record` (or via the **Pack In** window, slug: `pack-in`):

```json
{
  "_authorization_bearer": "<IDEMPIERE_BEARER_TOKEN>",
  "model": "AD_Package_Imp_Proc",
  "data": {
    "Name": "<fileName>"
  }
}
```

> **Note**: If using `idempiere_window_create_record`, specify `"window_name": "pack-in"`.

- If the user specified `AD_Override_Dict`, include `"AD_Override_Dict": true` (or `false`) in `data`.
- Verify that the record was created successfully.
- Extract and retain the numeric record ID as `AD_Package_Imp_Proc_ID` (from the returned `id` property).

### 3. Upload Archive as Attachment

Attach the 2Pack archive file to the `AD_Package_Imp_Proc` record using `idempiere_model_add_record_attachment`. Pass the local file path or URI path of the archive file directly in `data` (no Base64 encoding required):

```json
{
  "_authorization_bearer": "<IDEMPIERE_BEARER_TOKEN>",
  "tableName": "AD_Package_Imp_Proc",
  "record_id": "<AD_Package_Imp_Proc_ID>",
  "data": "<ArchiveFile>"
}
```

- Optionally specify `name`: `"<fileName>"` if you wish to override the attachment file name (if omitted, the file name is automatically derived from `<ArchiveFile>`).
- Verify that the attachment was added successfully from the response. If the tool returns an error, report it and **halt**.

### 4. Run Pack In Process

Execute the Pack In process using `idempiere_process_run`:

- `process_value`: `PackIn` (slug: `packin`)
- `record_id`: `<AD_Package_Imp_Proc_ID>` (must be passed as an **integer**, not a string)

```json
{
  "_authorization_bearer": "<IDEMPIERE_BEARER_TOKEN>",
  "process_value": "PackIn",
  "record_id": <AD_Package_Imp_Proc_ID>
}
```

Optional migration script parameters (`logMigrationScript`, `adempiereSys`, `migrationScriptComment`) may be passed if requested by the user:

```json
{
  "_authorization_bearer": "<IDEMPIERE_BEARER_TOKEN>",
  "process_value": "PackIn",
  "record_id": <AD_Package_Imp_Proc_ID>,
  "logMigrationScript": true,
  "adempiereSys": false,
  "migrationScriptComment": "<comment>"
}
```

### 5. Report Results

- Check `isError` and `summary` in the `idempiere_process_run` response (note: the REST endpoint returns HTTP 200 even when `PackIn` fails; errors are indicated by `"isError": true`, an unresolved count in `summary` such as `"Un-Resolved=..."`, or `"Please attach the pack out archive before running the process"`).
- Report the `AD_Package_Imp_Proc_ID`, package name, and `summary` outcome clearly to the user.
- If `isError` is `true` or `Un-Resolved > 0`:
  - Present the error details from `summary`, `logs`, or `AD_Package_Imp_Proc.P_Msg`.
  - Optionally query `AD_Package_Imp` (with `filter: "AD_Package_Imp_Proc_ID eq <AD_Package_Imp_Proc_ID>"`) and its `AD_Package_Imp_Detail` records using `idempiere_model_search_records` to report the specific elements that failed or remained unresolved.
