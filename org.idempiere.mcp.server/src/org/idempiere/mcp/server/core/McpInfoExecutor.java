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

public class McpInfoExecutor {

    public static String list_info_windows(String id, JsonObject args, String token, RestApiClient client) {
        try {
            String filter = args.has("filter") ? args.get("filter").getAsString() : "";
            String path = "/infos"
                    + (!filter.isEmpty() ? "?$filter=" + URLEncoder.encode(filter, StandardCharsets.UTF_8) : "");
            JsonElement response = client.get(path, token);
            return McpExecutorUtils.wrapJsonContent(id, response);
        } catch (Exception e) {
            return McpServiceImpl.createError(id, -32000, "List Info Windows Error: " + e.getMessage());
        }
    }

    public static String get_info_window_data(String id, JsonObject args, String token, RestApiClient client) {
        try {
            String infoSlug = McpExecutorUtils.slugify(args.get("infoSlug").getAsString());
            String parameters = args.has("parameters") ? args.get("parameters").getAsString() : "";
            String whereClause = args.has("where_clause") ? args.get("where_clause").getAsString() : "";
            String orderBy = args.has("order_by") ? args.get("order_by").getAsString() : "";
            int pageNo = args.has("page_no") ? args.get("page_no").getAsInt() : 0;

            StringBuilder sb = new StringBuilder("/infos/").append(URLEncoder.encode(infoSlug, StandardCharsets.UTF_8))
                    .append("?");
            sb.append("$page_no=").append(pageNo);

            if (!parameters.isEmpty()) {
                sb.append("&$parameters=").append(URLEncoder.encode(parameters, StandardCharsets.UTF_8));
            }
            if (!whereClause.isEmpty()) {
                sb.append("&$where_clause=").append(URLEncoder.encode(whereClause, StandardCharsets.UTF_8));
            }
            if (!orderBy.isEmpty()) {
                sb.append("&$order_by=").append(URLEncoder.encode(orderBy, StandardCharsets.UTF_8));
            }

            JsonElement response = client.get(sb.toString(), token);
            return McpExecutorUtils.wrapJsonContent(id, response);
        } catch (Exception e) {
            return McpServiceImpl.createError(id, -32000, "Get Info Window Data Error: " + e.getMessage());
        }
    }

    public static String get_info_window_columns(String id, JsonObject args, String token, RestApiClient client) {
        try {
            String infoSlug = McpExecutorUtils.slugify(args.get("infoSlug").getAsString());
            String path = "/infos/" + URLEncoder.encode(infoSlug, StandardCharsets.UTF_8) + "/columns";
            JsonElement response = client.get(path, token);
            return McpExecutorUtils.wrapJsonContent(id, response);
        } catch (Exception e) {
            return McpServiceImpl.createError(id, -32000, "Get Info Window Columns Error: " + e.getMessage());
        }
    }

    public static String get_info_window_processes(String id, JsonObject args, String token, RestApiClient client) {
        try {
            String infoSlug = McpExecutorUtils.slugify(args.get("infoSlug").getAsString());
            String path = "/infos/" + URLEncoder.encode(infoSlug, StandardCharsets.UTF_8) + "/processes";
            JsonElement response = client.get(path, token);
            return McpExecutorUtils.wrapJsonContent(id, response);
        } catch (Exception e) {
            return McpServiceImpl.createError(id, -32000, "Get Info Window Processes Error: " + e.getMessage());
        }
    }

    public static String get_info_window_related_infos(String id, JsonObject args, String token, RestApiClient client) {
        try {
            String infoSlug = McpExecutorUtils.slugify(args.get("infoSlug").getAsString());
            String path = "/infos/" + URLEncoder.encode(infoSlug, StandardCharsets.UTF_8) + "/relateds";
            JsonElement response = client.get(path, token);
            return McpExecutorUtils.wrapJsonContent(id, response);
        } catch (Exception e) {
            return McpServiceImpl.createError(id, -32000, "Get Info Window Related Infos Error: " + e.getMessage());
        }
    }
}
