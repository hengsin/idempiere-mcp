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
package org.idempiere.mcp.server.client;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.time.Duration;
import java.util.Map;
import java.util.logging.Level;

import org.compiere.util.CLogger;
import org.idempiere.mcp.server.config.McpConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class RestApiClient {

    private final CLogger log = CLogger.getCLogger(RestApiClient.class);
    private final HttpClient client;
    private final Gson gson;

    public RestApiClient() {
        this.client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(20))
                .build();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public JsonElement get(String path, String token) throws Exception {
        return execute("GET", path, null, token);
    }

    public JsonElement post(String path, JsonObject data, String token) throws Exception {
        return execute("POST", path, data, token);
    }

    public JsonElement put(String path, JsonObject data, String token) throws Exception {
        return execute("PUT", path, data, token);
    }

    public JsonElement delete(String path, String token) throws Exception {
        return execute("DELETE", path, null, token);
    }

    public String getYaml(String path, String token) throws Exception {
        return executeRaw("GET", path, null, token, "application/yaml");
    }

    public byte[] getBinary(String path, String token, String accept) throws Exception {
        return executeBinary("GET", path, null, token, accept);
    }

    private HttpRequest.Builder createBuilder(String path, String token, String accept) {
        String url = McpConfig.getBaseUrl() + (path.startsWith("/") ? path : "/" + path);

        if (log.isLoggable(Level.FINE)) {
            log.fine("Creating request for URL: " + url);
        }

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", accept != null ? accept : "application/json");

        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }

        return builder;
    }

    private JsonElement execute(String method, String path, JsonObject body, String token) throws Exception {
        HttpRequest.Builder builder = createBuilder(path, token, "application/json");
        builder.header("Content-Type", "application/json");
        setRequestMethod(builder, method, body);

        if (log.isLoggable(Level.INFO)) {
            log.info("Executing " + method + " " + path);
        }

        HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return handleResponse(response);
    }

    private String executeRaw(String method, String path, JsonObject body, String token, String accept)
            throws Exception {
        HttpRequest.Builder builder = createBuilder(path, token, accept);
        builder.header("Content-Type", "application/json");
        setRequestMethod(builder, method, body);

        if (log.isLoggable(Level.INFO)) {
            log.info("Executing Raw " + method + " " + path);
        }

        HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        checkResponse(response);
        return response.body();
    }

    private byte[] executeBinary(String method, String path, JsonObject body, String token, String accept)
            throws Exception {
        HttpRequest.Builder builder = createBuilder(path, token, accept);
        builder.header("Content-Type", "application/json");
        setRequestMethod(builder, method, body);

        if (log.isLoggable(Level.INFO)) {
            log.info("Executing Binary " + method + " " + path);
        }

        HttpResponse<byte[]> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());

        if (response.statusCode() >= 300) {
            String errorBody = new String(response.body());
            throw new McpApiException(response.statusCode(), errorBody);
        }
        return response.body();
    }

    public JsonElement putBinary(String path, byte[] data, String token, Map<String, String> headers)
            throws Exception {
        // Use createBuilder but we might need to override headers or add them
        // createBuilder sets Accept if we pass it, but here we might not care about
        // Accept or it's default
        HttpRequest.Builder builder = createBuilder(path, token, null);

        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                builder.header(entry.getKey(), entry.getValue());
            }
        }

        if (headers == null || !headers.containsKey("Content-Type")) {
            builder.header("Content-Type", "application/octet-stream");
        }

        builder.PUT(BodyPublishers.ofByteArray(data));

        if (log.isLoggable(Level.INFO)) {
            log.info("Executing PUT Binary " + path);
        }

        HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return handleResponse(response);
    }

    public String login(String userName, String password) throws Exception {
        String url = McpConfig.getBaseUrl() + "/auth/tokens";
        JsonObject body = new JsonObject();
        body.addProperty("userName", userName);
        body.addProperty("password", password);

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .POST(BodyPublishers.ofString(gson.toJson(body)));

        if (log.isLoggable(Level.INFO)) {
            log.info("Logging in user: " + userName);
        }

        HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 300) {
            throw new McpApiException(response.statusCode(), response.body());
        }

        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        if (json.has("token")) {
            return json.get("token").getAsString();
        } else {
            throw new McpApiException(response.statusCode(), "Token not found in login response: " + response.body());
        }
    }

    private void setRequestMethod(HttpRequest.Builder builder, String method, JsonObject body) {
        if ("POST".equals(method)) {
            builder.POST(BodyPublishers.ofString(gson.toJson(body != null ? body : new JsonObject())));
        } else if ("PUT".equals(method)) {
            builder.PUT(BodyPublishers.ofString(gson.toJson(body != null ? body : new JsonObject())));
        } else if ("DELETE".equals(method)) {
            builder.DELETE();
        } else {
            builder.GET();
        }
    }

    private void checkResponse(HttpResponse<?> response) throws McpApiException {
        if (response.statusCode() >= 300) {
            String bodyStr = "";
            if (response.body() instanceof String) {
                bodyStr = (String) response.body();
            } else if (response.body() instanceof byte[]) {
                bodyStr = new String((byte[]) response.body());
            } else {
                bodyStr = String.valueOf(response.body());
            }
            throw new McpApiException(response.statusCode(), bodyStr);
        }
    }

    private JsonElement handleResponse(HttpResponse<String> response) throws McpApiException {
        checkResponse(response);
        return JsonParser.parseString(response.body());
    }
}