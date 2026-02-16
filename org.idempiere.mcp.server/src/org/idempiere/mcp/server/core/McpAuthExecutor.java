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
import org.idempiere.mcp.server.web.McpServlet;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class McpAuthExecutor {

    /**
     * Create auth token (POST /auth/tokens)
     * 
     * @param id
     * @param args
     * @param token
     * @param client
     * @return MCP success/error response
     */
    public static String create(String id, JsonObject args, String token, String sessionId, RestApiClient client) {
        return McpExecutorUtils.execute(id, "Create Auth Token", () -> {
            JsonObject body = new JsonObject();
            body.addProperty("userName", args.get("userName").getAsString());
            body.addProperty("password", args.get("password").getAsString());
            if (args.has("parameters") && !args.get("parameters").isJsonNull()) {
                body.add("parameters", args.get("parameters").getAsJsonObject());
            }

            JsonElement response = client.post("/auth/tokens", body, token);
            String newToken = null;
            if (response != null && response.isJsonObject()) {
                JsonObject obj = response.getAsJsonObject();
                if (obj.has("token")) {
                    newToken = obj.get("token").getAsString();
                    if (sessionId != null) {
                        McpServlet.updateToken(sessionId, newToken);
                    }
                }
            }
            return McpExecutorUtils.wrapJsonContent(id, response);
        });
    }

    /**
     * Update auth token (PUT /auth/tokens)
     * 
     * @param id
     * @param args
     * @param token
     * @param client
     * @return MCP success/error response
     */
    public static String update(String id, JsonObject args, String token, String sessionId, RestApiClient client) {
        return McpExecutorUtils.execute(id, "Update Auth Token", () -> {
            JsonObject body = new JsonObject();
            if (args.has("clientId") && !args.get("clientId").isJsonNull()) {
                body.addProperty("clientId", args.get("clientId").getAsString());
            }
            if (args.has("roleId") && !args.get("roleId").isJsonNull()) {
                body.addProperty("roleId", args.get("roleId").getAsString());
            }
            if (args.has("organizationId") && !args.get("organizationId").isJsonNull()) {
                body.addProperty("organizationId", args.get("organizationId").getAsString());
            }
            if (args.has("warehouseId") && !args.get("warehouseId").isJsonNull()) {
                body.addProperty("warehouseId", args.get("warehouseId").getAsString());
            }
            if (args.has("language") && !args.get("language").isJsonNull()) {
                body.addProperty("language", args.get("language").getAsString());
            }

            JsonElement response = client.put("/auth/tokens", body, token);
            String newToken = null;
            if (response != null && response.isJsonObject()) {
                JsonObject obj = response.getAsJsonObject();
                if (obj.has("token")) {
                    newToken = obj.get("token").getAsString();
                    if (sessionId != null) {
                        McpServlet.updateToken(sessionId, newToken);
                    }
                }
            }
            return McpExecutorUtils.wrapJsonContent(id, response);
        });
    }

    /**
     * Logout (POST /auth/logout)
     * 
     * @param id
     * @param args
     * @param token
     * @param client
     * @return MCP success/error response
     */
    public static String logout(String id, JsonObject args, String token, String sessionId, RestApiClient client) {
        return McpExecutorUtils.execute(id, "idempiere_auth_logout", () -> {
            JsonObject body = new JsonObject();
            body.addProperty("token", token);

            JsonElement response = client.post("/auth/logout", body, null);
            McpServlet.updateToken(sessionId, null);

            return McpExecutorUtils.wrapJsonContent(id, response);
        });
    }
}
