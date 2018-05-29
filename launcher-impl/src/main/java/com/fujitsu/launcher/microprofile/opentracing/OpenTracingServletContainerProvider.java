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

import java.util.Set;

import javax.servlet.FilterRegistration.Dynamic;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.internal.spi.ServletContainerProvider;

import io.opentracing.contrib.jaxrs2.server.SpanFinishingFilter;

/**
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
        Dynamic dynamic = servletContext.addFilter("SpanFinishingFilter", SpanFinishingFilter.class);
        dynamic.addMappingForUrlPatterns(null, false, "/*");
        dynamic.setAsyncSupported(true);
    }

    @Override
    public void configure(final ResourceConfig resourceConfig) throws ServletException {

    }
}
