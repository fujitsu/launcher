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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.enterprise.inject.spi.Unmanaged;

import io.smallrye.health.SmallRyeHealthReporter;

/**
 * Produces an instance of {@link SmallRyeHealthReporter}.
 * 
 * @author Takahiro Nagao
 */
@ApplicationScoped
public class HealthReporterProducer {

    private SmallRyeHealthReporter reporter;
    private Unmanaged.UnmanagedInstance<SmallRyeHealthReporter> reporterInstance;

    @Produces
    @ApplicationScoped
    public SmallRyeHealthReporter getReporter() {
        if (reporter == null) {
            BeanManager bm = CDI.current().getBeanManager();
            Unmanaged<SmallRyeHealthReporter> unmanagedReporter = new Unmanaged<>(bm, SmallRyeHealthReporter.class);
            reporterInstance = unmanagedReporter.newInstance();
            reporter = reporterInstance.produce().inject().postConstruct().get();
        }
        return reporter;
    }

    public void disposeReporter(@Disposes SmallRyeHealthReporter reporter) {
        if (reporterInstance != null) {
            reporterInstance.preDestroy().dispose();
            reporterInstance = null;
        }
    }
}
