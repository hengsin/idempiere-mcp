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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.idempiere.mcp.server.client.RestApiClient;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class McpModelExecutor {

    public static String search(String id, JsonObject args, String token, RestApiClient client) {
        return McpExecutorUtils.execute(id, "Search Records", () -> {
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
            return McpExecutorUtils.wrapJsonContent(id, response);
        });
    }

    public static String get(String id, JsonObject args, String token, RestApiClient client) {
        return McpExecutorUtils.execute(id, "Get Record", () -> {
            String model = args.get("model").getAsString();
            JsonElement idEl = args.get("id");
            String path = "/models/" + model + "/"
                    + (McpExecutorUtils.isInteger(idEl) ? idEl.getAsInt() : idEl.getAsString());

            JsonElement response = client.get(path, token);
            return McpExecutorUtils.wrapJsonContent(id, response);
        });
    }

    public static String create(String id, JsonObject args, String token, RestApiClient client) {
        return McpExecutorUtils.execute(id, "Create Record", () -> {
            String model = args.get("model").getAsString();
            JsonObject data = args.get("data").getAsJsonObject();
            JsonElement response = client.post("/models/" + model, data, token);
            return McpExecutorUtils.wrapJsonContent(id, response);
        });
    }

    public static String update(String id, JsonObject args, String token, RestApiClient client) {
        return McpExecutorUtils.execute(id, "Update Record", () -> {
            String model = args.get("model").getAsString();
            JsonElement idEl = args.get("id");
            JsonObject data = args.get("data").getAsJsonObject();

            String path = "/models/" + model + "/"
                    + (McpExecutorUtils.isInteger(idEl) ? idEl.getAsInt() : idEl.getAsString());
            JsonElement response = client.put(path, data, token);
            return McpExecutorUtils.wrapJsonContent(id, response);
        });
    }

    public static String delete_record(String id, JsonObject args, String token, RestApiClient client) {
        return McpExecutorUtils.execute(id, "Delete Record", () -> {
            String tableName = args.get("tableName").getAsString();
            String recordId = args.get("record_id").getAsString();
            String path = "/models/" + URLEncoder.encode(tableName, StandardCharsets.UTF_8) + "/" + recordId;
            JsonElement response = client.delete(path, token);
            return McpExecutorUtils.wrapJsonContent(id, response);
        });
    }

    public static String get_record_property(String id, JsonObject args, String token, RestApiClient client) {
        return McpExecutorUtils.execute(id, "Get Record Property", () -> {
            String tableName = args.get("tableName").getAsString();
            String recordId = args.get("record_id").getAsString();
            String columnName = args.get("columnName").getAsString();
            String path = "/models/" + URLEncoder.encode(tableName, StandardCharsets.UTF_8) + "/" + recordId + "/"
                    + URLEncoder.encode(columnName, StandardCharsets.UTF_8);
            JsonElement response = client.get(path, token);
            return McpExecutorUtils.wrapJsonContent(id, response);
        });
    }

    public static String get_record_attachments(String id, JsonObject args, String token, RestApiClient client) {
        return McpExecutorUtils.execute(id, "Get Record Attachments", () -> {
            String tableName = args.get("tableName").getAsString();
            String recordId = args.get("record_id").getAsString();
            String path = "/models/" + URLEncoder.encode(tableName, StandardCharsets.UTF_8) + "/" + recordId
                    + "/attachments";
            JsonElement response = client.get(path, token);
            return McpExecutorUtils.wrapJsonContent(id, response);
        });
    }

    public static String add_record_attachment(String id, JsonObject args, String token, RestApiClient client) {
        return McpExecutorUtils.execute(id, "Add Record Attachment", () -> {
            String tableName = args.get("tableName").getAsString();
            String recordId = args.get("record_id").getAsString();
            JsonObject data = args.get("data").getAsJsonObject();
            String path = "/models/" + URLEncoder.encode(tableName, StandardCharsets.UTF_8) + "/" + recordId
                    + "/attachments";
            JsonElement response = client.post(path, data, token);
            return McpExecutorUtils.wrapJsonContent(id, response);
        });
    }

    public static String delete_record_attachments(String id, JsonObject args, String token, RestApiClient client) {
        return McpExecutorUtils.execute(id, "Delete Record Attachments", () -> {
            String tableName = args.get("tableName").getAsString();
            String recordId = args.get("record_id").getAsString();
            String path = "/models/" + URLEncoder.encode(tableName, StandardCharsets.UTF_8) + "/" + recordId
                    + "/attachments";
            JsonElement response = client.delete(path, token);
            return McpExecutorUtils.wrapJsonContent(id, response);
        });
    }

    public static String get_record_attachments_zip(String id, JsonObject args, String token, RestApiClient client) {
        return McpExecutorUtils.execute(id, "Get Record Attachments Zip", () -> {
            String tableName = args.get("tableName").getAsString();
            String recordId = args.get("record_id").getAsString();
            String path = "/models/" + URLEncoder.encode(tableName, StandardCharsets.UTF_8) + "/" + recordId
                    + "/attachments/zip";
            byte[] response = client.getBinary(path, token, "application/zip");
            return McpExecutorUtils.wrapBinaryContent(id, response, "application/zip");
        });
    }

    public static String get_record_attachment_by_name(String id, JsonObject args, String token, RestApiClient client) {
        return McpExecutorUtils.execute(id, "Get Record Attachment By Name", () -> {
            String tableName = args.get("tableName").getAsString();
            String recordId = args.get("record_id").getAsString();
            String fileName = args.get("fileName").getAsString();
            String path = "/models/" + URLEncoder.encode(tableName, StandardCharsets.UTF_8) + "/" + recordId
                    + "/attachments/" + URLEncoder.encode(fileName, StandardCharsets.UTF_8);
            byte[] response = client.getBinary(path, token, "application/octet-stream");
            return McpExecutorUtils.wrapBinaryContent(id, response, "application/octet-stream");
        });
    }

    public static String print_record(String id, JsonObject args, String token, RestApiClient client) {
        try {
            String tableName = args.get("tableName").getAsString();
            String recordId = args.get("record_id").getAsString();
            String path = "/models/" + URLEncoder.encode(tableName, StandardCharsets.UTF_8) + "/" + recordId + "/print";
            JsonElement response = client.get(path, token);
            return McpExecutorUtils.wrapJsonContent(id, response);
        } catch (Exception e) {
            return McpExecutorUtils.wrapToolError(id, "Print Record Error: " + e.getMessage());
        }
    }

    public static String listModelsTool(String id, JsonObject args, String token, RestApiClient client) {
        return McpExecutorUtils.execute(id, "List Models", () -> {
            StringBuilder path = new StringBuilder("/models");
            if (args != null && args.has("filter")) {
                String filter = args.get("filter").getAsString();
                if (!filter.isEmpty()) {
                    path.append("?$filter=").append(URLEncoder.encode(filter, StandardCharsets.UTF_8));
                }
            }
            JsonElement response = client.get(path.toString(), token);
            return McpExecutorUtils.wrapJsonContent(id, response);
        });
    }

    public static String getModelYamlTool(String id, JsonObject args, String token, RestApiClient client) {
        return McpExecutorUtils.execute(id, "Get Model YAML", () -> {
            String model = args.get("tableName").getAsString();
            String path = "/models/" + URLEncoder.encode(model, StandardCharsets.UTF_8) + "/yaml";
            String yaml = client.getYaml(path, token);
            JsonObject item = new JsonObject();
            // Use EmbeddedResource for YAML content since TextContent doesn't support mimeType
            item.addProperty("type", "resource");
            JsonObject resource = new JsonObject();
            resource.addProperty("uri", "idempiere://models/" + model + "/yaml");
            resource.addProperty("mimeType", "application/yaml");
            resource.addProperty("text", yaml);
            item.add("resource", resource);
            JsonArray content = new JsonArray();
            content.add(item);
            JsonObject result = new JsonObject();
            result.add("content", content);
            return McpServiceImpl.createSuccess(id, result);
        });
    }
}
