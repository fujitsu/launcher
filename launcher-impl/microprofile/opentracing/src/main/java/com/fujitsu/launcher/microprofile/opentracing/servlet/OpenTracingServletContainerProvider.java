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
package com.fujitsu.launcher.microprofile.opentracing.servlet;

import java.util.Set;

import javax.servlet.FilterRegistration.Dynamic;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.internal.spi.ServletContainerProvider;

import io.opentracing.contrib.jaxrs2.server.SpanFinishingFilter;

/**
 * Servlet container provider for adding {@link SpanFinishingFilter}.
 *
 * @author Takahiro Nagao
 */
public class OpenTracingServletContainerProvider implements ServletContainerProvider {

    @Override
    public void preInit(final ServletContext servletContext, final Set<Class<?>> classes) throws ServletException {
    }

    @Override
    public void postInit(final ServletContext servletContext, final Set<Class<?>> classes,
            final Set<String> servletNames) throws ServletException {
    }

    @Override
    public void onRegister(final ServletContext servletContext, final Set<String> servletNames)
            throws ServletException {
        Dynamic dynamic = servletContext.addFilter(SpanFinishingFilter.class.getName(), SpanFinishingFilter.class);

        if (dynamic != null) {
            dynamic.addMappingForUrlPatterns(null, false, "/*");
            dynamic.setAsyncSupported(true);
        }
    }

    @Override
    public void configure(final ResourceConfig resourceConfig) throws ServletException {
    }
}
