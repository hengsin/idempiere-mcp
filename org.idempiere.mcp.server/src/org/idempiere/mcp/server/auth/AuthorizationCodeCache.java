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
package org.idempiere.mcp.server.auth;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.compiere.util.CLogger;

/**
 * authorization code cache for oauth 2 flow.
 */
public class AuthorizationCodeCache {
    private static final CLogger log = CLogger.getCLogger(AuthorizationCodeCache.class);

    private static AuthorizationCodeCache instance;
    private final Map<String, AuthEntry> cache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupScheduler;

    private static class AuthEntry {
        final String token;
        final String redirectUri;
        final long expiresAt;

        AuthEntry(String token, String redirectUri, long expiresAt) {
            this.token = token;
            this.redirectUri = redirectUri;
            this.expiresAt = expiresAt;
        }
    }

    private AuthorizationCodeCache() {
        this.cleanupScheduler = Executors.newSingleThreadScheduledExecutor();
        this.cleanupScheduler.scheduleAtFixedRate(this::cleanup, 1, 1, TimeUnit.MINUTES);
    }

    public static synchronized AuthorizationCodeCache getInstance() {
        if (instance == null) {
            instance = new AuthorizationCodeCache();
        }
        return instance;
    }

    public void store(String code, String token, String redirectUri) {
        // Expiry 5 minutes
        long expiry = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5);
        cache.put(code, new AuthEntry(token, redirectUri, expiry));
    }

    public String consume(String code, String expectedRedirectUri) {
        AuthEntry entry = cache.remove(code);
        if (entry == null) {
            return null;
        }
        if (System.currentTimeMillis() > entry.expiresAt) {
            return null;
        }
        if (expectedRedirectUri != null && !expectedRedirectUri.equals(entry.redirectUri)) {
            log.warning("Redirect URI mismatch for code: " + code + ". Expected: " + expectedRedirectUri + ", Got: "
                    + entry.redirectUri);
            return null;
        }
        return entry.token;
    }

    private void cleanup() {
        long now = System.currentTimeMillis();
        cache.entrySet().removeIf(entry -> now > entry.getValue().expiresAt);
    }

    public void shutdown() {
        if (cleanupScheduler != null) {
            cleanupScheduler.shutdownNow();
        }
    }
}
