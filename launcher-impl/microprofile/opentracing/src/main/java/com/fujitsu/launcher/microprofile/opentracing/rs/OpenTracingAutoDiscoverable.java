/*
 * Copyright (c) 2018-2019 Fujitsu Limited and/or its affiliates. All rights
 * reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package com.fujitsu.launcher.microprofile.opentracing.rs;

import javax.annotation.Priority;
import javax.ws.rs.core.FeatureContext;

import org.glassfish.jersey.internal.spi.AutoDiscoverable;

/**
 * Registers JAX-RS features for tracing.
 * 
 * @author Takahiro Nagao
 */
@Priority(AutoDiscoverable.DEFAULT_PRIORITY)
public class OpenTracingAutoDiscoverable implements AutoDiscoverable {

    @Override
    public void configure(final FeatureContext context) {
        if (!context.getConfiguration().isRegistered(LauncherTracingDynamicFeature.class)) {
            context.register(LauncherTracingDynamicFeature.class);
        }
    }
}
