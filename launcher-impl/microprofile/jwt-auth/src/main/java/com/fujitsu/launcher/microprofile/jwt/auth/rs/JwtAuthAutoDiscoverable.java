/*
 * Copyright (c) 2019 Fujitsu Limited and/or its affiliates. All rights
 * reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package com.fujitsu.launcher.microprofile.jwt.auth.rs;

import javax.annotation.Priority;
import javax.ws.rs.core.FeatureContext;

import org.glassfish.internal.api.Globals;
import org.glassfish.jersey.internal.spi.AutoDiscoverable;
import org.glassfish.jersey.internal.spi.ForcedAutoDiscoverable;

import com.fujitsu.launcher.microprofile.jwt.auth.JwtAuthService;

import io.smallrye.jwt.auth.jaxrs.JWTAuthenticationFilter;
import io.smallrye.jwt.auth.jaxrs.JWTAuthorizationFilterRegistrar;

@Priority(AutoDiscoverable.DEFAULT_PRIORITY)
public class JwtAuthAutoDiscoverable implements ForcedAutoDiscoverable {

    @Override
    public void configure(FeatureContext context) {
        if (Globals.getDefaultHabitat().getService(JwtAuthService.class).isJwtAuthEnabled()) {
            if (!context.getConfiguration().isRegistered(JWTAuthorizationFilterRegistrar.class)) {
                context.register(JWTAuthorizationFilterRegistrar.class);
            }
            if (!context.getConfiguration().isRegistered(JWTAuthenticationFilter.class)) {
                context.register(JWTAuthenticationFilter.class);
            }
        }
    }
}
