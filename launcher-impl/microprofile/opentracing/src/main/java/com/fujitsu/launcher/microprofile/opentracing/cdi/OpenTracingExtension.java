/*
 * Copyright (c) 2018-2019 Fujitsu Limited and/or its affiliates. All rights
 * reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package com.fujitsu.launcher.microprofile.opentracing.cdi;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;

import io.opentracing.contrib.interceptors.OpenTracingInterceptor;

/**
 * A CDI extension for registering producers and interceptors for opentracing.
 *
 * @author Tsuyoshi Yoshitomi
 */
public class OpenTracingExtension implements Extension {

    public void beforeBeanDiscovery(@Observes BeforeBeanDiscovery bbd, BeanManager bm) {
        bbd.addAnnotatedType(bm.createAnnotatedType(TracerProducer.class), TracerProducer.class.getName());
        bbd.addAnnotatedType(bm.createAnnotatedType(OpenTracingInterceptor.class), OpenTracingInterceptor.class.getName());
    }
}
