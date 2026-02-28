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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletException;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.adempiere.base.Service;
import org.compiere.util.CLogger;
import org.compiere.util.Util;
import org.idempiere.mcp.server.api.IMcpService;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@WebServlet(name = "McpServlet", urlPatterns = { "/*" }, asyncSupported = true, loadOnStartup = 1)
public class McpServlet extends HttpServlet {
	private static final String STATUS_PATH = "/status";

	private static final String PROTOCOL_VERSION = "protocolVersion";

	private static final String SESSION_ID = "sessionId";

	private static final String APPLICATION_JSON_CONTENT_TYPE = "application/json";

	private static final String TEXT_EVENT_STREAM_CONTENT_TYPE = "text/event-stream";

	private static final long serialVersionUID = 1L;

	private static final String HEADER_AUTHORIZATION = "Authorization";
	private static final String PREFIX_BEARER = "Bearer ";

	private static final String STREAMING_SESSION_HEADER = "Mcp-Session-Id";

	private static final String MCP_PROTOCOL_VERSION_HEADER = "Mcp-Protocol-Version";
	private static final String DEFAULT_MCP_PROTOCOL_VERSION = "2024-11-05";
	private static final long DEFAULT_STREAMING_SESSION_TTL_MS = TimeUnit.MINUTES.toMillis(30);
	private static final long DEFAULT_CLEANUP_INTERVAL_MS = TimeUnit.MINUTES.toMillis(10);
	// Environment variable names
	private static final String ENV_MCP_PROTOCOL_VERSION = "MCP_PROTOCOL_VERSION";
	private static final String ENV_STREAMING_SESSION_TTL_MIN = "MCP_STREAMING_SESSION_TTL_MINUTES";
	private static final String ENV_STREAMING_SESSION_TTL_MS = "MCP_STREAMING_SESSION_TTL_MS";
	private static final String ENV_CLEANUP_INTERVAL_MIN = "MCP_CLEANUP_INTERVAL_MINUTES";
	private static final String ENV_CLEANUP_INTERVAL_MS = "MCP_CLEANUP_INTERVAL_MS";
	private static final String ENV_HEARTBEAT_INTERVAL_MS = "MCP_HEARTBEAT_INTERVAL_MS";
	private static final String ENV_MCP_CORS_ORIGIN = "MCP_CORS_ORIGIN";
	private static final String ENV_THREAD_POOL_SIZE = "MCP_THREAD_POOL_SIZE";
	// Configurable values
	private String protocolVersion = DEFAULT_MCP_PROTOCOL_VERSION;
	private long streamingSessionTtlMs = DEFAULT_STREAMING_SESSION_TTL_MS;
	private long cleanupIntervalMs = DEFAULT_CLEANUP_INTERVAL_MS;
	private long heartbeatIntervalMs = 15000; // Default 15s heartbeat
	private String corsOrigin = "*";
	private int threadPoolSize = 100;
	private static final CLogger log = CLogger.getCLogger(McpServlet.class);

	private ExecutorService requestExecutor;

	// Store active SSE sessions: SessionID -> AsyncContext
	private static final Map<String, AsyncContext> sessions = new ConcurrentHashMap<>();
	// Store active sessions: SessionID -> AuthToken
	private static final Map<String, String> tokens = new ConcurrentHashMap<>();
	// Track last access for sessions
	private static final Map<String, Long> lastAccess = new ConcurrentHashMap<>();

	private ScheduledExecutorService cleanupScheduler;
	private static long cleanedSessionsCount = 0; // metrics: total expired sessions cleaned

