/*
 * Copyright (c) 2018-2022 Fujitsu Limited and/or its affiliates. All rights
 * reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package com.fujitsu.launcher.microprofile.opentracing.rs;

import io.smallrye.opentracing.contrib.jaxrs2.client.ClientTracingFilter;
import io.smallrye.opentracing.contrib.jaxrs2.client.TracingProperties;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;

import io.opentracing.SpanContext;

/**
 * Client request filter to propagate a span context to child spans.
 * Required when handling asynchronous client requests.
 *
 * <p>This filter should be registered with a priority higher than that of {@link ClientTracingFilter}.
 * 
 * @author Takahiro Nagao
 */
public class SpanContextPropagationFilter implements ClientRequestFilter {

    private SpanContext parentSpanContext;

    public SpanContextPropagationFilter(SpanContext parentSpanContext) {
        this.parentSpanContext = parentSpanContext;
    }

    @Override
    public void filter(ClientRequestContext requestContext) {
        if (requestContext.getProperty(TracingProperties.CHILD_OF) == null) {
            requestContext.setProperty(TracingProperties.CHILD_OF, parentSpanContext);
        }
    }
}
