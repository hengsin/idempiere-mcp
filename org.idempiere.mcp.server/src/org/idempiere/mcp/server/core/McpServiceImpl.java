/**********************************************************************
* This file is part of iDempiere ERP Open Source                      *
* http://www.idempiere.org                                            *
*                                                                     *
* Copyright (C) Contributors                                          *
*                                                                     *
* This program is free software; you can redistribute it and/or       *
* modify it under the terms of the GNU General Public License         *
* as published by the Free Software Foundation; either version 2      *
* of the License, or (at your option) any later version.              *
*                                                                     *
* This program is distributed in the hope that it will be useful,     *
* but WITHOUT ANY WARRANTY; without even the implied warranty of      *
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the        *
* GNU General Public License for more details.                        *
*                                                                     *
* You should have received a copy of the GNU General Public License   *
* along with this program; if not, write to the Free Software         *
* Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,          *
* MA 02110-1301, USA.                                                 *
*                                                                     *
* Contributors:                                                       *
* - Trek Global Corporation                                           *
* - hengsin                                                           *
**********************************************************************/
package org.idempiere.mcp.server.core;

import org.idempiere.mcp.server.api.IMcpService;
import org.idempiere.mcp.server.client.RestApiClient;
import org.osgi.service.component.annotations.Component;

import java.util.logging.Level;

import org.compiere.util.CLogger;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@Component(service = IMcpService.class)
public class McpServiceImpl implements IMcpService {

    private final CLogger log = CLogger.getCLogger(McpServiceImpl.class);
    private final Gson gson = new Gson();
    private final RestApiClient restClient = new RestApiClient();

    @Override
    public String processRequest(String jsonRequest, String authToken) {
        String requestId = null;
        try {
            JsonObject req = JsonParser.parseString(jsonRequest).getAsJsonObject();
            if (req.has("id") && !req.get("id").isJsonNull()) {
                requestId = req.get("id").getAsString();
            }

            String method = req.get("method").getAsString();
            if (log.isLoggable(Level.INFO))
                log.info("MCP Request: " + method);

            JsonObject params = req.has("params") ? req.getAsJsonObject("params") : new JsonObject();

            switch (method) {
                case "initialize":
                    return handleInitialize(requestId); // CHANGED
                case "notifications/initialized":
                    return null; // No response needed for notification
                case "notifications/cancelled":
                    return null; // No response needed for notification
                case "ping":
                    return createSuccess(requestId, new JsonObject());
                case "tools/list":
                    return handleListTools(requestId);
                case "tools/call":
                    return handleToolCall(requestId, params, authToken);
                case "resources/list":
                    return handleListResources(requestId);
                case "resources/read":
                    return handleReadResource(requestId, params, authToken);
                default:
                    return createError(requestId, -32601, "Method not found: " + method);
            }
        } catch (Exception e) {
            log.saveError("MCP Error", e);
            return createError(requestId, -32603, "Internal Error: " + e.getMessage());
        }
    }

    private String handleInitialize(String id) {
        JsonObject capabilities = new JsonObject();

        // Declare support for Tools and Resources
        JsonObject tools = new JsonObject();
        tools.addProperty("listChanged", false);
        capabilities.add("tools", tools);
        JsonObject resources = new JsonObject();
        resources.addProperty("subscribe", false);
        resources.addProperty("listChanged", false);
        capabilities.add("resources", resources);

        JsonObject serverInfo = new JsonObject();
        serverInfo.addProperty("name", "iDempiere MCP Server");
        serverInfo.addProperty("version", "1.0.0");

        JsonObject result = new JsonObject();
        result.addProperty("protocolVersion", "2025-06-18");
        result.add("capabilities", capabilities);
        result.add("serverInfo", serverInfo);

        return createSuccess(id, result);
    }

