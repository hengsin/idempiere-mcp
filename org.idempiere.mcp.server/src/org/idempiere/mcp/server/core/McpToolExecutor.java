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

import org.idempiere.mcp.server.client.RestApiClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class McpToolExecutor {

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static String search(String id, JsonObject args, String token, RestApiClient client) {
        try {
            String model = args.get("model").getAsString();
            String filter = args.has("filter") ? args.get("filter").getAsString() : "";
            
            // Paging Support
            int limit = args.has("limit") ? args.get("limit").getAsInt() : 10;
            int offset = args.has("offset") ? args.get("offset").getAsInt() : 0;

            StringBuilder sb = new StringBuilder("/models/").append(model);
            sb.append("?$top=").append(limit);
            sb.append("&$skip=").append(offset);
            
            if (!filter.isEmpty()) {
                sb.append("&$filter=").append(URLEncoder.encode(filter, StandardCharsets.UTF_8));
            }

            JsonElement response = client.get(sb.toString(), token);
            return wrapJsonContent(id, response);
        } catch (Exception e) {
            return McpServiceImpl.createError(id, -32000, "Search Error: " + e.getMessage());
        }
    }

    public static String get(String id, JsonObject args, String token, RestApiClient client) {
        try {
            String model = args.get("model").getAsString();
            JsonElement idEl = args.get("id");
            String path = "/models/" + model + "/" + (isInteger(idEl) ? idEl.getAsInt() : idEl.getAsString());

            JsonElement response = client.get(path, token);
            return wrapJsonContent(id, response);
        } catch (Exception e) {
            return McpServiceImpl.createError(id, -32000, "Get Error: " + e.getMessage());
        }
    }

    public static String create(String id, JsonObject args, String token, RestApiClient client) {
        try {
            String model = args.get("model").getAsString();
            JsonObject data = args.get("data").getAsJsonObject();
            JsonElement response = client.post("/models/" + model, data, token);
            return wrapJsonContent(id, response);
        } catch (Exception e) {
            return McpServiceImpl.createError(id, -32000, "Create Error: " + e.getMessage());
        }
    }

    public static String update(String id, JsonObject args, String token, RestApiClient client) {
        try {
            String model = args.get("model").getAsString();
            JsonElement idEl = args.get("id");
            JsonObject data = args.get("data").getAsJsonObject();
            
            String path = "/models/" + model + "/" + (isInteger(idEl) ? idEl.getAsInt() : idEl.getAsString());
            JsonElement response = client.put(path, data, token);
            return wrapJsonContent(id, response);
        } catch (Exception e) {
            return McpServiceImpl.createError(id, -32000, "Update Error: " + e.getMessage());
        }
    }

    public static String delete_record(String id, JsonObject args, String token, RestApiClient client) {
        try {
            String tableName = args.get("tableName").getAsString();
            String recordId = args.get("record_id").getAsString();
            String path = "/models/" + URLEncoder.encode(tableName, StandardCharsets.UTF_8) + "/" + recordId;
            JsonElement response = client.delete(path, token);
            return wrapJsonContent(id, response);
        } catch (Exception e) {
            return McpServiceImpl.createError(id, -32000, "Delete Record Error: " + e.getMessage());
        }
    }

    public static String get_record_property(String id, JsonObject args, String token, RestApiClient client) {
        try {
            String tableName = args.get("tableName").getAsString();
            String recordId = args.get("record_id").getAsString();
            String columnName = args.get("columnName").getAsString();
            String path = "/models/" + URLEncoder.encode(tableName, StandardCharsets.UTF_8) + "/" + recordId + "/" + URLEncoder.encode(columnName, StandardCharsets.UTF_8);
            JsonElement response = client.get(path, token);
            return wrapJsonContent(id, response);
        } catch (Exception e) {
            return McpServiceImpl.createError(id, -32000, "Get Record Property Error: " + e.getMessage());
        }
    }

    public static String get_record_attachments(String id, JsonObject args, String token, RestApiClient client) {
        try {
            String tableName = args.get("tableName").getAsString();
            String recordId = args.get("record_id").getAsString();
            String path = "/models/" + URLEncoder.encode(tableName, StandardCharsets.UTF_8) + "/" + recordId + "/attachments";
            JsonElement response = client.get(path, token);
            return wrapJsonContent(id, response);
        } catch (Exception e) {
            return McpServiceImpl.createError(id, -32000, "Get Record Attachments Error: " + e.getMessage());
        }
    }

    public static String add_record_attachment(String id, JsonObject args, String token, RestApiClient client) {
        try {
            String tableName = args.get("tableName").getAsString();
            String recordId = args.get("record_id").getAsString();
            JsonObject data = args.get("data").getAsJsonObject();
            String path = "/models/" + URLEncoder.encode(tableName, StandardCharsets.UTF_8) + "/" + recordId + "/attachments";
            JsonElement response = client.post(path, data, token);
            return wrapJsonContent(id, response);
        } catch (Exception e) {
            return McpServiceImpl.createError(id, -32000, "Add Record Attachment Error: " + e.getMessage());
        }
    }

    public static String delete_record_attachments(String id, JsonObject args, String token, RestApiClient client) {
        try {
            String tableName = args.get("tableName").getAsString();
            String recordId = args.get("record_id").getAsString();
            String path = "/models/" + URLEncoder.encode(tableName, StandardCharsets.UTF_8) + "/" + recordId + "/attachments";
            JsonElement response = client.delete(path, token);
            return wrapJsonContent(id, response);
        } catch (Exception e) {
            return McpServiceImpl.createError(id, -32000, "Delete Record Attachments Error: " + e.getMessage());
        }
    }

    public static String get_record_attachments_zip(String id, JsonObject args, String token, RestApiClient client) {
        try {
            String tableName = args.get("tableName").getAsString();
            String recordId = args.get("record_id").getAsString();
            String path = "/models/" + URLEncoder.encode(tableName, StandardCharsets.UTF_8) + "/" + recordId + "/attachments/zip";
            byte[] response = client.getBinary(path, token, "application/zip");
            return wrapBinaryContent(id, response, "application/zip");
        } catch (Exception e) {
            return McpServiceImpl.createError(id, -32000, "Get Record Attachments Zip Error: " + e.getMessage());
        }
    }

    public static String get_record_attachment_by_name(String id, JsonObject args, String token, RestApiClient client) {
        try {
            String tableName = args.get("tableName").getAsString();
            String recordId = args.get("record_id").getAsString();
            String fileName = args.get("fileName").getAsString();
            String path = "/models/" + URLEncoder.encode(tableName, StandardCharsets.UTF_8) + "/" + recordId + "/attachments/" + URLEncoder.encode(fileName, StandardCharsets.UTF_8);
            byte[] response = client.getBinary(path, token, "application/octet-stream");
            return wrapBinaryContent(id, response, "application/octet-stream");
        } catch (Exception e) {
            return McpServiceImpl.createError(id, -32000, "Get Record Attachment By Name Error: " + e.getMessage());
        }
    }

    public static String print_record(String id, JsonObject args, String token, RestApiClient client) {
        try {
            String tableName = args.get("tableName").getAsString();
            String recordId = args.get("record_id").getAsString();
            String path = "/models/" + URLEncoder.encode(tableName, StandardCharsets.UTF_8) + "/" + recordId + "/print";
            JsonElement response = client.get(path, token);
            return wrapJsonContent(id, response);
        } catch (Exception e) {
            return McpServiceImpl.createError(id, -32000, "Print Record Error: " + e.getMessage());
        }
    }
    
    public static String listModelsTool(String id, JsonObject args, String token, RestApiClient client) {
        try {
            StringBuilder path = new StringBuilder("/models");
            if (args != null && args.has("filter")) {
                String filter = args.get("filter").getAsString();
                if (!filter.isEmpty()) {
                    path.append("?$filter=").append(URLEncoder.encode(filter, StandardCharsets.UTF_8));
                }
            }
            JsonElement response = client.get(path.toString(), token);
            return wrapJsonContent(id, response);
        } catch (Exception e) {
            return McpServiceImpl.createError(id, -32000, "List Models Error: " + e.getMessage());
        }
    }

    public static String getModelYamlTool(String id, JsonObject args, String token, RestApiClient client) {
        try {
            String model = args.get("tableName").getAsString();
            String path = "/models/" + URLEncoder.encode(model, StandardCharsets.UTF_8) + "/yaml";
            String yaml = client.getYaml(path, token);
            JsonObject item = new JsonObject();
            item.addProperty("type", "text");
            item.addProperty("mimeType", "application/yaml");
            item.addProperty("text", yaml);
            JsonArray content = new JsonArray(); content.add(item);
            JsonObject result = new JsonObject(); result.add("content", content);
            return McpServiceImpl.createSuccess(id, result);
        } catch (Exception e) {
            return McpServiceImpl.createError(id, -32000, "Get Model YAML Error: " + e.getMessage());
        }
    }

    public static String getProcessInfoTool(String id, JsonObject args, String token, RestApiClient client) {
        try {
            String processSlug = args.get("process_slug").getAsString();
            String path = "/processes/" + URLEncoder.encode(processSlug, StandardCharsets.UTF_8);
            JsonElement response = client.get(path, token);
            return wrapJsonContent(id, response);
        } catch (Exception e) {
            return McpServiceImpl.createError(id, -32000, "Get Process Info Error: " + e.getMessage());
        }
    }

    public static String runProcess(String id, JsonObject args, String token, RestApiClient client) {
        try {
            String processId = args.get("process_id").getAsString();
            JsonObject params = args.has("parameters") ? args.get("parameters").getAsJsonObject() : new JsonObject();
            
            JsonObject payload = new JsonObject();
            payload.add("parameters", params);

            JsonElement response = client.post("/processes/" + processId, payload, token);
            return wrapJsonContent(id, response);
        } catch (Exception e) {
            return McpServiceImpl.createError(id, -32000, "Process Error: " + e.getMessage());
        }
    }

    public static String list_server_jobs(String id, JsonObject args, String token, RestApiClient client) {
        try {
            String path = "/servers";
            JsonElement response = client.get(path, token);
            return wrapJsonContent(id, response);
        } catch (Exception e) {
            return McpServiceImpl.createError(id, -32000, "List Server Jobs Error: " + e.getMessage());
        }
    }

    public static String get_server_job(String id, JsonObject args, String token, RestApiClient client) {
        try {
            String serverId = args.get("server_id").getAsString();
            String path = "/servers/" + URLEncoder.encode(serverId, StandardCharsets.UTF_8);
            JsonElement response = client.get(path, token);
            return wrapJsonContent(id, response);
        } catch (Exception e) {
            return McpServiceImpl.createError(id, -32000, "Get Server Job Error: " + e.getMessage());
        }
    }

    public static String get_server_job_logs(String id, JsonObject args, String token, RestApiClient client) {
        try {
            String serverId = args.get("server_id").getAsString();
            String path = "/servers/" + URLEncoder.encode(serverId, StandardCharsets.UTF_8) + "/logs";
            JsonElement response = client.get(path, token);
            return wrapJsonContent(id, response);
        } catch (Exception e) {
            return McpServiceImpl.createError(id, -32000, "Get Server Job Logs Error: " + e.getMessage());
        }
    }

    public static String toggle_server_job_state(String id, JsonObject args, String token, RestApiClient client) {
        try {
            String serverId = args.get("server_id").getAsString();
            String path = "/servers/" + URLEncoder.encode(serverId, StandardCharsets.UTF_8) + "/state";
            JsonElement response = client.post(path, new JsonObject(), token);
            return wrapJsonContent(id, response);
        } catch (Exception e) {
            return McpServiceImpl.createError(id, -32000, "Toggle Server Job State Error: " + e.getMessage());
        }
    }

    public static String run_server_job(String id, JsonObject args, String token, RestApiClient client) {
        try {
            String serverId = args.get("server_id").getAsString();
            String path = "/servers/" + URLEncoder.encode(serverId, StandardCharsets.UTF_8) + "/run";
            JsonElement response = client.post(path, new JsonObject(), token);
            return wrapJsonContent(id, response);
        } catch (Exception e) {
            return McpServiceImpl.createError(id, -32000, "Run Server Job Error: " + e.getMessage());
        }
    }

    public static String reload_server_jobs(String id, JsonObject args, String token, RestApiClient client) {
        try {
            String path = "/servers/reload";
            JsonElement response = client.post(path, new JsonObject(), token);
            return wrapJsonContent(id, response);
        } catch (Exception e) {
            return McpServiceImpl.createError(id, -32000, "Reload Server Jobs Error: " + e.getMessage());
        }
    }

    public static String get_scheduler_details(String id, JsonObject args, String token, RestApiClient client) {
        try {
            String schedulerId = args.get("scheduler_id").getAsString();
            String path = "/servers/schedulers/" + URLEncoder.encode(schedulerId, StandardCharsets.UTF_8);
            JsonElement response = client.get(path, token);
            return wrapJsonContent(id, response);
        } catch (Exception e) {
            return McpServiceImpl.createError(id, -32000, "Get Scheduler Details Error: " + e.getMessage());
        }
    }

    public static String create_scheduler_job(String id, JsonObject args, String token, RestApiClient client) {
        try {
            String schedulerId = args.get("scheduler_id").getAsString();
            String path = "/servers/schedulers/" + URLEncoder.encode(schedulerId, StandardCharsets.UTF_8);
            JsonElement response = client.post(path, new JsonObject(), token);
            return wrapJsonContent(id, response);
        } catch (Exception e) {
            return McpServiceImpl.createError(id, -32000, "Create Scheduler Job Error: " + e.getMessage());
        }
    }

    public static String delete_scheduler_job(String id, JsonObject args, String token, RestApiClient client) {
        try {
            String schedulerId = args.get("scheduler_id").getAsString();
            String path = "/servers/schedulers/" + URLEncoder.encode(schedulerId, StandardCharsets.UTF_8);
            JsonElement response = client.delete(path, token);
            return wrapJsonContent(id, response);
        } catch (Exception e) {
            return McpServiceImpl.createError(id, -32000, "Delete Scheduler Job Error: " + e.getMessage());
        }
    }

    public static String list_windows(String id, JsonObject args, String token, RestApiClient client) {
        try {
            String filter = args.has("filter") ? args.get("filter").getAsString() : "";
            String path = "/windows" + (!filter.isEmpty() ? "?$filter=" + URLEncoder.encode(filter, StandardCharsets.UTF_8) : "");
            JsonElement response = client.get(path, token);
            return wrapJsonContent(id, response);
        } catch (Exception e) {
            return McpServiceImpl.createError(id, -32000, "List Windows Error: " + e.getMessage());
        }
    }

    public static String get_window_tabs(String id, JsonObject args, String token, RestApiClient client) {
        try {
            String windowSlug = args.get("window_slug").getAsString();
            String path = "/windows/" + URLEncoder.encode(windowSlug, StandardCharsets.UTF_8) + "/tabs";
            JsonElement response = client.get(path, token);
            return wrapJsonContent(id, response);
        } catch (Exception e) {
            return McpServiceImpl.createError(id, -32000, "Get Window Tabs Error: " + e.getMessage());
        }
    }

    public static String get_window_tab_fields(String id, JsonObject args, String token, RestApiClient client) {
        try {
            String windowSlug = args.get("window_slug").getAsString();
            String tabSlug = args.get("tab_slug").getAsString();
            String path = "/windows/" + URLEncoder.encode(windowSlug, StandardCharsets.UTF_8) + "/tabs/" + URLEncoder.encode(tabSlug, StandardCharsets.UTF_8) + "/fields";
            JsonElement response = client.get(path, token);
            return wrapJsonContent(id, response);
        } catch (Exception e) {
            return McpServiceImpl.createError(id, -32000, "Get Window Tab Fields Error: " + e.getMessage());
        }
    }

    public static String get_window_records(String id, JsonObject args, String token, RestApiClient client) {
        try {
            String windowSlug = args.get("window_slug").getAsString();
            String filter = args.has("filter") ? args.get("filter").getAsString() : "";
            String sortColumn = args.has("sort_column") ? args.get("sort_column").getAsString() : "";
            int pageNo = args.has("page_no") ? args.get("page_no").getAsInt() : 0;
            String path = "/windows/" + URLEncoder.encode(windowSlug, StandardCharsets.UTF_8) + "?$page_no=" + pageNo;
            if (!filter.isEmpty()) {
                path += "&$filter=" + URLEncoder.encode(filter, StandardCharsets.UTF_8);
            }
            if (!sortColumn.isEmpty()) {
                path += "&$sort_column=" + URLEncoder.encode(sortColumn, StandardCharsets.UTF_8);
            }
            JsonElement response = client.get(path, token);
            return wrapJsonContent(id, response);
        } catch (Exception e) {
            return McpServiceImpl.createError(id, -32000, "Get Window Records Error: " + e.getMessage());
        }
    }

    public static String create_window_record(String id, JsonObject args, String token, RestApiClient client) {
        try {
            String windowSlug = args.get("window_slug").getAsString();
            JsonObject data = args.get("data").getAsJsonObject();
            String path = "/windows/" + URLEncoder.encode(windowSlug, StandardCharsets.UTF_8);
            JsonElement response = client.post(path, data, token);
            return wrapJsonContent(id, response);
        } catch (Exception e) {
            return McpServiceImpl.createError(id, -32000, "Create Window Record Error: " + e.getMessage());
        }
    }

    public static String get_window_record(String id, JsonObject args, String token, RestApiClient client) {
        try {
            String windowSlug = args.get("window_slug").getAsString();
            String recordId = args.get("record_id").getAsString();
            String expand = args.has("expand") ? args.get("expand").getAsString() : "";
            String path = "/windows/" + URLEncoder.encode(windowSlug, StandardCharsets.UTF_8) + "/" + recordId;
            if (!expand.isEmpty()) {
                path += "?$expand=" + URLEncoder.encode(expand, StandardCharsets.UTF_8);
            }
            JsonElement response = client.get(path, token);
            return wrapJsonContent(id, response);
        } catch (Exception e) {
            return McpServiceImpl.createError(id, -32000, "Get Window Record Error: " + e.getMessage());
        }
    }

    public static String print_window_record(String id, JsonObject args, String token, RestApiClient client) {
        try {
            String windowSlug = args.get("window_slug").getAsString();
            String recordId = args.get("record_id").getAsString();
            String path = "/windows/" + URLEncoder.encode(windowSlug, StandardCharsets.UTF_8) + "/" + recordId + "/print";
            JsonElement response = client.get(path, token);
            return wrapJsonContent(id, response);
        } catch (Exception e) {
            return McpServiceImpl.createError(id, -32000, "Print Window Record Error: " + e.getMessage());
        }
    }

    public static String get_window_tab_record(String id, JsonObject args, String token, RestApiClient client) {
        try {
            String windowSlug = args.get("window_slug").getAsString();
            String tabSlug = args.get("tab_slug").getAsString();
            String recordId = args.get("record_id").getAsString();
            String path = "/windows/" + URLEncoder.encode(windowSlug, StandardCharsets.UTF_8) + "/tabs/" + URLEncoder.encode(tabSlug, StandardCharsets.UTF_8) + "/" + recordId;
            JsonElement response = client.get(path, token);
            return wrapJsonContent(id, response);
        } catch (Exception e) {
            return McpServiceImpl.createError(id, -32000, "Get Window Tab Record Error: " + e.getMessage());
        }
    }

    public static String update_window_tab_record(String id, JsonObject args, String token, RestApiClient client) {
        try {
            String windowSlug = args.get("window_slug").getAsString();
            String tabSlug = args.get("tab_slug").getAsString();
            String recordId = args.get("record_id").getAsString();
            JsonObject data = args.get("data").getAsJsonObject();
            String path = "/windows/" + URLEncoder.encode(windowSlug, StandardCharsets.UTF_8) + "/tabs/" + URLEncoder.encode(tabSlug, StandardCharsets.UTF_8) + "/" + recordId;
            JsonElement response = client.put(path, data, token);
            return wrapJsonContent(id, response);
        } catch (Exception e) {
            return McpServiceImpl.createError(id, -32000, "Update Window Tab Record Error: " + e.getMessage());
        }
    }

    public static String delete_window_tab_record(String id, JsonObject args, String token, RestApiClient client) {
        try {
            String windowSlug = args.get("window_slug").getAsString();
            String tabSlug = args.get("tab_slug").getAsString();
            String recordId = args.get("record_id").getAsString();
            String path = "/windows/" + URLEncoder.encode(windowSlug, StandardCharsets.UTF_8) + "/tabs/" + URLEncoder.encode(tabSlug, StandardCharsets.UTF_8) + "/" + recordId;
            JsonElement response = client.delete(path, token);
            return wrapJsonContent(id, response);
        } catch (Exception e) {
            return McpServiceImpl.createError(id, -32000, "Delete Window Tab Record Error: " + e.getMessage());
        }
    }

    public static String get_child_tab_records(String id, JsonObject args, String token, RestApiClient client) {
        try {
            String windowSlug = args.get("window_slug").getAsString();
            String tabSlug = args.get("tab_slug").getAsString();
            String recordId = args.get("record_id").getAsString();
            String childTabSlug = args.get("child_tab_slug").getAsString();
            String path = "/windows/" + URLEncoder.encode(windowSlug, StandardCharsets.UTF_8) + "/tabs/" + URLEncoder.encode(tabSlug, StandardCharsets.UTF_8) + "/" + recordId + "/" + URLEncoder.encode(childTabSlug, StandardCharsets.UTF_8);
            JsonElement response = client.get(path, token);
            return wrapJsonContent(id, response);
        } catch (Exception e) {
            return McpServiceImpl.createError(id, -32000, "Get Child Tab Records Error: " + e.getMessage());
        }
    }

    public static String create_child_tab_record(String id, JsonObject args, String token, RestApiClient client) {
        try {
            String windowSlug = args.get("window_slug").getAsString();
            String tabSlug = args.get("tab_slug").getAsString();
            String recordId = args.get("record_id").getAsString();
            String childTabSlug = args.get("child_tab_slug").getAsString();
            JsonObject data = args.get("data").getAsJsonObject();
            String path = "/windows/" + URLEncoder.encode(windowSlug, StandardCharsets.UTF_8) + "/tabs/" + URLEncoder.encode(tabSlug, StandardCharsets.UTF_8) + "/" + recordId + "/" + URLEncoder.encode(childTabSlug, StandardCharsets.UTF_8);
            JsonElement response = client.post(path, data, token);
            return wrapJsonContent(id, response);
        } catch (Exception e) {
            return McpServiceImpl.createError(id, -32000, "Create Child Tab Record Error: " + e.getMessage());
        }
    }

    private static String wrapBinaryContent(String id, byte[] data, String mimeType) {
        JsonObject item = new JsonObject();
        item.addProperty("type", "binary");
        item.addProperty("mimeType", mimeType);
        item.addProperty("data", java.util.Base64.getEncoder().encodeToString(data));
        
        JsonArray content = new JsonArray(); content.add(item);
        JsonObject result = new JsonObject(); result.add("content", content);
        return McpServiceImpl.createSuccess(id, result);
    }

    private static boolean isInteger(JsonElement e) {
        try { Integer.parseInt(e.getAsString()); return true; } catch (Exception ex) { return false; }
    }

    private static String wrapJsonContent(String id, JsonElement json) {
        JsonObject item = new JsonObject();
        item.addProperty("type", "text");
        item.addProperty("text", gson.toJson(json)); // Valid JSON string inside text
        
        JsonArray content = new JsonArray(); content.add(item);
        JsonObject result = new JsonObject(); result.add("content", content);
        return McpServiceImpl.createSuccess(id, result);
    }
}
