/*
 * Copyright (c) 2023 Fujitsu Limited and/or its affiliates. All rights
 * reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package com.fujitsu.launcher.microprofile.telemetry.tracing.rest.client;

import jakarta.ws.rs.ConstrainedTo;
import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.core.FeatureContext;
import org.glassfish.jersey.internal.spi.AutoDiscoverable;

@ConstrainedTo(RuntimeType.CLIENT)
public class OpenTelemetryClientAutoDiscoverable implements AutoDiscoverable {
    @Override
    public void configure(FeatureContext featureContext) {
        if (!featureContext.getConfiguration().isRegistered(OpenTelemetryPreInvocationInterceptor.class)) {
            featureContext.register(OpenTelemetryPreInvocationInterceptor.class);
        }
        if(!featureContext.getConfiguration().isRegistered(OpenTelemetryClientFeature.class)) {
            featureContext.register(OpenTelemetryClientFeature.class);
        }
    }
}
