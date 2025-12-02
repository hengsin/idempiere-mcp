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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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

@WebServlet(name = "McpServlet", urlPatterns = {"/*"}, asyncSupported = true, loadOnStartup = 1)
public class McpServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	private static final String SSE_POST_PATH = "/message";
	private static final String SSE_GET_PATH = "/sse";
    private static final String SSE_MESSAGE_EVENT = "message";
	private static final String SESSION_ID_PARAMETER = "sessionId";
	
	private static final String STREAMING_PATH = "/streaming";
	private static final String STREAMING_SESSION_HEADER = "Mcp-Session-Id";
	
    private static final String MCP_PROTOCOL_VERSION_HEADER = "Mcp-Protocol-Version";
    private static final String DEFAULT_MCP_PROTOCOL_VERSION = "2025-06-18";
    private static final long DEFAULT_STREAMING_SESSION_TTL_MS = TimeUnit.MINUTES.toMillis(30);
    private static final long DEFAULT_CLEANUP_INTERVAL_MS = TimeUnit.MINUTES.toMillis(10);
    // Environment variable names
    private static final String ENV_MCP_PROTOCOL_VERSION = "MCP_PROTOCOL_VERSION";
    private static final String ENV_STREAMING_SESSION_TTL_MIN = "MCP_STREAMING_SESSION_TTL_MINUTES";
    private static final String ENV_STREAMING_SESSION_TTL_MS = "MCP_STREAMING_SESSION_TTL_MS";
    private static final String ENV_CLEANUP_INTERVAL_MIN = "MCP_CLEANUP_INTERVAL_MINUTES";
    private static final String ENV_CLEANUP_INTERVAL_MS = "MCP_CLEANUP_INTERVAL_MS";
    // Configurable values
    private String protocolVersion = DEFAULT_MCP_PROTOCOL_VERSION;
    private long streamingSessionTtlMs = DEFAULT_STREAMING_SESSION_TTL_MS;
    private long cleanupIntervalMs = DEFAULT_CLEANUP_INTERVAL_MS;	
    private static final CLogger log = CLogger.getCLogger(McpServlet.class);
    
    // Store active SSE sessions: SessionID -> AsyncContext
    private static final Map<String, AsyncContext> sessions = new ConcurrentHashMap<>();
    // Store active sessions (SSE + Streaming): SessionID -> AuthToken
    private static final Map<String, String> tokens = new ConcurrentHashMap<>();
    // Track last access for sessions (SSE + Streaming)
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
        cleanupScheduler.scheduleAtFixedRate(this::cleanupSessions, cleanupIntervalMs, cleanupIntervalMs, TimeUnit.MILLISECONDS);
        log.info("MCP Servlet initialized. Session cleanup scheduled every " + cleanupIntervalMs + " ms, TTL=" + streamingSessionTtlMs + " ms, protocol=" + protocolVersion);
    }

    @Override
    public void destroy() {
        if (cleanupScheduler != null) {
            cleanupScheduler.shutdownNow();
        }
        super.destroy();
        log.info("MCP Servlet destroyed. Cleanup scheduler stopped.");
    }

    private void loadConfigFromEnv() {
        try {
            String pv = System.getenv(ENV_MCP_PROTOCOL_VERSION);
            if (pv != null && !pv.trim().isEmpty()) {
                protocolVersion = pv.trim();
            }
            // TTL: prefer ms, fallback to minutes
            String ttlMs = System.getenv(ENV_STREAMING_SESSION_TTL_MS);
            String ttlMin = System.getenv(ENV_STREAMING_SESSION_TTL_MIN);
            if (ttlMs != null && !ttlMs.trim().isEmpty()) {
                streamingSessionTtlMs = Long.parseLong(ttlMs.trim());
            } else if (ttlMin != null && !ttlMin.trim().isEmpty()) {
                streamingSessionTtlMs = TimeUnit.MINUTES.toMillis(Long.parseLong(ttlMin.trim()));
            }
            // Cleanup: prefer ms, fallback to minutes
            String clMs = System.getenv(ENV_CLEANUP_INTERVAL_MS);
            String clMin = System.getenv(ENV_CLEANUP_INTERVAL_MIN);
            if (clMs != null && !clMs.trim().isEmpty()) {
                cleanupIntervalMs = Long.parseLong(clMs.trim());
            } else if (clMin != null && !clMin.trim().isEmpty()) {
                cleanupIntervalMs = TimeUnit.MINUTES.toMillis(Long.parseLong(clMin.trim()));
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
                    try { ctx.complete(); } catch (Exception ex) { /* ignore */ }
                }
                lastAccess.remove(sessionId);
                cleanedSessionsCount++;
                if (log.isLoggable(Level.INFO))
                	log.info("Cleaned up expired session: " + sessionId);
            }
        }
    }

    private boolean checkAuthorizationToken(HttpServletRequest req) {
        String authHeader = req.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            return !Util.isEmpty(token, true);
        }
        return false;
    }

    private String extractToken(HttpServletRequest req) {
        String authHeader = req.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }
        return "";
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    	setCommonResponseHeader(resp);
    	// Token required for all endpoints
        if (!checkAuthorizationToken(req)) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        String path = req.getPathInfo();
        if (SSE_GET_PATH.equals(path)) {
        	resp.setContentType("text/event-stream");            
            
            // Start async to keep connection open
            AsyncContext asyncContext = req.startAsync();
            asyncContext.setTimeout(0); // Infinite timeout

            String sessionId = UUID.randomUUID().toString();
            sessions.put(sessionId, asyncContext);
            String token = extractToken(req);
            tokens.put(sessionId, token);
            lastAccess.put(sessionId, System.currentTimeMillis());

            // Cleanup on error/timeout
            setupListeners(asyncContext, sessionId);

            // Echo session id in header for clients that expect it
            resp.setHeader(STREAMING_SESSION_HEADER, sessionId);
            
            // MCP Handshake: Send 'endpoint' event telling client where to POST messages
            // The URI typically includes the session ID to route subsequent POSTs back to this stream
            String postEndpoint = req.getContextPath() + "/message?sessionId=" + sessionId;
            sendSseEvent(asyncContext, "endpoint", postEndpoint);
            
            if (log.isLoggable(Level.INFO))
				log.info("New MCP Client connected. Session: " + sessionId);
            return;
        }
        
        // GET /streaming opens SSE for existing session
        if (STREAMING_PATH.equals(path)) {
        	resp.setContentType("text/event-stream");
        	String sessionId = req.getHeader(STREAMING_SESSION_HEADER);
        	if (Util.isEmpty(sessionId, true)) {
        		resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing Mcp-Session-Id header");
        		return;
        	}
        	// must have existing session token from handshake
        	if (!tokens.containsKey(sessionId)) {
        		resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid or expired Session ID");
        		return;
        	}
        	// If there is already an SSE context, replace
        	AsyncContext existing = sessions.get(sessionId);
        	if (existing != null) {
        		try { existing.complete(); } catch (Exception ignore) {}
        	}
        	AsyncContext asyncContext = req.startAsync();
        	asyncContext.setTimeout(0);
        	sessions.put(sessionId, asyncContext);
        	lastAccess.put(sessionId, System.currentTimeMillis());
        	setupListeners(asyncContext, sessionId);
        	// Echo session id and send a small session event to confirm
        	resp.setHeader(STREAMING_SESSION_HEADER, sessionId);
        	JsonObject sessionEvent = new JsonObject();
        	sessionEvent.addProperty("sessionId", sessionId);
        	sessionEvent.addProperty("transport", "streamable-http");
        	sessionEvent.addProperty("protocolVersion", protocolVersion);
        	sendSseEvent(asyncContext, "session", sessionEvent.toString());
        	if (log.isLoggable(Level.INFO))
        		log.info("MCP Streamable HTTP SSE opened via GET. Session=" + sessionId);
        	return;
        }
        
        // status endpoint
        if ("/status".equals(path)) {
        	// Require auth like other endpoints
        	if (!checkAuthorizationToken(req)) {
        		resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        		return;
        	}
        	resp.setContentType("application/json");
        	setCommonResponseHeader(resp);
        	JsonObject json = new JsonObject();
        	json.addProperty("protocolVersion", protocolVersion);
        	json.addProperty("sessionTTLMillis", streamingSessionTtlMs);
        	json.addProperty("cleanupIntervalMillis", cleanupIntervalMs);
        	json.addProperty("activeSessionCount", sessions.size());
        	json.addProperty("trackedSessionCount", lastAccess.size());
        	json.addProperty("cleanedSessionTotal", cleanedSessionsCount);
        	json.addProperty("timestamp", System.currentTimeMillis());
        	// Provide a lightweight summary of session ids (may be large, so limit to first 50)
        	int limit = 50;
        	int i = 0;
        	JsonObject sessionSummary = new JsonObject();
        	for (String sid : sessions.keySet()) {
        		if (i++ >= limit) break;
        		Long last = lastAccess.get(sid);
        		sessionSummary.addProperty(sid, last != null ? last : -1L);
        	}
        	json.add("sessions", sessionSummary);
        	writeJson(resp, json);
        	return;
        }
        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

	private void setupListeners(AsyncContext asyncContext, String sessionId) {
		asyncContext.addListener(new AsyncListener() {
		    @Override public void onComplete(AsyncEvent event) { sessions.remove(sessionId); tokens.remove(sessionId); lastAccess.remove(sessionId); }
		    @Override public void onTimeout(AsyncEvent event) { sessions.remove(sessionId); tokens.remove(sessionId); lastAccess.remove(sessionId); }
		    @Override public void onError(AsyncEvent event) { sessions.remove(sessionId); tokens.remove(sessionId); lastAccess.remove(sessionId); }
		    @Override public void onStartAsync(AsyncEvent event) {}
		});
	}

	private void setCommonResponseHeader(HttpServletResponse resp) {
		resp.setCharacterEncoding("UTF-8");
		resp.setHeader("Cache-Control", "no-cache");
		resp.setHeader("Connection", "keep-alive");
		resp.setHeader("Access-Control-Allow-Origin", "*");
		// Expose headers so browser clients can read them
		resp.setHeader("Access-Control-Expose-Headers", STREAMING_SESSION_HEADER + ", " + MCP_PROTOCOL_VERSION_HEADER + ", Content-Type");
		resp.setHeader(MCP_PROTOCOL_VERSION_HEADER, protocolVersion);
	}

    private void sendSseEvent(AsyncContext ctx, String eventName, String data) {
        try {
            ServletResponse response = ctx.getResponse();
            response.setContentType("text/event-stream");
            PrintWriter writer = response.getWriter();
            synchronized (ctx) {
                writer.write("event: " + eventName + "\n");
                writer.write("data: " + data + "\n\n");
                writer.flush();
            }
        } catch (IOException e) {
            log.log(Level.WARNING, "Failed to send SSE event", e);
            ctx.complete();
            sessions.values().remove(ctx);
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    	setCommonResponseHeader(resp);
    	String path = req.getPathInfo();
    	if (!checkAuthorizationToken(req)) {
    		resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    		resp.setContentType("application/json");
    		resp.getWriter().write("{\"error\":\"Unauthorized\"}");
    		return;
    	}
    	
    	if (SSE_POST_PATH.equals(path)) {
    		doSSERequest(req, resp);
    		return;
    	}
    	
    	if (STREAMING_PATH.equals(path)) {
    		String sessionId = req.getHeader(STREAMING_SESSION_HEADER);
    		boolean isHandshake = Util.isEmpty(sessionId, true);
    		StringBuilder sb = new StringBuilder();
    		String line; while ((line = req.getReader().readLine()) != null) sb.append(line);
    		String jsonBody = sb.toString();
    		if (isHandshake) {
    			// Create new session and process initialize synchronously
    			sessionId = UUID.randomUUID().toString();
    			String token = extractToken(req);
    			tokens.put(sessionId, token);
    			lastAccess.put(sessionId, System.currentTimeMillis());
    			resp.setHeader(STREAMING_SESSION_HEADER, sessionId);
    			resp.setContentType("application/json");
    			try {
    				IMcpService service = Service.locator().locate(IMcpService.class).getService();
    				String response = null;
    				if (service != null) {
    					response = service.processRequest(jsonBody, token);
    					resp.setStatus(HttpServletResponse.SC_OK);
    					PrintWriter writer = resp.getWriter();
    					writer.write(response != null ? response : createErrorJson(-32603, "Empty response"));
    					writer.flush();
    				} else {
    					resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
    					PrintWriter writer = resp.getWriter();
    					writer.write(createErrorJson(-32000, "OSGi Service Not Found"));
    					writer.flush();
    				}
    				if (log.isLoggable(Level.INFO))
    					log.info("MCP Streamable HTTP initialize processed via POST. Session=" + sessionId);
    			} catch (Exception e) {
    				log.log(Level.SEVERE, "MCP initialize failed", e);
    				resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    				PrintWriter writer = resp.getWriter();
    				writer.write(createErrorJson(-32603, "Internal Error: " + e.getMessage()));
    				writer.flush();
    			}
    			return;
    		} else {
    			// Message POST: must have existing session
    			if (!tokens.containsKey(sessionId)) {
    				resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid or expired Session ID");
    				return;
    			}
    			lastAccess.put(sessionId, System.currentTimeMillis());
    			resp.setStatus(HttpServletResponse.SC_ACCEPTED); // response will be via SSE stream (GET)
    			processSSERequest(sessionId, jsonBody, req, resp);
    			return;
    		}
    	}
    	resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    private void doSSERequest(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		String sessionId = req.getParameter(SESSION_ID_PARAMETER);
		
		if (sessionId == null || !sessions.containsKey(sessionId) || !tokens.containsKey(sessionId)) {
		    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid or missing Session ID");
		    return;
		}

		// Read Request Body
		StringBuilder sb = new StringBuilder();
		String line;
		while ((line = req.getReader().readLine()) != null) sb.append(line);
		String jsonBody = sb.toString();

		// Handle the Request
		// Note: In MCP SSE transport, we respond 202 Accepted to the POST,
		// and send the actual JSON-RPC response via the SSE stream.
		resp.setStatus(HttpServletResponse.SC_ACCEPTED);
		
		// Process logic in a separate thread or immediately (sync for simplicity here)
		processSSERequest(sessionId, jsonBody, req, resp);
	}
    
    /**
     * Core MCP Logic Processor for SSE Protocol
     * @param sessionId
     * @param jsonBody
     * @param req
     * @param resp 
     */
    private void processSSERequest(String sessionId, String jsonBody, HttpServletRequest req, HttpServletResponse resp) {
    	// Execute via Service
        IMcpService service = Service.locator().locate(IMcpService.class).getService();
        String response = null;
        try {
	        if (service != null) {
	            try {
	            	String token = tokens.get(sessionId);
	                response = service.processRequest(jsonBody, token);
	            } catch (Exception e) {
	                log.log(Level.WARNING, "MCP Execution Failed", e);
	                response = createErrorJson(-32603, "Internal Error");
	            }
	        } else {
	            response = createErrorJson(-32000, "OSGi Service Not Found");
	        }
        } catch (Exception e) {
            log.log(Level.WARNING, "MCP Processing Error", e);
            response = createErrorJson(-32603, "Internal error: " + e.getMessage());
        }
        
        // Send response back via SSE if one exists
        if (response != null) {
            AsyncContext ctx = sessions.get(sessionId);
            if (ctx != null) {
                sendSseEvent(ctx, SSE_MESSAGE_EVENT, response);
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
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS, DELETE");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, " + STREAMING_SESSION_HEADER + ", " + MCP_PROTOCOL_VERSION_HEADER);
        resp.setHeader("Access-Control-Expose-Headers", STREAMING_SESSION_HEADER + ", " + MCP_PROTOCOL_VERSION_HEADER + ", Content-Type");
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
		String path = req.getPathInfo();
		if (!checkAuthorizationToken(req)) {
			resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			resp.setContentType("application/json");
			resp.getWriter().write("{\"error\":\"Unauthorized\"}");
			return;
		}
		
		if (STREAMING_PATH.equals(path)) {
			String sessionId = req.getHeader(STREAMING_SESSION_HEADER);
			if (Util.isEmpty(sessionId, true)) {
				resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing Mcp-Session-Id header");
				return;
			}
			// Close SSE context if present and remove session state
			AsyncContext ctx = sessions.remove(sessionId);
			if (ctx != null) {
				try { ctx.complete(); } catch (Exception ignore) {}
			}
			tokens.remove(sessionId);
			lastAccess.remove(sessionId);
			resp.setStatus(HttpServletResponse.SC_OK);
			resp.setContentType("application/json");
			JsonObject ack = new JsonObject();
			ack.addProperty("sessionId", sessionId);
			ack.addProperty("disconnected", true);
			ack.addProperty("protocolVersion", protocolVersion);
			writeJson(resp, ack);
			if (log.isLoggable(Level.INFO))
				log.info("MCP Streamable HTTP session disconnected via DELETE. Session=" + sessionId);
			return;
		}
		resp.sendError(HttpServletResponse.SC_NOT_FOUND);
	}
}
