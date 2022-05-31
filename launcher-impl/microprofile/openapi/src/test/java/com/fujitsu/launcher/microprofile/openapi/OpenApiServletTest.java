/*
 * Copyright (c) 2021-2022 Fujitsu Limited and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.fujitsu.launcher.microprofile.openapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.stream.Stream;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import io.smallrye.openapi.runtime.io.Format;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class OpenApiServletTest {

    HttpServletRequest mockRequest = mock(HttpServletRequest.class);
    HttpServletResponse mockResponse = mock(HttpServletResponse.class);

    OpenApiServlet servlet = spy(new OpenApiServlet());

    @ParameterizedTest
    @MethodSource({"provideOnlyAcceptHeader", "provideOnlyFormatQuery", "provideFormatQueryAndAcceptHeader"})
    public void testGetResponseFormat(String format, String acceptHeader, Format expected) {
        when(mockRequest.getParameter("format")).thenReturn(format);
        when(mockRequest.getHeader("Accept")).thenReturn(acceptHeader);
        Format result = servlet.getResponseFormat(mockRequest);
        assertEquals(result, expected);
    }

    private static Stream<Arguments> provideOnlyAcceptHeader() {
        return Stream.of(
                // one value
                Arguments.of(null, "", Format.YAML),
                Arguments.of(null, "text/plain", Format.YAML),
                Arguments.of(null, "application/json", Format.JSON),
                Arguments.of(null, "*/*", Format.YAML),
                Arguments.of(null, "text/*", Format.YAML),
                Arguments.of(null, "application/*", Format.JSON),
                Arguments.of(null, "hoge/hoge", null),

                // two value
                Arguments.of(null, "text/plain, application/json", Format.YAML),
                Arguments.of(null, "application/json, text/plain", Format.JSON),
                Arguments.of(null, "text/plain;q=0.9, application/json", Format.JSON),
                Arguments.of(null, "application/json;q=0.9, text/plain", Format.YAML),

                Arguments.of(null, "*/*, application/json", Format.JSON),
                Arguments.of(null, "text/*, application/json", Format.JSON),
                Arguments.of(null, "application/*, text/plain", Format.YAML),
                Arguments.of(null, "text/*, application/json;q=0.9", Format.YAML),
                Arguments.of(null, "application/*, text/plain;q=0.9", Format.JSON),

                // three value
                Arguments.of(null, "*/*, application/*, text/plain", Format.YAML),
                Arguments.of(null, "*/*, application/*, text/plain;q=0.9", Format.JSON),
                Arguments.of(null, "*/*, application/*;q=0.9, text/plain;q=0.8", Format.YAML)
        );
    }

    private static Stream<Arguments> provideOnlyFormatQuery() {
        return Stream.of(
                // one value
                Arguments.of("YAML", null, Format.YAML),
                Arguments.of("JSON", null, Format.JSON),
                Arguments.of("yaml", null, Format.YAML),
                Arguments.of("json", null, Format.JSON),
                Arguments.of("hoge", null, Format.YAML),
                Arguments.of(null, null, Format.YAML)
        );
    }

    private static Stream<Arguments> provideFormatQueryAndAcceptHeader() {
        return Stream.of(
                Arguments.of("YAML", "application/json", Format.YAML),
                Arguments.of("JSON", "text/plain", Format.JSON),
                Arguments.of("hoge", "application/json", Format.JSON)
        );
    }

    @Test
    public void testSendError406() throws IOException {
        doReturn(null).when(servlet).getResponseFormat(mockRequest);
        servlet.doGet(mockRequest, mockResponse);
        verify(mockResponse, times(1)).sendError(406);
    }

    @Test
    public void testThrowRuntimeException() {
        when(mockRequest.getParameter("format")).thenReturn(null);
        when(mockRequest.getHeader("Accept")).thenReturn("application/json:q=0.4"); //colon
        assertThrows(RuntimeException.class, () -> servlet.getResponseFormat(mockRequest));
    }

}
