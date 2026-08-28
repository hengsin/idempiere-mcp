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
package org.idempiere.mcp.server.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.adempiere.base.Service;
import org.compiere.util.CLogger;
import org.compiere.util.Util;
import org.idempiere.mcp.server.api.IMcpService;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@WebServlet(name = "McpServlet", urlPatterns = { "/*" }, asyncSupported = true, loadOnStartup = 1)
public class McpServlet extends HttpServlet {
	private static final String STATUS_PATH = "/status";

	private static final String PROTOCOL_VERSION = "protocolVersion";

	private static final String APPLICATION_JSON_CONTENT_TYPE = "application/json; charset=UTF-8";

	private static final long serialVersionUID = 1L;

	private static final String HEADER_AUTHORIZATION = "Authorization";
	private static final String PREFIX_BEARER = "Bearer ";

	public static final String MCP_PROTOCOL_VERSION_HEADER = "Mcp-Protocol-Version";
	public static final String MCP_METHOD_HEADER = "Mcp-Method";
	public static final String MCP_NAME_HEADER = "Mcp-Name";
	public static final String X_MCP_HEADER = "x-mcp-header";

	public static final String DEFAULT_MCP_PROTOCOL_VERSION = "2026-07-28";

	// JSON-RPC and MCP error codes
	public static final int ERROR_PARSE_ERROR = -32700;
	public static final int ERROR_INVALID_REQUEST = -32600;
	public static final int ERROR_METHOD_NOT_FOUND = -32601;
	public static final int ERROR_INVALID_PARAMS = -32602;
	public static final int ERROR_INTERNAL = -32603;
	public static final int ERROR_HEADER_MISMATCH = -32020;
	public static final int ERROR_MISSING_CLIENT_CAPABILITY = -32021;
	public static final int ERROR_UNSUPPORTED_PROTOCOL_VERSION = -32022;
	public static final int ERROR_SERVICE_NOT_FOUND = -32000;

	// Environment variable names
	private static final String ENV_MCP_PROTOCOL_VERSION = "MCP_PROTOCOL_VERSION";
	private static final String ENV_MCP_CORS_ORIGIN = "MCP_CORS_ORIGIN";
	private static final String ENV_THREAD_POOL_SIZE = "MCP_THREAD_POOL_SIZE";

	// Configurable values
	private String protocolVersion = DEFAULT_MCP_PROTOCOL_VERSION;
	private String corsOrigin = "*";
	private int threadPoolSize = 100;
	private static final CLogger log = CLogger.getCLogger(McpServlet.class);

	private ExecutorService requestExecutor;

	@Override
	public void init() throws ServletException {
		super.init();
		// load configuration from environment variables
		loadConfigFromEnv();
		requestExecutor = Executors.newFixedThreadPool(threadPoolSize);
		if (log.isLoggable(Level.INFO))
			log.info("Stateless MCP Servlet initialized. Protocol=" + protocolVersion + ", threadPool=" + threadPoolSize);
	}

	@Override
	public void destroy() {
		if (requestExecutor != null) {
			requestExecutor.shutdownNow();
		}
		super.destroy();
		if (log.isLoggable(Level.INFO))
			log.info("MCP Servlet destroyed.");
	}

	private void loadConfigFromEnv() {
		try {
			String pv = System.getenv(ENV_MCP_PROTOCOL_VERSION);
			if (pv != null && !pv.trim().isEmpty()) {
				protocolVersion = pv.trim();
			}

			String cors = System.getenv(ENV_MCP_CORS_ORIGIN);
			if (cors != null && !cors.trim().isEmpty()) {
				corsOrigin = cors.trim();
			}

			String tpSize = System.getenv(ENV_THREAD_POOL_SIZE);
			if (tpSize != null && !tpSize.trim().isEmpty()) {
				threadPoolSize = Integer.parseInt(tpSize.trim());
			}
		} catch (Exception e) {
			log.log(Level.WARNING, "Failed to load MCP servlet config from environment", e);
		}
	}

