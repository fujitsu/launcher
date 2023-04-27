/*
 * Copyright (c) 2019-2023 Fujitsu Limited and/or its affiliates. All rights
 * reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package com.fujitsu.launcher.microprofile.health;

import io.smallrye.health.AsyncHealthCheckFactory;
import io.smallrye.health.registry.HealthRegistries;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;

/**
 * Enables {@link HealthReporterProducer}.
 *
 * @author Takahiro Nagao
 */
public class HealthExtension implements Extension {

    public void beforeBeanDiscovery(@Observes BeforeBeanDiscovery bbd, BeanManager bm) {
        bbd.addAnnotatedType(bm.createAnnotatedType(AsyncHealthCheckFactory.class), AsyncHealthCheckFactory.class.getName());
        bbd.addAnnotatedType(bm.createAnnotatedType(HealthRegistries.class), HealthRegistries.class.getName());
        bbd.addAnnotatedType(bm.createAnnotatedType(HealthReporterProducer.class), HealthReporterProducer.class.getName());
    }
}