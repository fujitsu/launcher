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

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;

import io.opentracing.SpanContext;
import io.opentracing.contrib.jaxrs2.client.TracingProperties;

/**
 * This class is a {@link ClientRequestFilter} for opentracing in order to propagate a span context to child spans.
 * It's vital when handling asynchronous client request.
 *
 * <p>Without this filter, asynchronous client requests cannot retrieve parent span context information,
 * since active span sources based on thread-local storage (e.g. {@link ThreadLocalActiveSpanSource})
 * cannot properly provide active spans to asynchronous client requests,
 * which are executed on threads different from ones where the active spans are registered.
 * 
 * @author Takahiro Nagao
 */
public class SpanContextPropagationFilter implements ClientRequestFilter {

    private SpanContext parentSpanContext;

    public SpanContextPropagationFilter(SpanContext parentSpanContext) {
        this.parentSpanContext = parentSpanContext;
    }

    /**
     * Sets the {@link TracingProperties.CHILD_OF} property to the parent span context if it is not set.
     * So that this is called before {@link ClientTracingFilter},
     * ensure that this filter is registered with a priority higher than that of {@link ClientTracingFilter}.
     */
    @Override
    public void filter(ClientRequestContext requestContext) {
        if (requestContext.getProperty(TracingProperties.CHILD_OF) == null) {
            requestContext.setProperty(TracingProperties.CHILD_OF, parentSpanContext);
        }
    }
}
