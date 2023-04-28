package com.fujitsu.launcher.microprofile.telemetry.tracing.rest;

import jakarta.ws.rs.core.FeatureContext;
import org.glassfish.jersey.internal.spi.AutoDiscoverable;

public class OpenTelemetryAutoDiscoverable implements AutoDiscoverable {
    @Override
    public void configure(final FeatureContext context) {
        if (!context.getConfiguration().isRegistered(LauncherTracingDynamicFeature.class)) {
            context.register(LauncherTracingDynamicFeature.class);
        }
    }
}
