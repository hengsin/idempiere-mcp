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
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.idempiere.mcp.server.client.RestApiClient;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class McpMiscExecutor {

    // --- References ---

    public static String get_reference(String id, JsonObject args, String token, RestApiClient client) {
        return McpExecutorUtils.execute(id, "Get Reference", () -> {
            String refId = args.get("id").getAsString();
            String path = "/reference/" + URLEncoder.encode(refId, StandardCharsets.UTF_8);
            JsonElement response = client.get(path, token);
            return McpExecutorUtils.wrapJsonContent(id, response);
        });
    }

    // --- Caches ---

    public static String list_caches(String id, JsonObject args, String token, RestApiClient client) {
        return McpExecutorUtils.execute(id, "List Caches", () -> {
            String tableName = args.has("table_name") ? args.get("table_name").getAsString() : "";
            String name = args.has("name") ? args.get("name").getAsString() : "";

            StringBuilder sb = new StringBuilder("/caches");
            boolean first = true;
            if (!tableName.isEmpty()) {
                sb.append("?table_name=").append(URLEncoder.encode(tableName, StandardCharsets.UTF_8));
                first = false;
            }
            if (!name.isEmpty()) {
                sb.append(first ? "?" : "&").append("name=").append(URLEncoder.encode(name, StandardCharsets.UTF_8));
            }

            JsonElement response = client.get(sb.toString(), token);
            return McpExecutorUtils.wrapJsonContent(id, response);
        });
    }

    public static String reset_cache(String id, JsonObject args, String token, RestApiClient client) {
        return McpExecutorUtils.execute(id, "Reset Cache", () -> {
            String path = "/caches";
            if (args != null && args.has("record_id")) {
                int recordId = args.get("record_id").getAsInt();
                path += "?record_id=" + recordId;
            }
            JsonElement response = client.delete(path, token);
            return McpExecutorUtils.wrapJsonContent(id, response);
        });
    }

    // --- Nodes ---

    public static String list_nodes(String id, JsonObject args, String token, RestApiClient client) {
        return McpExecutorUtils.execute(id, "List Nodes", () -> {
            JsonElement response = client.get("/nodes", token);
            return McpExecutorUtils.wrapJsonContent(id, response);
        });
    }

    public static String get_node(String id, JsonObject args, String token, RestApiClient client) {
        try {
            String nodeId = args.get("id").getAsString();
            String path = "/nodes/" + URLEncoder.encode(nodeId, StandardCharsets.UTF_8);
            JsonElement response = client.get(path, token);
            return McpExecutorUtils.wrapJsonContent(id, response);
        } catch (Exception e) {
            return McpExecutorUtils.wrapToolError(id, "Get Node Error: " + e.getMessage());
        }
    }

    public static String get_node_logs(String id, JsonObject args, String token, RestApiClient client) {
        try {
            String nodeId = args.get("id").getAsString();
            String path = "/nodes/" + URLEncoder.encode(nodeId, StandardCharsets.UTF_8) + "/logs";
            JsonElement response = client.get(path, token);
            return McpExecutorUtils.wrapJsonContent(id, response);
        } catch (Exception e) {
            return McpExecutorUtils.wrapToolError(id, "Get Node Logs Error: " + e.getMessage());
        }
    }

    public static String delete_node_logs(String id, JsonObject args, String token, RestApiClient client) {
        try {
            String nodeId = args.get("id").getAsString();
            String path = "/nodes/" + URLEncoder.encode(nodeId, StandardCharsets.UTF_8) + "/logs";
            JsonElement response = client.delete(path, token);
            return McpExecutorUtils.wrapJsonContent(id, response);
        } catch (Exception e) {
            return McpExecutorUtils.wrapToolError(id, "Delete Node Logs Error: " + e.getMessage());
        }
    }

    public static String get_node_log_file(String id, JsonObject args, String token, RestApiClient client) {
        try {
            String nodeId = args.get("id").getAsString();
            String fileName = args.get("fileName").getAsString();
            String path = "/nodes/" + URLEncoder.encode(nodeId, StandardCharsets.UTF_8) + "/logs/"
                    + URLEncoder.encode(fileName, StandardCharsets.UTF_8);

            // Check if user wants base64 json or raw binary, defaulting to binary stream
            // for now as per other file tools
            byte[] response = client.getBinary(path, token, "application/octet-stream");
            return McpExecutorUtils.wrapBinaryContent(id, response, "application/octet-stream");
        } catch (Exception e) {
            return McpExecutorUtils.wrapToolError(id, "Get Node Log File Error: " + e.getMessage());
        }
    }

    public static String rotate_node_log(String id, JsonObject args, String token, RestApiClient client) {
        try {
            String nodeId = args.get("id").getAsString();
            String path = "/nodes/" + URLEncoder.encode(nodeId, StandardCharsets.UTF_8) + "/logs/rotate";
            JsonElement response = client.post(path, new JsonObject(), token);
            return McpExecutorUtils.wrapJsonContent(id, response);
        } catch (Exception e) {
            return McpExecutorUtils.wrapToolError(id, "Rotate Node Log Error: " + e.getMessage());
        }
    }

    // --- Status Lines ---

    public static String list_status_lines(String id, JsonObject args, String token, RestApiClient client) {
        try {
            String filter = args.has("filter") ? args.get("filter").getAsString() : "";
            boolean withMessages = args.has("with_messages") && args.get("with_messages").getAsBoolean();
            StringBuilder sb = new StringBuilder("/statuslines");
            boolean first = true;
            if (!filter.isEmpty()) {
                sb.append("?$filter=").append(URLEncoder.encode(filter, StandardCharsets.UTF_8));
                first = false;
            }
            if (withMessages) {
                sb.append(first ? "?" : "&").append("with_messages=true");
            }
            JsonElement response = client.get(sb.toString(), token);
            return McpExecutorUtils.wrapJsonContent(id, response);
        } catch (Exception e) {
            return McpExecutorUtils.wrapToolError(id, "List Status Lines Error: " + e.getMessage());
        }
    }

    public static String get_status_line(String id, JsonObject args, String token, RestApiClient client) {
        try {
            String statusLineId = args.get("id").getAsString();
            String path = "/statuslines/" + URLEncoder.encode(statusLineId, StandardCharsets.UTF_8);
            JsonElement response = client.get(path, token);
            return McpExecutorUtils.wrapJsonContent(id, response);
        } catch (Exception e) {
            return McpExecutorUtils.wrapToolError(id, "Get Status Line Error: " + e.getMessage());
        }
    }

    // --- Charts ---

    public static String get_charts_data(String id, JsonObject args, String token, RestApiClient client) {
        try {
            String filter = args.has("filter") ? args.get("filter").getAsString() : "";
            String path = "/charts/data"
                    + (!filter.isEmpty() ? "?$filter=" + URLEncoder.encode(filter, StandardCharsets.UTF_8) : "");
            JsonElement response = client.get(path, token);
            return McpExecutorUtils.wrapJsonContent(id, response);
        } catch (Exception e) {
            return McpExecutorUtils.wrapToolError(id, "Get Charts Data Error: " + e.getMessage());
        }
    }

    public static String get_chart(String id, JsonObject args, String token, RestApiClient client) {
        try {
            String chartId = args.get("id").getAsString();
            StringBuilder sb = new StringBuilder("/charts/").append(URLEncoder.encode(chartId, StandardCharsets.UTF_8));

            boolean first = true;
            if (args.has("width")) {
                sb.append("?width=").append(args.get("width").getAsInt());
                first = false;
            }
            if (args.has("height")) {
                sb.append(first ? "?" : "&").append("height=").append(args.get("height").getAsInt());
                first = false;
            }
            if (args.has("json") && !args.get("json").getAsString().isEmpty()) {
                sb.append(first ? "?" : "&").append("json=")
                        .append(URLEncoder.encode(args.get("json").getAsString(), StandardCharsets.UTF_8));
            }

            // Check if asking for JSON or Image
            if (args.has("json") && !args.get("json").getAsString().isEmpty()) {
                JsonElement response = client.get(sb.toString(), token);
                return McpExecutorUtils.wrapJsonContent(id, response);
            } else {
                byte[] response = client.getBinary(sb.toString(), token, "*/*");
                return McpExecutorUtils.wrapBinaryContent(id, response, "*/*");
            }
        } catch (Exception e) {
            return McpExecutorUtils.wrapToolError(id, "Get Chart Error: " + e.getMessage());
        }
    }

    public static String get_chart_data(String id, JsonObject args, String token, RestApiClient client) {
        try {
            String chartId = args.get("id").getAsString();
            String path = "/charts/" + URLEncoder.encode(chartId, StandardCharsets.UTF_8) + "/data";
            JsonElement response = client.get(path, token);
            return McpExecutorUtils.wrapJsonContent(id, response);
        } catch (Exception e) {
            return McpExecutorUtils.wrapToolError(id, "Get Chart Data Error: " + e.getMessage());
        }
    }

    // --- MenuTree ---

    public static String get_menu_tree(String id, JsonObject args, String token, RestApiClient client) {
        try {
            String menuId = args.get("id").getAsString();
            String path = "/menutree/" + URLEncoder.encode(menuId, StandardCharsets.UTF_8);
            JsonElement response = client.get(path, token);
            return McpExecutorUtils.wrapJsonContent(id, response);
        } catch (Exception e) {
            return McpExecutorUtils.wrapToolError(id, "Get Menu Tree Error: " + e.getMessage());
        }
    }

    // --- Uploads ---

    public static String initiate_upload(String id, JsonObject args, String token, RestApiClient client) {
        try {
            JsonObject data = args.get("data").getAsJsonObject();
            JsonElement response = client.post("/uploads", data, token);
            return McpExecutorUtils.wrapJsonContent(id, response);
        } catch (Exception e) {
            return McpExecutorUtils.wrapToolError(id, "Initiate Upload Error: " + e.getMessage());
        }
    }

    public static String list_pending_uploads(String id, JsonObject args, String token, RestApiClient client) {
        try {
            JsonElement response = client.get("/uploads", token);
            return McpExecutorUtils.wrapJsonContent(id, response);
        } catch (Exception e) {
            return McpExecutorUtils.wrapToolError(id, "List Pending Uploads Error: " + e.getMessage());
        }
    }

    public static String upload_chunk(String id, JsonObject args, String token, RestApiClient client) {
        try {
            String uploadId = args.get("uploadId").getAsString();
            int chunkOrder = args.get("chunkOrder").getAsInt();
            long totalChunks = args.get("totalChunks").getAsLong();
            String base64Data = args.get("data").getAsString();
            byte[] dataBytes = Base64.getDecoder().decode(base64Data);

            // Calculate SHA-256
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(dataBytes);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            String sha256 = hexString.toString();

            String path = "/uploads/" + URLEncoder.encode(uploadId, StandardCharsets.UTF_8) + "/chunks/" + chunkOrder;

            Map<String, String> headers = new HashMap<>();
            headers.put("X-Total-Chunks", String.valueOf(totalChunks));
            headers.put("X-Content-SHA256", sha256);
            headers.put("Content-Type", "application/octet-stream");

            JsonElement response = client.putBinary(path, dataBytes, token, headers);
            return McpExecutorUtils.wrapJsonContent(id, response);

        } catch (Exception e) {
            return McpExecutorUtils.wrapToolError(id, "Upload Chunk Error: " + e.getMessage());
        }
    }

    public static String get_upload_status(String id, JsonObject args, String token, RestApiClient client) {
        try {
            String uploadId = args.get("uploadId").getAsString();
            String path = "/uploads/" + URLEncoder.encode(uploadId, StandardCharsets.UTF_8);
            if (args.has("expiresInSeconds")) {
                path += "?expiresInSeconds=" + args.get("expiresInSeconds").getAsInt();
            }
            JsonElement response = client.get(path, token);
            return McpExecutorUtils.wrapJsonContent(id, response);
        } catch (Exception e) {
            return McpExecutorUtils.wrapToolError(id, "Get Upload Status Error: " + e.getMessage());
        }
    }

    public static String cancel_upload(String id, JsonObject args, String token, RestApiClient client) {
        try {
            String uploadId = args.get("uploadId").getAsString();
            String path = "/uploads/" + URLEncoder.encode(uploadId, StandardCharsets.UTF_8);
            JsonElement response = client.delete(path, token);
            return McpExecutorUtils.wrapJsonContent(id, response);
        } catch (Exception e) {
            return McpExecutorUtils.wrapToolError(id, "Cancel Upload Error: " + e.getMessage());
        }
    }

    public static String get_uploaded_file(String id, JsonObject args, String token, RestApiClient client) {
        try {
            String uploadId = args.get("uploadId").getAsString();
            String path = "/uploads/" + URLEncoder.encode(uploadId, StandardCharsets.UTF_8) + "/file";
            if (args.has("json") && !args.get("json").getAsString().isEmpty()) {
                path += "?json=" + URLEncoder.encode(args.get("json").getAsString(), StandardCharsets.UTF_8);
                JsonElement response = client.get(path, token);
                return McpExecutorUtils.wrapJsonContent(id, response);
            } else {
                byte[] response = client.getBinary(path, token, "application/octet-stream");
                return McpExecutorUtils.wrapBinaryContent(id, response, "application/octet-stream");
            }
        } catch (Exception e) {
            return McpExecutorUtils.wrapToolError(id, "Get Uploaded File Error: " + e.getMessage());
        }
    }

    public static String copy_uploaded_file(String id, JsonObject args, String token, RestApiClient client) {
        try {
            String uploadId = args.get("uploadId").getAsString();
            JsonObject data = args.get("data").getAsJsonObject();
            String path = "/uploads/" + URLEncoder.encode(uploadId, StandardCharsets.UTF_8) + "/copy";
            JsonElement response = client.post(path, data, token);
            return McpExecutorUtils.wrapJsonContent(id, response);
        } catch (Exception e) {
            return McpExecutorUtils.wrapToolError(id, "Copy Uploaded File Error: " + e.getMessage());
        }
    }

    // --- Batch ---

    public static String execute_batch(String id, JsonObject args, String token, RestApiClient client) {
        try {
            JsonObject data = args.get("data").getAsJsonObject();
            String path = "/batch";
            if (args.has("transaction")) {
                path += "?transaction=" + args.get("transaction").getAsBoolean();
            }
            JsonElement response = client.post(path, data, token);
            return McpExecutorUtils.wrapJsonContent(id, response);
        } catch (Exception e) {
            return McpExecutorUtils.wrapToolError(id, "Execute Batch Error: " + e.getMessage());
        }
    }
}
