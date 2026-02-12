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

import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.regex.Pattern;

import org.compiere.util.CLogger;
import org.idempiere.mcp.server.client.McpApiException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class McpExecutorUtils {

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final Pattern NONLATIN = Pattern.compile("[^\\w_-]");
    private static final Pattern SEPARATORS = Pattern.compile("[\\s\\p{Punct}&&[^-]&&[^_]]");

    public static String execute(String id, String opName, Callable<String> action) {
        try {
            return action.call();
        } catch (McpApiException e) {
        	CLogger.getCLogger(McpExecutorUtils.class).log(Level.SEVERE, e.getMessage(), e);
            return McpServiceImpl.createError(id, -32000, opName + " API Error: " + e.getMessage());
        } catch (Exception e) {
        	CLogger.getCLogger(McpExecutorUtils.class).log(Level.SEVERE, e.getMessage(), e);
            return McpServiceImpl.createError(id, -32000, opName + " Error: " + e.getMessage());
        }
    }

    public static String wrapJsonContent(String id, JsonElement json) {
        JsonObject item = new JsonObject();
        item.addProperty("type", "text");
        item.addProperty("text", gson.toJson(json)); // Valid JSON string inside text

        JsonArray content = new JsonArray();
        content.add(item);
        JsonObject result = new JsonObject();
        result.add("content", content);
        return McpServiceImpl.createSuccess(id, result);
    }

    public static String wrapBinaryContent(String id, byte[] data, String mimeType) {
        JsonObject item = new JsonObject();
        item.addProperty("type", "binary");
        item.addProperty("mimeType", mimeType);
        item.addProperty("data", java.util.Base64.getEncoder().encodeToString(data));

        JsonArray content = new JsonArray();
        content.add(item);
        JsonObject result = new JsonObject();
        result.add("content", content);
        return McpServiceImpl.createSuccess(id, result);
    }

    public static boolean isInteger(JsonElement e) {
        try {
            Integer.parseInt(e.getAsString());
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * convert arbitrary text to slug
     * 
     * @param input
     * @return slug
     */
    public static String slugify(String input) {
        String noseparators = SEPARATORS.matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(noseparators, Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH).replaceAll("-{2,}", "-").replaceAll("^-|-$", "");
    }
}
