/*
 * Copyright (c) 2021-2022 Fujitsu Limited and/or its affiliates. All rights
 * reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package com.fujitsu.launcher.microprofile.faulttolerance;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;

public class ProviderExtension implements Extension {

    public void beforeBeanDiscovery(@Observes BeforeBeanDiscovery bbd, BeanManager bm) {
            bbd.addAnnotatedType(bm.createAnnotatedType(IntegrateAsyncExecutorProvider.class),
                    IntegrateAsyncExecutorProvider.class.getName());
            bbd.addAnnotatedType(bm.createAnnotatedType(ThreadFactoryProvider.class),
                    ThreadFactoryProvider.class.getName());
    }
}