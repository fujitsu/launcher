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
package com.fujitsu.launcher.microprofile.jwt.auth.cdi;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;

import io.smallrye.jwt.auth.principal.JWTAuthContextInfo;
import io.smallrye.jwt.config.JWTAuthContextInfoProvider;

// Just put additional log message if mandatory property is set incorrectly.
@Dependent
public class VerboseJwtAuthContextInfoProvider extends JWTAuthContextInfoProvider {
    @Produces
    @ApplicationScoped
    @Override
    public JWTAuthContextInfo getContextInfo() {
        try {
            return super.getContextInfo();
        } catch (IllegalStateException e) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, e.toString());
            throw e;
        }
    }
}
