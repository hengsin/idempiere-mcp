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

public class McpViewExecutor {

    public static String list_views(String id, JsonObject args, String token, RestApiClient client) {
        return McpExecutorUtils.execute(id, "List Views", () -> {
            String filter = args.has("filter") ? args.get("filter").getAsString() : "";
            String path = "/views"
                    + (!filter.isEmpty() ? "?$filter=" + URLEncoder.encode(filter, StandardCharsets.UTF_8) : "");
            JsonElement response = client.get(path, token);
            return McpExecutorUtils.wrapJsonContent(id, response);
        });
    }

    public static String get_view_yaml(String id, JsonObject args, String token, RestApiClient client) {
        return McpExecutorUtils.execute(id, "Get View YAML", () -> {
            String viewName = McpExecutorUtils.slugify(args.get("viewName").getAsString());
            String path = "/views/" + URLEncoder.encode(viewName, StandardCharsets.UTF_8) + "/yaml";
            String yaml = client.getYaml(path, token);
            JsonObject item = new JsonObject();
            item.addProperty("type", "text");
            item.addProperty("mimeType", "application/yaml");
            item.addProperty("text", yaml);
            JsonArray content = new JsonArray();
            content.add(item);
            JsonObject result = new JsonObject();
            result.add("content", content);
            return McpServiceImpl.createSuccess(id, result);
        });
    }

    public static String search_view_records(String id, JsonObject args, String token, RestApiClient client) {
        return McpExecutorUtils.execute(id, "Search View Records", () -> {
            String viewName = McpExecutorUtils.slugify(args.get("viewName").getAsString());
            String filter = args.has("filter") ? args.get("filter").getAsString() : "";
            String sortColumn = args.has("sort_column") ? args.get("sort_column").getAsString() : "";
            int limit = args.has("limit") ? args.get("limit").getAsInt() : 10;
            int offset = args.has("offset") ? args.get("offset").getAsInt() : 0;

            StringBuilder sb = new StringBuilder("/views/").append(URLEncoder.encode(viewName, StandardCharsets.UTF_8));
            sb.append("?$top=").append(limit);
            sb.append("&$skip=").append(offset);

            if (!filter.isEmpty()) {
                sb.append("&$filter=").append(URLEncoder.encode(filter, StandardCharsets.UTF_8));
            }
            if (!sortColumn.isEmpty()) {
                sb.append("&$orderby=").append(URLEncoder.encode(sortColumn, StandardCharsets.UTF_8));
            }

            JsonElement response = client.get(sb.toString(), token);
            return McpExecutorUtils.wrapJsonContent(id, response);
        });
    }

    public static String create_view_record(String id, JsonObject args, String token, RestApiClient client) {
        return McpExecutorUtils.execute(id, "Create View Record", () -> {
            String viewName = McpExecutorUtils.slugify(args.get("viewName").getAsString());
            JsonObject data = args.get("data").getAsJsonObject();
            String path = "/views/" + URLEncoder.encode(viewName, StandardCharsets.UTF_8);
            JsonElement response = client.post(path, data, token);
            return McpExecutorUtils.wrapJsonContent(id, response);
        });
    }

    public static String get_view_record(String id, JsonObject args, String token, RestApiClient client) {
        return McpExecutorUtils.execute(id, "Get View Record", () -> {
            String viewName = McpExecutorUtils.slugify(args.get("viewName").getAsString());
            String recordId = args.get("id").getAsString();
            String path = "/views/" + URLEncoder.encode(viewName, StandardCharsets.UTF_8) + "/" + recordId;
            JsonElement response = client.get(path, token);
            return McpExecutorUtils.wrapJsonContent(id, response);
        });
    }

    public static String update_view_record(String id, JsonObject args, String token, RestApiClient client) {
        return McpExecutorUtils.execute(id, "Update View Record", () -> {
            String viewName = McpExecutorUtils.slugify(args.get("viewName").getAsString());
            String recordId = args.get("id").getAsString();
            JsonObject data = args.get("data").getAsJsonObject();
            String path = "/views/" + URLEncoder.encode(viewName, StandardCharsets.UTF_8) + "/" + recordId;
            JsonElement response = client.put(path, data, token);
            return McpExecutorUtils.wrapJsonContent(id, response);
        });
    }

    public static String delete_view_record(String id, JsonObject args, String token, RestApiClient client) {
        return McpExecutorUtils.execute(id, "Delete View Record", () -> {
            String viewName = McpExecutorUtils.slugify(args.get("viewName").getAsString());
            String recordId = args.get("id").getAsString();
            String path = "/views/" + URLEncoder.encode(viewName, StandardCharsets.UTF_8) + "/" + recordId;
            JsonElement response = client.delete(path, token);
            return McpExecutorUtils.wrapJsonContent(id, response);
        });
    }

