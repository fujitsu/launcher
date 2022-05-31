/*
 * Copyright (c) 2019-2022 Fujitsu Limited and/or its affiliates. All rights
 * reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package com.fujitsu.launcher.microprofile.health;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import io.smallrye.health.SmallRyeHealth;
import io.smallrye.health.SmallRyeHealthReporter;

/**
 * Provides the health check endpoints.
 *
 * @author Takahiro Nagao
 */
@WebServlet
public class HealthServlet extends HttpServlet {

    public static final String HEALTH = "/health";
    public static final String HEALTH_LIVE = "/health/live";
    public static final String HEALTH_READY = "/health/ready";
    public static final String HEALTH_STARTED = "/health/started";

    @Inject
    private SmallRyeHealthReporter reporter;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    private void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (reporter == null) {
            Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Health reporter has not been prepared");
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return;
        }

        String path = request.getRequestURI();

        if (HEALTH.equals(path)) {
            report(response, reporter.getHealth());
        } else if (HEALTH_LIVE.equals(path)) {
            report(response, reporter.getLiveness());
        } else if (HEALTH_READY.equals(path)) {
            report(response, reporter.getReadiness());
        } else if (HEALTH_STARTED.equals(path)) {
            report(response, reporter.getStartup());
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("Endpoint " + path + " not found");
        }
    }

    private void report(HttpServletResponse response, SmallRyeHealth health) throws ServletException, IOException {
        if (health.isDown()) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        }
        reporter.reportHealth(response.getOutputStream(), health);
    }
}