    private String handleListTools(String id) {
        JsonArray tools = new JsonArray();

        tools.add(createTool("search_records",
                "Search iDempiere records."
                        + "**Table Schema**: Use the 'get_model_yaml' tool for model schema definition in openapi yaml format."
                        + "**Hint**: Use the 'search_records' and 'get_record' tools for 'ad_table' and 'ad_column' model get the schema of table and column."
                        + "**Discovery of relation**: Window is a UI definition for user to work with a group of related tables/models, 'get_window_tabs' tool "
                        + "can be used to discover the relations between tables/models.",
                new String[] { "model" },
                new String[] { "model", "string", "Table Name (e.g. C_Order)."
                        + "**Hint**: Check 'idempiere://metadata/models' first to find Table Names."
                        + "**Hint**: Use the 'list_models' tools get the list of models (tables)."
                        + "**Hint** 'get_window_records' and 'get_child_tab_records' tool can also be used to search records within a window context." },
                new String[] { "filter", "string", "OData Filter. "
                        + "**Examples**: 'GrandTotal gt 1000 and IsActive eq true', 'contains(tolower(Name), 'service')', 'startswith(DocumentNo, 'INV-')'. "
                        + "**oData Functions implemented**: "
                        + "contains(), startswith(), endswith(), tolower() and toupper()."
                        + "**Search priority for text field**: Value, Name, DocumentNo, Description, ReferenceNo, Email, Phone and Note"
                        + "**Hint for Text field search**: use the OData 'contains' function for partial match and 'tolower' for case insensitive search."
                        + "**Hint for filter**: If filtering can't be fulfilled with the implemented OData functions, "
                        + "consider using the 'get_window_records' tool that support SQL filter clause." },
                new String[] { "limit", "integer", "Max records (default 10)" },
                new String[] { "offset", "integer", "Records to skip (default 0)" }));

        tools.add(createTool("get_record",
                "Get a record by ID (Integer or UUID). "
                        + "**Table Schema**: Use the 'get_model_yaml' tool for model schema definition in openapi yaml format."
                        + "**Hint**: Use the 'search_records' and 'get_record' tools for 'ad_table' and 'ad_column' model get the schema of table and column."
                        + "**Discovery of relation**: Window is a UI definition for user to work with a group of related tables/models, 'get_window_tabs' tool "
                        + "can be used to discover the relations between tables/models.",
                new String[] { "model", "id" },
                new String[] { "model", "string", "Table Name"
                        + "**Hint**: Check 'idempiere://metadata/models' first to find Table Names."
                        + "**Hint**: Use the 'list_models' tools get the list of models (tables)."
                        + "**Hint** 'get_window_records' and 'get_child_tab_records' tool can also be used to search records within a window context." },
                new String[] { "id", "string", "Record ID" }));

        tools.add(createTool("create_record", "Create a record. "
                + "**Table Schema**: Use the 'get_model_yaml' tool for model schema definition in openapi yaml format."
                + "**Hint**: Use the 'search_records' and 'get_record' tools for 'ad_table' and 'ad_column' model get the schema of table and column."
                + "**Discovery of relation**: Window is a UI definition for user to work with a group of related tables/models, 'get_window_tabs' tool "
                + "**Important Note**: Prompts for missing mandatory fields. "
                + "**Security**: Prompts for confirmation before saving unless user explicitly ask for no confirmation. "
                + "**Prompt for Confirmation**: When prompt for confirmation, show a table with field names and values.",
                new String[] { "model", "data" },
                new String[] { "model", "string", "Table Name."
                        + "**Hint**: Check 'idempiere://metadata/models' first to find Table Names."
                        + "**Hint**: Use the 'list_models' tools get the list of models (tables)."
                        + "**Hint** 'get_window_records' and 'get_child_tab_records' tool can also be used to search records within a window context." },
                new String[] { "data", "object", "JSON Object of fields."
                        + "**Special property in json data object**: 'doc-action' field to perform document actions. "
                        + "**Values**: CO (Complete), VO (Void), RE (Re-Activate), RC (Reverse Correct), RA (Reverse Accrual), PR (Prepare), CL (Close)." }));

        tools.add(createTool("update_record", "Update a record. "
                + "**Table Schema**: Use the 'get_model_yaml' tool for model schema definition in openapi yaml format."
                + "**Hint**: Use the 'search_records' and 'get_record' tools for 'ad_table' and 'ad_column' model get the schema of table and column."
                + "**Discovery of relation**: Window is a UI definition for user to work with a group of related tables/models, 'get_window_tabs' tool "
                + "**Important Note**: Prompts for missing mandatory fields. "
                + "**Security**: Prompts for confirmation before saving unless user explicitly ask for no confirmation. "
                + "**Prompt for Confirmation**: When prompt for confirmation, show a table with field names and values.",
                new String[] { "model", "id", "data" },
                new String[] { "model", "string", "Table Name."
                        + "**Hint**: Check 'idempiere://metadata/models' first to find Table Names."
                        + "**Hint**: Use the 'list_models' tools get the list of models (tables)."
                        + "**Hint** 'get_window_records' and 'get_child_tab_records' tool can also be used to search records within a window context." },
                new String[] { "id", "string", "Record ID" },
                new String[] { "data", "object", "Fields to update."
                        + "**Special property in json data object**: 'doc-action' field to perform document actions. "
                        + "**Values**: CO (Complete), VO (Void), RE (Re-Activate), RC (Reverse Correct), RA (Reverse Accrual), PR (Prepare), CL (Close)." }));

        tools.add(createTool("delete_record", "Delete record by id. 'delete_window_tab_record' tool can also "
                + "be used to delete record within a window context."
                + "**Table Schema**: Use the 'get_model_yaml' tool for model schema definition in openapi yaml format."
                + "**Hint**: Use the 'search_records' and 'get_record' tools for 'ad_table' and 'ad_column' model get the schema of table and column."
                + "**Important Note**: Prompts for missing mandatory fields. "
                + "**Security**: Prompts for confirmation before saving unless user explicitly ask for no confirmation. "
                + "**Prompt for Confirmation**: When prompt for confirmation, show a table with field names and values.",
                new String[] { "tableName", "record_id" },
                new String[] { "tableName", "string", "table name."
                        + "**Hint**: Check 'idempiere://metadata/models' first to find Table Names."
                        + "**Hint**: Use the 'list_models' tools get the list of models (tables)."
                        + "**Hint** 'get_window_records' and 'get_child_tab_records' tool can also be used to search records within a window context." },
                new String[] { "record_id", "string", "record id" }));

        tools.add(createTool("get_record_property", "Get property value of a record."
                + "**Table Schema**: Use the 'get_model_yaml' tool for model schema definition in openapi yaml format.",
                new String[] { "tableName", "record_id", "columnName" },
                new String[] { "tableName", "string", "table name"
                        + "**Hint**: Check 'idempiere://metadata/models' first to find Table Names."
                        + "**Hint**: Use the 'list_models' tools get the list of models (tables)."
                        + "**Hint** 'get_window_records' and 'get_child_tab_records' tool can also be used to search records within a window context." },
                new String[] { "record_id", "string", "record id" },
                new String[] { "columnName", "string", "column name" }));

        tools.add(createTool("get_record_attachments", "Get list of attachment of a record",
                new String[] { "tableName", "record_id" },
                new String[] { "tableName", "string", "table name."
                        + "**Hint**: Check 'idempiere://metadata/models' first to find Table Names."
                        + "**Hint**: Use the 'list_models' tools get the list of models (tables)."
                        + "**Hint** 'get_window_records' and 'get_child_tab_records' tool can also be used to search records within a window context." },
                new String[] { "record_id", "string", "record id" }));

        tools.add(createTool("add_record_attachment", "Add/update attachment by record id and file name",
                new String[] { "tableName", "record_id", "data" },
                new String[] { "tableName", "string", "table name."
                        + "**Hint**: Check 'idempiere://metadata/models' first to find Table Names."
                        + "**Hint**: Use the 'list_models' tools get the list of models (tables)."
                        + "**Hint** 'get_window_records' and 'get_child_tab_records' tool can also be used to search records within a window context." },
                new String[] { "record_id", "string", "record id" },
                new String[] { "data", "object",
                        "JSON Object required keys: 'name' (filename string) and 'data' (base64 encoded file content string)." }));

        tools.add(createTool("delete_record_attachments", "Delete attachment of a record",
                new String[] { "tableName", "record_id" },
                new String[] { "tableName", "string", "table name."
                        + "**Hint**: Check 'idempiere://metadata/models' first to find Table Names."
                        + "**Hint**: Use the 'list_models' tools get the list of models (tables)."
                        + "**Hint** 'get_window_records' and 'get_child_tab_records' tool can also be used to search records within a window context." },
                new String[] { "record_id", "string", "record id" }));

        tools.add(createTool("get_record_attachments_zip", "Get attachment of a record as zip archive",
                new String[] { "tableName", "record_id" },
                new String[] { "tableName", "string", "table name."
                        + "**Hint**: Check 'idempiere://metadata/models' first to find Table Names."
                        + "**Hint**: Use the 'list_models' tools get the list of models (tables)."
                        + "**Hint** 'get_window_records' and 'get_child_tab_records' tool can also be used to search records within a window context." },
                new String[] { "record_id", "string", "record id" }));

        tools.add(createTool("get_record_attachment_by_name", "Get attachment of a record by file name",
                new String[] { "tableName", "record_id", "fileName" },
                new String[] { "tableName", "string", "table name"
                        + "**Hint**: Check 'idempiere://metadata/models' first to find Table Names."
                        + "**Hint**: Use the 'list_models' tools get the list of models (tables)."
                        + "**Hint** 'get_window_records' and 'get_child_tab_records' tool can also be used to search records within a window context." },
                new String[] { "record_id", "string", "record id" },
                new String[] { "fileName", "string", "file name" }));

        tools.add(createTool("print_record", "Print document of a record", new String[] { "tableName", "record_id" },
                new String[] { "tableName", "string", "table name."
                        + "**Hint**: Check 'idempiere://metadata/models' first to find Table Names."
                        + "**Hint**: Use the 'list_models' tools get the list of models (tables)."
                        + "**Hint** 'get_window_records' and 'get_child_tab_records' tool can also be used to search records within a window context." },
                new String[] { "record_id", "string", "record id" }));

        tools.add(createTool("list_models", "List application models (tables)", new String[] {},
                new String[] { "filter", "string", "OData $filter expression (optional)."
                        + "**oData Functions implemented**: "
                        + "contains(), startswith(), endswith(), tolower() and toupper()." }));
        tools.add(createTool("get_model_yaml", "Get OpenAPI YAML schema for a model (table)",
                new String[] { "tableName" },
                new String[] { "tableName", "string", "Table Name (e.g. C_Order)."
                        + "**Hint**: Check 'idempiere://metadata/models' first to find Table Names."
                        + "**Hint**: Use the 'list_models' tools get the list of models (tables)."
                        + "**Hint** 'get_window_records' and 'get_child_tab_records' tool can also be used to search records within a window context." }));

        tools.add(createTool("get_process_info", "Get details of a process by value/search key (ad_process.value).",
                new String[] { "process_value" },
                new String[] { "process_value", "string", "process value/search key (ad_process.value)."
                        + "**Search for Process**: 1. 'idempiere://metadata/processes' MCP resource for value property."
                        + "2. use the 'search_records' tool with ad_process as model."
                        + "3. use the 'get_window_records' tool for process window" }));

        tools.add(createTool("run_process",
                "Run a process/report. "
                        + "**Search for Process**: 1. 'idempiere://metadata/processes' MCP resource for value property."
                        + "2. use the search_records tool with ad_process as model."
                        + "3. use the get_window_records tool for process window",
                new String[] { "process_value" },
                new String[] { "process_value", "string", "value/search key property of the process (ad_process.value)."
                        + "**Search for Process**: 1. 'idempiere://metadata/processes' MCP resource for value property."
                        + "2. use the 'search_records' tool with ad_process as model."
                        + "3. use the 'get_window_records' tool for process window" },
                new String[] { "parameters", "object", "Process parameters as a JSON object. "
                        + "**Example**: {\"C_Order_ID\": 1000010, \"IsGenerated\": \"Y\"}." }));

        tools.add(createTool("list_server_jobs", "Get server jobs", new String[] {}, new String[] {}));

        tools.add(createTool("get_server_job", "Get server job by id", new String[] { "server_id" },
                new String[] { "server_id", "string", "server job id" }));

        tools.add(createTool("get_server_job_logs", "Get server job logs by id", new String[] { "server_id" },
                new String[] { "server_id", "string", "server job id" }));

        tools.add(createTool("toggle_server_job_state", "Toggle server job state (stop to start and vice versa)",
                new String[] { "server_id" },
                new String[] { "server_id", "string", "server job id" }));
        tools.add(createTool("run_server_job", "Run server job by id", new String[] { "server_id" },
                new String[] { "server_id", "string", "server job id" }));

        tools.add(createTool("reload_server_jobs", "Reload all server jobs", new String[] {}, new String[] {}));

        tools.add(createTool("get_scheduler_details", "Get scheduler details by id", new String[] { "scheduler_id" },
                new String[] { "scheduler_id", "string", "scheduler id" }));

        tools.add(createTool("create_scheduler_job", "Create new server job for scheduler",
                new String[] { "scheduler_id" },
                new String[] { "scheduler_id", "string", "scheduler id" }));

        tools.add(createTool("delete_scheduler_job", "Remove server job of a scheduler."
                + "**Important Note**: Prompts for confirmation unless user explicitly ask for no confirmation.",
                new String[] { "scheduler_id" },
                new String[] { "scheduler_id", "string", "scheduler id" }));

        tools.add(createTool("list_windows", "Get list of windows. Window is a UI definition for user "
                + "to work with a group of related tables/models.", new String[] {},
                new String[] { "filter", "string", "OData $filter expression (optional)"
                        + "**oData Functions implemented**: "
                        + "contains(), startswith(), endswith(), tolower() and toupper()." }));
        tools.add(createTool("get_window_tabs", "Get list of tabs of a window. Each tab is map to a table/model.",
                new String[] { "window_name" },
                new String[] { "window_name", "string", "window name (ad_window.name)."
                        + "**Hint**: Use 'list_windows' tool to search for window." }));
        tools.add(createTool("get_window_tab_fields",
                "Get list of fields of a window tab. Each field is a UI element for user to "
                        + "interact with a specific table column (model property).",
                new String[] { "window_name", "tab_name" },
                new String[] { "window_name", "string", "window name (ad_window.name)."
                        + "**Hint**: Use 'list_windows' tool to search for window." },
                new String[] { "tab_name", "string", "window tab name (ad_tab.name)." }));
        tools.add(createTool("get_window_records", "Search records of a window.", new String[] { "window_name" },
                new String[] { "window_name", "string", "window name (ad_window.name)."
                        + "**Hint**: Use 'list_windows' tool to search for window." },
                new String[] { "filter", "string", "SQL filter clause (optional)."
                        + "**Security**: The filter clause is sanitized to prevent SQL injection."
                        + "**Hint for Text field search**: use like for partial match and 'lower' sql function for case insensitive search." },
                new String[] { "sort_column", "string",
                        "Column to sort by, use ! prefix for descending sort (optional)" },
                new String[] { "page_no", "integer", "Current page no (optional)" }));
        tools.add(createTool("create_window_record", "Create window record. "
                + "**Distinction**: Use this tool when you need to trigger window logic, callouts, and validations. For raw data access without UI logic, use 'create_record'. "
                + "**Important Note**: Prompts for missing mandatory fields. "
                + "**Security**: Prompts for confirmation before saving unless user explicitly ask for no confirmation. "
                + "**Prompt for confirmation**: When prompt for confirmation, show a table with field names and values.",
                new String[] { "window_name", "data" },
                new String[] { "window_name", "string", "window name (ad_window.name)."
                        + "**Hint**: Use 'list_windows' tool to search for window." },
                new String[] { "data", "object", "JSON Object of fields"
                        + "**Special property in json data object**: 'doc-action' field to perform document actions. "
                        + "**Values**: CO (Complete), VO (Void), RE (Re-Activate), RC (Reverse Correct), RA (Reverse Accrual), PR (Prepare), CL (Close)."
                        + "**Field in the json data object**: use column name (ad_column.name), not field name as json property." }));
        tools.add(createTool("get_window_record", "Get record by id.", new String[] { "window_name", "record_id" },
                new String[] { "window_name", "string", "window name (ad_window.name)."
                        + "**Hint**: Use 'list_windows' tool to search for window." },
                new String[] { "record_id", "string", "record id" },
                new String[] { "expand", "string", "comma separated list of child tabs to load (optional)" }));
        tools.add(createTool("print_window_record", "Print document of a record.",
                new String[] { "window_name", "record_id" },
                new String[] { "window_name", "string", "window name (ad_window.name)."
                        + "**Hint**: Use 'list_windows' tool to search for window." },
                new String[] { "record_id", "string", "record id" }));
        tools.add(createTool("get_window_tab_record", "Get tab record by id.",
                new String[] { "window_name", "tab_name", "record_id" },
                new String[] { "window_name", "string", "window name (ad_window.name)."
                        + "**Hint**: Use 'list_windows' tool to search for window." },
                new String[] { "tab_name", "string", "tab name (ad_tab.name)." },
                new String[] { "record_id", "string", "record id" }));
        tools.add(createTool("update_window_tab_record", "Update tab record by id. "
                + "**Important Note**: Prompts for missing mandatory fields. "
                + "**Security**: Prompts for confirmation before saving unless user explicitly ask for no confirmation. "
                + "**Prompt for confirmation**: When prompt for confirmation, show a table with field names and values.",
                new String[] { "window_name", "tab_name", "record_id", "data" },
                new String[] { "window_name", "string", "window name (ad_window.name)."
                        + "**Hint**: Use 'list_windows' tool to search for window." },
                new String[] { "tab_name", "string", "tab name (ad_tab.name)." },
                new String[] { "record_id", "string", "record id" },
                new String[] { "data", "object", "JSON Object of fields."
                        + "**Special property in json data object**: 'doc-action' field to perform document actions. "
                        + "**Values**: CO (Complete), VO (Void), RE (Re-Activate), RC (Reverse Correct), RA (Reverse Accrual), PR (Prepare), CL (Close)."
                        + "**Field in the json data object**: use column name (ad_column.name), not field name as json property." }));
        tools.add(createTool("delete_window_tab_record", "Delete tab record by id."
                + "**Important Note**: Prompts for confirmation before deleting unless user explicitly ask for no confirmation. "
                + "**Prompt for confirmation**: When prompt for confirmation, show a table with field names and values.",
                new String[] { "window_name", "tab_name", "record_id" },
                new String[] { "window_name", "string", "window name (ad_window.name)."
                        + "**Field in the json data object**: use column name (ad_column.name), not field name as json property." },
                new String[] { "tab_name", "string", "tab name (ad_tab.name)." },
                new String[] { "record_id", "string", "record id" }));
        tools.add(createTool("get_child_tab_records", "Get child tab records",
                new String[] { "window_name", "tab_name", "record_id", "child_tab_name" },
                new String[] { "window_name", "string", "window name (ad_window.name)."
                        + "**Hint**: Use 'list_windows' tool to search for window." },
                new String[] { "tab_name", "string", "tab name (ad_tab.name)." },
                new String[] { "record_id", "string", "record id" },
                new String[] { "child_tab_name", "string", "child tab name (ad_tab.name)." }));
        tools.add(createTool("create_child_tab_record", "Create child tab record."
                + "**Important Note**: Prompts for missing mandatory fields. "
                + "**Security**: Prompts for confirmation before saving unless user explicitly ask for no confirmation. "
                + "**Prompt for confirmation**: When prompt for confirmation, show a table with field names and values.",
                new String[] { "window_name", "tab_name", "record_id", "child_tab_name", "data" },
                new String[] { "window_name", "string", "window name (ad_window.name)."
                        + "**Hint**: Use 'list_windows' tool to search for window." },
                new String[] { "tab_name", "string", "tab name (ad_tab.name)." },
                new String[] { "record_id", "string", "record id" },
                new String[] { "child_tab_name", "string", "child tab name (ad_tab.name)." },
                new String[] { "data", "object", "JSON Object of fields."
                        + "**Field in the json data object**: use column name (ad_column.name), not field name as json property." }));

        // Views Tools
        tools.add(createTool("list_views", "List available views.", new String[] {},
                new String[] { "filter", "string", "Filter expression (optional)." }));
        tools.add(createTool("get_view_yaml", "Get OpenAPI YAML schema for a view.",
                new String[] { "viewName" },
                new String[] { "viewName", "string", "View Name." }));
        tools.add(createTool("search_view_records", "Search records in a view.",
                new String[] { "viewName" },
                new String[] { "viewName", "string", "View Name." },
                new String[] { "filter", "string", "Filter expression (optional)." },
                new String[] { "sort_column", "string", "Column to sort by (optional)." },
                new String[] { "limit", "integer", "Max records (default 10)" },
                new String[] { "offset", "integer", "Records to skip (default 0)" }));
        tools.add(createTool("create_view_record", "Create a record in a view.",
                new String[] { "viewName", "data" },
                new String[] { "viewName", "string", "View Name." },
                new String[] { "data", "object", "JSON Object of fields." }));
        tools.add(createTool("get_view_record", "Get a record by ID from a view.",
                new String[] { "viewName", "id" },
                new String[] { "viewName", "string", "View Name." },
                new String[] { "id", "string", "Record ID." }));
        tools.add(createTool("update_view_record", "Update a record in a view.",
                new String[] { "viewName", "id", "data" },
                new String[] { "viewName", "string", "View Name." },
                new String[] { "id", "string", "Record ID." },
                new String[] { "data", "object", "Fields to update." }));
        tools.add(createTool("delete_view_record", "Delete a record from a view.",
                new String[] { "viewName", "id" },
                new String[] { "viewName", "string", "View Name." },
                new String[] { "id", "string", "Record ID." }));
        tools.add(createTool("get_view_record_property", "Get a property of a view record.",
                new String[] { "viewName", "id", "columnName" },
                new String[] { "viewName", "string", "View Name." },
                new String[] { "id", "string", "Record ID." },
                new String[] { "columnName", "string", "Column Name." }));
        tools.add(createTool("get_view_record_attachments", "Get attachments of a view record.",
                new String[] { "viewName", "id" },
                new String[] { "viewName", "string", "View Name." },
                new String[] { "id", "string", "Record ID." }));
        tools.add(createTool("add_view_record_attachment", "Add an attachment to a view record.",
                new String[] { "viewName", "id", "data" },
                new String[] { "viewName", "string", "View Name." },
                new String[] { "id", "string", "Record ID." },
                new String[] { "data", "object",
                        "JSON Object required keys: 'name' (filename string) and 'data' (base64 encoded file content string)." }));
        tools.add(createTool("delete_view_record_attachments", "Delete attachments of a view record.",
                new String[] { "viewName", "id" },
                new String[] { "viewName", "string", "View Name." },
                new String[] { "id", "string", "Record ID." }));
        tools.add(createTool("get_view_record_attachments_zip", "Get attachments of a view record as ZIP.",
                new String[] { "viewName", "id" },
                new String[] { "viewName", "string", "View Name." },
                new String[] { "id", "string", "Record ID." }));
        tools.add(createTool("get_view_record_attachment_by_name", "Get a specific attachment from a view record.",
                new String[] { "viewName", "id", "fileName" },
                new String[] { "viewName", "string", "View Name." },
                new String[] { "id", "string", "Record ID." },
                new String[] { "fileName", "string", "File Name." }));
        tools.add(createTool("print_view_record", "Print a view record.",
                new String[] { "viewName", "id" },
                new String[] { "viewName", "string", "View Name." },
                new String[] { "id", "string", "Record ID." }));

        // References
        tools.add(createTool("get_reference", "Get reference definition.",
                new String[] { "id" },
                new String[] { "id", "string", "Reference ID, UUID, or Name." }));

        // Caches
        tools.add(createTool("list_caches", "List active caches (Admin only).", new String[] {},
                new String[] { "table_name", "string", "Optional Table Name filter." },
                new String[] { "name", "string", "Optional Name filter." }));
        tools.add(createTool("reset_cache", "Reset cache (Admin only).", new String[] {},
                new String[] { "record_id", "integer", "Optional Record ID." }));

        // Nodes
        tools.add(createTool("list_nodes", "List server nodes.", new String[] {}));
        tools.add(createTool("get_node", "Get server node details.",
                new String[] { "id" },
                new String[] { "id", "string", "Node UUID." }));
        tools.add(createTool("get_node_logs", "Get list of log files for a node.",
                new String[] { "id" },
                new String[] { "id", "string", "Node UUID." }));
        tools.add(createTool("delete_node_logs", "Remove server log files for a node.",
                new String[] { "id" },
                new String[] { "id", "string", "Node UUID." }));
        tools.add(createTool("get_node_log_file", "Get server log file content.",
                new String[] { "id", "fileName" },
                new String[] { "id", "string", "Node UUID." },
                new String[] { "fileName", "string", "Log File Name." }));
        tools.add(createTool("rotate_node_log", "Rotate server log for a node.",
                new String[] { "id" },
                new String[] { "id", "string", "Node UUID." }));

        // Info Windows
        tools.add(createTool("list_info_windows", "Get list of info windows.", new String[] {},
                new String[] { "filter", "string", "Optional Filter." }));
        tools.add(createTool("get_info_window_data", "Get records using info window.",
                new String[] { "infoSlug" },
                new String[] { "infoSlug", "string", "Info Window Slug." },
                new String[] { "parameters", "string", "JSON for info window query parameters (optional)." },
                new String[] { "where_clause", "string", "Where clause (optional)." },
                new String[] { "order_by", "string", "Order by clause (optional)." },
                new String[] { "page_no", "integer", "Current page number (default 0)." }));
        tools.add(createTool("get_info_window_columns", "Get column definitions of an info window.",
                new String[] { "infoSlug" },
                new String[] { "infoSlug", "string", "Info Window Slug." }));
        tools.add(createTool("get_info_window_processes", "Get process definitions of an info window.",
                new String[] { "infoSlug" },
                new String[] { "infoSlug", "string", "Info Window Slug." }));
        tools.add(createTool("get_info_window_related_infos", "Get related info windows of an info window.",
                new String[] { "infoSlug" },
                new String[] { "infoSlug", "string", "Info Window Slug." }));

        // Workflows
        tools.add(createTool("list_workflow_activities", "Get workflow nodes for user.", new String[] {},
                new String[] { "userId", "string", "Optional User ID/UUID (defaults to current user)." }));
        tools.add(createTool("approve_workflow_activity", "Approve a pending workflow activity.",
                new String[] { "id" },
                new String[] { "id", "string", "Activity ID/UUID." },
                new String[] { "message", "string", "Approval message (optional)." }));
        tools.add(createTool("reject_workflow_activity", "Reject a pending workflow activity.",
                new String[] { "id" },
                new String[] { "id", "string", "Activity ID/UUID." },
                new String[] { "message", "string", "Reject message (optional)." }));
        tools.add(createTool("forward_workflow_activity", "Forward a pending workflow activity.",
                new String[] { "id", "userTo" },
                new String[] { "id", "string", "Activity ID/UUID." },
                new String[] { "userTo", "string", "User ID/UUID to forward to." },
                new String[] { "message", "string", "Forward message (optional)." }));
        tools.add(createTool("acknowledge_workflow_activity", "Acknowledge a pending workflow activity.",
                new String[] { "id" },
                new String[] { "id", "string", "Activity ID/UUID." },
                new String[] { "message", "string", "Acknowledge message (optional)." }));
        tools.add(createTool("set_workflow_activity_user_choice", "Set user selection value.",
                new String[] { "id", "value" },
                new String[] { "id", "string", "Activity ID/UUID." },
                new String[] { "value", "string", "Value to set." }));

        // Status Lines
        tools.add(createTool("list_status_lines", "Get list of status lines.", new String[] {},
                new String[] { "filter", "string", "Optional Filter." },
                new String[] { "with_messages", "boolean", "Include output messages." }));
        tools.add(createTool("get_status_line", "Get output message of a status line.",
                new String[] { "id" },
                new String[] { "id", "string", "Status Line ID/UUID." }));

        // Charts
        tools.add(createTool("get_charts_data", "Get multiple chart data.", new String[] {},
                new String[] { "filter", "string", "Optional Filter." }));
        tools.add(createTool("get_chart", "Get chart as image or json data.",
                new String[] { "id" },
                new String[] { "id", "string", "Chart ID/UUID." },
                new String[] { "width", "integer", "Optional Width." },
                new String[] { "height", "integer", "Optional Height." },
                new String[] { "json", "string", "Optional. If present, return as JSON." }));
        tools.add(createTool("get_chart_data", "Get chart data.",
                new String[] { "id" },
                new String[] { "id", "string", "Chart ID/UUID." }));

        // MenuTree
        tools.add(createTool("get_menu_tree", "Get menu tree by id.",
                new String[] { "id" },
                new String[] { "id", "string", "Menu Tree ID/UUID." }));

        // Uploads
        tools.add(createTool("initiate_upload", "Initiate new upload session.",
                new String[] { "data" },
                new String[] { "data", "object", "UploadInitiationRequest info." }));
        tools.add(createTool("list_pending_uploads", "Retrieves the pending uploads from current user.",
                new String[] {}));
        tools.add(createTool("upload_chunk", "Upload a chunk for an upload session.",
                new String[] { "uploadId", "chunkOrder", "totalChunks", "data" },
                new String[] { "uploadId", "string", "Upload UUID." },
                new String[] { "chunkOrder", "integer", "Chunk sequence (starts from 1)." },
                new String[] { "totalChunks", "integer", "Total number of chunks." },
                new String[] { "data", "string", "Base64 encoded chunk content." }));
        tools.add(createTool("get_upload_status", "Retrieves the current status of an ongoing or completed upload.",
                new String[] { "uploadId" },
                new String[] { "uploadId", "string", "Upload UUID." },
                new String[] { "expiresInSeconds", "integer", "Presigned URL expiration." }));
        tools.add(createTool("cancel_upload", "Cancels an ongoing/completed upload.",
                new String[] { "uploadId" },
                new String[] { "uploadId", "string", "Upload UUID." }));
        tools.add(createTool("get_uploaded_file", "Get uploaded file content.",
                new String[] { "uploadId" },
                new String[] { "uploadId", "string", "Upload UUID." },
                new String[] { "json", "string", "If present, return as JSON." }));
        tools.add(createTool("copy_uploaded_file", "Copy uploaded file to attachment, image or archive.",
                new String[] { "uploadId", "data" },
                new String[] { "uploadId", "string", "Upload UUID." },
                new String[] { "data", "object", "CopyUploadedFileRequest info." }));

        // Batch
        tools.add(createTool("execute_batch", "Execute batch request.",
                new String[] { "data" },
                new String[] { "data", "object", "BatchRequest info." },
                new String[] { "transaction", "boolean", "Process in single transaction (default true)." }));

        JsonObject res = new JsonObject();
        res.add("tools", tools);
        return createSuccess(id, res);
    }

