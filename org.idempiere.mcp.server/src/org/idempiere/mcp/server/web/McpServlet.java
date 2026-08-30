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
import java.util.Enumeration;
import java.util.Map;
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
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@WebServlet(name = "McpServlet", urlPatterns = { "/*" }, asyncSupported = true, loadOnStartup = 1)
public class McpServlet extends HttpServlet {
	public static final String AUTHORIZATION_BEARER_ARGUMENT = "_authorization_bearer";

	private static final String STATUS_PATH = "/status";

	private static final String APPLICATION_JSON_CONTENT_TYPE = "application/json; charset=UTF-8";

	private static final long serialVersionUID = 1L;

	private static final String HEADER_AUTHORIZATION = "Authorization";
	private static final String PREFIX_BEARER = "Bearer ";

	public static final String MCP_PROTOCOL_VERSION_HEADER = "Mcp-Protocol-Version";
	public static final String MCP_METHOD_HEADER = "Mcp-Method";
	public static final String MCP_NAME_HEADER = "Mcp-Name";
	public static final String X_MCP_HEADER = "x-mcp-header";

	public static final String DEFAULT_MCP_PROTOCOL_VERSION = "2026-07-28";
	public static final String LEGACY_MCP_PROTOCOL_VERSION = "2025-11-25";
	public static final String PRINT_REQUEST_PROPERTY = "org.idempiere.mcp.print.request";

	private static final Gson prettyGson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

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
	private static final String ENV_MCP_CORS_ORIGIN = "MCP_CORS_ORIGIN";
	private static final String ENV_THREAD_POOL_SIZE = "MCP_THREAD_POOL_SIZE";

	// Configurable values
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
			log.info("Stateless MCP Servlet initialized. Protocols=[" + LEGACY_MCP_PROTOCOL_VERSION + "," 
				+ DEFAULT_MCP_PROTOCOL_VERSION + "], threadPool=" + threadPoolSize);
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

	private String extractToken(HttpServletRequest req, JsonObject jsonObject) {
		String authHeader = req.getHeader(HEADER_AUTHORIZATION);
		if (authHeader != null && authHeader.startsWith(PREFIX_BEARER)) {
			String token = authHeader.substring(PREFIX_BEARER.length()).trim();
			if (!Util.isEmpty(token, true))
				return token;
		}
		// optionally get token from JSON request body
		if (jsonObject != null) {
			String token = getTokenFromJsonObject(jsonObject);
			if (token != null)
				return token;
		}
		return null;
	}

