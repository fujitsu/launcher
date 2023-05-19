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

import jakarta.enterprise.inject.spi.CDI;
import jakarta.ws.rs.core.Feature;
import jakarta.ws.rs.core.FeatureContext;

public class OpenTelemetryClientFeature implements Feature {
    @Override
    public boolean configure(FeatureContext featureContext) {
        if (featureContext.getConfiguration().isRegistered(OpenTelemetryClientFilter.class)) {
            return false;
        }
        try {
            var clientFilter = CDI.current().select(OpenTelemetryClientFilter.class).get();
            if (clientFilter != null) {
                featureContext.register(clientFilter);
                return true;
            }
            return false;
        } catch (IllegalStateException e) {
            return false;
        }
    }
}