	private String extractToken(HttpServletRequest req) {
		String authHeader = req.getHeader(HEADER_AUTHORIZATION);
		if (authHeader != null && authHeader.startsWith(PREFIX_BEARER)) {
			String token = authHeader.substring(PREFIX_BEARER.length()).trim();
			return Util.isEmpty(token, true) ? null : token;
		}
		return null;
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		buildRestBaseURL(req);
		setCommonResponseHeader(resp);
		String path = req.getPathInfo();
		// status endpoint
		if (STATUS_PATH.equals(path)) {
			doGetStatus(resp);
			return;
		}

		resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED,
				"GET method is only supported for " + STATUS_PATH + ". Use POST for MCP requests.");
	}

	private void doGetStatus(HttpServletResponse resp) {
		resp.setContentType(APPLICATION_JSON_CONTENT_TYPE);
		setCommonResponseHeader(resp);
		JsonObject json = new JsonObject();
		json.addProperty(PROTOCOL_VERSION, protocolVersion);
		json.addProperty("status", "running");
		json.addProperty("timestamp", System.currentTimeMillis());
		writeJson(resp, json);
	}

	private void setCommonResponseHeader(HttpServletResponse resp) {
		resp.setCharacterEncoding("UTF-8");
		resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
		resp.setHeader("Access-Control-Allow-Origin", corsOrigin);
		resp.setHeader("Access-Control-Expose-Headers",
				MCP_PROTOCOL_VERSION_HEADER + ", " + MCP_METHOD_HEADER + ", " + MCP_NAME_HEADER + ", Content-Type");
		resp.setHeader(MCP_PROTOCOL_VERSION_HEADER, protocolVersion);
		// Prevent buffering in proxies/reverse proxies
		resp.setHeader("X-Accel-Buffering", "no");
	}

	private static final AtomicReference<String> restBaseUrl = new AtomicReference<>(null);
	
	/**
	 * Build REST Base URL
	 * @param req
	 */
	private void buildRestBaseURL(HttpServletRequest req) {
		if (restBaseUrl.get() == null) {
			// Always use http for internal loopback calls to REST API.
			// The incoming request may arrive via HTTPS (e.g. nginx reverse proxy),
			// but the REST API listens on plain HTTP on localhost.
			String scheme = "http";
			String host = "localhost";
			int port = req.getLocalPort();

			// Construct the base URL, handling default ports
			StringBuilder urlBuilder = new StringBuilder();
			urlBuilder.append(scheme).append("://").append(host);
			if (port != -1 && !((scheme.equals("http") && port == 80) || (scheme.equals("https") && port == 443))) {
				urlBuilder.append(":").append(port);
			}
			urlBuilder.append("/api/v1");
			restBaseUrl.set(urlBuilder.toString());
		}		
	}
	
	/**
	 * Get REST base URL constructed from the initial request
	 * @return REST Base URL
	 */
	public static String getRestBaseURL() {
		return restBaseUrl.get();
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		buildRestBaseURL(req);
		setCommonResponseHeader(resp);

		String jsonBody = readBody(req);
		JsonObject jsonObject;
		try {
			JsonElement parsed = JsonParser.parseString(jsonBody);
			if (!parsed.isJsonObject()) {
				sendJsonRpcError(resp, null, ERROR_INVALID_REQUEST, "Invalid JSON-RPC request: expected JSON object");
				return;
			}
			jsonObject = parsed.getAsJsonObject();
		} catch (Exception e) {
			sendJsonRpcError(resp, null, ERROR_PARSE_ERROR, "Parse error: " + e.getMessage());
			return;
		}

		JsonElement idElement = jsonObject.get("id");

		if (!jsonObject.has("method") || jsonObject.get("method").isJsonNull()) {
			sendJsonRpcError(resp, idElement, ERROR_INVALID_REQUEST, "Missing required 'method' property");
			return;
		}

		String method = jsonObject.get("method").getAsString();
		JsonObject params = (jsonObject.has("params") && jsonObject.get("params").isJsonObject())
				? jsonObject.getAsJsonObject("params") : new JsonObject();

		// Protocol Version validation
		String headerVersion = req.getHeader(MCP_PROTOCOL_VERSION_HEADER);
		String metaVersion = null;
		if (params.has("_meta") && params.get("_meta").isJsonObject()) {
			JsonObject meta = params.getAsJsonObject("_meta");
			if (meta.has("io.modelcontextprotocol/protocolVersion") && !meta.get("io.modelcontextprotocol/protocolVersion").isJsonNull()) {
				metaVersion = meta.get("io.modelcontextprotocol/protocolVersion").getAsString();
			}
		}
		String requestedVersion = (headerVersion != null && !headerVersion.trim().isEmpty()) ? headerVersion.trim() : metaVersion;
		if (requestedVersion != null && !protocolVersion.equals(requestedVersion)) {
			sendJsonRpcError(resp, idElement, ERROR_UNSUPPORTED_PROTOCOL_VERSION,
					"Unsupported protocol version: " + requestedVersion + ". Supported version: " + protocolVersion);
			return;
		}

		// Header Mismatch Validation (SEP-2243)
		String headerMethod = req.getHeader(MCP_METHOD_HEADER);
		if (headerMethod != null && !headerMethod.trim().isEmpty()) {
			if (!headerMethod.trim().equals(method)) {
				sendJsonRpcError(resp, idElement, ERROR_HEADER_MISMATCH,
						"Header mismatch: " + MCP_METHOD_HEADER + " header '" + headerMethod.trim() + "' does not match request method '" + method + "'");
				return;
			}
		}

		String headerName = req.getHeader(MCP_NAME_HEADER);
		if (headerName != null && !headerName.trim().isEmpty()) {
			String targetName = null;
			if (params.has("name") && !params.get("name").isJsonNull()) {
				targetName = params.get("name").getAsString();
			} else if (params.has("uri") && !params.get("uri").isJsonNull()) {
				targetName = params.get("uri").getAsString();
			}
			if (targetName != null && !headerName.trim().equals(targetName)) {
				sendJsonRpcError(resp, idElement, ERROR_HEADER_MISMATCH,
						"Header mismatch: " + MCP_NAME_HEADER + " header '" + headerName.trim() + "' does not match request target name '" + targetName + "'");
				return;
			}
		}

		// server/discover RPC handling
		if ("server/discover".equals(method)) {
			sendDiscoverResponse(resp, idElement);
			return;
		}

		// Stateless Request Processing
		String token = extractToken(req);
		executeRequest(token, jsonBody, resp);
	}

	private void sendDiscoverResponse(HttpServletResponse resp, JsonElement idElement) {
		JsonObject result = new JsonObject();
		result.addProperty(PROTOCOL_VERSION, protocolVersion);

		JsonArray supportedVersions = new JsonArray();
		supportedVersions.add(protocolVersion);
		result.add("supportedProtocolVersions", supportedVersions);

		JsonObject capabilities = new JsonObject();
		JsonObject tools = new JsonObject();
		tools.addProperty("listChanged", false);
		capabilities.add("tools", tools);
		JsonObject resources = new JsonObject();
		resources.addProperty("subscribe", false);
		resources.addProperty("listChanged", false);
		capabilities.add("resources", resources);
		result.add("capabilities", capabilities);

		JsonObject serverInfo = new JsonObject();
		serverInfo.addProperty("name", "iDempiere MCP Server");
		serverInfo.addProperty("version", "1.0.0");
		result.add("serverInfo", serverInfo);

		JsonObject meta = new JsonObject();
		meta.add("io.modelcontextprotocol/serverInfo", serverInfo);
		result.add("_meta", meta);

		result.addProperty("resultType", "complete");

		JsonObject json = new JsonObject();
		json.addProperty("jsonrpc", "2.0");
		if (idElement != null && !idElement.isJsonNull()) {
			json.add("id", idElement);
		} else {
			json.add("id", JsonNull.INSTANCE);
		}
		json.add("result", result);

		writeJsonResponse(resp, json.toString(), HttpServletResponse.SC_OK);
	}

	private String readBody(HttpServletRequest req) throws IOException {
		StringBuilder sb = new StringBuilder();
		String line;
		java.io.BufferedReader reader = req.getReader();
		while ((line = reader.readLine()) != null)
			sb.append(line);
		return sb.toString();
	}

	private void executeRequest(String token, String jsonBody, HttpServletResponse resp) {
		log.info("MCP executeRequest - body=" + 
				(jsonBody.length() > 100 ? jsonBody.substring(0, 100) + "..." : jsonBody));
		
		IMcpService service = Service.locator().locate(IMcpService.class).getService();
		String response = null;
		try {
			if (service != null) {
				try {
					log.info("MCP calling service.processRequest...");
					response = service.processRequest(jsonBody, token);
					log.info("MCP service returned response: " + (response != null ? response.substring(0, Math.min(200, response.length())) + "..." : "null"));
				} catch (Exception e) {
					log.log(Level.WARNING, "MCP Execution Failed", e);
					response = createErrorJson(-32603, "Internal Error");
				}
			} else {
				log.warning("MCP Service not found!");
				response = createErrorJson(-32000, "OSGi Service Not Found");
			}
		} catch (Exception e) {
			log.log(Level.WARNING, "MCP Processing Error", e);
			response = createErrorJson(-32603, "Internal error: " + e.getMessage());
		}

		if (response != null) {
			writeJsonResponse(resp, response, HttpServletResponse.SC_OK);
		} else {
			// Notifications don't require JSON-RPC response, send 202 Accepted
			log.info("MCP notification processed, sending 202 Accepted");
			try {
				resp.setStatus(HttpServletResponse.SC_ACCEPTED);
				resp.flushBuffer();
			} catch (IOException e) {
				log.log(Level.WARNING, "Failed to send notification acknowledgment", e);
			}
		}
	}

	private void sendJsonRpcError(HttpServletResponse resp, JsonElement idElement, int code, String message) {
		JsonObject json = new JsonObject();
		json.addProperty("jsonrpc", "2.0");
		if (idElement != null && !idElement.isJsonNull()) {
			json.add("id", idElement);
		} else {
			json.add("id", JsonNull.INSTANCE);
		}
		JsonObject error = new JsonObject();
		error.addProperty("code", code);
		error.addProperty("message", message);
		json.add("error", error);

		writeJsonResponse(resp, json.toString(), HttpServletResponse.SC_OK);
	}

	private void writeJsonResponse(HttpServletResponse resp, String jsonContent, int statusCode) {
		try {
			byte[] responseBytes = jsonContent.getBytes(StandardCharsets.UTF_8);
			resp.setContentType(APPLICATION_JSON_CONTENT_TYPE);
			resp.setContentLength(responseBytes.length);
			resp.setStatus(statusCode);
			resp.getOutputStream().write(responseBytes);
			resp.getOutputStream().flush();
		} catch (IOException e) {
			log.log(Level.WARNING, "Failed to write JSON response", e);
		}
	}

	private String createErrorJson(int code, String message) {
		JsonObject json = new JsonObject();
		json.addProperty("jsonrpc", "2.0");
		json.add("id", JsonNull.INSTANCE);
		JsonObject error = new JsonObject();
		error.addProperty("code", code);
		error.addProperty("message", message);
		json.add("error", error);
		return json.toString();
	}

	// Allow Options for CORS Pre-flight checks
	@Override
	protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		buildRestBaseURL(req);
		resp.setHeader("Access-Control-Allow-Origin", corsOrigin);
		resp.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
		resp.setHeader("Access-Control-Allow-Headers",
				"Content-Type, Authorization, " + MCP_PROTOCOL_VERSION_HEADER + ", " + MCP_METHOD_HEADER + ", " + MCP_NAME_HEADER + ", " + X_MCP_HEADER);
		resp.setHeader("Access-Control-Expose-Headers",
				MCP_PROTOCOL_VERSION_HEADER + ", " + MCP_METHOD_HEADER + ", " + MCP_NAME_HEADER + ", Content-Type");
		resp.setStatus(HttpServletResponse.SC_OK);
	}

	private void writeJson(HttpServletResponse resp, JsonObject obj) {
		try {
			PrintWriter writer = resp.getWriter();
			writer.write(obj.toString());
			writer.flush();
		} catch (IOException e) {
			log.saveError("Failed to write JSON response", e);
		}
	}

	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		setCommonResponseHeader(resp);
		resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "DELETE method is not supported in stateless MCP");
	}
}