	private String getTokenFromJsonObject(JsonObject jsonObject) {
		if (jsonObject == null)
			return null;

		// params.arguments["_authorization_bearer"]
		if (jsonObject.has("params") && jsonObject.get("params").isJsonObject()) {
			JsonObject params = jsonObject.getAsJsonObject("params");
			if (params.has("arguments") && params.get("arguments").isJsonObject()) {
				String key = AUTHORIZATION_BEARER_ARGUMENT;
				JsonObject arguments = params.getAsJsonObject("arguments");
				if (arguments.has(key) && !arguments.get(key).isJsonNull()) {
					JsonElement el = arguments.get(key);
					if (el.isJsonPrimitive()) {
						String val = el.getAsString();
						if (Util.isEmpty(val, true))
							return null;
						return val.trim();
					}
				}
			}
		}
		
		return null;
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if (isPrintRequest()) {
			String body = null;
			try {
				body = readBody(req);
			} catch (Exception e) {
				body = "<error reading body: " + e.getMessage() + ">";
			}
			printRequest(req, body);
		}
		
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
		JsonArray protocols = new JsonArray();
		protocols.add(LEGACY_MCP_PROTOCOL_VERSION);
		protocols.add(DEFAULT_MCP_PROTOCOL_VERSION);
		json.add("supportedProtocolVersions", protocols);
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
		String jsonBody = readBody(req);
		if (isPrintRequest()) {
			printRequest(req, jsonBody);
		}
		
		buildRestBaseURL(req);
		setCommonResponseHeader(resp);

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
		if (requestedVersion != null && !LEGACY_MCP_PROTOCOL_VERSION.equals(requestedVersion)
			 && !DEFAULT_MCP_PROTOCOL_VERSION.equals(requestedVersion)) {
			sendJsonRpcError(resp, idElement, ERROR_UNSUPPORTED_PROTOCOL_VERSION,
					"Unsupported protocol version: " + requestedVersion + ". Supported versions: " 
					+ LEGACY_MCP_PROTOCOL_VERSION + ", " + DEFAULT_MCP_PROTOCOL_VERSION);
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

		// Stateless Request Processing
		String token = extractToken(req, jsonObject);
		executeRequest(token, jsonObject, resp);
	}

	private String readBody(HttpServletRequest req) throws IOException {
		StringBuilder sb = new StringBuilder();
		String line;
		java.io.BufferedReader reader = req.getReader();
		while ((line = reader.readLine()) != null)
			sb.append(line);
		return sb.toString();
	}

	private void executeRequest(String token, JsonObject jsonObject, HttpServletResponse resp) {
		if (log.isLoggable(Level.INFO) && jsonObject != null) {
			log.info("MCP executeRequest - body=" + 
					(jsonObject.toString().length() > 100 ? jsonObject.toString().substring(0, 100) + "..." : jsonObject.toString()));
		}
		
		IMcpService service = Service.locator().locate(IMcpService.class).getService();
		String response = null;
		try {
			if (service != null) {
				try {
					log.info("MCP calling service.processRequest...");
					response = service.processRequest(jsonObject, token);
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

	public static boolean isPrintRequest() {
		return Boolean.getBoolean(PRINT_REQUEST_PROPERTY) || "true".equalsIgnoreCase(System.getProperty(PRINT_REQUEST_PROPERTY));
	}

	private void printRequest(HttpServletRequest req, String body) {
		StringBuilder sb = new StringBuilder();
		sb.append("==================== MCP Request ====================\n");
		sb.append("Method: ").append(req.getMethod()).append("\n");
		sb.append("URL: ").append(req.getRequestURL());
		if (req.getQueryString() != null && !req.getQueryString().isEmpty()) {
			sb.append("?").append(req.getQueryString());
		}
		sb.append("\n");
		sb.append("Protocol: ").append(req.getProtocol()).append("\n");
		sb.append("Remote Addr: ").append(req.getRemoteAddr()).append("\n");

		sb.append("Headers:\n");
		Enumeration<String> headerNames = req.getHeaderNames();
		if (headerNames != null) {
			while (headerNames.hasMoreElements()) {
				String headerName = headerNames.nextElement();
				Enumeration<String> headerValues = req.getHeaders(headerName);
				while (headerValues.hasMoreElements()) {
					sb.append("  ").append(headerName).append(": ").append(headerValues.nextElement()).append("\n");
				}
			}
		}

		Map<String, String[]> parameterMap = req.getParameterMap();
		if (parameterMap != null && !parameterMap.isEmpty()) {
			sb.append("Parameters:\n");
			for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
				sb.append("  ").append(entry.getKey()).append(": ")
						.append(String.join(", ", entry.getValue())).append("\n");
			}
		}

		sb.append("Body:\n");
		if (body != null && !body.trim().isEmpty()) {
			sb.append(formatBody(body)).append("\n");
		} else {
			sb.append("<empty>\n");
		}
		sb.append("=====================================================");
		System.out.println(sb.toString());
	}

	private String formatBody(String body) {
		if (body == null || body.trim().isEmpty()) {
			return body;
		}
		try {
			JsonElement jsonElement = JsonParser.parseString(body);
			if (jsonElement != null && (jsonElement.isJsonObject() || jsonElement.isJsonArray())) {
				return prettyGson.toJson(jsonElement);
			}
		} catch (Exception e) {
			// Not a JSON object or array, return raw body
		}
		return body;
	}
}
