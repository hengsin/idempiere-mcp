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

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.time.Duration;

import org.idempiere.mcp.server.config.McpConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class RestApiClient {

    private final HttpClient client;
    private final Gson gson;
    private final String baseUrl;

    public RestApiClient() {
        this.client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(20))
                .build();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.baseUrl = McpConfig.getBaseUrl();
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

    private JsonElement execute(String method, String path, JsonObject body, String token) throws Exception {
        String url = baseUrl + (path.startsWith("/") ? path : "/" + path);

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json");

        if ("POST".equals(method)) {
            builder.POST(BodyPublishers.ofString(gson.toJson(body)));
        } else if ("PUT".equals(method)) {
            builder.PUT(BodyPublishers.ofString(gson.toJson(body)));
        } else if ("DELETE".equals(method)) {
            builder.DELETE();
        } else {
            builder.GET();
        }

        HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 300) {
            // Attempt to return the error JSON if available
            try {
                return JsonParser.parseString(response.body());
            } catch (Exception e) {
                throw new IOException("API Error " + response.statusCode() + ": " + response.body());
            }
        }

        return JsonParser.parseString(response.body());
    }

    private String executeRaw(String method, String path, JsonObject body, String token, String accept)
            throws Exception {
        String url = baseUrl + (path.startsWith("/") ? path : "/" + path);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + token)
                .header("Accept", accept)
                .header("Content-Type", "application/json");
        if ("POST".equals(method)) {
            builder.POST(BodyPublishers.ofString(gson.toJson(body)));
        } else if ("PUT".equals(method)) {
            builder.PUT(BodyPublishers.ofString(gson.toJson(body)));
        } else {
            builder.GET();
        }
        HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 300) {
            throw new IOException("API Error " + response.statusCode() + ": " + response.body());
        }
        return response.body();
    }

    public JsonElement putBinary(String path, byte[] data, String token, java.util.Map<String, String> headers)
            throws Exception {
        String url = baseUrl + (path.startsWith("/") ? path : "/" + path);

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + token);

        if (headers != null) {
            for (java.util.Map.Entry<String, String> entry : headers.entrySet()) {
                builder.header(entry.getKey(), entry.getValue());
            }
        }

        // If content type is not set in headers, default to octet-stream
        if (headers == null || !headers.containsKey("Content-Type")) {
            builder.header("Content-Type", "application/octet-stream");
        }

        builder.PUT(BodyPublishers.ofByteArray(data));

        HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 300) {
            try {
                return JsonParser.parseString(response.body());
            } catch (Exception e) {
                throw new IOException("API Error " + response.statusCode() + ": " + response.body());
            }
        }

        return JsonParser.parseString(response.body());
    }

    private byte[] executeBinary(String method, String path, JsonObject body, String token, String accept)
            throws Exception {
        String url = baseUrl + (path.startsWith("/") ? path : "/" + path);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + token)
                .header("Accept", accept)
                .header("Content-Type", "application/json");
        if ("POST".equals(method)) {
            builder.POST(BodyPublishers.ofString(gson.toJson(body)));
        } else if ("PUT".equals(method)) {
            builder.PUT(BodyPublishers.ofString(gson.toJson(body)));
        } else {
            builder.GET();
        }
        HttpResponse<byte[]> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() >= 300) {
            throw new IOException("API Error " + response.statusCode() + ": " + new String(response.body()));
        }
        return response.body();
    }

    public String login(String userName, String password) throws Exception {
        String url = baseUrl + "/auth/tokens";
        // Create AuthenticationRequest body
        JsonObject body = new JsonObject();
        body.addProperty("userName", userName);
        body.addProperty("password", password);

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .POST(BodyPublishers.ofString(gson.toJson(body)));

        HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 300) {
            throw new IOException("Login Failed " + response.statusCode() + ": " + response.body());
        }

        // Parse response to extract token
        // Response format: { "token": "..." }
        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        if (json.has("token")) {
            return json.get("token").getAsString();
        } else {
            throw new IOException("Token not found in login response: " + response.body());
        }
    }
}