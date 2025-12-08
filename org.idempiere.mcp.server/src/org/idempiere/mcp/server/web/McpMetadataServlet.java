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

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

@WebServlet(name = "McpMetadataServlet", urlPatterns = { "/.well-known/oauth-authorization-server" }, loadOnStartup = 1)
public class McpMetadataServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");

        String scheme = req.getScheme();
        String serverName = req.getServerName();
        int serverPort = req.getServerPort();
        String contextPath = req.getContextPath();

        // Reconstruct base URL
        StringBuilder baseUrl = new StringBuilder();
        baseUrl.append(scheme).append("://").append(serverName);
        if ((serverPort != 80 && "http".equals(scheme)) || (serverPort != 443 && "https".equals(scheme))) {
            baseUrl.append(":").append(serverPort);
        }
        baseUrl.append(contextPath);

        String issuer = baseUrl.toString();

        JsonObject json = new JsonObject();
        json.addProperty("issuer", issuer);
        json.addProperty("authorization_endpoint", issuer + "/oauth/authorize");
        json.addProperty("token_endpoint", issuer + "/oauth/token");

        JsonArray responseTypes = new JsonArray();
        responseTypes.add("code");
        json.add("response_types_supported", responseTypes);

        JsonArray grantTypes = new JsonArray();
        grantTypes.add("authorization_code");
        json.add("grant_types_supported", grantTypes);

        JsonArray tokenEndpointAuthMethods = new JsonArray();
        tokenEndpointAuthMethods.add("none");
        json.add("token_endpoint_auth_methods_supported", tokenEndpointAuthMethods);

        // JsonArray codeChallengeMethods = new JsonArray();
        // json.add("code_challenge_methods_supported", codeChallengeMethods); // Not
        // supported yet?

        resp.getWriter().write(json.toString());
    }
}
