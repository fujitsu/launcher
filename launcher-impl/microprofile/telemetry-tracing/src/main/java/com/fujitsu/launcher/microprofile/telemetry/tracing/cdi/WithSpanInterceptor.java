/*
 * Copyright (c) 2023 Fujitsu Limited and/or its affiliates. All rights
 * reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * This file incorporates work authored by SmallRye OpenTelemetry,
 * licensed under the Apache License, Version 2.0, which is available at
 * http://www.apache.org/licenses/LICENSE-2.0.
 */
package com.fujitsu.launcher.microprofile.telemetry.tracing.cdi;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.opentelemetry.instrumentation.api.annotation.support.MethodSpanAttributesExtractor;
import io.opentelemetry.instrumentation.api.annotation.support.ParameterAttributeNamesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.SpanNameExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.util.SpanNames;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import static com.fujitsu.launcher.microprofile.telemetry.tracing.config.OpenTelemetryConfig.INSTRUMENTATION_NAME;
import static com.fujitsu.launcher.microprofile.telemetry.tracing.config.OpenTelemetryConfig.INSTRUMENTATION_VERSION;

public class WithSpanInterceptor {
    private final Instrumenter<MethodRequest, Void> instrumenter;

    public WithSpanInterceptor(final OpenTelemetry openTelemetry) {
        InstrumenterBuilder<MethodRequest, Void> builder = Instrumenter.builder(openTelemetry, INSTRUMENTATION_NAME,
                new MethodRequestSpanNameExtractor());
        builder.setInstrumentationVersion(INSTRUMENTATION_VERSION);

        MethodSpanAttributesExtractor<MethodRequest, Void> attributesExtractor = MethodSpanAttributesExtractor.newInstance(
                MethodRequest::getMethod,
                new WithSpanParameterAttributeNamesExtractor(),
                MethodRequest::getArgs);

        this.instrumenter = builder.addAttributesExtractor(attributesExtractor)
                .buildInstrumenter(methodRequest -> spanKindFromMethod(methodRequest.getMethod()));
    }

    private static SpanKind spanKindFromMethod(Method method) {
        WithSpan annotation = method.getDeclaredAnnotation(WithSpan.class);
        if (annotation == null) {
            return SpanKind.INTERNAL;
        }
        return annotation.kind();
    }

    @AroundInvoke
    public Object span(final InvocationContext invocationContext) throws Exception {
        MethodRequest methodRequest = new MethodRequest(invocationContext.getMethod(), invocationContext.getParameters());

        Context parentContext = Context.current();
        Context spanContext = null;
        Scope scope = null;
        boolean shouldStart = instrumenter.shouldStart(parentContext, methodRequest);
        if (shouldStart) {
            spanContext = instrumenter.start(parentContext, methodRequest);
            scope = spanContext.makeCurrent();
        }

        try {
            Object result = invocationContext.proceed();

            if (shouldStart) {
                instrumenter.end(spanContext, methodRequest, null, null);
            }

            return result;
        } finally {
            if (scope != null) {
                scope.close();
            }
        }
    }

    private static final class MethodRequestSpanNameExtractor implements SpanNameExtractor<MethodRequest> {
        @Override
        public String extract(final MethodRequest methodRequest) {
            WithSpan annotation = methodRequest.getMethod().getDeclaredAnnotation(WithSpan.class);
            String spanName = annotation.value();
            if (spanName.isEmpty()) {
                spanName = SpanNames.fromMethod(methodRequest.getMethod());
            }
            return spanName;
        }
    }

    private static final class WithSpanParameterAttributeNamesExtractor implements ParameterAttributeNamesExtractor {
        private static String attributeName(Parameter parameter) {
            SpanAttribute spanAttribute = parameter.getDeclaredAnnotation(SpanAttribute.class);
            if (spanAttribute == null) {
                return null;
            }
            String value = spanAttribute.value();
            if (!value.isEmpty()) {
                return value;
            } else if (parameter.isNamePresent()) {
                return parameter.getName();
            } else {
                return null;
            }
        }

        @Override
        public String[] extract(final Method method, final Parameter[] parameters) {
            String[] attributeNames = new String[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                attributeNames[i] = attributeName(parameters[i]);
            }
            return attributeNames;
        }
    }
}
