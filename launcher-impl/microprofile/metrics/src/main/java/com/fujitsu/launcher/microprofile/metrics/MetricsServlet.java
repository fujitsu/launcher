/*
 * Copyright (c) 2019 Fujitsu Limited and/or its affiliates. All rights
 * reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package com.fujitsu.launcher.microprofile.metrics;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.smallrye.metrics.MetricsRequestHandler;
import io.smallrye.metrics.MetricsRequestHandler.Responder;

@WebServlet
public class MetricsServlet extends HttpServlet {

    @Inject
    private MetricsRequestHandler handler;

    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response) throws IOException {
        processRequest(request, response);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        processRequest(request, response);
    }

    private void processRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (handler == null) {
            Logger.getLogger(this.getClass().getName()).log(Level.INFO, "No metrics request handler is installed.");
            return;
        }

        Stream<String> acceptHeaders = Collections.list(request.getHeaders("Accept")).stream();

        Responder responder = new Responder() {
            public void respondWith(int status, String message, Map<String, String> headers) throws IOException {
                headers.forEach(response::addHeader);
                response.getWriter().write(message);
                response.setStatus(status);
            }
        };

        handler.handleRequest(request.getRequestURI(), request.getMethod(), acceptHeaders, responder);
    }
}