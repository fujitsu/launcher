/*
 * Copyright (c) 2019-2022 Fujitsu Limited and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package com.fujitsu.launcher.microprofile.openapi;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import io.smallrye.openapi.api.OpenApiDocument;
import io.smallrye.openapi.runtime.io.Format;
import io.smallrye.openapi.runtime.io.OpenApiSerializer;

import org.glassfish.jersey.message.internal.AcceptableMediaType;
import org.glassfish.jersey.message.internal.HttpHeaderReader;

import java.text.ParseException;

import jakarta.ws.rs.core.MediaType;

/**
 * 
 * @author Takahiro Nagao
 * @author Katsuhiro Kunisada
 * @author Koki Kosaka
 */
@SuppressWarnings("serial")
@WebServlet
public class OpenApiServlet extends HttpServlet {
    public static final Map<Format, String> ACCEPTED_TYPES = new HashMap<>();

    static {
        ACCEPTED_TYPES.put(Format.YAML, MediaType.TEXT_PLAIN);
        ACCEPTED_TYPES.put(Format.JSON, MediaType.APPLICATION_JSON);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Format format = getResponseFormat(request);
        if (format != null) {
            String oai = OpenApiSerializer.serialize(OpenApiDocument.INSTANCE.get(), format);
            response.setContentType(ACCEPTED_TYPES.get(format));
            response.getWriter().write(oai);
            response.setStatus(HttpServletResponse.SC_OK);
        } else {
            response.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE);
        }
    }

    protected Format getResponseFormat(HttpServletRequest request) {
        try {
            Format format = parseFormatQueryParameter(request);
            if (format != null) return format;
            return parseAcceptHeader(request);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private Format parseFormatQueryParameter(HttpServletRequest request) {
        String formatParam = request.getParameter("format");
        if (formatParam != null) {
            for (Format f : Format.values()) {
                if (f.name().equalsIgnoreCase(formatParam)) return f;
            }
        }
        return null;
    }

    private Format parseAcceptHeader(HttpServletRequest request) throws ParseException {
        String acceptHeader = request.getHeader("Accept");
        // sorted by quality value
        List<AcceptableMediaType> mediaTypes = HttpHeaderReader.readAcceptMediaType(acceptHeader);
        if (mediaTypes.isEmpty()) {
            mediaTypes.add(AcceptableMediaType.valueOf(MediaType.WILDCARD_TYPE));
        }

        for (AcceptableMediaType mediaType : mediaTypes) {
            if (mediaType.isCompatible(MediaType.TEXT_PLAIN_TYPE)) {
                return Format.YAML;
            }
            if (mediaType.isCompatible(MediaType.APPLICATION_JSON_TYPE)) {
                return Format.JSON;
            }
        }
        return null;
    }
}
