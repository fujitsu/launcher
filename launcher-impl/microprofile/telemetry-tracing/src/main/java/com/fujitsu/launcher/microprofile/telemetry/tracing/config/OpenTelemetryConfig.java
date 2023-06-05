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
package com.fujitsu.launcher.microprofile.telemetry.tracing.config;

import java.util.Map;
import java.util.Optional;

public interface OpenTelemetryConfig {
    // TODO Get from glassfish-version.properties
    static String INSTRUMENTATION_NAME = "Launcher";

    String INSTRUMENTATION_VERSION = Optional.of("5.0").orElse("SNAPSHOT");

    Map<String, String> properties();
}