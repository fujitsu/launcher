package com.fujitsu.launcher.microprofile.telemetry.tracing.rest;

import jakarta.enterprise.inject.spi.CDI;
import jakarta.ws.rs.container.DynamicFeature;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.FeatureContext;
import jakarta.ws.rs.ext.Provider;

@Provider
public class LauncherTracingDynamicFeature implements DynamicFeature {

    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext featureContext) {
        var serverFilter = CDI.current().select(OpenTelemetryServerFilter.class).get();
        if (serverFilter != null) {
            featureContext.register(serverFilter);
        }
        var clientFilter = CDI.current().select(OpenTelemetryClientFilter.class).get();
        if (clientFilter != null) {
            featureContext.register(clientFilter);
        }
    }
}
