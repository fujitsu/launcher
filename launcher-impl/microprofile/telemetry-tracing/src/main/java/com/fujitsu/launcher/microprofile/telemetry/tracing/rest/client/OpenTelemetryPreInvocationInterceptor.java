/*
 * Copyright (c) 2023 Fujitsu Limited and/or its affiliates. All rights
 * reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package com.fujitsu.launcher.microprofile.telemetry.tracing.rest.client;

import io.opentelemetry.context.Context;
import jakarta.ws.rs.client.ClientRequestContext;
import org.glassfish.jersey.client.spi.PreInvocationInterceptor;

public class OpenTelemetryPreInvocationInterceptor implements PreInvocationInterceptor {
    @Override
    public void beforeRequest(ClientRequestContext clientRequestContext) {
        Context parentContext = Context.current();
        clientRequestContext.setProperty("otel.span.client.parentContext", parentContext);
    }
}
