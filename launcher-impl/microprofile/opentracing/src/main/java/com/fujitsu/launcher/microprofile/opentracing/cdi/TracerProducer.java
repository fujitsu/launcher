/*
 * Copyright (c) 2018-2021 Fujitsu Limited and/or its affiliates. All rights
 * reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package com.fujitsu.launcher.microprofile.opentracing.cdi;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import io.opentracing.Tracer;
import io.opentracing.contrib.tracerresolver.TracerResolver;
import io.opentracing.util.GlobalTracer;

/**
 * A producer of the singleton {@link Tracer} for CDI.
 * The tracer is obtained via {@link TracerResolver}.
 *
 * @author Tsuyoshi Yoshitomi
 */
@ApplicationScoped
public class TracerProducer {

    private static Tracer tracer;

    @Produces
    @Singleton
    public static Tracer getTracer() {

        if (tracer != null) {
            return tracer;
        }

        tracer = TracerResolver.resolveTracer();

        if (tracer == null) {
            tracer = GlobalTracer.get(); // defaults to noop tracer
        }

        Logger.getLogger(TracerProducer.class.getName()).log(Level.INFO, "Registering tracer {0}",
                tracer.getClass().getName());
        GlobalTracer.registerIfAbsent(tracer);

        return tracer;
    }
}
