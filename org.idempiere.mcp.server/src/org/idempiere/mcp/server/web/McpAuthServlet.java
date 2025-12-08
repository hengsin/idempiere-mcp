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
import java.util.UUID;
import java.util.logging.Level;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.compiere.util.CLogger;
import org.compiere.util.Util;
import org.idempiere.mcp.server.auth.AuthorizationCodeCache;
import org.idempiere.mcp.server.client.RestApiClient;

import com.google.gson.JsonObject;

@WebServlet(name = "McpAuthServlet", urlPatterns = { "/oauth/*" }, loadOnStartup = 1)
public class McpAuthServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final CLogger log = CLogger.getCLogger(McpAuthServlet.class);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo();
        if ("/authorize".equals(path)) {
            handleAuthorize(req, resp);
            return;
        }
        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo();
        if ("/login".equals(path)) {
            handleLogin(req, resp);
            return;
        } else if ("/token".equals(path)) {
            handleToken(req, resp);
            return;
        }
        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    private void handleAuthorize(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String redirectUri = req.getParameter("redirect_uri");
        String state = req.getParameter("state");
        String clientId = req.getParameter("client_id");

        // Render simple login form
        resp.setContentType("text/html");
        PrintWriter out = resp.getWriter();
        out.println("<html>");
        out.println("<body>");
        out.println("<h2>Login to iDempiere MCP</h2>");
        out.println("<form action='login' method='post'>");
        if (Util.isEmpty(redirectUri, true)) {
            out.println("<div style='color:red'>Missing redirect_uri parameter!</div>");
        } else {
            out.println("<input type='hidden' name='redirect_uri' value='" + escape(redirectUri) + "'/>");
            out.println("<input type='hidden' name='state' value='" + escape(state) + "'/>");
            out.println("<input type='hidden' name='client_id' value='" + escape(clientId) + "'/>");
            out.println("<div><label>Username:</label> <input type='text' name='username'/></div>");
            out.println("<div><label>Password:</label> <input type='password' name='password'/></div>");
            out.println("<div><input type='submit' value='Login'/></div>");
        }
        out.println("</form>");
        out.println("</body>");
        out.println("</html>");
    }

    private void handleLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        String redirectUri = req.getParameter("redirect_uri");
        String state = req.getParameter("state");
        String clientId = req.getParameter("client_id");

        try {
            RestApiClient client = new RestApiClient();
            // This will throw if login fails
            String token = client.login(username, password);

            // Generate Authorization Code
            String code = UUID.randomUUID().toString();

            // Store code
            AuthorizationCodeCache.getInstance().store(code, token, redirectUri);

            // Redirect
            StringBuilder sb = new StringBuilder(redirectUri);
            sb.append(redirectUri.contains("?") ? "&" : "?");
            sb.append("code=").append(code);
            if (state != null) {
                sb.append("&state=").append(state);
            }
            resp.sendRedirect(sb.toString());

        } catch (Exception e) {
            log.log(Level.WARNING, "OAuth Login Failed", e);
            resp.setContentType("text/html");
            PrintWriter out = resp.getWriter();
            out.println("<html><body>");
            out.println("<h2 style='color:red'>Login Failed</h2>");
            out.println("<p>" + escape(e.getMessage()) + "</p>");
            out.println("<a href='authorize?redirect_uri=" + escape(redirectUri) + "&state=" + escape(state)
                    + "&client_id=" + escape(clientId) + "'>Try Again</a>");
            out.println("</body></html>");
        }
    }

    private void handleToken(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String grantType = req.getParameter("grant_type");
        String code = req.getParameter("code");
        String redirectUri = req.getParameter("redirect_uri");

        resp.setContentType("application/json");

        if (!"authorization_code".equals(grantType)) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writeJsonError(resp, "unsupported_grant_type", "Only authorization_code is supported");
            return;
        }

        String token = AuthorizationCodeCache.getInstance().consume(code, redirectUri);

        if (token == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writeJsonError(resp, "invalid_grant", "Invalid or expired authorization code");
            return;
        }

        JsonObject json = new JsonObject();
        json.addProperty("access_token", token);
        json.addProperty("token_type", "Bearer");
        json.addProperty("expires_in", 3600); // Approximate

        resp.getWriter().write(json.toString());
    }

    private void writeJsonError(HttpServletResponse resp, String error, String desc) throws IOException {
        JsonObject json = new JsonObject();
        json.addProperty("error", error);
        json.addProperty("error_description", desc);
        resp.getWriter().write(json.toString());
    }

    private String escape(String s) {
        if (s == null)
            return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'",
                "&#x27;");
    }
}
