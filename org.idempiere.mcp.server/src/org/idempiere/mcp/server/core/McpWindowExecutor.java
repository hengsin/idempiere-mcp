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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class McpWindowExecutor {

    public static String list_windows(String id, JsonObject args, String token, RestApiClient client) {
        try {
            String filter = args.has("filter") ? args.get("filter").getAsString() : "";
            String path = "/windows"
                    + (!filter.isEmpty() ? "?$filter=" + URLEncoder.encode(filter, StandardCharsets.UTF_8) : "");
            JsonElement response = client.get(path, token);
            return McpExecutorUtils.wrapJsonContent(id, response);
        } catch (Exception e) {
            return McpServiceImpl.createError(id, -32000, "List Windows Error: " + e.getMessage());
        }
    }

    public static String get_window_tabs(String id, JsonObject args, String token, RestApiClient client) {
        return McpExecutorUtils.execute(id, "Get Window Tabs", () -> {
            String windowSlug = McpExecutorUtils.slugify(args.get("window_name").getAsString());
            String path = "/windows/" + URLEncoder.encode(windowSlug, StandardCharsets.UTF_8) + "/tabs";
            JsonElement response = client.get(path, token);
            return McpExecutorUtils.wrapJsonContent(id, response);
        });
    }

    public static String get_window_tab_fields(String id, JsonObject args, String token, RestApiClient client) {
        return McpExecutorUtils.execute(id, "Get Window Tab Fields", () -> {
            String windowSlug = McpExecutorUtils.slugify(args.get("window_name").getAsString());
            String tabSlug = McpExecutorUtils.slugify(args.get("tab_name").getAsString());
            String path = "/windows/" + URLEncoder.encode(windowSlug, StandardCharsets.UTF_8) + "/tabs/"
                    + URLEncoder.encode(tabSlug, StandardCharsets.UTF_8) + "/fields";
            JsonElement response = client.get(path, token);
            return McpExecutorUtils.wrapJsonContent(id, response);
        });
    }

    public static String get_window_records(String id, JsonObject args, String token, RestApiClient client) {
        return McpExecutorUtils.execute(id, "Get Window Records", () -> {
            String windowSlug = McpExecutorUtils.slugify(args.get("window_name").getAsString());
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
            return McpExecutorUtils.wrapJsonContent(id, response);
        });
    }

    public static String create_window_record(String id, JsonObject args, String token, RestApiClient client) {
        return McpExecutorUtils.execute(id, "Create Window Record", () -> {
            String windowSlug = McpExecutorUtils.slugify(args.get("window_name").getAsString());
            JsonObject data = args.get("data").getAsJsonObject();
            String path = "/windows/" + URLEncoder.encode(windowSlug, StandardCharsets.UTF_8);
            JsonElement response = client.post(path, data, token);
            return McpExecutorUtils.wrapJsonContent(id, response);
        });
    }

    public static String get_window_record(String id, JsonObject args, String token, RestApiClient client) {
        return McpExecutorUtils.execute(id, "Get Window Record", () -> {
            String windowSlug = McpExecutorUtils.slugify(args.get("window_name").getAsString());
            String recordId = args.get("record_id").getAsString();
            String expand = args.has("expand") ? args.get("expand").getAsString() : "";
            String path = "/windows/" + URLEncoder.encode(windowSlug, StandardCharsets.UTF_8) + "/" + recordId;
            if (!expand.isEmpty()) {
                path += "?$expand=" + URLEncoder.encode(expand, StandardCharsets.UTF_8);
            }
            JsonElement response = client.get(path, token);
            return McpExecutorUtils.wrapJsonContent(id, response);
        });
    }

    public static String print_window_record(String id, JsonObject args, String token, RestApiClient client) {
        return McpExecutorUtils.execute(id, "Print Window Record", () -> {
            String windowSlug = McpExecutorUtils.slugify(args.get("window_name").getAsString());
            String recordId = args.get("record_id").getAsString();
            String path = "/windows/" + URLEncoder.encode(windowSlug, StandardCharsets.UTF_8) + "/" + recordId
                    + "/print";
            JsonElement response = client.get(path, token);
            return McpExecutorUtils.wrapJsonContent(id, response);
        });
    }

    public static String get_window_tab_record(String id, JsonObject args, String token, RestApiClient client) {
        return McpExecutorUtils.execute(id, "Get Window Tab Record", () -> {
            String windowSlug = McpExecutorUtils.slugify(args.get("window_name").getAsString());
            String tabSlug = McpExecutorUtils.slugify(args.get("tab_name").getAsString());
            String recordId = args.get("record_id").getAsString();
            String path = "/windows/" + URLEncoder.encode(windowSlug, StandardCharsets.UTF_8) + "/tabs/"
                    + URLEncoder.encode(tabSlug, StandardCharsets.UTF_8) + "/" + recordId;
            JsonElement response = client.get(path, token);
            return McpExecutorUtils.wrapJsonContent(id, response);
        });
    }

    public static String update_window_tab_record(String id, JsonObject args, String token, RestApiClient client) {
        return McpExecutorUtils.execute(id, "Update Window Tab Record", () -> {
            String windowSlug = McpExecutorUtils.slugify(args.get("window_name").getAsString());
            String tabSlug = McpExecutorUtils.slugify(args.get("tab_name").getAsString());
            String recordId = args.get("record_id").getAsString();
            JsonObject data = args.get("data").getAsJsonObject();
            String path = "/windows/" + URLEncoder.encode(windowSlug, StandardCharsets.UTF_8) + "/tabs/"
                    + URLEncoder.encode(tabSlug, StandardCharsets.UTF_8) + "/" + recordId;
            JsonElement response = client.put(path, data, token);
            return McpExecutorUtils.wrapJsonContent(id, response);
        });
    }

    public static String delete_window_tab_record(String id, JsonObject args, String token, RestApiClient client) {
        return McpExecutorUtils.execute(id, "Delete Window Tab Record", () -> {
            String windowSlug = McpExecutorUtils.slugify(args.get("window_name").getAsString());
            String tabSlug = McpExecutorUtils.slugify(args.get("tab_name").getAsString());
            String recordId = args.get("record_id").getAsString();
            String path = "/windows/" + URLEncoder.encode(windowSlug, StandardCharsets.UTF_8) + "/tabs/"
                    + URLEncoder.encode(tabSlug, StandardCharsets.UTF_8) + "/" + recordId;
            JsonElement response = client.delete(path, token);
            return McpExecutorUtils.wrapJsonContent(id, response);
        });
    }

    public static String get_child_tab_records(String id, JsonObject args, String token, RestApiClient client) {
        return McpExecutorUtils.execute(id, "Get Child Tab Records", () -> {
            String windowSlug = McpExecutorUtils.slugify(args.get("window_name").getAsString());
            String tabSlug = McpExecutorUtils.slugify(args.get("tab_name").getAsString());
            String recordId = args.get("record_id").getAsString();
            String childTabSlug = McpExecutorUtils.slugify(args.get("child_tab_name").getAsString());
            String path = "/windows/" + URLEncoder.encode(windowSlug, StandardCharsets.UTF_8) + "/tabs/"
                    + URLEncoder.encode(tabSlug, StandardCharsets.UTF_8) + "/" + recordId + "/"
                    + URLEncoder.encode(childTabSlug, StandardCharsets.UTF_8);
            JsonElement response = client.get(path, token);
            return McpExecutorUtils.wrapJsonContent(id, response);
        });
    }

    public static String create_child_tab_record(String id, JsonObject args, String token, RestApiClient client) {
        return McpExecutorUtils.execute(id, "Create Child Tab Record", () -> {
            String windowSlug = McpExecutorUtils.slugify(args.get("window_name").getAsString());
            String tabSlug = McpExecutorUtils.slugify(args.get("tab_name").getAsString());
            String recordId = args.get("record_id").getAsString();
            String childTabSlug = McpExecutorUtils.slugify(args.get("child_tab_name").getAsString());
            JsonObject data = args.get("data").getAsJsonObject();
            String path = "/windows/" + URLEncoder.encode(windowSlug, StandardCharsets.UTF_8) + "/tabs/"
                    + URLEncoder.encode(tabSlug, StandardCharsets.UTF_8) + "/" + recordId + "/"
                    + URLEncoder.encode(childTabSlug, StandardCharsets.UTF_8);
            JsonElement response = client.post(path, data, token);
            return McpExecutorUtils.wrapJsonContent(id, response);
        });
    }
}
