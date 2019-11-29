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
package com.fujitsu.launcher.microprofile.jwt.auth;

import javax.inject.Inject;

import org.glassfish.api.StartupRunLevel;
import org.glassfish.hk2.runlevel.RunLevel;
import org.glassfish.web.deployment.annotation.handlers.LoginConfigHandler;
import org.jvnet.hk2.annotations.Service;

@Service(name = "jwt-auth-service")
@RunLevel(StartupRunLevel.VAL)
public class JwtAuthService {

    @Inject
    LoginConfigHandler loginConfigHandler;

    public boolean isJwtAuthEnabled() {
        return loginConfigHandler.isAnnotationPresent() && "MP-JWT".equals(loginConfigHandler.getAuthMethod());
    }
}
