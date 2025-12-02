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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.idempiere.mcp.server.client.RestApiClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class McpResourceExecutor {
    
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static String listModels(String id, String token, RestApiClient client) {
        try {
            JsonElement resp = client.get("/models", token);
            return wrap(id, "idempiere://metadata/models", resp);
        } catch (Exception e) { return McpServiceImpl.createError(id, -32000, e.getMessage()); }
    }

    public static String listProcesses(String id, String token, RestApiClient client) {
        try {
            String filter = urlEncode("IsActive eq true");
            JsonElement resp = client.get("/processes?$filter=" + filter, token);
            return wrap(id, "idempiere://metadata/processes", resp);
        } catch (Exception e) { return McpServiceImpl.createError(id, -32000, e.getMessage()); }
    }

    private static String wrap(String id, String uri, JsonElement json) {
        JsonObject item = new JsonObject();
        item.addProperty("uri", uri);
        item.addProperty("mimeType", "application/json");
        item.addProperty("text", gson.toJson(json));
        
        JsonArray contents = new JsonArray(); contents.add(item);
        JsonObject res = new JsonObject(); res.add("contents", contents);
        return McpServiceImpl.createSuccess(id, res);
    }
    
    private static String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 encoding not supported", e);
        }
    }
}