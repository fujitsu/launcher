/*
 * Copyright (c) 2019-2022 Fujitsu Limited and/or its affiliates. All rights
 * reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package com.fujitsu.launcher.microprofile.health;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;

import io.smallrye.health.AsyncHealthCheckFactory;
import io.smallrye.health.registry.LivenessHealthRegistry;
import io.smallrye.health.registry.ReadinessHealthRegistry;
import io.smallrye.health.registry.StartupHealthRegistry;
import io.smallrye.health.registry.WellnessHealthRegistry;

/**
 * Enables {@link HealthReporterProducer}.
 *
 * @author Takahiro Nagao
 */
public class HealthExtension implements Extension {

    public void beforeBeanDiscovery(@Observes BeforeBeanDiscovery bbd, BeanManager bm) {
        bbd.addAnnotatedType(bm.createAnnotatedType(AsyncHealthCheckFactory.class), AsyncHealthCheckFactory.class.getName());
        bbd.addAnnotatedType(bm.createAnnotatedType(LivenessHealthRegistry.class), LivenessHealthRegistry.class.getName());
        bbd.addAnnotatedType(bm.createAnnotatedType(ReadinessHealthRegistry.class), ReadinessHealthRegistry.class.getName());
        bbd.addAnnotatedType(bm.createAnnotatedType(StartupHealthRegistry.class), StartupHealthRegistry.class.getName());
        bbd.addAnnotatedType(bm.createAnnotatedType(WellnessHealthRegistry.class), WellnessHealthRegistry.class.getName());
        bbd.addAnnotatedType(bm.createAnnotatedType(HealthReporterProducer.class), HealthReporterProducer.class.getName());
    }
}
