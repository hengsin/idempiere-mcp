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
package org.idempiere.mcp.server.config;

import org.compiere.util.CLogger;

public class McpConfig {
    
    @SuppressWarnings("unused")
	private static final CLogger log = CLogger.getCLogger(McpConfig.class);

    // --- Connectivity: iDempiere REST API Config ---
    public static final String KEY_URL = "IDEMPIERE_API_URL";
    
    // Defaults
    private static final String DEFAULT_URL = "http://localhost:8080/api/v1";

    public static String get(String key, String defaultValue) {
        String val = System.getenv(key);
        if (val != null && !val.isBlank()) return val;
        val = System.getProperty(key);
        if (val != null && !val.isBlank()) return val;
        return defaultValue;
    }

    public static String getBaseUrl() { 
        return get(KEY_URL, DEFAULT_URL); 
    }
}
