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

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.regex.Pattern;

import org.compiere.util.CLogger;
import org.compiere.util.Util;
import org.idempiere.mcp.server.client.McpApiException;
import org.idempiere.mcp.server.client.RestApiClient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class McpExecutorUtils {

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final Pattern NONLATIN = Pattern.compile("[^\\w_-]");
    private static final Pattern SEPARATORS = Pattern.compile("[\\s\\p{Punct}&&[^-]&&[^_]]");

    public static String execute(String id, String opName, Callable<String> action) {
        try {
            return action.call();
        } catch (McpApiException e) {
        	CLogger.getCLogger(McpExecutorUtils.class).log(Level.SEVERE, e.getMessage(), e);
            // Use tool error format (isError: true) instead of JSON-RPC error for tool execution failures
            return wrapToolError(id, opName + " API Error: " + e.getMessage());
        } catch (Exception e) {
        	CLogger.getCLogger(McpExecutorUtils.class).log(Level.SEVERE, e.getMessage(), e);
            // Use tool error format (isError: true) instead of JSON-RPC error for tool execution failures
            return wrapToolError(id, opName + " Error: " + e.getMessage());
        }
    }

    /**
     * Create a tool error response with isError flag as per MCP spec.
     * Tool errors should return a successful result with isError=true, not a JSON-RPC error.
     */
    public static String wrapToolError(String id, String errorMessage) {
        JsonObject item = new JsonObject();
        item.addProperty("type", "text");
        item.addProperty("text", errorMessage);

        JsonArray content = new JsonArray();
        content.add(item);
        JsonObject result = new JsonObject();
        result.add("content", content);
        result.addProperty("isError", true);
        return McpServiceImpl.createSuccess(id, result);
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
        // Check if it's an image type - use ImageContent format
        if (mimeType != null && mimeType.startsWith("image/")) {
            item.addProperty("type", "image");
            item.addProperty("data", java.util.Base64.getEncoder().encodeToString(data));
            item.addProperty("mimeType", mimeType);
        } else {
            // For other binary types, use EmbeddedResource format
            item.addProperty("type", "resource");
            JsonObject resource = new JsonObject();
            resource.addProperty("uri", "data:" + mimeType + ";base64,embedded");
            resource.addProperty("mimeType", mimeType);
            resource.addProperty("blob", java.util.Base64.getEncoder().encodeToString(data));
            item.add("resource", resource);
        }

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

    /**
     * Extract dictionary maintenance and migration script headers from tool arguments.
     *
     * @param args tool arguments
     * @return map of HTTP headers
     */
    public static Map<String, String> extractMigrationHeaders(JsonObject args) {
        Map<String, String> headers = new HashMap<>();
        if (args == null) {
            return headers;
        }
        if (args.has("logMigrationScript") && !args.get("logMigrationScript").isJsonNull()) {
            JsonElement el = args.get("logMigrationScript");
            boolean val = (el.isJsonPrimitive() && el.getAsJsonPrimitive().isBoolean() && el.getAsBoolean())
                    || "Y".equalsIgnoreCase(el.getAsString())
                    || "true".equalsIgnoreCase(el.getAsString());
            headers.put(RestApiClient.HEADER_LOG_MIGRATION_SCRIPT, val ? "Y" : "N");
        }
        if (args.has("adempiereSys") && !args.get("adempiereSys").isJsonNull()) {
            JsonElement el = args.get("adempiereSys");
            boolean val = (el.isJsonPrimitive() && el.getAsJsonPrimitive().isBoolean() && el.getAsBoolean())
                    || "Y".equalsIgnoreCase(el.getAsString())
                    || "true".equalsIgnoreCase(el.getAsString());
            headers.put(RestApiClient.HEADER_ADEMPIERE_SYS, val ? "Y" : "N");
        }
        if (args.has("migrationScriptComment") && !args.get("migrationScriptComment").isJsonNull()) {
            String comment = args.get("migrationScriptComment").getAsString();
            if (!Util.isEmpty(comment, true)) {
                String sanitized = comment.replace("\r", " ").replace("\n", " ").trim();
                if (!Util.isEmpty(sanitized, true)) {
                    headers.put(RestApiClient.HEADER_MIGRATION_SCRIPT_COMMENT, sanitized);
                }
            }
        }
        return headers;
    }

    /**
     * Resolve attachment payload from tool arguments.
     * Supports:
     * - local file path or URI path (as 'data' string or inside 'data' json object)
     * - base64 encoded data (with 'name')
     * - optional 'name' or 'fileName' override
     *
     * @param args tool arguments
     * @return JsonObject payload for iDempiere REST API attachments endpoint
     * @throws Exception if data cannot be resolved or read
     */
    public static JsonObject resolveAttachmentPayload(JsonObject args) throws Exception {
        if (args == null || !args.has("data") || args.get("data").isJsonNull()) {
            throw new IllegalArgumentException("'data' argument is required");
        }

        JsonElement dataEl = args.get("data");
        String name = null;
        if (args.has("name") && !args.get("name").isJsonNull()) {
            name = args.get("name").getAsString();
        } else if (args.has("fileName") && !args.get("fileName").isJsonNull()) {
            name = args.get("fileName").getAsString();
        }

        Boolean overwrite = null;
        if (args.has("overwrite") && !args.get("overwrite").isJsonNull()) {
            overwrite = args.get("overwrite").getAsBoolean();
        }

        String pathOrUriOrBase64 = null;
        if (dataEl.isJsonObject()) {
            JsonObject obj = dataEl.getAsJsonObject();
            if ((name == null || name.isEmpty()) && obj.has("name") && !obj.get("name").isJsonNull()) {
                name = obj.get("name").getAsString();
            }
            if (overwrite == null && obj.has("overwrite") && !obj.get("overwrite").isJsonNull()) {
                overwrite = obj.get("overwrite").getAsBoolean();
            }
            if (obj.has("data") && !obj.get("data").isJsonNull()) {
                pathOrUriOrBase64 = obj.get("data").getAsString();
            } else {
                throw new IllegalArgumentException("Attachment object must contain 'data' property");
            }
        } else if (dataEl.isJsonPrimitive()) {
            String str = dataEl.getAsString().trim();
            if (str.startsWith("{") && str.endsWith("}")) {
                try {
                    JsonElement parsed = JsonParser.parseString(str);
                    if (parsed.isJsonObject()) {
                        JsonObject obj = parsed.getAsJsonObject();
                        if ((name == null || name.isEmpty()) && obj.has("name") && !obj.get("name").isJsonNull()) {
                            name = obj.get("name").getAsString();
                        }
                        if (overwrite == null && obj.has("overwrite") && !obj.get("overwrite").isJsonNull()) {
                            overwrite = obj.get("overwrite").getAsBoolean();
                        }
                        if (obj.has("data") && !obj.get("data").isJsonNull()) {
                            pathOrUriOrBase64 = obj.get("data").getAsString();
                        }
                    }
                } catch (Exception e) {
                    // Not a valid JSON object, treat as path or URI string
                }
            }
            if (pathOrUriOrBase64 == null) {
                pathOrUriOrBase64 = str;
            }
        } else {
            throw new IllegalArgumentException("Invalid 'data' argument type");
        }

        byte[] bytes = null;
        String base64Data = null;

        URI uri = null;
        int colonIdx = pathOrUriOrBase64.indexOf(':');
        // Do not treat Windows drive letters (e.g. C:\) as URI schemes
        if (colonIdx > 1) {
            try {
                URI candidate = URI.create(pathOrUriOrBase64);
                if (candidate.isAbsolute()) {
                    uri = candidate;
                }
            } catch (Exception e) {
                // Not a valid URI
            }
        }

        if (uri != null) {
            if ("file".equalsIgnoreCase(uri.getScheme())) {
                Path filePath = Path.of(uri);
                bytes = Files.readAllBytes(filePath);
                if (name == null || name.isEmpty()) {
                    name = filePath.getFileName() != null ? filePath.getFileName().toString() : "attachment";
                }
            } else {
                URL url = uri.toURL();
                URLConnection conn = url.openConnection();
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(30000);
                try (InputStream in = conn.getInputStream()) {
                    bytes = in.readAllBytes();
                }
                if (name == null || name.isEmpty()) {
                    String header = conn.getHeaderField("Content-Disposition");
                    if (header != null) {
                        name = extractFilenameFromContentDisposition(header);
                    }
                    if (name == null || name.isEmpty()) {
                        String uriPath = uri.getPath();
                        if (uriPath != null && !uriPath.isEmpty()) {
                            int lastSlash = uriPath.lastIndexOf('/');
                            String sub = lastSlash >= 0 ? uriPath.substring(lastSlash + 1) : uriPath;
                            if (!sub.isEmpty()) {
                                name = sub;
                            }
                        }
                    }
                    if (name == null || name.isEmpty()) {
                        name = "attachment";
                    }
                }
            }
        } else {
            Path localPath = null;
            try {
                localPath = Path.of(pathOrUriOrBase64);
            } catch (Exception e) {
                // Ignore invalid path syntax
            }

            if (localPath != null && Files.exists(localPath)) {
                bytes = Files.readAllBytes(localPath);
                if (name == null || name.isEmpty()) {
                    name = localPath.getFileName() != null ? localPath.getFileName().toString() : "attachment";
                }
            } else {
                if (name != null && !name.isEmpty() && isBase64(pathOrUriOrBase64)) {
                    base64Data = pathOrUriOrBase64;
                } else if (localPath != null) {
                    throw new NoSuchFileException(pathOrUriOrBase64);
                } else {
                    throw new IllegalArgumentException("Cannot resolve attachment file or URI: " + pathOrUriOrBase64);
                }
            }
        }

        if (base64Data == null && bytes != null) {
            base64Data = Base64.getEncoder().encodeToString(bytes);
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("name", name);
        payload.addProperty("data", base64Data);
        if (overwrite != null) {
            payload.addProperty("overwrite", overwrite);
        }
        return payload;
    }

    private static String extractFilenameFromContentDisposition(String header) {
        if (header == null) return null;
        for (String part : header.split(";")) {
            part = part.trim();
            if (part.toLowerCase().startsWith("filename*=")) {
                String val = part.substring(10).trim();
                int idx = val.indexOf("''");
                if (idx >= 0) {
                    val = val.substring(idx + 2);
                }
                if (val.startsWith("\"") && val.endsWith("\"") && val.length() > 1) {
                    val = val.substring(1, val.length() - 1);
                }
                try {
                    return URLDecoder.decode(val, StandardCharsets.UTF_8);
                } catch (Exception e) {
                    return val;
                }
            } else if (part.toLowerCase().startsWith("filename=")) {
                String val = part.substring(9).trim();
                if (val.startsWith("\"") && val.endsWith("\"") && val.length() > 1) {
                    val = val.substring(1, val.length() - 1);
                }
                return val;
            }
        }
        return null;
    }

    private static boolean isBase64(String s) {
        if (s == null || s.length() < 4) return false;
        try {
            Base64.getDecoder().decode(s);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Resolve binary data from a string which can be:
     * - a local file path
     * - a URI path (file:, http:, https:, etc.)
     * - a base64 encoded string
     *
     * @param pathOrUriOrBase64 local file path, URI path, or base64 string
     * @return byte[] binary content
     * @throws Exception if reading fails or file/URI cannot be resolved
     */
    public static byte[] resolveBinaryData(String pathOrUriOrBase64) throws Exception {
        if (pathOrUriOrBase64 == null || pathOrUriOrBase64.trim().isEmpty()) {
            throw new IllegalArgumentException("'data' cannot be empty");
        }
        String input = pathOrUriOrBase64.trim();

        URI uri = null;
        int colonIdx = input.indexOf(':');
        // Do not treat Windows drive letters (e.g. C:\) as URI schemes
        if (colonIdx > 1) {
            try {
                URI candidate = URI.create(input);
                if (candidate.isAbsolute()) {
                    uri = candidate;
                }
            } catch (Exception e) {
                // Not a valid URI
            }
        }

        if (uri != null) {
            if ("file".equalsIgnoreCase(uri.getScheme())) {
                Path filePath = Path.of(uri);
                return Files.readAllBytes(filePath);
            } else {
                URL url = uri.toURL();
                URLConnection conn = url.openConnection();
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(30000);
                try (InputStream in = conn.getInputStream()) {
                    return in.readAllBytes();
                }
            }
        }

        Path localPath = null;
        try {
            localPath = Path.of(input);
        } catch (Exception e) {
            // Ignore invalid path syntax
        }

        if (localPath != null && Files.exists(localPath)) {
            return Files.readAllBytes(localPath);
        }

        if (isBase64(input)) {
            try {
                return Base64.getDecoder().decode(input);
            } catch (Exception e) {
                // Ignore and fall through
            }
        }

        if (localPath != null) {
            throw new NoSuchFileException(input);
        }

        throw new IllegalArgumentException("Cannot resolve data from file path or URI: " + input);
    }
}
