/*
 * Copyright (c) 2019 Fujitsu Limited and/or its affiliates. All rights
 * reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * This file incorporates work authored by SmallRye OpenTracing,
 * licensed under the Apache License, Version 2.0, which is available at
 * http://www.apache.org/licenses/LICENSE-2.0.
 */
package com.fujitsu.launcher.microprofile.opentracing.rs;

import org.eclipse.microprofile.opentracing.Traced;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.spi.RestClientListener;

import com.fujitsu.launcher.microprofile.opentracing.cdi.TracerProducer;

import io.opentracing.Tracer;
import io.smallrye.opentracing.OpenTracingAsyncInterceptorFactory;
import io.smallrye.opentracing.SmallRyeClientTracingFeature;

/**
 * Patched version of {@link io.smallrye.opentracing.SmallRyeRestClientListener}.
 * 
 * @author Pavol Loffay (original)
 * @author Takahiro Nagao (patched)
 */
public class LauncherRestClientListener implements RestClientListener {

    @Override
    public void onNewClient(Class<?> clientInterface, RestClientBuilder restClientBuilder) {
        Traced traced = clientInterface.getAnnotation(Traced.class);
        if (traced != null && !traced.value()) {
            // tracing is disabled
            return;
        }

        Tracer tracer = TracerProducer.getTracer();
        restClientBuilder.register(new SmallRyeClientTracingFeature(tracer));
        restClientBuilder.register(new OpenTracingAsyncInterceptorFactory(tracer));
    }
}
