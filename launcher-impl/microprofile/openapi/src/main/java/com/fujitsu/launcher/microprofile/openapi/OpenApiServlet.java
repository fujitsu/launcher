/*
 * Copyright (c) 2019 Fujitsu Limited and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package com.fujitsu.launcher.microprofile.openapi;

import java.io.IOException;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.smallrye.openapi.api.OpenApiDocument;
import io.smallrye.openapi.runtime.io.OpenApiSerializer;
import io.smallrye.openapi.runtime.io.OpenApiSerializer.Format;

/**
 * 
 * @author Takahiro Nagao
 * @author Katsuhiro Kunisada
 */
@SuppressWarnings("serial")
@WebServlet
public class OpenApiServlet extends HttpServlet {
    public static final String MEDIA_TYPE_YAML = "text/plain";
    public static final String MEDIA_TYPE_JSON = "application/json";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Format format = Format.YAML;
        String type = MEDIA_TYPE_YAML;

        if (isJson(request)) {
            format = Format.JSON;
            type = MEDIA_TYPE_JSON;
        }

        String oai = OpenApiSerializer.serialize(OpenApiDocument.INSTANCE.get(), format);
        response.getWriter().write(oai);
        response.setContentType(type);
        response.setStatus(200);
    }

    private boolean isJson(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.equals(MEDIA_TYPE_JSON)) {
            return true;
        }
        String formatParam = request.getParameter("format");
        if (formatParam != null && formatParam.equals("json")) {
            return true;
        }
        return false;
    }
}
