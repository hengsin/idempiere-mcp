# iDempiere MCP Server Tools Reference

Comprehensive reference of all **88 Model Context Protocol (MCP) tools** provided by the `org.idempiere.mcp.server` bundle for [iDempiere ERP](https://www.idempiere.org/).

## Overview

- **Supported MCP Protocols**: `2025-11-25`, `2026-07-28` (stateless).
- **Total Tools**: 88 tools organized into 17 functional categories.
- **Transport**: Streamable HTTP Transport (endpoint: `http://<host>:<port>/mcp/`).
- **Authentication Mechanisms**:
  - **HTTP Authorization Header**: `Authorization: Bearer <token>`
  - **Request Body Parameter**: Every tool accepts an optional `_authorization_bearer` argument in `params.arguments`. The server inspects the HTTP header first and falls back to `_authorization_bearer`.
- **Available MCP Resources**:
  - `idempiere://metadata/models`: List all available database tables/models.
  - `idempiere://metadata/processes`: List all available processes, reports, and workflows.

---

<a id="table-of-contents"></a>
## Table of Contents

1. [Authentication (3)](#authentication)
2. [Data Models & Tables (14)](#data-models--tables)
3. [Windows & Tabs (12)](#windows--tabs)
4. [Views (14)](#views)
5. [Processes & Reports (2)](#processes--reports)
6. [Server Jobs (6)](#server-jobs)
7. [Schedulers (3)](#schedulers)
8. [Workflows (6)](#workflows)
9. [Info Windows (5)](#info-windows)
10. [File Uploads (7)](#file-uploads)
11. [Server Nodes & Logs (6)](#server-nodes--logs)
12. [System Caches (2)](#system-caches)
13. [Charts & Visualizations (3)](#charts--visualizations)
14. [Status Lines (2)](#status-lines)
15. [Menu Tree (1)](#menu-tree)
16. [References (1)](#references)
17. [Batch Execution (1)](#batch-execution)
18. [MCP Resources](#mcp-resources)
19. [Common Features & Conventions](#common-features--conventions)

---

<a id="quick-reference-table"></a>
## Quick Reference Table

| Category | Tool Name | Description | Required Parameters |
|---|---|---|---|
| Authentication | [`idempiere_auth_create_token`](#idempiere_auth_create_token) | Create authorization token (login). | `userName`, `password` |
| Authentication | [`idempiere_auth_update_token`](#idempiere_auth_update_token) | Update authorization token. | `clientId`, `roleId`, `organizationId`, `warehouseId`, `language` |
| Authentication | [`idempiere_auth_logout`](#idempiere_auth_logout) | Logout. | *None* |
| Data Models & Tables | [`idempiere_model_search_records`](#idempiere_model_search_records) | Search iDempiere records. | `model` |
| Data Models & Tables | [`idempiere_model_get_record`](#idempiere_model_get_record) | Get a record by ID (Integer or UUID). | `model`, `id` |
| Data Models & Tables | [`idempiere_model_create_record`](#idempiere_model_create_record) | Create a record. | `model`, `data` |
| Data Models & Tables | [`idempiere_model_update_record`](#idempiere_model_update_record) | Update a record. | `model`, `id`, `data` |
| Data Models & Tables | [`idempiere_model_delete_record`](#idempiere_model_delete_record) | Delete record by id. 'idempiere_window_delete_tab_record' tool can also be used to delete record within a window context. | `tableName`, `record_id` |
| Data Models & Tables | [`idempiere_model_get_record_property`](#idempiere_model_get_record_property) | Get property value of a record. | `tableName`, `record_id`, `columnName` |
| Data Models & Tables | [`idempiere_model_get_record_attachments`](#idempiere_model_get_record_attachments) | Get list of attachment of a record | `tableName`, `record_id` |
| Data Models & Tables | [`idempiere_model_add_record_attachment`](#idempiere_model_add_record_attachment) | Add/update attachment by record id and file name | `tableName`, `record_id`, `data` |
| Data Models & Tables | [`idempiere_model_delete_record_attachment`](#idempiere_model_delete_record_attachment) | Delete attachment of a record | `tableName`, `record_id` |
| Data Models & Tables | [`idempiere_model_get_record_attachments_zip`](#idempiere_model_get_record_attachments_zip) | Get attachment of a record as zip archive | `tableName`, `record_id` |
| Data Models & Tables | [`idempiere_model_get_record_attachment_by_name`](#idempiere_model_get_record_attachment_by_name) | Get attachment of a record by file name | `tableName`, `record_id`, `fileName` |
| Data Models & Tables | [`idempiere_model_print_record`](#idempiere_model_print_record) | Print document of a record | `tableName`, `record_id` |
| Data Models & Tables | [`idempiere_model_list_models`](#idempiere_model_list_models) | List application models (tables) | *None* |
| Data Models & Tables | [`idempiere_model_get_yaml`](#idempiere_model_get_yaml) | Get OpenAPI YAML schema for a model (table) | `tableName` |
| Windows & Tabs | [`idempiere_window_list_windows`](#idempiere_window_list_windows) | Get list of windows. Window is a UI definition for user to work with a group of related tables/models. | *None* |
| Windows & Tabs | [`idempiere_window_get_tabs`](#idempiere_window_get_tabs) | Get list of tabs of a window. Each tab is map to a table/model. | `window_name` |
| Windows & Tabs | [`idempiere_window_get_tab_fields`](#idempiere_window_get_tab_fields) | Get list of fields of a window tab. Each field is a UI element for user to interact with a specific table column (model property). | `window_name`, `tab_name` |
| Windows & Tabs | [`idempiere_window_get_records`](#idempiere_window_get_records) | Search records of a window. | `window_name` |
| Windows & Tabs | [`idempiere_window_create_record`](#idempiere_window_create_record) | Create window record. | `window_name`, `data` |
| Windows & Tabs | [`idempiere_window_get_record`](#idempiere_window_get_record) | Get record by id. | `window_name`, `record_id` |
| Windows & Tabs | [`idempiere_window_print_record`](#idempiere_window_print_record) | Print document of a record. | `window_name`, `record_id` |
| Windows & Tabs | [`idempiere_window_get_tab_record`](#idempiere_window_get_tab_record) | Get tab record by id. | `window_name`, `tab_name`, `record_id` |
| Windows & Tabs | [`idempiere_window_update_tab_record`](#idempiere_window_update_tab_record) | Update tab record by id. | `window_name`, `tab_name`, `record_id`, `data` |
| Windows & Tabs | [`idempiere_window_delete_tab_record`](#idempiere_window_delete_tab_record) | Delete tab record by id. | `window_name`, `tab_name`, `record_id` |
| Windows & Tabs | [`idempiere_window_get_child_tab_records`](#idempiere_window_get_child_tab_records) | Get child tab records | `window_name`, `tab_name`, `record_id`, `child_tab_name` |
| Windows & Tabs | [`idempiere_window_create_child_tab_record`](#idempiere_window_create_child_tab_record) | Create child tab record. | `window_name`, `tab_name`, `record_id`, `child_tab_name`, `data` |
| Views | [`idempiere_view_list_views`](#idempiere_view_list_views) | List available views. | *None* |
| Views | [`idempiere_view_get_yaml`](#idempiere_view_get_yaml) | Get OpenAPI YAML schema for a view. | `viewName` |
| Views | [`idempiere_view_search_records`](#idempiere_view_search_records) | Search records in a view. | `viewName` |
| Views | [`idempiere_view_create_record`](#idempiere_view_create_record) | Create a record in a view. | `viewName`, `data` |
| Views | [`idempiere_view_get_record`](#idempiere_view_get_record) | Get a record by ID from a view. | `viewName`, `id` |
| Views | [`idempiere_view_update_record`](#idempiere_view_update_record) | Update a record in a view. | `viewName`, `id`, `data` |
| Views | [`idempiere_view_delete_record`](#idempiere_view_delete_record) | Delete a record from a view. | `viewName`, `id` |
| Views | [`idempiere_view_get_record_property`](#idempiere_view_get_record_property) | Get a property of a view record. | `viewName`, `id`, `columnName` |
| Views | [`idempiere_view_get_record_attachments`](#idempiere_view_get_record_attachments) | Get attachments of a view record. | `viewName`, `id` |
| Views | [`idempiere_view_add_record_attachment`](#idempiere_view_add_record_attachment) | Add an attachment to a view record. | `viewName`, `id`, `data` |
| Views | [`idempiere_view_delete_record_attachments`](#idempiere_view_delete_record_attachments) | Delete attachments of a view record. | `viewName`, `id` |
| Views | [`idempiere_view_get_record_attachments_zip`](#idempiere_view_get_record_attachments_zip) | Get attachments of a view record as ZIP. | `viewName`, `id` |
| Views | [`idempiere_view_get_record_attachment_by_name`](#idempiere_view_get_record_attachment_by_name) | Get a specific attachment from a view record. | `viewName`, `id`, `fileName` |
| Views | [`idempiere_view_print_record`](#idempiere_view_print_record) | Print a view record. | `viewName`, `id` |
| Processes & Reports | [`idempiere_process_get_info`](#idempiere_process_get_info) | Get details of a process by value/search key (ad_process.value). | `process_value` |
| Processes & Reports | [`idempiere_process_run`](#idempiere_process_run) | Run a process/report. | `process_value` |
| Server Jobs | [`idempiere_server_job_list_jobs`](#idempiere_server_job_list_jobs) | Get server jobs | *None* |
| Server Jobs | [`idempiere_server_job_get`](#idempiere_server_job_get) | Get server job by id | `server_id` |
| Server Jobs | [`idempiere_server_job_get_logs`](#idempiere_server_job_get_logs) | Get server job logs by id | `server_id` |
| Server Jobs | [`idempiere_server_job_toggle_state`](#idempiere_server_job_toggle_state) | Toggle server job state (stop to start and vice versa) | `server_id` |
| Server Jobs | [`idempiere_server_job_run`](#idempiere_server_job_run) | Run server job by id | `server_id` |
| Server Jobs | [`idempiere_server_job_reload_jobs`](#idempiere_server_job_reload_jobs) | Reload all server jobs | *None* |
| Schedulers | [`idempiere_scheduler_get_details`](#idempiere_scheduler_get_details) | Get scheduler details by id | `scheduler_id` |
| Schedulers | [`idempiere_scheduler_create_job`](#idempiere_scheduler_create_job) | Create new server job for scheduler | `scheduler_id` |
| Schedulers | [`idempiere_scheduler_delete_job`](#idempiere_scheduler_delete_job) | Remove server job of a scheduler. | `scheduler_id` |
| Workflows | [`idempiere_workflow_list_activities`](#idempiere_workflow_list_activities) | Get workflow nodes for user. | *None* |
| Workflows | [`idempiere_workflow_approve_activity`](#idempiere_workflow_approve_activity) | Approve a pending workflow activity. | `id` |
| Workflows | [`idempiere_workflow_reject_activity`](#idempiere_workflow_reject_activity) | Reject a pending workflow activity. | `id` |
| Workflows | [`idempiere_workflow_forward_activity`](#idempiere_workflow_forward_activity) | Forward a pending workflow activity. | `id`, `userTo` |
| Workflows | [`idempiere_workflow_acknowledge_activity`](#idempiere_workflow_acknowledge_activity) | Acknowledge a pending workflow activity. | `id` |
| Workflows | [`idempiere_workflow_set_activity_user_choice`](#idempiere_workflow_set_activity_user_choice) | Set user selection value. | `id`, `value` |
| Info Windows | [`idempiere_info_list_info_windows`](#idempiere_info_list_info_windows) | Get list of info windows. | *None* |
| Info Windows | [`idempiere_info_get_info_window_data`](#idempiere_info_get_info_window_data) | Get records using info window. | `infoSlug` |
| Info Windows | [`idempiere_info_get_info_window_columns`](#idempiere_info_get_info_window_columns) | Get column definitions of an info window. | `infoSlug` |
| Info Windows | [`idempiere_info_get_info_window_processes`](#idempiere_info_get_info_window_processes) | Get process definitions of an info window. | `infoSlug` |
| Info Windows | [`idempiere_info_get_info_window_related_infos`](#idempiere_info_get_info_window_related_infos) | Get related info windows of an info window. | `infoSlug` |
| File Uploads | [`idempiere_upload_initiate`](#idempiere_upload_initiate) | Initiate new upload session. | `data` |
| File Uploads | [`idempiere_upload_list_pending_uploads`](#idempiere_upload_list_pending_uploads) | Retrieves the pending uploads from current user. | *None* |
| File Uploads | [`idempiere_upload_chunk`](#idempiere_upload_chunk) | Upload a chunk for an upload session. | `uploadId`, `chunkOrder`, `totalChunks`, `data` |
| File Uploads | [`idempiere_upload_get_status`](#idempiere_upload_get_status) | Retrieves the current status of an ongoing or completed upload. | `uploadId` |
| File Uploads | [`idempiere_upload_cancel`](#idempiere_upload_cancel) | Cancels an ongoing/completed upload. | `uploadId` |
| File Uploads | [`idempiere_upload_get_uploaded_file`](#idempiere_upload_get_uploaded_file) | Get uploaded file content. | `uploadId` |
| File Uploads | [`idempiere_upload_copy_uploaded_file`](#idempiere_upload_copy_uploaded_file) | Copy uploaded file to attachment, image or archive. | `uploadId`, `data` |
| Server Nodes & Logs | [`idempiere_node_list_nodes`](#idempiere_node_list_nodes) | List server nodes. | *None* |
| Server Nodes & Logs | [`idempiere_node_get`](#idempiere_node_get) | Get server node details. | `id` |
| Server Nodes & Logs | [`idempiere_node_get_logs`](#idempiere_node_get_logs) | Get list of log files for a node. | `id` |
| Server Nodes & Logs | [`idempiere_node_delete_log`](#idempiere_node_delete_log) | Remove server log files for a node. | `id` |
| Server Nodes & Logs | [`idempiere_node_get_log_file`](#idempiere_node_get_log_file) | Get server log file content. | `id`, `fileName` |
| Server Nodes & Logs | [`idempiere_node_rotate_log`](#idempiere_node_rotate_log) | Rotate server log for a node. | `id` |
| System Caches | [`idempiere_cache_list_caches`](#idempiere_cache_list_caches) | List active caches (Admin only). | *None* |
| System Caches | [`idempiere_cache_reset`](#idempiere_cache_reset) | Reset cache (Admin only). | *None* |
| Charts & Visualizations | [`idempiere_chart_get_charts_data`](#idempiere_chart_get_charts_data) | Get multiple chart data. | *None* |
| Charts & Visualizations | [`idempiere_chart_get`](#idempiere_chart_get) | Get chart as image or json data. | `id` |
| Charts & Visualizations | [`idempiere_chart_get_data`](#idempiere_chart_get_data) | Get chart data. | `id` |
| Status Lines | [`idempiere_status_list_status_lines`](#idempiere_status_list_status_lines) | Get list of status lines. | *None* |
| Status Lines | [`idempiere_status_get_status_line`](#idempiere_status_get_status_line) | Get output message of a status line. | `id` |
| Menu Tree | [`idempiere_menu_tree_get`](#idempiere_menu_tree_get) | Get menu tree by id. | `id` |
| References | [`idempiere_reference_get`](#idempiere_reference_get) | Get reference definition. | `id` |
| Batch Execution | [`idempiere_execute_batch`](#idempiere_execute_batch) | Execute batch request. | `data` |

---

<a id="authentication"></a>
## Authentication (3 tools)

Tools for authenticating sessions, acquiring tokens, updating tenant/org/role/warehouse login context, and terminating sessions.

<a id="idempiere_auth_create_token"></a>
### `idempiere_auth_create_token`

Create authorization token (login).

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `userName` | `string` | **Yes** | User name or email address. |
| `password` | `string` (format: `password`) | **Yes** | User password. |
| `parameters` | `object` | No | Optional authentication parameters: clientId (Tenant id or Tenant search key, e.g., 11 or GardenWorld, search from AD_Client), roleId (Role id or Role name, e.g., 102 or GardenWorld Admin, search from AD_Role), organizationId (Organization id or Organization name, e.g., 11 or HQ, search from AD_Org), warehouseId (Warehouse identifier, e.g., 103 or HQ Warehouse, search from M_Warehouse), language (Language code, e.g., en_US, search from AD_Language). |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

<a id="idempiere_auth_update_token"></a>
### `idempiere_auth_update_token`

Update authorization token.

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `clientId` | `string` | **Yes** | Tenant id or Tenant search key, e.g., 11 or GardenWorld, search from AD_Client. |
| `roleId` | `string` | **Yes** | Role id or Role name, e.g., 102 or GardenWorld Admin, search from AD_Role. |
| `organizationId` | `string` | **Yes** | Organization id or Organization name, e.g., 11 or HQ, search from AD_Org. |
| `warehouseId` | `string` | **Yes** | Warehouse identifier, e.g., 103 or HQ Warehouse, search from M_Warehouse. |
| `language` | `string` | **Yes** | Language code, e.g., en_US, search from AD_Language. |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

<a id="idempiere_auth_logout"></a>
### `idempiere_auth_logout`

Logout.

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

[↑ Back to Table of Contents](#table-of-contents)

---

<a id="data-models--tables"></a>
## Data Models & Tables (14 tools)

Direct database model/table operations for raw CRUD actions, OData queries, OpenAPI YAML schema generation, document printing, and record attachments.

<a id="idempiere_model_search_records"></a>
### `idempiere_model_search_records`

Search iDempiere records.

- **Table Schema**: Use the 'idempiere_model_get_yaml' tool for model schema definition in openapi yaml format.
- **Hint**: Use the 'idempiere_model_search_records' and 'idempiere_model_get_record' tools with 'ad_table' and 'ad_column' model to get the schema of table and column.
- **Discovery of relation**: AD_Reference_ID and AD_Reference_Value_ID of ad_column can be used to discover the relations between tables/models.

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `model` | `string` | **Yes** | Table Name (e.g. C_Order).<br>• **Hint**: Check 'idempiere://metadata/models' first to find Table Names.<br>• **Hint**: Use the 'idempiere_model_list_models' tools get the list of models (tables).<br>• **Hint** 'idempiere_window_get_records' and 'idempiere_window_get_child_tab_records' tool can also be used to search records within a window context. |
| `filter` | `string` | No | OData Filter.<br>• **Examples**: 'GrandTotal gt 1000 and IsActive eq true', 'contains(tolower(Name), 'service')', 'startswith(DocumentNo, 'INV-')'.<br>• **oData Functions implemented**: contains(), startswith(), endswith(), tolower() and toupper().<br>• **Search priority for text field**: Value, Name, DocumentNo, Description, ReferenceNo, Email, Phone and Note<br>• **Hint for Text field search**: use the OData 'contains' function for partial match and 'tolower' for case insensitive search. |
| `limit` | `integer` | No | Max records (default 10) |
| `offset` | `integer` | No | Records to skip (default 0) |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

<a id="idempiere_model_get_record"></a>
### `idempiere_model_get_record`

Get a record by ID (Integer or UUID).

- **Table Schema**: Use the 'idempiere_model_get_yaml' tool for model schema definition in openapi yaml format.
- **Hint**: Use the 'idempiere_model_search_records' and 'idempiere_model_get_record' tools with 'ad_table' and 'ad_column' model to get the schema of table and column.
- **Discovery of relation**: AD_Reference_ID and AD_Reference_Value_ID of ad_column can be used to discover the relations between tables/models.

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `model` | `string` | **Yes** | Table Name<br>• **Hint**: Check 'idempiere://metadata/models' first to find Table Names.<br>• **Hint**: Use the 'idempiere_model_list_models' tools get the list of models (tables).<br>• **Hint** 'idempiere_window_get_records' and 'idempiere_window_get_child_tab_records' tool can also be used to search records within a window context. |
| `id` | `string` | **Yes** | Record ID |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

<a id="idempiere_model_create_record"></a>
### `idempiere_model_create_record`

Create a record.

- **Table Schema**: Use the 'idempiere_model_get_yaml' tool for model schema definition in openapi yaml format.
- **Hint**: Use the 'idempiere_model_search_records' and 'idempiere_model_get_record' tools with 'ad_table' and 'ad_column' model to get the schema of table and column.
- **Discovery of relation**: AD_Reference_ID and AD_Reference_Value_ID of ad_column can be used to discover the relations between tables/models.
- **Important Note**: Ask user for missing mandatory fields.
- **Security**: Ask user for confirmation before saving unless user explicitly ask for no confirmation.
- **Ask User for Confirmation**: When asking user for confirmation, show a table with field names and values.

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `model` | `string` | **Yes** | Table Name.<br>• **Hint**: Check 'idempiere://metadata/models' first to find Table Names.<br>• **Hint**: Use the 'idempiere_model_list_models' tools get the list of models (tables).<br>• **Hint** 'idempiere_window_get_records' and 'idempiere_window_get_child_tab_records' tool can also be used to search records within a window context. |
| `data` | `object` | **Yes** | JSON Object of fields.<br>• **Special property in json data object**: 'doc-action' field to perform document actions.<br>• **Values**: CO (Complete), VO (Void), RE (Re-Activate), RC (Reverse Correct), RA (Reverse Accrual), PR (Prepare), CL (Close). |
| `logMigrationScript` | `boolean` | No | Optional. If true, log SQL migration script on server (requires System Administrator on System client). |
| `adempiereSys` | `boolean` | No | Optional. If true, enable Dictionary Maintenance mode and use centralized IDs (requires System Administrator on System client). |
| `migrationScriptComment` | `string` | No | Optional. Migration script comment, e.g. 'IDEMPIERE-1234 Short description' (requires System Administrator on System client). |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

<a id="idempiere_model_update_record"></a>
### `idempiere_model_update_record`

Update a record.

- **Table Schema**: Use the 'idempiere_model_get_yaml' tool for model schema definition in openapi yaml format.
- **Hint**: Use the 'idempiere_model_search_records' and 'idempiere_model_get_record' tools with 'ad_table' and 'ad_column' model to get the schema of table and column.
- **Discovery of relation**: AD_Reference_ID and AD_Reference_Value_ID of ad_column can be used to discover the relations between tables/models.
- **Important Note**: Prompts for missing mandatory fields.
- **Security**: Prompts for confirmation before saving unless user explicitly ask for no confirmation.
- **Prompt for Confirmation**: When prompt for confirmation, show a table with field names and values.

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `model` | `string` | **Yes** | Table Name.<br>• **Hint**: Check 'idempiere://metadata/models' first to find Table Names.<br>• **Hint**: Use the 'list_models' tools get the list of models (tables).<br>• **Hint** 'get_window_records' and 'get_child_tab_records' tool can also be used to search records within a window context. |
| `id` | `string` | **Yes** | Record ID |
| `data` | `object` | **Yes** | Fields to update.<br>• **Special property in json data object**: 'doc-action' field to perform document actions.<br>• **Values**: CO (Complete), VO (Void), RE (Re-Activate), RC (Reverse Correct), RA (Reverse Accrual), PR (Prepare), CL (Close). |
| `logMigrationScript` | `boolean` | No | Optional. If true, log SQL migration script on server (requires System Administrator on System client). |
| `adempiereSys` | `boolean` | No | Optional. If true, enable Dictionary Maintenance mode and use centralized IDs (requires System Administrator on System client). |
| `migrationScriptComment` | `string` | No | Optional. Migration script comment, e.g. 'IDEMPIERE-1234 Short description' (requires System Administrator on System client). |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

<a id="idempiere_model_delete_record"></a>
### `idempiere_model_delete_record`

Delete record by id. 'idempiere_window_delete_tab_record' tool can also be used to delete record within a window context.

- **Table Schema**: Use the 'idempiere_model_get_yaml' tool for model schema definition in openapi yaml format.
- **Hint**: Use the 'idempiere_model_search_records' and 'idempiere_model_get_record' tools with 'ad_table' and 'ad_column' model to get the schema of table and column.
- **Important Note**: Ask user for missing mandatory fields.
- **Security**: Ask user for confirmation before saving unless user explicitly ask for no confirmation.
- **Ask User for Confirmation**: When asking user for confirmation, show a table with field names and values.

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `tableName` | `string` | **Yes** | table name.<br>• **Hint**: Check 'idempiere://metadata/models' first to find Table Names.<br>• **Hint**: Use the 'idempiere_model_list_models' tools get the list of models (tables).<br>• **Hint** 'idempiere_window_get_records' and 'idempiere_window_get_child_tab_records' tool can also be used to search records within a window context. |
| `record_id` | `string` | **Yes** | record id |
| `logMigrationScript` | `boolean` | No | Optional. If true, log SQL migration script on server (requires System Administrator on System client). |
| `adempiereSys` | `boolean` | No | Optional. If true, enable Dictionary Maintenance mode and use centralized IDs (requires System Administrator on System client). |
| `migrationScriptComment` | `string` | No | Optional. Migration script comment, e.g. 'IDEMPIERE-1234 Short description' (requires System Administrator on System client). |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

<a id="idempiere_model_get_record_property"></a>
### `idempiere_model_get_record_property`

Get property value of a record.

- **Table Schema**: Use the 'idempiere_model_get_yaml' tool for model schema definition in openapi yaml format.

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `tableName` | `string` | **Yes** | table name<br>• **Hint**: Check 'idempiere://metadata/models' first to find Table Names.<br>• **Hint**: Use the 'idempiere_model_list_models' tools get the list of models (tables).<br>• **Hint** 'idempiere_window_get_records' and 'idempiere_window_get_child_tab_records' tool can also be used to search records within a window context. |
| `record_id` | `string` | **Yes** | record id |
| `columnName` | `string` | **Yes** | column name |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

<a id="idempiere_model_get_record_attachments"></a>
### `idempiere_model_get_record_attachments`

Get list of attachment of a record

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `tableName` | `string` | **Yes** | table name.<br>• **Hint**: Check 'idempiere://metadata/models' first to find Table Names.<br>• **Hint**: Use the 'idempiere_model_list_models' tools get the list of models (tables).<br>• **Hint** 'idempiere_window_get_records' and 'idempiere_window_get_child_tab_records' tool can also be used to search records within a window context. |
| `record_id` | `string` | **Yes** | record id |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

<a id="idempiere_model_add_record_attachment"></a>
### `idempiere_model_add_record_attachment`

Add/update attachment by record id and file name

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `tableName` | `string` | **Yes** | table name.<br>• **Hint**: Check 'idempiere://metadata/models' first to find Table Names.<br>• **Hint**: Use the 'idempiere_model_list_models' tools get the list of models (tables).<br>• **Hint** 'idempiere_window_get_records' and 'idempiere_window_get_child_tab_records' tool can also be used to search records within a window context. |
| `record_id` | `string` | **Yes** | record id |
| `data` | `object` | **Yes** | JSON Object required keys: 'name' (filename string) and 'data' (base64 encoded file content string). |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

<a id="idempiere_model_delete_record_attachment"></a>
### `idempiere_model_delete_record_attachment`

Delete attachment of a record

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `tableName` | `string` | **Yes** | table name.<br>• **Hint**: Check 'idempiere://metadata/models' first to find Table Names.<br>• **Hint**: Use the 'idempiere_model_list_models' tools get the list of models (tables).<br>• **Hint** 'idempiere_window_get_records' and 'idempiere_window_get_child_tab_records' tool can also be used to search records within a window context. |
| `record_id` | `string` | **Yes** | record id |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

<a id="idempiere_model_get_record_attachments_zip"></a>
### `idempiere_model_get_record_attachments_zip`

Get attachment of a record as zip archive

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `tableName` | `string` | **Yes** | table name.<br>• **Hint**: Check 'idempiere://metadata/models' first to find Table Names.<br>• **Hint**: Use the 'idempiere_model_list_models' tools get the list of models (tables).<br>• **Hint** 'idempiere_window_get_records' and 'idempiere_window_get_child_tab_records' tool can also be used to search records within a window context. |
| `record_id` | `string` | **Yes** | record id |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

<a id="idempiere_model_get_record_attachment_by_name"></a>
### `idempiere_model_get_record_attachment_by_name`

Get attachment of a record by file name

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `tableName` | `string` | **Yes** | table name<br>• **Hint**: Check 'idempiere://metadata/models' first to find Table Names.<br>• **Hint**: Use the 'idempiere_model_list_models' tools get the list of models (tables).<br>• **Hint** 'idempiere_window_get_records' and 'idempiere_window_get_child_tab_records' tool can also be used to search records within a window context. |
| `record_id` | `string` | **Yes** | record id |
| `fileName` | `string` | **Yes** | file name |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

<a id="idempiere_model_print_record"></a>
### `idempiere_model_print_record`

Print document of a record

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `tableName` | `string` | **Yes** | table name.<br>• **Hint**: Check 'idempiere://metadata/models' first to find Table Names.<br>• **Hint**: Use the 'idempiere_model_list_models' tools get the list of models (tables).<br>• **Hint** 'idempiere_window_get_records' and 'idempiere_window_get_child_tab_records' tool can also be used to search records within a window context. |
| `record_id` | `string` | **Yes** | record id |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

<a id="idempiere_model_list_models"></a>
### `idempiere_model_list_models`

List application models (tables)

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `filter` | `string` | No | OData Filter.<br>• **Examples**: 'GrandTotal gt 1000 and IsActive eq true', 'contains(tolower(Name), 'service')', 'startswith(DocumentNo, 'INV-')'.<br>• **oData Functions implemented**: contains(), startswith(), endswith(), tolower() and toupper().<br>• **Search priority for text field**: Value, Name, DocumentNo, Description, ReferenceNo, Email, Phone and Note<br>• **Hint for Text field search**: use the OData 'contains' function for partial match and 'tolower' for case insensitive search. |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

<a id="idempiere_model_get_yaml"></a>
### `idempiere_model_get_yaml`

Get OpenAPI YAML schema for a model (table)

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `tableName` | `string` | **Yes** | Table Name (e.g. C_Order).<br>• **Hint**: Check 'idempiere://metadata/models' first to find Table Names.<br>• **Hint**: Use the 'idempiere_model_list_models' tools get the list of models (tables).<br>• **Hint** 'idempiere_window_get_records' and 'idempiere_window_get_child_tab_records' tool can also be used to search records within a window context. |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

[↑ Back to Table of Contents](#table-of-contents)

---

<a id="windows--tabs"></a>
## Windows & Tabs (12 tools)

UI Window and Tab interactions. Unlike raw model tools, Window tools evaluate UI business logic, execute callouts, apply default values, and enforce tab-level validations.

<a id="idempiere_window_list_windows"></a>
### `idempiere_window_list_windows`

Get list of windows. Window is a UI definition for user to work with a group of related tables/models.

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `filter` | `string` | No | OData Filter.<br>• **Examples**: 'GrandTotal gt 1000 and IsActive eq true', 'contains(tolower(Name), 'service')', 'startswith(DocumentNo, 'INV-')'.<br>• **oData Functions implemented**: contains(), startswith(), endswith(), tolower() and toupper().<br>• **Search priority for text field**: Value, Name, DocumentNo, Description, ReferenceNo, Email, Phone and Note<br>• **Hint for Text field search**: use the OData 'contains' function for partial match and 'tolower' for case insensitive search. |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

<a id="idempiere_window_get_tabs"></a>
### `idempiere_window_get_tabs`

Get list of tabs of a window. Each tab is map to a table/model.

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `window_name` | `string` | **Yes** | window name (ad_window.name).<br>• **Hint**: Use 'idempiere_window_list_windows' tool to search for window. |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

<a id="idempiere_window_get_tab_fields"></a>
### `idempiere_window_get_tab_fields`

Get list of fields of a window tab. Each field is a UI element for user to interact with a specific table column (model property).

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `window_name` | `string` | **Yes** | window name (ad_window.name).<br>• **Hint**: Use 'idempiere_window_list_windows' tool to search for window. |
| `tab_name` | `string` | **Yes** | window tab name (ad_tab.name). |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

<a id="idempiere_window_get_records"></a>
### `idempiere_window_get_records`

Search records of a window.

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `window_name` | `string` | **Yes** | window name (ad_window.name).<br>• **Hint**: Use 'idempiere_window_list_windows' tool to search for window. |
| `filter` | `string` | No | OData Filter.<br>• **Examples**: 'GrandTotal gt 1000 and IsActive eq true', 'contains(tolower(Name), 'service')', 'startswith(DocumentNo, 'INV-')'.<br>• **oData Functions implemented**: contains(), startswith(), endswith(), tolower() and toupper().<br>• **Search priority for text field**: Value, Name, DocumentNo, Description, ReferenceNo, Email, Phone and Note<br>• **Hint for Text field search**: use the OData 'contains' function for partial match and 'tolower' for case insensitive search. |
| `sort_column` | `string` | No | Column to sort by, use ! prefix for descending sort (optional) |
| `page_no` | `integer` | No | Current page no (optional) |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

<a id="idempiere_window_create_record"></a>
### `idempiere_window_create_record`

Create window record.

- **Distinction**: Use this tool when you need to trigger window logic, callouts, and validations. For raw data access without UI logic, use 'idempiere_model_create_record'.
- **Important Note**: Ask user for missing mandatory fields.
- **Security**: Ask user for confirmation before saving unless user explicitly ask for no confirmation.
- **Ask User for confirmation**: When asking user for confirmation, show a table with field names and values.

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `window_name` | `string` | **Yes** | window name (ad_window.name).<br>• **Hint**: Use 'idempiere_window_list_windows' tool to search for window. |
| `data` | `object` | **Yes** | JSON Object of fields<br>• **Special property in json data object**: 'doc-action' field to perform document actions.<br>• **Values**: CO (Complete), VO (Void), RE (Re-Activate), RC (Reverse Correct), RA (Reverse Accrual), PR (Prepare), CL (Close).<br>• **Field in the json data object**: use column name (ad_column.name), not field name as json property. |
| `save` | `boolean` | No | If false, applies default values and callouts without persisting changes to the database (optional, default true). |
| `logMigrationScript` | `boolean` | No | Optional. If true, log SQL migration script on server (requires System Administrator on System client). |
| `adempiereSys` | `boolean` | No | Optional. If true, enable Dictionary Maintenance mode and use centralized IDs (requires System Administrator on System client). |
| `migrationScriptComment` | `string` | No | Optional. Migration script comment, e.g. 'IDEMPIERE-1234 Short description' (requires System Administrator on System client). |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

<a id="idempiere_window_get_record"></a>
### `idempiere_window_get_record`

Get record by id.

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `window_name` | `string` | **Yes** | window name (ad_window.name).<br>• **Hint**: Use 'idempiere_window_list_windows' tool to search for window. |
| `record_id` | `string` | **Yes** | record id |
| `expand` | `string` | No | comma separated list of child tabs to load (optional) |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

<a id="idempiere_window_print_record"></a>
### `idempiere_window_print_record`

Print document of a record.

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `window_name` | `string` | **Yes** | window name (ad_window.name).<br>• **Hint**: Use 'idempiere_window_list_windows' tool to search for window. |
| `record_id` | `string` | **Yes** | record id |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

<a id="idempiere_window_get_tab_record"></a>
### `idempiere_window_get_tab_record`

Get tab record by id.

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `window_name` | `string` | **Yes** | window name (ad_window.name).<br>• **Hint**: Use 'idempiere_window_list_windows' tool to search for window. |
| `tab_name` | `string` | **Yes** | tab name (ad_tab.name). |
| `record_id` | `string` | **Yes** | record id |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

<a id="idempiere_window_update_tab_record"></a>
### `idempiere_window_update_tab_record`

Update tab record by id.

- **Important Note**: Ask user for missing mandatory fields.
- **Security**: Ask user for confirmation before saving unless user explicitly ask for no confirmation.
- **Ask User for confirmation**: When asking user for confirmation, show a table with field names and values.

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `window_name` | `string` | **Yes** | window name (ad_window.name).<br>• **Hint**: Use 'idempiere_window_list_windows' tool to search for window. |
| `tab_name` | `string` | **Yes** | tab name (ad_tab.name). |
| `record_id` | `string` | **Yes** | record id |
| `data` | `object` | **Yes** | JSON Object of fields.<br>• **Special property in json data object**: 'doc-action' field to perform document actions.<br>• **Values**: CO (Complete), VO (Void), RE (Re-Activate), RC (Reverse Correct), RA (Reverse Accrual), PR (Prepare), CL (Close).<br>• **Field in the json data object**: use column name (ad_column.name), not field name as json property. |
| `save` | `boolean` | No | If false, applies default values and callouts without persisting changes to the database (optional, default true). |
| `logMigrationScript` | `boolean` | No | Optional. If true, log SQL migration script on server (requires System Administrator on System client). |
| `adempiereSys` | `boolean` | No | Optional. If true, enable Dictionary Maintenance mode and use centralized IDs (requires System Administrator on System client). |
| `migrationScriptComment` | `string` | No | Optional. Migration script comment, e.g. 'IDEMPIERE-1234 Short description' (requires System Administrator on System client). |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

<a id="idempiere_window_delete_tab_record"></a>
### `idempiere_window_delete_tab_record`

Delete tab record by id.

- **Important Note**: Ask user for confirmation before deleting unless user explicitly ask for no confirmation.
- **Ask User for confirmation**: When asking user for confirmation, show a table with field names and values.

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `window_name` | `string` | **Yes** | window name (ad_window.name).<br>• **Field in the json data object**: use column name (ad_column.name), not field name as json property. |
| `tab_name` | `string` | **Yes** | tab name (ad_tab.name). |
| `record_id` | `string` | **Yes** | record id |
| `logMigrationScript` | `boolean` | No | Optional. If true, log SQL migration script on server (requires System Administrator on System client). |
| `adempiereSys` | `boolean` | No | Optional. If true, enable Dictionary Maintenance mode and use centralized IDs (requires System Administrator on System client). |
| `migrationScriptComment` | `string` | No | Optional. Migration script comment, e.g. 'IDEMPIERE-1234 Short description' (requires System Administrator on System client). |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

<a id="idempiere_window_get_child_tab_records"></a>
### `idempiere_window_get_child_tab_records`

Get child tab records

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `window_name` | `string` | **Yes** | window name (ad_window.name).<br>• **Hint**: Use 'idempiere_window_list_windows' tool to search for window. |
| `tab_name` | `string` | **Yes** | tab name (ad_tab.name). |
| `record_id` | `string` | **Yes** | record id |
| `child_tab_name` | `string` | **Yes** | child tab name (ad_tab.name). |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

<a id="idempiere_window_create_child_tab_record"></a>
### `idempiere_window_create_child_tab_record`

Create child tab record.

- **Important Note**: Ask user for missing mandatory fields.
- **Security**: Ask user for confirmation before saving unless user explicitly ask for no confirmation.
- **Ask User for confirmation**: When asking user for confirmation, show a table with field names and values.

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `window_name` | `string` | **Yes** | window name (ad_window.name).<br>• **Hint**: Use 'idempiere_window_list_windows' tool to search for window. |
| `tab_name` | `string` | **Yes** | tab name (ad_tab.name). |
| `record_id` | `string` | **Yes** | record id |
| `child_tab_name` | `string` | **Yes** | child tab name (ad_tab.name). |
| `data` | `object` | **Yes** | JSON Object of fields.<br>• **Field in the json data object**: use column name (ad_column.name), not field name as json property. |
| `save` | `boolean` | No | If false, applies default values and callouts without persisting changes to the database (optional, default true). |
| `logMigrationScript` | `boolean` | No | Optional. If true, log SQL migration script on server (requires System Administrator on System client). |
| `adempiereSys` | `boolean` | No | Optional. If true, enable Dictionary Maintenance mode and use centralized IDs (requires System Administrator on System client). |
| `migrationScriptComment` | `string` | No | Optional. Migration script comment, e.g. 'IDEMPIERE-1234 Short description' (requires System Administrator on System client). |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

[↑ Back to Table of Contents](#table-of-contents)

---

<a id="views"></a>
## Views (14 tools)

Search, query, and perform CRUD operations, attachment handling, and document printing on database views, as well as retrieve OpenAPI YAML schemas.

<a id="idempiere_view_list_views"></a>
### `idempiere_view_list_views`

List available views.

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `filter` | `string` | No | OData Filter.<br>• **Examples**: 'GrandTotal gt 1000 and IsActive eq true', 'contains(tolower(Name), 'service')', 'startswith(DocumentNo, 'INV-')'.<br>• **oData Functions implemented**: contains(), startswith(), endswith(), tolower() and toupper().<br>• **Search priority for text field**: Value, Name, DocumentNo, Description, ReferenceNo, Email, Phone and Note<br>• **Hint for Text field search**: use the OData 'contains' function for partial match and 'tolower' for case insensitive search. |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

<a id="idempiere_view_get_yaml"></a>
### `idempiere_view_get_yaml`

Get OpenAPI YAML schema for a view.

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `viewName` | `string` | **Yes** | View Name. |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

<a id="idempiere_view_search_records"></a>
### `idempiere_view_search_records`

Search records in a view.

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `viewName` | `string` | **Yes** | View Name. |
| `filter` | `string` | No | OData Filter.<br>• **Examples**: 'GrandTotal gt 1000 and IsActive eq true', 'contains(tolower(Name), 'service')', 'startswith(DocumentNo, 'INV-')'.<br>• **oData Functions implemented**: contains(), startswith(), endswith(), tolower() and toupper().<br>• **Search priority for text field**: Value, Name, DocumentNo, Description, ReferenceNo, Email, Phone and Note<br>• **Hint for Text field search**: use the OData 'contains' function for partial match and 'tolower' for case insensitive search. |
| `sort_column` | `string` | No | Column to sort by (optional). |
| `limit` | `integer` | No | Max records (default 10) |
| `offset` | `integer` | No | Records to skip (default 0) |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

<a id="idempiere_view_create_record"></a>
### `idempiere_view_create_record`

Create a record in a view.

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `viewName` | `string` | **Yes** | View Name. |
| `data` | `object` | **Yes** | JSON Object of fields. |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

<a id="idempiere_view_get_record"></a>
### `idempiere_view_get_record`

Get a record by ID from a view.

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `viewName` | `string` | **Yes** | View Name. |
| `id` | `string` | **Yes** | Record ID. |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

<a id="idempiere_view_update_record"></a>
### `idempiere_view_update_record`

Update a record in a view.

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `viewName` | `string` | **Yes** | View Name. |
| `id` | `string` | **Yes** | Record ID. |
| `data` | `object` | **Yes** | Fields to update. |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

<a id="idempiere_view_delete_record"></a>
### `idempiere_view_delete_record`

Delete a record from a view.

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `viewName` | `string` | **Yes** | View Name. |
| `id` | `string` | **Yes** | Record ID. |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

<a id="idempiere_view_get_record_property"></a>
### `idempiere_view_get_record_property`

Get a property of a view record.

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `viewName` | `string` | **Yes** | View Name. |
| `id` | `string` | **Yes** | Record ID. |
| `columnName` | `string` | **Yes** | Column Name. |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

<a id="idempiere_view_get_record_attachments"></a>
### `idempiere_view_get_record_attachments`

Get attachments of a view record.

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `viewName` | `string` | **Yes** | View Name. |
| `id` | `string` | **Yes** | Record ID. |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

<a id="idempiere_view_add_record_attachment"></a>
### `idempiere_view_add_record_attachment`

Add an attachment to a view record.

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `viewName` | `string` | **Yes** | View Name. |
| `id` | `string` | **Yes** | Record ID. |
| `data` | `object` | **Yes** | JSON Object required keys: 'name' (filename string) and 'data' (base64 encoded file content string). |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

<a id="idempiere_view_delete_record_attachments"></a>
### `idempiere_view_delete_record_attachments`

Delete attachments of a view record.

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `viewName` | `string` | **Yes** | View Name. |
| `id` | `string` | **Yes** | Record ID. |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

<a id="idempiere_view_get_record_attachments_zip"></a>
### `idempiere_view_get_record_attachments_zip`

Get attachments of a view record as ZIP.

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `viewName` | `string` | **Yes** | View Name. |
| `id` | `string` | **Yes** | Record ID. |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

<a id="idempiere_view_get_record_attachment_by_name"></a>
### `idempiere_view_get_record_attachment_by_name`

Get a specific attachment from a view record.

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `viewName` | `string` | **Yes** | View Name. |
| `id` | `string` | **Yes** | Record ID. |
| `fileName` | `string` | **Yes** | File Name. |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

<a id="idempiere_view_print_record"></a>
### `idempiere_view_print_record`

Print a view record.

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `viewName` | `string` | **Yes** | View Name. |
| `id` | `string` | **Yes** | Record ID. |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

[↑ Back to Table of Contents](#table-of-contents)

---

<a id="processes--reports"></a>
## Processes & Reports (2 tools)

Retrieve process metadata/parameters and execute backend iDempiere business processes, reports, and document engines.

<a id="idempiere_process_get_info"></a>
### `idempiere_process_get_info`

Get details of a process by value/search key (ad_process.value).

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `process_value` | `string` | **Yes** | process value/search key (ad_process.value).<br>• **Search for Process**: 1. 'idempiere://metadata/processes' MCP resource for value property.2. use the 'idempiere_model_search_records' tool with ad_process as model.3. use the 'idempiere_window_get_records' tool for process window |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

<a id="idempiere_process_run"></a>
### `idempiere_process_run`

Run a process/report.

- **Search for Process**: 1. 'idempiere://metadata/processes' MCP resource for value property.2. use the 'idempiere_model_search_records' tool with ad_process as model.3. use the 'idempiere_window_get_records' tool for process window

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `process_value` | `string` | **Yes** | value/search key property of the process (ad_process.value).<br>• **Search for Process**: 1. 'idempiere://metadata/processes' MCP resource for value property.2. use the 'idempiere_model_search_records' tool with ad_process as model.3. use the 'idempiere_window_get_records' tool for process window |
| `parameters` | `object` | No | Process parameters as a JSON object. |
| `logMigrationScript` | `boolean` | No | Optional. If true, log SQL migration script on server (requires System Administrator on System client). |
| `adempiereSys` | `boolean` | No | Optional. If true, enable Dictionary Maintenance mode and use centralized IDs (requires System Administrator on System client). |
| `migrationScriptComment` | `string` | No | Optional. Migration script comment, e.g. 'IDEMPIERE-1234 Short description' (requires System Administrator on System client). |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

[↑ Back to Table of Contents](#table-of-contents)

---

<a id="server-jobs"></a>
## Server Jobs (6 tools)

Manage, monitor, run, reload, and view logs of background server jobs running within the iDempiere instance.

<a id="idempiere_server_job_list_jobs"></a>
### `idempiere_server_job_list_jobs`

Get server jobs

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

<a id="idempiere_server_job_get"></a>
### `idempiere_server_job_get`

Get server job by id

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `server_id` | `string` | **Yes** | server job id |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

<a id="idempiere_server_job_get_logs"></a>
### `idempiere_server_job_get_logs`

Get server job logs by id

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `server_id` | `string` | **Yes** | server job id |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

<a id="idempiere_server_job_toggle_state"></a>
### `idempiere_server_job_toggle_state`

Toggle server job state (stop to start and vice versa)

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `server_id` | `string` | **Yes** | server job id |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

<a id="idempiere_server_job_run"></a>
### `idempiere_server_job_run`

Run server job by id

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `server_id` | `string` | **Yes** | server job id |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

<a id="idempiere_server_job_reload_jobs"></a>
### `idempiere_server_job_reload_jobs`

Reload all server jobs

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

[↑ Back to Table of Contents](#table-of-contents)

---

<a id="schedulers"></a>
## Schedulers (3 tools)

Inspect scheduler definitions and create or delete background server jobs linked to schedulers.

<a id="idempiere_scheduler_get_details"></a>
### `idempiere_scheduler_get_details`

Get scheduler details by id

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `scheduler_id` | `string` | **Yes** | scheduler id |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

<a id="idempiere_scheduler_create_job"></a>
### `idempiere_scheduler_create_job`

Create new server job for scheduler

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `scheduler_id` | `string` | **Yes** | scheduler id |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

<a id="idempiere_scheduler_delete_job"></a>
### `idempiere_scheduler_delete_job`

Remove server job of a scheduler.

- **Important Note**: Ask user for confirmation unless user explicitly ask for no confirmation.

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `scheduler_id` | `string` | **Yes** | scheduler id |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

[↑ Back to Table of Contents](#table-of-contents)

---

<a id="workflows"></a>
## Workflows (6 tools)

Manage user workflow activities including listing pending tasks, approving, rejecting, forwarding, acknowledging, or selecting user choices.

<a id="idempiere_workflow_list_activities"></a>
### `idempiere_workflow_list_activities`

Get workflow nodes for user.

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `userId` | `string` | No | Optional User ID/UUID (defaults to current user). |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

<a id="idempiere_workflow_approve_activity"></a>
### `idempiere_workflow_approve_activity`

Approve a pending workflow activity.

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `id` | `string` | **Yes** | Activity ID/UUID. |
| `message` | `string` | No | Approval message (optional). |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

<a id="idempiere_workflow_reject_activity"></a>
### `idempiere_workflow_reject_activity`

Reject a pending workflow activity.

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `id` | `string` | **Yes** | Activity ID/UUID. |
| `message` | `string` | No | Reject message (optional). |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

<a id="idempiere_workflow_forward_activity"></a>
### `idempiere_workflow_forward_activity`

Forward a pending workflow activity.

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `id` | `string` | **Yes** | Activity ID/UUID. |
| `userTo` | `string` | **Yes** | User ID/UUID to forward to. |
| `message` | `string` | No | Forward message (optional). |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

<a id="idempiere_workflow_acknowledge_activity"></a>
### `idempiere_workflow_acknowledge_activity`

Acknowledge a pending workflow activity.

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `id` | `string` | **Yes** | Activity ID/UUID. |
| `message` | `string` | No | Acknowledge message (optional). |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

<a id="idempiere_workflow_set_activity_user_choice"></a>
### `idempiere_workflow_set_activity_user_choice`

Set user selection value.

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `id` | `string` | **Yes** | Activity ID/UUID. |
| `value` | `string` | **Yes** | Value to set. |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

[↑ Back to Table of Contents](#table-of-contents)

---

<a id="info-windows"></a>
## Info Windows (5 tools)

Discover and execute Info Windows (lookup queries), retrieve column definitions, associated processes, and linked info windows.

<a id="idempiere_info_list_info_windows"></a>
### `idempiere_info_list_info_windows`

Get list of info windows.

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `filter` | `string` | No | OData Filter.<br>• **Examples**: 'GrandTotal gt 1000 and IsActive eq true', 'contains(tolower(Name), 'service')', 'startswith(DocumentNo, 'INV-')'.<br>• **oData Functions implemented**: contains(), startswith(), endswith(), tolower() and toupper().<br>• **Search priority for text field**: Value, Name, DocumentNo, Description, ReferenceNo, Email, Phone and Note<br>• **Hint for Text field search**: use the OData 'contains' function for partial match and 'tolower' for case insensitive search. |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

<a id="idempiere_info_get_info_window_data"></a>
### `idempiere_info_get_info_window_data`

Get records using info window.

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `infoSlug` | `string` | **Yes** | Info Window Slug. |
| `parameters` | `string` | No | JSON for info window query parameters (optional). |
| `where_clause` | `string` | No | Where clause (optional). |
| `order_by` | `string` | No | Order by clause (optional). |
| `page_no` | `integer` | No | Current page number (default 0). |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

<a id="idempiere_info_get_info_window_columns"></a>
### `idempiere_info_get_info_window_columns`

Get column definitions of an info window.

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `infoSlug` | `string` | **Yes** | Info Window Slug. |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

<a id="idempiere_info_get_info_window_processes"></a>
### `idempiere_info_get_info_window_processes`

Get process definitions of an info window.

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `infoSlug` | `string` | **Yes** | Info Window Slug. |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

<a id="idempiere_info_get_info_window_related_infos"></a>
### `idempiere_info_get_info_window_related_infos`

Get related info windows of an info window.

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `infoSlug` | `string` | **Yes** | Info Window Slug. |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

[↑ Back to Table of Contents](#table-of-contents)

---

<a id="file-uploads"></a>
## File Uploads (7 tools)

Session-based chunked file uploads, status monitoring, cancellation, and copying uploaded files to record attachments, images, or archives.

<a id="idempiere_upload_initiate"></a>
### `idempiere_upload_initiate`

Initiate new upload session.

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `data` | `object` | **Yes** | UploadInitiationRequest info. |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

<a id="idempiere_upload_list_pending_uploads"></a>
### `idempiere_upload_list_pending_uploads`

Retrieves the pending uploads from current user.

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

<a id="idempiere_upload_chunk"></a>
### `idempiere_upload_chunk`

Upload a chunk for an upload session.

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `uploadId` | `string` | **Yes** | Upload UUID. |
| `chunkOrder` | `integer` | **Yes** | Chunk sequence (starts from 1). |
| `totalChunks` | `integer` | **Yes** | Total number of chunks. |
| `data` | `string` | **Yes** | Base64 encoded chunk content. |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

<a id="idempiere_upload_get_status"></a>
### `idempiere_upload_get_status`

Retrieves the current status of an ongoing or completed upload.

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `uploadId` | `string` | **Yes** | Upload UUID. |
| `expiresInSeconds` | `integer` | No | Presigned URL expiration. |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

<a id="idempiere_upload_cancel"></a>
### `idempiere_upload_cancel`

Cancels an ongoing/completed upload.

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `uploadId` | `string` | **Yes** | Upload UUID. |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

<a id="idempiere_upload_get_uploaded_file"></a>
### `idempiere_upload_get_uploaded_file`

Get uploaded file content.

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `uploadId` | `string` | **Yes** | Upload UUID. |
| `json` | `string` | No | If present, return as JSON. |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

<a id="idempiere_upload_copy_uploaded_file"></a>
### `idempiere_upload_copy_uploaded_file`

Copy uploaded file to attachment, image or archive.

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `uploadId` | `string` | **Yes** | Upload UUID. |
| `data` | `object` | **Yes** | CopyUploadedFileRequest info. |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

[↑ Back to Table of Contents](#table-of-contents)

---

<a id="server-nodes--logs"></a>
## Server Nodes & Logs (6 tools)

Cluster node administration, retrieving node status, viewing server log files, log rotation, and deleting log files.

<a id="idempiere_node_list_nodes"></a>
### `idempiere_node_list_nodes`

List server nodes.

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

<a id="idempiere_node_get"></a>
### `idempiere_node_get`

Get server node details.

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `id` | `string` | **Yes** | Node UUID. |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

<a id="idempiere_node_get_logs"></a>
### `idempiere_node_get_logs`

Get list of log files for a node.

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `id` | `string` | **Yes** | Node UUID. |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

<a id="idempiere_node_delete_log"></a>
### `idempiere_node_delete_log`

Remove server log files for a node.

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `id` | `string` | **Yes** | Node UUID. |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

<a id="idempiere_node_get_log_file"></a>
### `idempiere_node_get_log_file`

Get server log file content.

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `id` | `string` | **Yes** | Node UUID. |
| `fileName` | `string` | **Yes** | Log File Name. |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

<a id="idempiere_node_rotate_log"></a>
### `idempiere_node_rotate_log`

Rotate server log for a node.

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `id` | `string` | **Yes** | Node UUID. |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

[↑ Back to Table of Contents](#table-of-contents)

---

<a id="system-caches"></a>
## System Caches (2 tools)

System administrator tools to inspect active memory caches and reset cache tables.

<a id="idempiere_cache_list_caches"></a>
### `idempiere_cache_list_caches`

List active caches (Admin only).

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `table_name` | `string` | No | Optional Table Name filter. |
| `name` | `string` | No | Optional Name filter. |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

<a id="idempiere_cache_reset"></a>
### `idempiere_cache_reset`

Reset cache (Admin only).

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `record_id` | `integer` | No | Optional Record ID. |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

[↑ Back to Table of Contents](#table-of-contents)

---

<a id="charts--visualizations"></a>
## Charts & Visualizations (3 tools)

Retrieve performance indicator and business chart data, render chart graphics (PNG), or retrieve raw JSON datasets.

<a id="idempiere_chart_get_charts_data"></a>
### `idempiere_chart_get_charts_data`

Get multiple chart data.

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `filter` | `string` | No | OData Filter.<br>• **Examples**: 'GrandTotal gt 1000 and IsActive eq true', 'contains(tolower(Name), 'service')', 'startswith(DocumentNo, 'INV-')'.<br>• **oData Functions implemented**: contains(), startswith(), endswith(), tolower() and toupper().<br>• **Search priority for text field**: Value, Name, DocumentNo, Description, ReferenceNo, Email, Phone and Note<br>• **Hint for Text field search**: use the OData 'contains' function for partial match and 'tolower' for case insensitive search. |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

<a id="idempiere_chart_get"></a>
### `idempiere_chart_get`

Get chart as image or json data.

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `id` | `string` | **Yes** | Chart ID/UUID. |
| `width` | `integer` | No | Optional Width. |
| `height` | `integer` | No | Optional Height. |
| `json` | `string` | No | Optional. If present, return as JSON. |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

<a id="idempiere_chart_get_data"></a>
### `idempiere_chart_get_data`

Get chart data.

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `id` | `string` | **Yes** | Chart ID/UUID. |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

[↑ Back to Table of Contents](#table-of-contents)

---

<a id="status-lines"></a>
## Status Lines (2 tools)

Retrieve user notification status lines and their output messages.

<a id="idempiere_status_list_status_lines"></a>
### `idempiere_status_list_status_lines`

Get list of status lines.

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `filter` | `string` | No | OData Filter.<br>• **Examples**: 'GrandTotal gt 1000 and IsActive eq true', 'contains(tolower(Name), 'service')', 'startswith(DocumentNo, 'INV-')'.<br>• **oData Functions implemented**: contains(), startswith(), endswith(), tolower() and toupper().<br>• **Search priority for text field**: Value, Name, DocumentNo, Description, ReferenceNo, Email, Phone and Note<br>• **Hint for Text field search**: use the OData 'contains' function for partial match and 'tolower' for case insensitive search. |
| `with_messages` | `boolean` | No | Include output messages. |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

<a id="idempiere_status_get_status_line"></a>
### `idempiere_status_get_status_line`

Get output message of a status line.

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `id` | `string` | **Yes** | Status Line ID/UUID. |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

[↑ Back to Table of Contents](#table-of-contents)

---

<a id="menu-tree"></a>
## Menu Tree (1 tools)

Retrieve the full hierarchical navigation menu tree configured for the user session.

<a id="idempiere_menu_tree_get"></a>
### `idempiere_menu_tree_get`

Get menu tree by id.

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `id` | `string` | **Yes** | Menu Tree ID/UUID. |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

[↑ Back to Table of Contents](#table-of-contents)

---

<a id="references"></a>
## References (1 tools)

Retrieve reference definitions (Lists, Tables, Data Types) used by table columns and UI elements.

<a id="idempiere_reference_get"></a>
### `idempiere_reference_get`

Get reference definition.

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `id` | `string` | **Yes** | Reference ID, UUID, or Name. |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

[↑ Back to Table of Contents](#table-of-contents)

---

<a id="batch-execution"></a>
## Batch Execution (1 tools)

Execute multiple transactional operations in a single atomic batch request.

<a id="idempiere_execute_batch"></a>
### `idempiere_execute_batch`

Execute batch request.

#### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `data` | `object` | **Yes** | BatchRequest info. |
| `transaction` | `boolean` | No | Process in single transaction (default true). |
| `_authorization_bearer` | `string` | No | Optional HTTP Authorization Bearer Token. |

[↑ Back to Table of Contents](#table-of-contents)

---

<a id="mcp-resources"></a>
## MCP Resources

The server exposes the following MCP resources (`resources/list`, `resources/read`) for system discovery:

| URI | Name | Description |
|---|---|---|
| `idempiere://metadata/models` | List All Models | Lists all registered tables and data models in the iDempiere Application Dictionary. |
| `idempiere://metadata/processes` | List All Processes | Lists all registered processes, reports, and workflows available for execution. |

[↑ Back to Table of Contents](#table-of-contents)

---

<a id="common-features--conventions"></a>
## Common Features & Conventions

### 1. Model vs. Window Tool Distinction
- **Model Tools (`idempiere_model_*`)**: Perform raw data operations directly against underlying database tables. They bypass window callouts and UI field validations.
- **Window Tools (`idempiere_window_*`)**: Operate in the context of an iDempiere Window/Tab. They automatically fire callouts, apply default values, calculate dependent fields, and enforce tab-level validation rules.

### 2. OData Query Filtering
Search and listing tools accept standard OData filter expressions in the `filter` parameter:
- **Comparison Operators**: `eq`, `ne`, `gt`, `ge`, `lt`, `le`, `and`, `or`, `not`
- **String Functions**: `contains()`, `startswith()`, `endswith()`, `tolower()`, `toupper()`
- **Examples**:
  - `GrandTotal gt 1000 and IsActive eq true`
  - `contains(tolower(Name), 'service')`
  - `startswith(DocumentNo, 'INV-')`
- **Recommended Text Search Priority**: `Value`, `Name`, `DocumentNo`, `Description`, `ReferenceNo`, `Email`, `Phone`, `Note`

### 3. Document Actions (`doc-action`)
When creating or updating transactional documents (orders, invoices, payments, etc.), the `doc-action` property can be included inside the `data` object to transition document status:

| Code | Action | Description |
|---|---|---|
| `CO` | Complete | Finalize and complete the document |
| `VO` | Void | Void the document |
| `RE` | Re-Activate | Re-activate a completed document |
| `RC` | Reverse - Correct | Reverse document with corrective accounting entries |
| `RA` | Reverse - Accrual | Reverse accrual entries |
| `PR` | Prepare | Prepare document for completion |
| `CL` | Close | Permanently close the document |

### 4. Data Dictionary Maintenance & Migration Scripts
Tools that modify records (`create`, `update`, `delete`, and `process_run`) support server-side migration script recording for System Administrators on the System client (`Client_ID=0`):
- `logMigrationScript` (`boolean`): If `true`, logs an SQL migration script on the server (`X-ID-LogMigrationScript`).
- `adempiereSys` (`boolean`): If `true`, enables Dictionary Maintenance mode and uses centralized official IDs (`X-ID-AdempiereSys`).
- `migrationScriptComment` (`string`): Description comment for the migration script, e.g. `'IDEMPIERE-1234 Short description'` (`X-ID-MigrationScriptComment`).

### 5. AI Assistant Safety & Confirmation Rules
- For create, update, and delete actions, AI assistants are instructed to prompt the user for confirmation with a formatted summary table before persisting changes unless the user explicitly requested no confirmation.
- Always verify mandatory fields and prompt the user if required fields are missing.

[↑ Back to Table of Contents](#table-of-contents)