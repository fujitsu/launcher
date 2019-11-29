/*
 * Copyright (c) 2019 Fujitsu Limited and/or its affiliates. All rights
 * reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package com.fujitsu.launcher.microprofile.opentracing.rs;

import java.util.concurrent.ExecutorService;

import javax.enterprise.inject.spi.CDI;
import javax.ws.rs.Priorities;
import javax.ws.rs.client.ClientBuilder;

import org.eclipse.microprofile.opentracing.ClientTracingRegistrarProvider;
import org.glassfish.jersey.client.JerseyClientBuilder;

import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.contrib.concurrent.TracedExecutorService;
import io.smallrye.opentracing.SmallRyeClientTracingFeature;

/**
 * Configures {@link JerseyClientBuilder} to enable client-side tracing.
 * 
 * @author Takahiro Nagao
 */
public class LauncherClientTracingRegistrarProvider implements ClientTracingRegistrarProvider {

    static final int CLIENT_TRACING_FILTER_PRIORITY = Priorities.HEADER_DECORATOR;
    static final int CONTEXT_PROPAGATION_FILTER_PRIORITY = Priorities.HEADER_DECORATOR - 1;

    @Override
    public ClientBuilder configure(ClientBuilder clientBuilder) {
        return configure(clientBuilder, null);
    }

    @Override
    public ClientBuilder configure(ClientBuilder clientBuilder, ExecutorService executorService) {
        if (clientBuilder instanceof JerseyClientBuilder) {
            JerseyClientBuilder jerseyClientBuilder = (JerseyClientBuilder) clientBuilder;
            Tracer tracer = CDI.current().select(Tracer.class).get();

            registerClientFeatures(jerseyClientBuilder::register, tracer);

            if (executorService != null) {
                jerseyClientBuilder.executorService(new TracedExecutorService(executorService, tracer));
            }

            return jerseyClientBuilder;
        } else {
            return clientBuilder;
        }
    }

    public static <T> void registerClientFeatures(Registrar<T> registrar, Tracer tracer) {
        Span activeSpan = tracer.activeSpan();

        registrar.register(new SmallRyeClientTracingFeature(tracer), CLIENT_TRACING_FILTER_PRIORITY);

        if (activeSpan != null) {
            // Register SpanContextPropagationFilter with priority higher than ClientTracingFilter
            // so that it is called before ClientTracingFilter.
            // Without this filter, asynchronous client requests cannot retrieve parent span context information,
            // since active span sources based on thread-local storage (e.g. ThreadLocalActiveSpanSource)
            // cannot properly provide active spans to asynchronous client requests,
            // which are executed on threads different from ones where the active spans are registered.
            SpanContext parentSpanContext = activeSpan.context();
            registrar.register(new SpanContextPropagationFilter(parentSpanContext),
                    CONTEXT_PROPAGATION_FILTER_PRIORITY);
        }
    }

    public static interface Registrar<T> {
        T register(Object component, int priority);
    }
}
