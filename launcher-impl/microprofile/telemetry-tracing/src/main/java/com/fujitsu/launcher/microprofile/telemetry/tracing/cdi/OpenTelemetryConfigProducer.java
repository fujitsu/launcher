/*
 * Copyright (c) 2023 Fujitsu Limited and/or its affiliates. All rights
 * reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * This file incorporates work authored by SmallRye OpenTelemetry,
 * licensed under the Apache License, Version 2.0, which is available at
 * http://www.apache.org/licenses/LICENSE-2.0.
 */
package com.fujitsu.launcher.microprofile.telemetry.tracing.cdi;

import com.fujitsu.launcher.microprofile.telemetry.tracing.config.OpenTelemetryConfig;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.Config;

import java.util.HashMap;
import java.util.Map;


@Singleton
public class OpenTelemetryConfigProducer {
    @Inject
    Config config;

    @Produces
    @Singleton
    public OpenTelemetryConfig produces() {
        return new OpenTelemetryConfig() {
            @Override
            public Map<String, String> properties() {
                Map<String, String> properties = new HashMap<>();
                // Default otel disabled
                properties.put("otel.sdk.disabled", "true");
                for (String propertyName : config.getPropertyNames()) {
                    if (propertyName.startsWith("otel.") || propertyName.startsWith("OTEL_")) {
                        config.getOptionalValue(propertyName, String.class).ifPresent(
                                value -> properties.put(propertyName, value));
                    }
                }
                return properties;
            }
        };
    }
}