    public static String get_view_record_property(String id, JsonObject args, String token, RestApiClient client) {
        return McpExecutorUtils.execute(id, "Get View Record Property", () -> {
            String viewName = McpExecutorUtils.slugify(args.get("viewName").getAsString());
            String recordId = args.get("id").getAsString();
            String columnName = args.get("columnName").getAsString();
            String path = "/views/" + URLEncoder.encode(viewName, StandardCharsets.UTF_8) + "/" + recordId + "/"
                    + URLEncoder.encode(columnName, StandardCharsets.UTF_8);
            JsonElement response = client.get(path, token);
            return McpExecutorUtils.wrapJsonContent(id, response);
        });
    }

    public static String get_view_record_attachments(String id, JsonObject args, String token, RestApiClient client) {
        return McpExecutorUtils.execute(id, "Get View Record Attachments", () -> {
            String viewName = McpExecutorUtils.slugify(args.get("viewName").getAsString());
            String recordId = args.get("id").getAsString();
            String path = "/views/" + URLEncoder.encode(viewName, StandardCharsets.UTF_8) + "/" + recordId
                    + "/attachments";
            JsonElement response = client.get(path, token);
            return McpExecutorUtils.wrapJsonContent(id, response);
        });
    }

    public static String add_view_record_attachment(String id, JsonObject args, String token, RestApiClient client) {
        return McpExecutorUtils.execute(id, "Add View Record Attachment", () -> {
            String viewName = McpExecutorUtils.slugify(args.get("viewName").getAsString());
            String recordId = args.get("id").getAsString();
            JsonObject data = args.get("data").getAsJsonObject();
            String path = "/views/" + URLEncoder.encode(viewName, StandardCharsets.UTF_8) + "/" + recordId
                    + "/attachments";
            JsonElement response = client.post(path, data, token);
            return McpExecutorUtils.wrapJsonContent(id, response);
        });
    }

    public static String delete_view_record_attachments(String id, JsonObject args, String token,
            RestApiClient client) {
        return McpExecutorUtils.execute(id, "Delete View Record Attachments", () -> {
            String viewName = McpExecutorUtils.slugify(args.get("viewName").getAsString());
            String recordId = args.get("id").getAsString();
            String path = "/views/" + URLEncoder.encode(viewName, StandardCharsets.UTF_8) + "/" + recordId
                    + "/attachments";
            JsonElement response = client.delete(path, token);
            return McpExecutorUtils.wrapJsonContent(id, response);
        });
    }

    public static String get_view_record_attachments_zip(String id, JsonObject args, String token,
            RestApiClient client) {
        return McpExecutorUtils.execute(id, "Get View Record Attachments ZIP", () -> {
            String viewName = McpExecutorUtils.slugify(args.get("viewName").getAsString());
            String recordId = args.get("id").getAsString();
            String path = "/views/" + URLEncoder.encode(viewName, StandardCharsets.UTF_8) + "/" + recordId
                    + "/attachments/zip";
            byte[] response = client.getBinary(path, token, "application/zip");
            return McpExecutorUtils.wrapBinaryContent(id, response, "application/zip");
        });
    }

    public static String get_view_record_attachment_by_name(String id, JsonObject args, String token,
            RestApiClient client) {
        return McpExecutorUtils.execute(id, "Get View Record Attachment By Name", () -> {
            String viewName = McpExecutorUtils.slugify(args.get("viewName").getAsString());
            String recordId = args.get("id").getAsString();
            String fileName = args.get("fileName").getAsString();
            String path = "/views/" + URLEncoder.encode(viewName, StandardCharsets.UTF_8) + "/" + recordId
                    + "/attachments/" + URLEncoder.encode(fileName, StandardCharsets.UTF_8);
            byte[] response = client.getBinary(path, token, "application/octet-stream");
            return McpExecutorUtils.wrapBinaryContent(id, response, "application/octet-stream");
        });
    }

    public static String print_view_record(String id, JsonObject args, String token, RestApiClient client) {
        return McpExecutorUtils.execute(id, "Print View Record", () -> {
            String viewName = McpExecutorUtils.slugify(args.get("viewName").getAsString());
            String recordId = args.get("id").getAsString();
            String path = "/views/" + URLEncoder.encode(viewName, StandardCharsets.UTF_8) + "/" + recordId + "/print";
            JsonElement response = client.get(path, token);
            return McpExecutorUtils.wrapJsonContent(id, response);
        });
    }
}
