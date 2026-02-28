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

public class McpProcessExecutor {

    public static String getProcessInfoTool(String id, JsonObject args, String token, RestApiClient client) {
        return McpExecutorUtils.execute(id, "Get Process Info", () -> {
            String processSlug = McpExecutorUtils.slugify(args.get("process_value").getAsString());
            String path = "/processes/" + URLEncoder.encode(processSlug, StandardCharsets.UTF_8);
            JsonElement response = client.get(path, token);
            return McpExecutorUtils.wrapJsonContent(id, response);
        });
    }

    public static String runProcess(String id, JsonObject args, String token, RestApiClient client) {
        return McpExecutorUtils.execute(id, "Run Process", () -> {
            String processId = McpExecutorUtils.slugify(args.get("process_value").getAsString());
            JsonObject params = args.has("parameters") ? args.get("parameters").getAsJsonObject() : new JsonObject();

            JsonObject payload = params.size() > 0 ? params : new JsonObject();

            JsonElement response = client.post("/processes/" + processId, payload, token);
            return McpExecutorUtils.wrapJsonContent(id, response);
        });
    }

    public static String list_server_jobs(String id, JsonObject args, String token, RestApiClient client) {
        return McpExecutorUtils.execute(id, "List Server Jobs", () -> {
            String path = "/servers";
            JsonElement response = client.get(path, token);
            return McpExecutorUtils.wrapJsonContent(id, response);
        });
    }

    public static String get_server_job(String id, JsonObject args, String token, RestApiClient client) {
        return McpExecutorUtils.execute(id, "Get Server Job", () -> {
            String serverId = args.get("server_id").getAsString();
            String path = "/servers/" + URLEncoder.encode(serverId, StandardCharsets.UTF_8);
            JsonElement response = client.get(path, token);
            return McpExecutorUtils.wrapJsonContent(id, response);
        });
    }

    public static String get_server_job_logs(String id, JsonObject args, String token, RestApiClient client) {
        return McpExecutorUtils.execute(id, "Get Server Job Logs", () -> {
            String serverId = args.get("server_id").getAsString();
            String path = "/servers/" + URLEncoder.encode(serverId, StandardCharsets.UTF_8) + "/logs";
            JsonElement response = client.get(path, token);
            return McpExecutorUtils.wrapJsonContent(id, response);
        });
    }

    public static String toggle_server_job_state(String id, JsonObject args, String token, RestApiClient client) {
        return McpExecutorUtils.execute(id, "Toggle Server Job State", () -> {
            String serverId = args.get("server_id").getAsString();
            String path = "/servers/" + URLEncoder.encode(serverId, StandardCharsets.UTF_8) + "/state";
            JsonElement response = client.post(path, new JsonObject(), token);
            return McpExecutorUtils.wrapJsonContent(id, response);
        });
    }

    public static String run_server_job(String id, JsonObject args, String token, RestApiClient client) {
        return McpExecutorUtils.execute(id, "Run Server Job", () -> {
            String serverId = args.get("server_id").getAsString();
            String path = "/servers/" + URLEncoder.encode(serverId, StandardCharsets.UTF_8) + "/run";
            JsonElement response = client.post(path, new JsonObject(), token);
            return McpExecutorUtils.wrapJsonContent(id, response);
        });
    }

    public static String reload_server_jobs(String id, JsonObject args, String token, RestApiClient client) {
        return McpExecutorUtils.execute(id, "Reload Server Jobs", () -> {
            String path = "/servers/reload";
            JsonElement response = client.post(path, new JsonObject(), token);
            return McpExecutorUtils.wrapJsonContent(id, response);
        });
    }

    public static String get_scheduler_details(String id, JsonObject args, String token, RestApiClient client) {
        return McpExecutorUtils.execute(id, "Get Scheduler Details", () -> {
            String schedulerId = args.get("scheduler_id").getAsString();
            String path = "/servers/schedulers/" + URLEncoder.encode(schedulerId, StandardCharsets.UTF_8);
            JsonElement response = client.get(path, token);
            return McpExecutorUtils.wrapJsonContent(id, response);
        });
    }

    public static String create_scheduler_job(String id, JsonObject args, String token, RestApiClient client) {
        return McpExecutorUtils.execute(id, "Create Scheduler Job", () -> {
            String schedulerId = args.get("scheduler_id").getAsString();
            String path = "/servers/schedulers/" + URLEncoder.encode(schedulerId, StandardCharsets.UTF_8);
            JsonElement response = client.post(path, new JsonObject(), token);
            return McpExecutorUtils.wrapJsonContent(id, response);
        });
    }

    public static String delete_scheduler_job(String id, JsonObject args, String token, RestApiClient client) {
        try {
            String schedulerId = args.get("scheduler_id").getAsString();
            String path = "/servers/schedulers/" + URLEncoder.encode(schedulerId, StandardCharsets.UTF_8);
            JsonElement response = client.delete(path, token);
            return McpExecutorUtils.wrapJsonContent(id, response);
        } catch (Exception e) {
            return McpExecutorUtils.wrapToolError(id, "Delete Scheduler Job Error: " + e.getMessage());
        }
    }
}
