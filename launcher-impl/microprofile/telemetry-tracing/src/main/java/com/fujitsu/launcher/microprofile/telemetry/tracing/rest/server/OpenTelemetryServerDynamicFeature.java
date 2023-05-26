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
package com.fujitsu.launcher.microprofile.telemetry.tracing.rest.server;

import jakarta.enterprise.inject.spi.CDI;
import jakarta.ws.rs.container.DynamicFeature;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.FeatureContext;
import jakarta.ws.rs.ext.Provider;

@Provider
public class OpenTelemetryServerDynamicFeature implements DynamicFeature {

    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext featureContext) {
        if (featureContext.getConfiguration().isRegistered(OpenTelemetryServerFilter.class)) {
            return;
        }

        try {
            var serverFilter = CDI.current().select(OpenTelemetryServerFilter.class).get();
            if (serverFilter != null) {
                featureContext.register(serverFilter);
            }
        } catch (IllegalStateException e) {
            // noop
        }
    }
}
