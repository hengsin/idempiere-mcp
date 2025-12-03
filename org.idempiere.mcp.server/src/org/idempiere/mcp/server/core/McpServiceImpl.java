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
                case "initialize":      return handleInitialize(requestId); // CHANGED
                case "notifications/initialized": return null; // No response needed for notification
                case "notifications/cancelled":  return null; // No response needed for notification
                case "ping":            return createSuccess(requestId, new JsonObject());
                case "tools/list":      return handleListTools(requestId);
                case "tools/call":      return handleToolCall(requestId, params, authToken);
                case "resources/list":  return handleListResources(requestId);
                case "resources/read":  return handleReadResource(requestId, params, authToken);
                default:                return createError(requestId, -32601, "Method not found: " + method);
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
            + "**Discovery of relation**: Window is a UI definition for user to work with a group of related tables/models, 'get_window_tabs' tool "
            + "can be used to discover the relations between tables/models.",
            new String[]{"model"}, 
            new String[]{"model","string","Table Name (e.g. C_Order)."
            		+ "**Hint**: Check 'idempiere://metadata/models' first to find Table Names."
            		+ "**Hint**: Use the 'list_models' tools get the list of models (tables)."
            		+ "**Hint** 'get_window_records' and 'get_child_tab_records' tool can also be used to search records within a window context."},
            new String[]{"filter","string","OData Filter (e.g. GrandTotal gt 100). "
            		+ "**oData Functions implemented**: "
            		+ "contains(), startswith(), endswith(), tolower() and toupper()."
            		+ "**Search priority for text field**: Value, Name, DocumentNo, Description, ReferenceNo, Email, Phone and Note"
                    + "**Hint for Text field search**: use the OData 'contains' function for partial match and 'tolower' for case insensitive search."
                    + "**Hint for filter**: If filtering can't be fulfilled with the implemented OData functions, "
                    + "consider using the 'get_window_records' tool that support SQL filter clause."},
            new String[]{"limit","integer","Max records (default 10)"},
            new String[]{"offset","integer","Records to skip (default 0)"}
        ));
        
        tools.add(createTool("get_record", 
            "Get a record by ID (Integer or UUID). "
    		+ "**Discovery of relation**: Window is a UI definition for user to work with a group of related tables/models, 'get_window_tabs' tool "
            + "can be used to discover the relations between tables/models.", 
            new String[]{"model","id"}, 
            new String[]{"model","string","Table Name"
            		+ "**Hint**: Check 'idempiere://metadata/models' first to find Table Names."
            		+ "**Hint**: Use the 'list_models' tools get the list of models (tables)."
            		+ "**Hint** 'get_window_records' and 'get_child_tab_records' tool can also be used to search records within a window context."},
            new String[]{"id","string","Record ID"}
        ));
        
        tools.add(createTool("create_record", "Create a record. "
        		+ "**Table Schema**: Use the 'get_model_yaml' tool for model schema definition in openapi yaml format."
        		+ "**Discovery of relation**: Window is a UI definition for user to work with a group of related tables/models, 'get_window_tabs' tool "
                + "**Important Note**: Prompts for missing mandatory fields. "
                + "**Security**: Prompts for confirmation before saving unless user explicitly ask for no confirmation. "
                + "**Prompt for Confirmation**: When prompt for confirmation, show a table with field names and values.", 
            new String[]{"model","data"}, 
            new String[]{"model","string","Table Name."
            		+ "**Hint**: Check 'idempiere://metadata/models' first to find Table Names."
            		+ "**Hint**: Use the 'list_models' tools get the list of models (tables)."
            		+ "**Hint** 'get_window_records' and 'get_child_tab_records' tool can also be used to search records within a window context."}, 
            new String[]{"data","object","JSON Object of fields."
            		+ "**Special property in json data object**: 'doc-action' field to perform document actions - CO for Complete, "
            		+ "VO for Void, RE for Re-Activate, RC for Reverse Correct, RA for Reverse Accrual, PR for Prepare and CL for Close."}
        ));
        
        tools.add(createTool("update_record", "Update a record. "
        		+ "**Table Schema**: Use the 'get_model_yaml' tool for model schema definition in openapi yaml format."
        		+ "**Discovery of relation**: Window is a UI definition for user to work with a group of related tables/models, 'get_window_tabs' tool "
        		+ "**Important Note**: Prompts for missing mandatory fields. "
                + "**Security**: Prompts for confirmation before saving unless user explicitly ask for no confirmation. "
                + "**Prompt for Confirmation**: When prompt for confirmation, show a table with field names and values.", 
            new String[]{"model","id","data"}, 
            new String[]{"model","string","Table Name."
            		+ "**Hint**: Check 'idempiere://metadata/models' first to find Table Names."
            		+ "**Hint**: Use the 'list_models' tools get the list of models (tables)."
            		+ "**Hint** 'get_window_records' and 'get_child_tab_records' tool can also be used to search records within a window context."}, 
            new String[]{"id","string","Record ID"}, 
            new String[]{"data","object","Fields to update."
            		+ "**Special property in json data object**: 'doc-action' field to perform document actions - CO for Complete, "
            		+ "VO for Void, RE for Re-Activate, RC for Reverse Correct, RA for Reverse Accrual, PR for Prepare and CL for Close."}
        ));

        tools.add(createTool("delete_record", "Delete record by id. 'delete_window_tab_record' tool can also "
        		+ "be used to delete record within a window context."
        		+ "**Important Note**: Prompts for missing mandatory fields. "
                + "**Security**: Prompts for confirmation before saving unless user explicitly ask for no confirmation. "
                + "**Prompt for Confirmation**: When prompt for confirmation, show a table with field names and values.", new String[]{"tableName", "record_id"},
        	new String[]{"tableName","string","table name."
        			+ "**Hint**: Check 'idempiere://metadata/models' first to find Table Names."
            		+ "**Hint**: Use the 'list_models' tools get the list of models (tables)."
            		+ "**Hint** 'get_window_records' and 'get_child_tab_records' tool can also be used to search records within a window context."},
        	new String[]{"record_id","string","record id"}
        ));
        
        tools.add(createTool("get_record_property", "Get property value of a record."
        		+ "**Table Schema**: Use the 'get_model_yaml' tool for model schema definition in openapi yaml format.", 
        		new String[]{"tableName", "record_id", "columnName"},
        	new String[]{"tableName","string","table name"
        			+ "**Hint**: Check 'idempiere://metadata/models' first to find Table Names."
            		+ "**Hint**: Use the 'list_models' tools get the list of models (tables)."
            		+ "**Hint** 'get_window_records' and 'get_child_tab_records' tool can also be used to search records within a window context."},
        	new String[]{"record_id","string","record id"},
        	new String[]{"columnName","string","column name"}
        ));
        
        tools.add(createTool("get_record_attachments", "Get list of attachment of a record", new String[]{"tableName", "record_id"},
        	new String[]{"tableName","string","table name."
        			+ "**Hint**: Check 'idempiere://metadata/models' first to find Table Names."
            		+ "**Hint**: Use the 'list_models' tools get the list of models (tables)."
            		+ "**Hint** 'get_window_records' and 'get_child_tab_records' tool can also be used to search records within a window context."},
        	new String[]{"record_id","string","record id"}
        ));
        
        tools.add(createTool("add_record_attachment", "Add/update attachment by record id and file name", new String[]{"tableName", "record_id", "data"},
        	new String[]{"tableName","string","table name."
        			+ "**Hint**: Check 'idempiere://metadata/models' first to find Table Names."
            		+ "**Hint**: Use the 'list_models' tools get the list of models (tables)."
            		+ "**Hint** 'get_window_records' and 'get_child_tab_records' tool can also be used to search records within a window context."},
        	new String[]{"record_id","string","record id"},
        	new String[]{"data","object","JSON Object with name and base64 encoded data"}
        ));
        
        tools.add(createTool("delete_record_attachments", "Delete attachment of a record", new String[]{"tableName", "record_id"},
        	new String[]{"tableName","string","table name."
        			+ "**Hint**: Check 'idempiere://metadata/models' first to find Table Names."
            		+ "**Hint**: Use the 'list_models' tools get the list of models (tables)."
            		+ "**Hint** 'get_window_records' and 'get_child_tab_records' tool can also be used to search records within a window context."},
        	new String[]{"record_id","string","record id"}
        ));
        
        tools.add(createTool("get_record_attachments_zip", "Get attachment of a record as zip archive", new String[]{"tableName", "record_id"},
        	new String[]{"tableName","string","table name."
        			+ "**Hint**: Check 'idempiere://metadata/models' first to find Table Names."
            		+ "**Hint**: Use the 'list_models' tools get the list of models (tables)."
            		+ "**Hint** 'get_window_records' and 'get_child_tab_records' tool can also be used to search records within a window context."},
        	new String[]{"record_id","string","record id"}
        ));
        
        tools.add(createTool("get_record_attachment_by_name", "Get attachment of a record by file name", new String[]{"tableName", "record_id", "fileName"},
        	new String[]{"tableName","string","table name"
        			+ "**Hint**: Check 'idempiere://metadata/models' first to find Table Names."
            		+ "**Hint**: Use the 'list_models' tools get the list of models (tables)."
            		+ "**Hint** 'get_window_records' and 'get_child_tab_records' tool can also be used to search records within a window context."},
        	new String[]{"record_id","string","record id"},
        	new String[]{"fileName","string","file name"}
        ));
        
        tools.add(createTool("print_record", "Print document of a record", new String[]{"tableName", "record_id"},
        	new String[]{"tableName","string","table name."
        			+ "**Hint**: Check 'idempiere://metadata/models' first to find Table Names."
            		+ "**Hint**: Use the 'list_models' tools get the list of models (tables)."
            		+ "**Hint** 'get_window_records' and 'get_child_tab_records' tool can also be used to search records within a window context."},
        	new String[]{"record_id","string","record id"}
        ));
            
        tools.add(createTool("list_models", "List application models (tables)", new String[]{},
            new String[]{"filter","string","OData $filter expression (optional)."
            		+ "**oData Functions implemented**: "
            		+ "contains(), startswith(), endswith(), tolower() and toupper()."}
        ));
        tools.add(createTool("get_model_yaml", "Get OpenAPI YAML schema for a model (table)", new String[]{"tableName"},
            new String[]{"tableName","string","Table Name (e.g. C_Order)."
            		+ "**Hint**: Check 'idempiere://metadata/models' first to find Table Names."
            		+ "**Hint**: Use the 'list_models' tools get the list of models (tables)."
            		+ "**Hint** 'get_window_records' and 'get_child_tab_records' tool can also be used to search records within a window context."}
        ));
        
        tools.add(createTool("get_process_info", "Get details of a process by value/search key (ad_process.value).", new String[]{"process_value"},
        	new String[]{"process_value","string","process value/search key (ad_process.value)."
        			+ "**Search for Process**: 1. 'idempiere://metadata/processes' MCP resource for value property."
                    + "2. use the 'search_records' tool with ad_process as model."
                    + "3. use the 'get_window_records' tool for process window"}
        ));
        
        tools.add(createTool("run_process", 
            "Run a process/report. "
            + "**Search for Process**: 1. 'idempiere://metadata/processes' MCP resource for value property."
            + "2. use the search_records tool with ad_process as model."
            + "3. use the get_window_records tool for process window", 
            new String[]{"process_value"},
            new String[]{"process_value","string","value/search key property of the process (ad_process.value)."
            		+ "**Search for Process**: 1. 'idempiere://metadata/processes' MCP resource for value property."
                    + "2. use the 'search_records' tool with ad_process as model."
                    + "3. use the 'get_window_records' tool for process window"},
            new String[]{"parameters","object","Process parameters. "
            		+ "**Important Note**: Do not quote the parameter with single quote character."}
        ));
        
        tools.add(createTool("list_server_jobs", "Get server jobs", new String[]{}, new String[]{}));
        
        tools.add(createTool("get_server_job", "Get server job by id", new String[]{"server_id"},
        	new String[]{"server_id","string","server job id"}
        ));
        
        tools.add(createTool("get_server_job_logs", "Get server job logs by id", new String[]{"server_id"},
        	new String[]{"server_id","string","server job id"}
        ));
        
        tools.add(createTool("toggle_server_job_state", "Toggle server job state (stop to start and vice versa)", new String[]{"server_id"},
        	new String[]{"server_id","string","server job id"}
        ));
        tools.add(createTool("run_server_job", "Run server job by id", new String[]{"server_id"},
        	new String[]{"server_id","string","server job id"}
        ));
        
        tools.add(createTool("reload_server_jobs", "Reload all server jobs", new String[]{}, new String[]{}));
        
        tools.add(createTool("get_scheduler_details", "Get scheduler details by id", new String[]{"scheduler_id"},
        	new String[]{"scheduler_id","string","scheduler id"}
        ));
        
        tools.add(createTool("create_scheduler_job", "Create new server job for scheduler", new String[]{"scheduler_id"},
        	new String[]{"scheduler_id","string","scheduler id"}
        ));
        
        tools.add(createTool("delete_scheduler_job", "Remove server job of a scheduler."
        		+ "**Important Note**: Prompts for confirmation unless user explicitly ask for no confirmation.", new String[]{"scheduler_id"},
        	new String[]{"scheduler_id","string","scheduler id"}
        ));
        
        tools.add(createTool("list_windows", "Get list of windows. Window is a UI definition for user "
        		+ "to work with a group of related tables/models.", new String[]{},
        	new String[]{"filter","string","OData $filter expression (optional)"
        			+ "**oData Functions implemented**: "
            		+ "contains(), startswith(), endswith(), tolower() and toupper()."}
        ));
        tools.add(createTool("get_window_tabs", "Get list of tabs of a window. Each tab is map to a table/model.", new String[]{"window_name"},
        	new String[]{"window_name","string","window name (ad_window.name)."
        			+ "**Hint**: Use 'list_windows' tool to search for window."}
        ));
        tools.add(createTool("get_window_tab_fields", "Get list of fields of a window tab. Each field is a UI element for user to "
        		+ "interact with a specific table column (model property).", new String[]{"window_name", "tab_name"},
        	new String[]{"window_name","string","window name (ad_window.name)."
        			+ "**Hint**: Use 'list_windows' tool to search for window."},
        	new String[]{"tab_name","string","window tab name (ad_tab.name)."}
        ));
        tools.add(createTool("get_window_records", "Search records of a window.", new String[]{"window_name"},
        	new String[]{"window_name","string","window name (ad_window.name)."
        			+ "**Hint**: Use 'list_windows' tool to search for window."},
        	new String[]{"filter","string","SQL filter clause (optional)."
        			+ "**Security**: The filter clause is sanitized to prevent SQL injection."
        			+ "**Hint for Text field search**: use like for partial match and 'lower' sql function for case insensitive search."},
        	new String[]{"sort_column","string","Column to sort by, use ! prefix for descending sort (optional)"},
        	new String[]{"page_no","integer","Current page no (optional)"}
        ));
        tools.add(createTool("create_window_record", "Create window record. "        		
        		+ "**Important Note**: Prompts for missing mandatory fields. "
        		+ "**Security**: Prompts for confirmation before saving unless user explicitly ask for no confirmation. "
                + "**Prompt for confirmation**: When prompt for confirmation, show a table with field names and values.", new String[]{"window_name", "data"},
        	new String[]{"window_name","string","window name (ad_window.name)."
        			+ "**Hint**: Use 'list_windows' tool to search for window."},
        	new String[]{"data","object","JSON Object of fields"
        			+ "**Special property in json data object**: 'doc-action' field to perform document actions - CO for Complete, "
            		+ "VO for Void, RE for Re-Activate, RC for Reverse Correct, RA for Reverse Accrual, PR for Prepare and CL for Close."
            		+ "**Field in the json data object**: use column name (ad_column.name), not field name as json property."}
        ));
        tools.add(createTool("get_window_record", "Get record by id.", new String[]{"window_name", "record_id"},
        	new String[]{"window_name","string","window name (ad_window.name)."
        			+ "**Hint**: Use 'list_windows' tool to search for window."},
        	new String[]{"record_id","string","record id"},
        	new String[]{"expand","string","comma separated list of child tabs to load (optional)"}
        ));
        tools.add(createTool("print_window_record", "Print document of a record.", new String[]{"window_name", "record_id"},
        	new String[]{"window_name","string","window name (ad_window.name)."
        			+ "**Hint**: Use 'list_windows' tool to search for window."},
        	new String[]{"record_id","string","record id"}
        ));
        tools.add(createTool("get_window_tab_record", "Get tab record by id.", new String[]{"window_name", "tab_name", "record_id"},
        	new String[]{"window_name","string","window name (ad_window.name)."
        			+ "**Hint**: Use 'list_windows' tool to search for window."},
        	new String[]{"tab_name","string","tab name (ad_tab.name)."},
        	new String[]{"record_id","string","record id"}
        ));
        tools.add(createTool("update_window_tab_record", "Update tab record by id. "        		
        		+ "**Important Note**: Prompts for missing mandatory fields. "
        		+ "**Security**: Prompts for confirmation before saving unless user explicitly ask for no confirmation. "
        		+ "**Prompt for confirmation**: When prompt for confirmation, show a table with field names and values.", new String[]{"window_name", "tab_name", "record_id", "data"},
        	new String[]{"window_name","string","window name (ad_window.name)."
        			+ "**Hint**: Use 'list_windows' tool to search for window."},
        	new String[]{"tab_name","string","tab name (ad_tab.name)."},
        	new String[]{"record_id","string","record id"},
        	new String[]{"data","object","JSON Object of fields."
        			+ "**Special property in json data object**: 'doc-action' field to perform document actions - CO for Complete, "
            		+ "VO for Void, RE for Re-Activate, RC for Reverse Correct, RA for Reverse Accrual, PR for Prepare and CL for Close."
            		+ "**Field in the json data object**: use column name (ad_column.name), not field name as json property."}
        ));
        tools.add(createTool("delete_window_tab_record", "Delete tab record by id."
        		+ "**Important Note**: Prompts for confirmation before deleting unless user explicitly ask for no confirmation. "
        		+ "**Prompt for confirmation**: When prompt for confirmation, show a table with field names and values.", new String[]{"window_name", "tab_name", "record_id"},
        	new String[]{"window_name","string","window name (ad_window.name)."
        			+ "**Field in the json data object**: use column name (ad_column.name), not field name as json property."},
        	new String[]{"tab_name","string","tab name (ad_tab.name)."},
        	new String[]{"record_id","string","record id"}
        ));
        tools.add(createTool("get_child_tab_records", "Get child tab records", new String[]{"window_name", "tab_name", "record_id", "child_tab_name"},
        	new String[]{"window_name","string","window name (ad_window.name)."
        			+ "**Hint**: Use 'list_windows' tool to search for window."},
        	new String[]{"tab_name","string","tab name (ad_tab.name)."},
        	new String[]{"record_id","string","record id"},
        	new String[]{"child_tab_name","string","child tab name (ad_tab.name)."}
        ));
        tools.add(createTool("create_child_tab_record", "Create child tab record."
        		+ "**Important Note**: Prompts for missing mandatory fields. "
        		+ "**Security**: Prompts for confirmation before saving unless user explicitly ask for no confirmation. "
                + "**Prompt for confirmation**: When prompt for confirmation, show a table with field names and values.", new String[]{"window_name", "tab_name", "record_id", "child_tab_name", "data"},
        	new String[]{"window_name","string","window name (ad_window.name)."
        			+ "**Hint**: Use 'list_windows' tool to search for window."},
        	new String[]{"tab_name","string","tab name (ad_tab.name)."},
        	new String[]{"record_id","string","record id"},
        	new String[]{"child_tab_name","string","child tab name (ad_tab.name)."},
        	new String[]{"data","object","JSON Object of fields."
        			+ "**Field in the json data object**: use column name (ad_column.name), not field name as json property."}
        ));
        JsonObject res = new JsonObject(); res.add("tools", tools);
        return createSuccess(id, res);
    }

    private String handleToolCall(String id, JsonObject params, String token) {
        String name = params.get("name").getAsString();
        JsonObject args = params.getAsJsonObject("arguments");

        switch (name) {
            case "search_records": return McpToolExecutor.search(id, args, token, restClient);
            case "get_record":     return McpToolExecutor.get(id, args, token, restClient);
            case "create_record":  return McpToolExecutor.create(id, args, token, restClient);
            case "update_record":  return McpToolExecutor.update(id, args, token, restClient);
            case "delete_record": return McpToolExecutor.delete_record(id, args, token, restClient);
            case "get_record_property": return McpToolExecutor.get_record_property(id, args, token, restClient);
            case "get_record_attachments": return McpToolExecutor.get_record_attachments(id, args, token, restClient);
            case "add_record_attachment": return McpToolExecutor.add_record_attachment(id, args, token, restClient);
            case "delete_record_attachments": return McpToolExecutor.delete_record_attachments(id, args, token, restClient);
            case "get_record_attachments_zip": return McpToolExecutor.get_record_attachments_zip(id, args, token, restClient);
            case "get_record_attachment_by_name": return McpToolExecutor.get_record_attachment_by_name(id, args, token, restClient);
            case "print_record": return McpToolExecutor.print_record(id, args, token, restClient);            
            case "list_models":    return McpToolExecutor.listModelsTool(id, args, token, restClient);
            case "get_model_yaml": return McpToolExecutor.getModelYamlTool(id, args, token, restClient);
            case "get_process_info": return McpToolExecutor.getProcessInfoTool(id, args, token, restClient);
            case "run_process":    return McpToolExecutor.runProcess(id, args, token, restClient);            
            case "list_server_jobs": return McpToolExecutor.list_server_jobs(id, args, token, restClient);
            case "get_server_job": return McpToolExecutor.get_server_job(id, args, token, restClient);
            case "get_server_job_logs": return McpToolExecutor.get_server_job_logs(id, args, token, restClient);
            case "toggle_server_job_state": return McpToolExecutor.toggle_server_job_state(id, args, token, restClient);
            case "run_server_job": return McpToolExecutor.run_server_job(id, args, token, restClient);
            case "reload_server_jobs": return McpToolExecutor.reload_server_jobs(id, args, token, restClient);
            case "get_scheduler_details": return McpToolExecutor.get_scheduler_details(id, args, token, restClient);
            case "create_scheduler_job": return McpToolExecutor.create_scheduler_job(id, args, token, restClient);
            case "delete_scheduler_job": return McpToolExecutor.delete_scheduler_job(id, args, token, restClient);
            case "list_windows": return McpToolExecutor.list_windows(id, args, token, restClient);
            case "get_window_tabs": return McpToolExecutor.get_window_tabs(id, args, token, restClient);
            case "get_window_tab_fields": return McpToolExecutor.get_window_tab_fields(id, args, token, restClient);
            case "get_window_records": return McpToolExecutor.get_window_records(id, args, token, restClient);
            case "create_window_record": return McpToolExecutor.create_window_record(id, args, token, restClient);
            case "get_window_record": return McpToolExecutor.get_window_record(id, args, token, restClient);
            case "print_window_record": return McpToolExecutor.print_window_record(id, args, token, restClient);
            case "get_window_tab_record": return McpToolExecutor.get_window_tab_record(id, args, token, restClient);
            case "update_window_tab_record": return McpToolExecutor.update_window_tab_record(id, args, token, restClient);
            case "delete_window_tab_record": return McpToolExecutor.delete_window_tab_record(id, args, token, restClient);
            case "get_child_tab_records": return McpToolExecutor.get_child_tab_records(id, args, token, restClient);
            case "create_child_tab_record": return McpToolExecutor.create_child_tab_record(id, args, token, restClient);
            default:                return createError(id, -32601, "Tool not found: " + name);
        }
    }

    private String handleListResources(String id) {
        JsonArray res = new JsonArray();
        res.add(createRes("idempiere://metadata/models", "List All Models"));
        res.add(createRes("idempiere://metadata/processes", "List All Processes"));
        
        JsonObject r = new JsonObject(); r.add("resources", res);
        return createSuccess(id, r);
    }

    private String handleReadResource(String id, JsonObject params, String token) {
        String uri = params.get("uri").getAsString();
        if (uri.equals("idempiere://metadata/models")) return McpResourceExecutor.listModels(id, token, restClient);
        if (uri.equals("idempiere://metadata/processes")) return McpResourceExecutor.listProcesses(id, token, restClient);
        return createError(id, -32602, "Resource not found");
    }

    // --- Helpers ---
    private JsonObject createTool(String name, String desc, String[] req, String[]... props) {
        JsonObject t = new JsonObject(); t.addProperty("name", name); t.addProperty("description", desc);
        JsonObject schema = new JsonObject(); schema.addProperty("type", "object");
        JsonObject pObj = new JsonObject();
        for(String[] p : props) { if (p.length == 3) { JsonObject i = new JsonObject(); i.addProperty("type", p[1]); i.addProperty("description", p[2]); pObj.add(p[0], i); } }
        schema.add("properties", pObj); schema.add("required", gson.toJsonTree(req));
        t.add("inputSchema", schema); return t;
    }
    private JsonObject createRes(String uri, String name) {
        JsonObject r = new JsonObject(); r.addProperty("uri", uri); r.addProperty("name", name); return r;
    }
    public static String createSuccess(String id, JsonObject res) {
        JsonObject o = new JsonObject(); o.addProperty("jsonrpc", "2.0"); o.addProperty("id", id); o.add("result", res); return o.toString();
    }
    public static String createError(String id, int code, String msg) {
        JsonObject o = new JsonObject(); o.addProperty("jsonrpc", "2.0"); o.addProperty("id", id); 
        JsonObject e = new JsonObject(); e.addProperty("code", code); e.addProperty("message", msg); o.add("error", e); return o.toString();
    }
}