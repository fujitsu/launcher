package com.fujitsu.launcher.microprofile.telemetry.tracing.rest;

import jakarta.enterprise.inject.spi.CDI;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.spi.RestClientListener;

public class LauncherRestClientListener implements RestClientListener {
    @Override
    public void onNewClient(Class<?> aClass, RestClientBuilder restClientBuilder) {
        var clientFilter = CDI.current().select(OpenTelemetryClientFilter.class).get();
        if (clientFilter != null) {
            restClientBuilder.register(clientFilter);
        }
    }
}