    private String handleToolCall(String id, JsonObject params, String token) {
        String name = params.get("name").getAsString();
        JsonObject args = params.getAsJsonObject("arguments");

        switch (name) {
            case "search_records":
                return McpToolExecutor.search(id, args, token, restClient);
            case "get_record":
                return McpToolExecutor.get(id, args, token, restClient);
            case "create_record":
                return McpToolExecutor.create(id, args, token, restClient);
            case "update_record":
                return McpToolExecutor.update(id, args, token, restClient);
            case "delete_record":
                return McpToolExecutor.delete_record(id, args, token, restClient);
            case "get_record_property":
                return McpToolExecutor.get_record_property(id, args, token, restClient);
            case "get_record_attachments":
                return McpToolExecutor.get_record_attachments(id, args, token, restClient);
            case "add_record_attachment":
                return McpToolExecutor.add_record_attachment(id, args, token, restClient);
            case "delete_record_attachments":
                return McpToolExecutor.delete_record_attachments(id, args, token, restClient);
            case "get_record_attachments_zip":
                return McpToolExecutor.get_record_attachments_zip(id, args, token, restClient);
            case "get_record_attachment_by_name":
                return McpToolExecutor.get_record_attachment_by_name(id, args, token, restClient);
            case "print_record":
                return McpToolExecutor.print_record(id, args, token, restClient);
            case "list_models":
                return McpToolExecutor.listModelsTool(id, args, token, restClient);
            case "get_model_yaml":
                return McpToolExecutor.getModelYamlTool(id, args, token, restClient);
            case "get_process_info":
                return McpToolExecutor.getProcessInfoTool(id, args, token, restClient);
            case "run_process":
                return McpToolExecutor.runProcess(id, args, token, restClient);
            case "list_server_jobs":
                return McpToolExecutor.list_server_jobs(id, args, token, restClient);
            case "get_server_job":
                return McpToolExecutor.get_server_job(id, args, token, restClient);
            case "get_server_job_logs":
                return McpToolExecutor.get_server_job_logs(id, args, token, restClient);
            case "toggle_server_job_state":
                return McpToolExecutor.toggle_server_job_state(id, args, token, restClient);
            case "run_server_job":
                return McpToolExecutor.run_server_job(id, args, token, restClient);
            case "reload_server_jobs":
                return McpToolExecutor.reload_server_jobs(id, args, token, restClient);
            case "get_scheduler_details":
                return McpToolExecutor.get_scheduler_details(id, args, token, restClient);
            case "create_scheduler_job":
                return McpToolExecutor.create_scheduler_job(id, args, token, restClient);
            case "delete_scheduler_job":
                return McpToolExecutor.delete_scheduler_job(id, args, token, restClient);
            case "list_windows":
                return McpToolExecutor.list_windows(id, args, token, restClient);
            case "get_window_tabs":
                return McpToolExecutor.get_window_tabs(id, args, token, restClient);
            case "get_window_tab_fields":
                return McpToolExecutor.get_window_tab_fields(id, args, token, restClient);
            case "get_window_records":
                return McpToolExecutor.get_window_records(id, args, token, restClient);
            case "create_window_record":
                return McpToolExecutor.create_window_record(id, args, token, restClient);
            case "get_window_record":
                return McpToolExecutor.get_window_record(id, args, token, restClient);
            case "print_window_record":
                return McpToolExecutor.print_window_record(id, args, token, restClient);
            case "get_window_tab_record":
                return McpToolExecutor.get_window_tab_record(id, args, token, restClient);
            case "update_window_tab_record":
                return McpToolExecutor.update_window_tab_record(id, args, token, restClient);
            case "delete_window_tab_record":
                return McpToolExecutor.delete_window_tab_record(id, args, token, restClient);
            case "get_child_tab_records":
                return McpToolExecutor.get_child_tab_records(id, args, token, restClient);
            case "create_child_tab_record":
                return McpToolExecutor.create_child_tab_record(id, args, token, restClient);
            case "list_views":
                return McpToolExecutor.list_views(id, args, token, restClient);
            case "get_view_yaml":
                return McpToolExecutor.get_view_yaml(id, args, token, restClient);
            case "search_view_records":
                return McpToolExecutor.search_view_records(id, args, token, restClient);
            case "create_view_record":
                return McpToolExecutor.create_view_record(id, args, token, restClient);
            case "get_view_record":
                return McpToolExecutor.get_view_record(id, args, token, restClient);
            case "update_view_record":
                return McpToolExecutor.update_view_record(id, args, token, restClient);
            case "delete_view_record":
                return McpToolExecutor.delete_view_record(id, args, token, restClient);
            case "get_view_record_property":
                return McpToolExecutor.get_view_record_property(id, args, token, restClient);
            case "get_view_record_attachments":
                return McpToolExecutor.get_view_record_attachments(id, args, token, restClient);
            case "add_view_record_attachment":
                return McpToolExecutor.add_view_record_attachment(id, args, token, restClient);
            case "delete_view_record_attachments":
                return McpToolExecutor.delete_view_record_attachments(id, args, token, restClient);
            case "get_view_record_attachments_zip":
                return McpToolExecutor.get_view_record_attachments_zip(id, args, token, restClient);
            case "get_view_record_attachment_by_name":
                return McpToolExecutor.get_view_record_attachment_by_name(id, args, token, restClient);
            case "print_view_record":
                return McpToolExecutor.print_view_record(id, args, token, restClient);
            case "get_reference":
                return McpToolExecutor.get_reference(id, args, token, restClient);
            case "list_caches":
                return McpToolExecutor.list_caches(id, args, token, restClient);
            case "reset_cache":
                return McpToolExecutor.reset_cache(id, args, token, restClient);
            case "list_nodes":
                return McpToolExecutor.list_nodes(id, args, token, restClient);
            case "get_node":
                return McpToolExecutor.get_node(id, args, token, restClient);
            case "get_node_logs":
                return McpToolExecutor.get_node_logs(id, args, token, restClient);
            case "delete_node_logs":
                return McpToolExecutor.delete_node_logs(id, args, token, restClient);
            case "get_node_log_file":
                return McpToolExecutor.get_node_log_file(id, args, token, restClient);
            case "rotate_node_log":
                return McpToolExecutor.rotate_node_log(id, args, token, restClient);
            case "list_info_windows":
                return McpToolExecutor.list_info_windows(id, args, token, restClient);
            case "get_info_window_data":
                return McpToolExecutor.get_info_window_data(id, args, token, restClient);
            case "get_info_window_columns":
                return McpToolExecutor.get_info_window_columns(id, args, token, restClient);
            case "get_info_window_processes":
                return McpToolExecutor.get_info_window_processes(id, args, token, restClient);
            case "get_info_window_related_infos":
                return McpToolExecutor.get_info_window_related_infos(id, args, token, restClient);
            case "list_workflow_activities":
                return McpToolExecutor.list_workflow_activities(id, args, token, restClient);
            case "approve_workflow_activity":
                return McpToolExecutor.approve_workflow_activity(id, args, token, restClient);
            case "reject_workflow_activity":
                return McpToolExecutor.reject_workflow_activity(id, args, token, restClient);
            case "forward_workflow_activity":
                return McpToolExecutor.forward_workflow_activity(id, args, token, restClient);
            case "acknowledge_workflow_activity":
                return McpToolExecutor.acknowledge_workflow_activity(id, args, token, restClient);
            case "set_workflow_activity_user_choice":
                return McpToolExecutor.set_workflow_activity_user_choice(id, args, token, restClient);
            case "list_status_lines":
                return McpToolExecutor.list_status_lines(id, args, token, restClient);
            case "get_status_line":
                return McpToolExecutor.get_status_line(id, args, token, restClient);
            case "get_charts_data":
                return McpToolExecutor.get_charts_data(id, args, token, restClient);
            case "get_chart":
                return McpToolExecutor.get_chart(id, args, token, restClient);
            case "get_chart_data":
                return McpToolExecutor.get_chart_data(id, args, token, restClient);
            case "get_menu_tree":
                return McpToolExecutor.get_menu_tree(id, args, token, restClient);
            case "initiate_upload":
                return McpToolExecutor.initiate_upload(id, args, token, restClient);
            case "list_pending_uploads":
                return McpToolExecutor.list_pending_uploads(id, args, token, restClient);
            case "upload_chunk":
                return McpToolExecutor.upload_chunk(id, args, token, restClient);
            case "get_upload_status":
                return McpToolExecutor.get_upload_status(id, args, token, restClient);
            case "cancel_upload":
                return McpToolExecutor.cancel_upload(id, args, token, restClient);
            case "get_uploaded_file":
                return McpToolExecutor.get_uploaded_file(id, args, token, restClient);
            case "copy_uploaded_file":
                return McpToolExecutor.copy_uploaded_file(id, args, token, restClient);
            case "execute_batch":
                return McpToolExecutor.execute_batch(id, args, token, restClient);
            default:
                return createError(id, -32601, "Tool not found: " + name);
        }
    }

