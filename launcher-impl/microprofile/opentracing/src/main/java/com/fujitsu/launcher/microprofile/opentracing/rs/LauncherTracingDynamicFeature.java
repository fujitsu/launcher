/*
 * Copyright (c) 2019 Fujitsu Limited and/or its affiliates. All rights
 * reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0

 * This file incorporates work authored by SmallRye OpenTracing,
 * licensed under the Apache License, Version 2.0, which is available at
 * http://www.apache.org/licenses/LICENSE-2.0.
 */
package com.fujitsu.launcher.microprofile.opentracing.rs;

import java.util.Optional;
import java.util.logging.Logger;

import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import com.fujitsu.launcher.microprofile.opentracing.cdi.TracerProducer;

import io.opentracing.Tracer;
import io.opentracing.contrib.jaxrs2.server.OperationNameProvider.ClassNameOperationName;
import io.opentracing.contrib.jaxrs2.server.OperationNameProvider.WildcardOperationName;
import io.opentracing.contrib.jaxrs2.server.ServerTracingDynamicFeature;
import io.opentracing.contrib.jaxrs2.server.ServerTracingDynamicFeature.Builder;

/**
 * Patched version of {@link io.smallrye.opentracing.SmallRyeTracingDynamicFeature}.
 * 
 * @author Pavol Loffay (original)
 * @author Takahiro Nagao (patched)
 */
@Provider
public class LauncherTracingDynamicFeature implements DynamicFeature {

    private static final Logger logger = Logger.getLogger(LauncherTracingDynamicFeature.class.getName());

    private final ServerTracingDynamicFeature delegate;

    public LauncherTracingDynamicFeature() {
        Tracer tracer = TracerProducer.getTracer();
        Config config = ConfigProvider.getConfig();
        Optional<String> skipPattern = config.getOptionalValue("mp.opentracing.server.skip-pattern", String.class);
        Optional<String> operationNameProvider = config.getOptionalValue("mp.opentracing.server.operation-name-provider",
                String.class);

        Builder builder = new Builder(tracer)
                .withOperationNameProvider(ClassNameOperationName.newBuilder())
                .withTraceSerialization(false);
        if (skipPattern.isPresent()) {
            builder.withSkipPattern(skipPattern.get());
        }
        if (operationNameProvider.isPresent()) {
            if ("http-path".equalsIgnoreCase(operationNameProvider.get())) {
                builder.withOperationNameProvider(WildcardOperationName.newBuilder());
            } else if (!"class-method".equalsIgnoreCase(operationNameProvider.get())) {
                logger.warning("Provided operation name does not match http-path or class-method. Using default class-method.");
            }
        }
        this.delegate = builder.build();
    }

    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {
        this.delegate.configure(resourceInfo, context);
    }
}
