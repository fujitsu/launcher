/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2018 Fujitsu Limited and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the term of the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/fujitsu/launcher/blob/master/LICENSE.txt
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]" 
 */
package com.fujitsu.launcher.microprofile.opentracing;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Priority;
import javax.ws.rs.core.FeatureContext;

import org.glassfish.jersey.internal.spi.AutoDiscoverable;

import io.opentracing.Tracer;
import io.opentracing.contrib.jaxrs2.client.ClientTracingFeature;
import io.opentracing.contrib.jaxrs2.server.OperationNameProvider;
import io.opentracing.contrib.jaxrs2.server.ServerTracingDynamicFeature;
import io.opentracing.util.GlobalTracer;

/**
 *
 * @author Takahiro Nagao
 */
@Priority(AutoDiscoverable.DEFAULT_PRIORITY)
public class OpenTracingAutoDiscoverable implements AutoDiscoverable {

    private static ServerTracingDynamicFeature serverTracingDynamicFeature;
    private static ClientTracingFeature clientTracingFeature;

    @Override
    public void configure(final FeatureContext context) {
        if (!GlobalTracer.isRegistered()) {
            Tracer tracer = TracerProducer.getTracer();
            if (tracer == null) {
                Logger.getLogger(this.getClass().getName()).log(Level.INFO, "No tracer of OpenTracing was found.");
                return;
            }
            Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Registering OpenTracing tracer, with {0}", tracer.getClass().getName());
    
            GlobalTracer.register(tracer);
        }

        if (GlobalTracer.isRegistered()) {
            if (serverTracingDynamicFeature == null || !context.getConfiguration().isRegistered(serverTracingDynamicFeature)) {
                serverTracingDynamicFeature =
                        new ServerTracingDynamicFeature
                        .Builder(GlobalTracer.get())
                        .withOperationNameProvider(OperationNameProvider.ClassNameOperationName.newBuilder())
                        .withTraceSerialization(false)
                        .build();
                context.register(serverTracingDynamicFeature);
            }

            if (clientTracingFeature == null || !context.getConfiguration().isRegistered(clientTracingFeature)) {
                clientTracingFeature = 
                        new ClientTracingFeature
                        .Builder(GlobalTracer.get())
                        .withTraceSerialization(false)
                        .build();
                context.register(clientTracingFeature);
            }
        }
    }
}
