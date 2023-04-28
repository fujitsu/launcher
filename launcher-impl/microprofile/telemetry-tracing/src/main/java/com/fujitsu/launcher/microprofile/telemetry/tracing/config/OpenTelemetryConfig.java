package com.fujitsu.launcher.microprofile.telemetry.tracing.config;

import java.util.Map;
import java.util.Optional;

public interface OpenTelemetryConfig {
    static String INSTRUMENTATION_NAME = "com.fujitsu.launcher";

    String INSTRUMENTATION_VERSION = Optional.ofNullable(OpenTelemetryConfig.class.getPackage().getImplementationVersion())
            .orElse("SNAPSHOT");

    Map<String, String> properties();
}