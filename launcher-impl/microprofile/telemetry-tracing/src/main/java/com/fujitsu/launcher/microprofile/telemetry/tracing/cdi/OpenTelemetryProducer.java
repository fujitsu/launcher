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
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdkBuilder;
import io.opentelemetry.sdk.common.CompletableResultCode;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Singleton
public class OpenTelemetryProducer {
    @Inject
    OpenTelemetryConfig config;

    @Produces
    @Singleton
    public OpenTelemetry getOpenTelemetry() {
        AutoConfiguredOpenTelemetrySdkBuilder builder = AutoConfiguredOpenTelemetrySdk.builder();

        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        if (contextClassLoader != null) {
            builder.setServiceClassLoader(contextClassLoader);
        }

        return builder
                .setResultAsGlobal(false)
                .registerShutdownHook(false)
                .addPropertiesSupplier(() -> config.properties())
                .build()
                .getOpenTelemetrySdk();
    }

    @Produces
    @Singleton
    public Tracer getTracer() {
        return CDI.current().select(OpenTelemetry.class).get().getTracer(OpenTelemetryConfig.INSTRUMENTATION_NAME);
    }

    @Produces
    @RequestScoped
    public Span getSpan() {
        return Span.current();
    }

    @Produces
    @RequestScoped
    public Baggage getBaggage() {
        return Baggage.current();
    }

    void close(@Disposes final OpenTelemetry openTelemetry) {
        OpenTelemetrySdk openTelemetrySdk = (OpenTelemetrySdk) openTelemetry;
        List<CompletableResultCode> shutdown = new ArrayList<>();
        shutdown.add(openTelemetrySdk.getSdkTracerProvider().shutdown());
        shutdown.add(openTelemetrySdk.getSdkMeterProvider().shutdown());
        shutdown.add(openTelemetrySdk.getSdkLoggerProvider().shutdown());
        CompletableResultCode.ofAll(shutdown).join(10, TimeUnit.SECONDS);
    }
}