	@Override
	public void init() throws ServletException {
		super.init();
		// load configuration from environment variables
		loadConfigFromEnv();
		// start background cleanup
		cleanupScheduler = Executors.newSingleThreadScheduledExecutor();
		cleanupScheduler.scheduleAtFixedRate(this::cleanupSessions, cleanupIntervalMs, cleanupIntervalMs,
				TimeUnit.MILLISECONDS);
		requestExecutor = Executors.newFixedThreadPool(threadPoolSize);
		if (log.isLoggable(Level.INFO))
			log.info("MCP Servlet initialized. Session cleanup scheduled every " + cleanupIntervalMs
				+ " ms, Heartbeat every " + heartbeatIntervalMs + " ms, TTL=" + streamingSessionTtlMs + " ms, protocol="
				+ protocolVersion + ", threadPool=" + threadPoolSize);
	}

	@Override
	public void destroy() {
		if (cleanupScheduler != null) {
			cleanupScheduler.shutdownNow();
		}
		if (requestExecutor != null) {
			requestExecutor.shutdownNow();
		}
		super.destroy();
		if (log.isLoggable(Level.INFO))
			log.info("MCP Servlet destroyed. Cleanup scheduler stopped.");
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

			streamingSessionTtlMs = getLongEnv(ENV_STREAMING_SESSION_TTL_MS, ENV_STREAMING_SESSION_TTL_MIN,
					DEFAULT_STREAMING_SESSION_TTL_MS);
			cleanupIntervalMs = getLongEnv(ENV_CLEANUP_INTERVAL_MS, ENV_CLEANUP_INTERVAL_MIN,
					DEFAULT_CLEANUP_INTERVAL_MS);

			// Heartbeat
			String hbMs = System.getenv(ENV_HEARTBEAT_INTERVAL_MS);
			if (hbMs != null && !hbMs.trim().isEmpty()) {
				heartbeatIntervalMs = Long.parseLong(hbMs.trim());
			}

			// Thread Pool
			String tpSize = System.getenv(ENV_THREAD_POOL_SIZE);
			if (tpSize != null && !tpSize.trim().isEmpty()) {
				threadPoolSize = Integer.parseInt(tpSize.trim());
			}
		} catch (Exception e) {
			log.log(Level.WARNING, "Failed to load MCP servlet config from environment", e);
			// keep defaults
		}
		// enforce minimums to avoid too aggressive cleanup
		if (cleanupIntervalMs < TimeUnit.SECONDS.toMillis(30)) {
			cleanupIntervalMs = TimeUnit.SECONDS.toMillis(30);
		}
		if (streamingSessionTtlMs < TimeUnit.MINUTES.toMillis(1)) {
			streamingSessionTtlMs = TimeUnit.MINUTES.toMillis(1);
		}
	}

	private long getLongEnv(String keyMs, String keyMin, long defaultValue) {
		try {
			String valMs = System.getenv(keyMs);
			String valMin = System.getenv(keyMin);
			if (valMs != null && !valMs.trim().isEmpty()) {
				return Long.parseLong(valMs.trim());
			} else if (valMin != null && !valMin.trim().isEmpty()) {
				return TimeUnit.MINUTES.toMillis(Long.parseLong(valMin.trim()));
			}
		} catch (Exception e) {
			log.log(Level.WARNING, "Failed to parse env vars: " + keyMs + " or " + keyMin, e);
		}
		return defaultValue;
	}

	private void cleanupSessions() {
		long now = System.currentTimeMillis();
		for (Map.Entry<String, Long> e : lastAccess.entrySet()) {
			String sessionId = e.getKey();
			long last = e.getValue();
			if (now - last > streamingSessionTtlMs) {
				// remove streaming token
				tokens.remove(sessionId);
				// close SSE async context if present
				AsyncContext ctx = sessions.remove(sessionId);
				if (ctx != null) {
					try {
						ctx.complete();
					} catch (Exception ex) {
						/* ignore */ }
				}
				lastAccess.remove(sessionId);
				cleanedSessionsCount++;
				if (log.isLoggable(Level.INFO))
					log.info("Cleaned up expired session: " + sessionId);
			}
		}
	}

	/**
	 * Update or remove token for a session
	 * 
	 * @param sessionId
	 * @param token     null or empty string to remove existing token of a session
	 */
	public static void updateToken(String sessionId, String token) {
		if (!Util.isEmpty(sessionId, true) && !Util.isEmpty(token, true)) {
			tokens.put(sessionId, token);
		} else if (tokens.containsKey(sessionId)) {
			tokens.remove(sessionId);
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
		
		// GET opens asynchronous connection
		String sessionId = req.getHeader(STREAMING_SESSION_HEADER);
		// MUST have session id
		if (Util.isEmpty(sessionId, true)) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
					String.format("Missing %s header", STREAMING_SESSION_HEADER));
			return;
		}
		
		// Session must have been created via initialize first
		if (!lastAccess.containsKey(sessionId)) {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid or expired session ID. Call initialize first.");
			return;
		}
	    
	    resp.setContentType(TEXT_EVENT_STREAM_CONTENT_TYPE);		
		// If there is already an asynchronous connection, replace
		AsyncContext existing = sessions.get(sessionId);
		if (existing != null) {
			log.info("MCP replacing existing SSE connection for session: " + sessionId);
			try {
				existing.complete();
			} catch (Exception ignore) {
			}
		}
		
		AsyncContext asyncContext = req.startAsync();
		asyncContext.setTimeout(0);
		sessions.put(sessionId, asyncContext);
		lastAccess.put(sessionId, System.currentTimeMillis());
		setupListeners(asyncContext, sessionId);
		// Echo session id
		resp.setHeader(STREAMING_SESSION_HEADER, sessionId);
		resp.setStatus(HttpServletResponse.SC_OK);
		
		// Send SSE comment to confirm stream is open (optional but helps with proxies)
	    try {
	        PrintWriter writer = resp.getWriter();
	        writer.write(": stream opened\n\n");
	        writer.flush();
	    } catch (IOException e) {
	        log.log(Level.WARNING, "Failed to write stream confirmation", e);
	    }	    
		log.info("MCP SSE stream opened for session: " + sessionId);
	}

	private void doGetStatus(HttpServletResponse resp) {
		resp.setContentType(APPLICATION_JSON_CONTENT_TYPE);
		setCommonResponseHeader(resp);
		JsonObject json = new JsonObject();
		json.addProperty(PROTOCOL_VERSION, protocolVersion);
		json.addProperty("sessionTTLMillis", streamingSessionTtlMs);
		json.addProperty("cleanupIntervalMillis", cleanupIntervalMs);
		json.addProperty("activeSessionCount", sessions.size());
		json.addProperty("trackedSessionCount", lastAccess.size());
		json.addProperty("cleanedSessionTotal", cleanedSessionsCount);
		json.addProperty("timestamp", System.currentTimeMillis());
		// Provide a lightweight summary of session ids (may be large, so limit to first
		// 50)
		int limit = 50;
		int i = 0;
		JsonObject sessionSummary = new JsonObject();
		for (String sid : sessions.keySet()) {
			if (i++ >= limit)
				break;
			Long last = lastAccess.get(sid);
			sessionSummary.addProperty(sid, last != null ? last : -1L);
		}
		json.add("sessions", sessionSummary);
		writeJson(resp, json);
	}

	private void setupListeners(AsyncContext asyncContext, String sessionId) {
		asyncContext.addListener(new AsyncListener() {
			@Override
			public void onComplete(AsyncEvent event) {
				log.info("MCP SSE session completed: " + sessionId);
				sessions.remove(sessionId);
				tokens.remove(sessionId);
				lastAccess.remove(sessionId);
			}

			@Override
			public void onTimeout(AsyncEvent event) {
				log.info("MCP SSE session timeout: " + sessionId);
				sessions.remove(sessionId);
				tokens.remove(sessionId);
				lastAccess.remove(sessionId);
			}

			@Override
			public void onError(AsyncEvent event) {
				log.warning("MCP SSE session error: " + sessionId + " - " + event.getThrowable());
				sessions.remove(sessionId);
				tokens.remove(sessionId);
				lastAccess.remove(sessionId);
			}

			@Override
			public void onStartAsync(AsyncEvent event) {
			}
		});
	}

	private void setCommonResponseHeader(HttpServletResponse resp) {
		resp.setCharacterEncoding("UTF-8");
		resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
		resp.setHeader("Connection", "keep-alive");
		resp.setHeader("Access-Control-Allow-Origin", corsOrigin);
		// Expose headers so browser clients can read them
		resp.setHeader("Access-Control-Expose-Headers",
				STREAMING_SESSION_HEADER + ", " + MCP_PROTOCOL_VERSION_HEADER + ", Content-Type");
		resp.setHeader(MCP_PROTOCOL_VERSION_HEADER, protocolVersion);
		// Prevent buffering in proxies/reverse proxies
		resp.setHeader("X-Accel-Buffering", "no");
	}

	private void sendStreamingEvent(AsyncContext ctx, String eventName, String data) {
		try {
			ServletResponse response = ctx.getResponse();
			response.setContentType(TEXT_EVENT_STREAM_CONTENT_TYPE);
			PrintWriter writer = response.getWriter();
			synchronized (writer) {
				writer.write("event: " + eventName + "\n");
				writer.write("data: " + data + "\n\n");
				writer.flush();
			}
			if (log.isLoggable(Level.FINE))
				log.fine("Sent SSE event: " + eventName);
		} catch (IOException e) {
			log.log(Level.WARNING, "Failed to send Streaming event", e);
			try {
				ctx.complete();
			} catch (Exception ignore) {}
			sessions.values().remove(ctx);
		}
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

		String sessionId = req.getHeader(STREAMING_SESSION_HEADER);				
		String jsonBody = readBody(req);
		JsonObject jsonObject = JsonParser.parseString(jsonBody).getAsJsonObject();
		String method = jsonObject.get("method").getAsString();
		boolean isInitialize = "initialize".equals(method);
		if (isInitialize && Util.isEmpty(sessionId, true)) {
			sessionId = UUID.randomUUID().toString();
			String token = extractToken(req);
			if (token != null)
				tokens.put(sessionId, token);
			lastAccess.put(sessionId, System.currentTimeMillis());								
		} else {			
			if (Util.isEmpty(sessionId, true)) {
				resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
						String.format("Missing %s header", STREAMING_SESSION_HEADER));
				return;
			}
			if (!lastAccess.containsKey(sessionId)) {
				resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid or expired session ID");
				return;
			}
			lastAccess.put(sessionId, System.currentTimeMillis());			
		}
		
		resp.setHeader(STREAMING_SESSION_HEADER, sessionId);
		processRequest(sessionId, jsonBody, resp);
	}

	private String readBody(HttpServletRequest req) throws IOException {
		StringBuilder sb = new StringBuilder();
		String line;
		java.io.BufferedReader reader = req.getReader();
		while ((line = reader.readLine()) != null)
			sb.append(line);
		return sb.toString();
	}

	/**
	 * Core MCP Logic Processor
	 * 
	 * @param sessionId
	 * @param jsonBody
	 * @param resp 
	 */
	private void processRequest(String sessionId, String jsonBody, HttpServletResponse resp) {
		AsyncContext ctx = sessions.get(sessionId);
		if (ctx != null) {
			// Client has SSE stream open - process async and respond via SSE
			log.info("MCP processRequest - using SSE for session: " + sessionId);
			resp.setStatus(HttpServletResponse.SC_ACCEPTED);
			try {
				resp.flushBuffer();
			} catch (IOException e) {
				log.log(Level.WARNING, "Failed to flush 202 response", e);
			}
			requestExecutor.submit(() -> {
				try {
					executeRequest(sessionId, jsonBody, resp, true);
				} catch (Exception e) {
					log.log(Level.SEVERE, "MCP async execution failed", e);
				}
			});
		} else {
			// No SSE stream - respond directly in POST response
			log.info("MCP processRequest - direct response (no SSE) for session: " + sessionId);
			executeRequest(sessionId, jsonBody, resp, false);
		}
	}

	private void executeRequest(String sessionId, String jsonBody, HttpServletResponse resp, boolean isAsync) {
		log.info("MCP executeRequest - sessionId=" + sessionId + ", isAsync=" + isAsync + ", body=" + 
				(jsonBody.length() > 100 ? jsonBody.substring(0, 100) + "..." : jsonBody));
		
		// Execute via Service
		IMcpService service = Service.locator().locate(IMcpService.class).getService();
		String response = null;
		try {
			if (service != null) {
				try {
					String token = tokens.get(sessionId);
					log.info("MCP calling service.processRequest...");
					response = service.processRequest(jsonBody, token, sessionId);
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

		// Send response back
		if (response != null) {
			if (isAsync) {
				// Was supposed to use SSE - check if context is still valid
				AsyncContext ctx = sessions.get(sessionId);
				if (ctx != null) {
					log.info("MCP sending response via SSE stream");
					sendStreamingEvent(ctx, "message", response);
				} else {
					// SSE context was closed during processing - log warning but can't send response
					log.warning("MCP SSE context closed during async processing, cannot send response for session: " + sessionId);
				}
			} else {
				// No SSE stream - respond directly in HTTP response
				log.info("MCP sending response directly in HTTP response");
				try {
					byte[] responseBytes = response.getBytes(java.nio.charset.StandardCharsets.UTF_8);
					resp.setContentType(APPLICATION_JSON_CONTENT_TYPE);
					resp.setContentLength(responseBytes.length);
					resp.setStatus(HttpServletResponse.SC_OK);
					resp.getOutputStream().write(responseBytes);
					resp.getOutputStream().flush();
				} catch (IOException e) {
					log.log(Level.WARNING, "Failed to write response", e);
				}
			}
		} else {
			// Notifications don't require JSON-RPC response, but we still need to send HTTP response
			// Return 202 Accepted to acknowledge the notification was received
			if (!isAsync) {
				log.info("MCP notification processed, sending 202 Accepted");
				try {
					resp.setStatus(HttpServletResponse.SC_ACCEPTED);
					resp.flushBuffer();
				} catch (IOException e) {
					log.log(Level.WARNING, "Failed to send notification acknowledgment", e);
				}
			}
		}
	}

	private String createErrorJson(int code, String message) {
		JsonObject json = new JsonObject();
		json.addProperty("jsonrpc", "2.0");
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
		resp.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS, DELETE");
		resp.setHeader("Access-Control-Allow-Headers",
				"Content-Type, Authorization, " + STREAMING_SESSION_HEADER + ", " + MCP_PROTOCOL_VERSION_HEADER);
		resp.setHeader("Access-Control-Expose-Headers",
				STREAMING_SESSION_HEADER + ", " + MCP_PROTOCOL_VERSION_HEADER + ", Content-Type");
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

		String sessionId = req.getHeader(STREAMING_SESSION_HEADER);
		if (Util.isEmpty(sessionId, true)) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, String.format("Missing %s header", STREAMING_SESSION_HEADER));
			return;
		}
		// Close SSE context if present and remove session state
		AsyncContext ctx = sessions.remove(sessionId);
		if (ctx != null) {
			try {
				ctx.complete();
			} catch (Exception ignore) {
			}
		}
		tokens.remove(sessionId);
		lastAccess.remove(sessionId);
		resp.setStatus(HttpServletResponse.SC_OK);
		resp.setContentType(APPLICATION_JSON_CONTENT_TYPE);
		JsonObject ack = new JsonObject();
		ack.addProperty(SESSION_ID, sessionId);
		ack.addProperty("disconnected", true);
		ack.addProperty(PROTOCOL_VERSION, protocolVersion);
		writeJson(resp, ack);
		if (log.isLoggable(Level.INFO))
			log.info("MCP Streamable HTTP session disconnected via DELETE. Session=" + sessionId);
	}
}
