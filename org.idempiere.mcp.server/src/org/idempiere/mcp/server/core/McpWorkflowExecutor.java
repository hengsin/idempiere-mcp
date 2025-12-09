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

public class McpWorkflowExecutor {

    public static String list_workflow_activities(String id, JsonObject args, String token, RestApiClient client) {
        try {
            String userId = args.has("userId") ? args.get("userId").getAsString() : "";
            String path = "/workflow"
                    + (!userId.isEmpty() ? "/" + URLEncoder.encode(userId, StandardCharsets.UTF_8) : "");
            JsonElement response = client.get(path, token);
            return McpExecutorUtils.wrapJsonContent(id, response);
        } catch (Exception e) {
            return McpServiceImpl.createError(id, -32000, "List Workflow Activities Error: " + e.getMessage());
        }
    }

    public static String approve_workflow_activity(String id, JsonObject args, String token, RestApiClient client) {
        try {
            String activityId = args.get("id").getAsString();
            String path = "/workflow/approve/" + URLEncoder.encode(activityId, StandardCharsets.UTF_8);
            JsonObject data = new JsonObject();
            if (args.has("message")) {
                data.addProperty("message", args.get("message").getAsString());
            }
            JsonElement response = client.put(path, data, token);
            return McpExecutorUtils.wrapJsonContent(id, response);
        } catch (Exception e) {
            return McpServiceImpl.createError(id, -32000, "Approve Workflow Activity Error: " + e.getMessage());
        }
    }

    public static String reject_workflow_activity(String id, JsonObject args, String token, RestApiClient client) {
        try {
            String activityId = args.get("id").getAsString();
            String path = "/workflow/reject/" + URLEncoder.encode(activityId, StandardCharsets.UTF_8);
            JsonObject data = new JsonObject();
            if (args.has("message")) {
                data.addProperty("message", args.get("message").getAsString());
            }
            JsonElement response = client.put(path, data, token);
            return McpExecutorUtils.wrapJsonContent(id, response);
        } catch (Exception e) {
            return McpServiceImpl.createError(id, -32000, "Reject Workflow Activity Error: " + e.getMessage());
        }
    }

    public static String forward_workflow_activity(String id, JsonObject args, String token, RestApiClient client) {
        try {
            String activityId = args.get("id").getAsString();
            String path = "/workflow/forward/" + URLEncoder.encode(activityId, StandardCharsets.UTF_8);
            JsonObject data = new JsonObject();
            data.addProperty("userTo", args.get("userTo").getAsString());
            if (args.has("message")) {
                data.addProperty("message", args.get("message").getAsString());
            }
            JsonElement response = client.put(path, data, token);
            return McpExecutorUtils.wrapJsonContent(id, response);
        } catch (Exception e) {
            return McpServiceImpl.createError(id, -32000, "Forward Workflow Activity Error: " + e.getMessage());
        }
    }

    public static String acknowledge_workflow_activity(String id, JsonObject args, String token, RestApiClient client) {
        try {
            String activityId = args.get("id").getAsString();
            String path = "/workflow/acknowledge/" + URLEncoder.encode(activityId, StandardCharsets.UTF_8);
            JsonObject data = new JsonObject();
            if (args.has("message")) {
                data.addProperty("message", args.get("message").getAsString());
            }
            JsonElement response = client.put(path, data, token);
            return McpExecutorUtils.wrapJsonContent(id, response);
        } catch (Exception e) {
            return McpServiceImpl.createError(id, -32000, "Acknowledge Workflow Activity Error: " + e.getMessage());
        }
    }

    public static String set_workflow_activity_user_choice(String id, JsonObject args, String token,
            RestApiClient client) {
        try {
            String activityId = args.get("id").getAsString();
            String path = "/workflow/setuserchoice/" + URLEncoder.encode(activityId, StandardCharsets.UTF_8);
            JsonObject data = new JsonObject();
            data.addProperty("value", args.get("value").getAsString());
            JsonElement response = client.put(path, data, token);
            return McpExecutorUtils.wrapJsonContent(id, response);
        } catch (Exception e) {
            return McpServiceImpl.createError(id, -32000,
                    "Set Workflow Activity User Choice Error: " + e.getMessage());
        }
    }
}