    private String handleListResources(String id) {
        JsonArray res = new JsonArray();
        res.add(createRes("idempiere://metadata/models", "List All Models"));
        res.add(createRes("idempiere://metadata/processes", "List All Processes"));

        JsonObject r = new JsonObject();
        r.add("resources", res);
        return createSuccess(id, r);
    }

    private String handleReadResource(String id, JsonObject params, String token) {
        String uri = params.get("uri").getAsString();
        if (uri.equals("idempiere://metadata/models"))
            return McpResourceExecutor.listModels(id, token, restClient);
        if (uri.equals("idempiere://metadata/processes"))
            return McpResourceExecutor.listProcesses(id, token, restClient);
        return createError(id, -32602, "Resource not found");
    }

    // --- Helpers ---
    private JsonObject createTool(String name, String desc, String[] req, String[]... props) {
        JsonObject t = new JsonObject();
        t.addProperty("name", name);
        t.addProperty("description", desc);
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        JsonObject pObj = new JsonObject();
        for (String[] p : props) {
            if (p.length == 3) {
                JsonObject i = new JsonObject();
                i.addProperty("type", p[1]);
                i.addProperty("description", p[2]);
                pObj.add(p[0], i);
            }
        }
        schema.add("properties", pObj);
        schema.add("required", gson.toJsonTree(req));
        t.add("inputSchema", schema);
        return t;
    }

    private JsonObject createRes(String uri, String name) {
        JsonObject r = new JsonObject();
        r.addProperty("uri", uri);
        r.addProperty("name", name);
        return r;
    }

    public static String createSuccess(String id, JsonObject res) {
        JsonObject o = new JsonObject();
        o.addProperty("jsonrpc", "2.0");
        o.addProperty("id", id);
        o.add("result", res);
        return o.toString();
    }

    public static String createError(String id, int code, String msg) {
        JsonObject o = new JsonObject();
        o.addProperty("jsonrpc", "2.0");
        o.addProperty("id", id);
        JsonObject e = new JsonObject();
        e.addProperty("code", code);
        e.addProperty("message", msg);
        o.add("error", e);
        return o.toString();
    }
}