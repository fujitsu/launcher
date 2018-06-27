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

import java.lang.reflect.Method;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.ws.rs.Path;

import org.eclipse.microprofile.opentracing.Traced;

import io.opentracing.ActiveSpan;
import io.opentracing.Tracer;
import io.opentracing.Tracer.SpanBuilder;

/**
 * A CDI interceptor for tracing method invocations, excluding JAX-RS endpoints.
 * 
 * @author Takahiro Nagao
 */
@Interceptor
@Traced
@Priority(Interceptor.Priority.LIBRARY_BEFORE)
public class OpenTracingInterceptor {

    @Inject
    private Tracer tracer;

    @AroundInvoke
    public Object traceAround(InvocationContext ic) throws Exception {
        Method method = ic.getMethod();

        if (toTrace(method)) {
            SpanBuilder spanBuilder = tracer.buildSpan(operationNameFor(method));
            try (ActiveSpan span = spanBuilder.startActive()) {
                return ic.proceed();
            }
        } else {
            return ic.proceed();
        }
    }

    private boolean toTrace(Method method) {
        Class<?> clazz = method.getDeclaringClass();

        // exclude JAX-RS endpoints
        if (method.isAnnotationPresent(Path.class)) {
            return false;
        }

        if (method.isAnnotationPresent(Traced.class)) {
            return method.getAnnotation(Traced.class).value();
        }

        if (clazz.isAnnotationPresent(Traced.class)) {
            return clazz.getAnnotation(Traced.class).value();
        }

        return false;
    }

    private String operationNameFor(Method method) {
        Class<?> clazz = method.getDeclaringClass();

        if (method.isAnnotationPresent(Traced.class)) {
            String operationName = method.getAnnotation(Traced.class).operationName();
            if (operationName != null && !operationName.isEmpty()) {
                return operationName;
            }
        }

        if (clazz.isAnnotationPresent(Traced.class)) {
            String operationName = clazz.getAnnotation(Traced.class).operationName();
            if (operationName != null && !operationName.isEmpty()) {
                return operationName;
            }
        }

        return clazz.getName() + "." + method.getName();
    }
}
