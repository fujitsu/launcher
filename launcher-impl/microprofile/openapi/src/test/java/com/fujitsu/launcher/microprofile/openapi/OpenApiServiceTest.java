/*
 * Copyright (c) 2023 Fujitsu Limited and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.fujitsu.launcher.microprofile.openapi;

import io.smallrye.openapi.api.OpenApiConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 *
 * @author Koki Kosaka
 */
public class OpenApiServiceTest {

    OpenApiService openApiService = spy(new OpenApiService());

    OpenApiConfig mockOpenApiConfig = mock(OpenApiConfig.class);

    String dummyPackage = "com.example";
    String dummyPackageA = dummyPackage + ".aaa";
    String dummyPackageB = dummyPackageA + ".bbb";
    String dummyPackageC = dummyPackageB + ".ccc";
    String dummyClassA = dummyPackageA + ".ClassA";
    String dummyClassB = dummyPackageB + ".ClassB";
    String dummyClassC = dummyPackageC + ".ClassC";

    String dummyPackageX = dummyPackage + ".xxx";
    String dummyPackageY = dummyPackageX + ".yyy";
    String dummyClassX = dummyPackageX + ".ClassX";
    String dummyClassY = dummyPackageY + ".ClassY";


    static final boolean SCAN = true;
    static final boolean NOT_SCAN = false;

    @BeforeEach
    public void cleanMock() {
        when(mockOpenApiConfig.scanClasses()).thenReturn(Set.of());
        when(mockOpenApiConfig.scanPackages()).thenReturn(Set.of());
        when(mockOpenApiConfig.scanExcludeClasses()).thenReturn(Set.of());
        when(mockOpenApiConfig.scanExcludePackages()).thenReturn(Set.of());
    }

    @Test
    public void testNullOrEmptyEntry() {
        verify(null, NOT_SCAN);
        verify("", NOT_SCAN);
    }

    @Test
    public void testNotSetScanProperties() {
        verify(getEntry(dummyClassA), SCAN);
        verify(getEntry(dummyClassB), SCAN);
        verify(getEntry(dummyClassC), SCAN);
        verify(getEntry(dummyClassX), SCAN);
        verify(getEntry(dummyClassY), SCAN);
    }

    @Test
    public void testScanPackages() {
        when(mockOpenApiConfig.scanPackages()).thenReturn(Set.of(dummyPackageB));

        verify(getEntry(dummyClassA), NOT_SCAN);
        verify(getEntry(dummyClassB), SCAN);
        verify(getEntry(dummyClassC), SCAN);
        verify(getEntry(dummyClassX), NOT_SCAN);
        verify(getEntry(dummyClassY), NOT_SCAN);
    }

    @Test
    public void testScanPackagesMultiple_SubPkg() {
        when(mockOpenApiConfig.scanPackages()).thenReturn(Set.of(dummyPackageB, dummyPackageC));

        verify(getEntry(dummyClassA), NOT_SCAN);
        verify(getEntry(dummyClassB), SCAN);
        verify(getEntry(dummyClassC), SCAN);
        verify(getEntry(dummyClassX), NOT_SCAN);
        verify(getEntry(dummyClassY), NOT_SCAN);
    }


    @Test
    public void testScanPackagesMultiple_OtherPkg() {
        when(mockOpenApiConfig.scanPackages()).thenReturn(Set.of(dummyPackageB, dummyPackageY));

        verify(getEntry(dummyClassA), NOT_SCAN);
        verify(getEntry(dummyClassB), SCAN);
        verify(getEntry(dummyClassC), SCAN);
        verify(getEntry(dummyClassX), NOT_SCAN);
        verify(getEntry(dummyClassY), SCAN);
    }

    @Test
    public void testScanExcludePackages() {
        when(mockOpenApiConfig.scanExcludePackages()).thenReturn(Set.of(dummyPackageB));

        verify(getEntry(dummyClassA), SCAN);
        verify(getEntry(dummyClassB), NOT_SCAN);
        verify(getEntry(dummyClassC), NOT_SCAN);
        verify(getEntry(dummyClassX), SCAN);
        verify(getEntry(dummyClassY), SCAN);
    }

    @Test
    public void testScanExcludePackagesMultiple_SubPkg() {
        when(mockOpenApiConfig.scanExcludePackages()).thenReturn(Set.of(dummyPackageB, dummyPackageC));

        verify(getEntry(dummyClassA), SCAN);
        verify(getEntry(dummyClassB), NOT_SCAN);
        verify(getEntry(dummyClassC), NOT_SCAN);
        verify(getEntry(dummyClassX), SCAN);
        verify(getEntry(dummyClassY), SCAN);
    }

    @Test
    public void testScanExcludePackagesMultiple_OtherPkg() {
        when(mockOpenApiConfig.scanExcludePackages()).thenReturn(Set.of(dummyPackageB, dummyPackageY));

        verify(getEntry(dummyClassA), SCAN);
        verify(getEntry(dummyClassB), NOT_SCAN);
        verify(getEntry(dummyClassC), NOT_SCAN);
        verify(getEntry(dummyClassX), SCAN);
        verify(getEntry(dummyClassY), NOT_SCAN);
    }

    @Test
    public void testScanClasses() {
        when(mockOpenApiConfig.scanClasses()).thenReturn(Set.of(dummyClassB));

        verify(getEntry(dummyClassA), NOT_SCAN);
        verify(getEntry(dummyClassB), SCAN);
        verify(getEntry(dummyClassC), NOT_SCAN);
        verify(getEntry(dummyClassX), NOT_SCAN);
        verify(getEntry(dummyClassY), NOT_SCAN);
    }

    @Test
    public void testScanClassesMultiple() {
        when(mockOpenApiConfig.scanClasses()).thenReturn(Set.of(dummyClassB, dummyClassX));

        verify(getEntry(dummyClassA), NOT_SCAN);
        verify(getEntry(dummyClassB), SCAN);
        verify(getEntry(dummyClassC), NOT_SCAN);
        verify(getEntry(dummyClassX), SCAN);
        verify(getEntry(dummyClassY), NOT_SCAN);
    }

    @Test
    public void testScanExcludeClasses() {
        when(mockOpenApiConfig.scanExcludeClasses()).thenReturn(Set.of(dummyClassB));

        verify(getEntry(dummyClassA), SCAN);
        verify(getEntry(dummyClassB), NOT_SCAN);
        verify(getEntry(dummyClassC), SCAN);
        verify(getEntry(dummyClassX), SCAN);
        verify(getEntry(dummyClassY), SCAN);
    }

    @Test
    public void testScanExcludeClassesMultiple() {
        when(mockOpenApiConfig.scanExcludeClasses()).thenReturn(Set.of(dummyClassB, dummyClassX));

        verify(getEntry(dummyClassA), SCAN);
        verify(getEntry(dummyClassB), NOT_SCAN);
        verify(getEntry(dummyClassC), SCAN);
        verify(getEntry(dummyClassX), NOT_SCAN);
        verify(getEntry(dummyClassY), SCAN);
    }

    @Test
    public void testSpecifyScanPackagesAndScanExcludePackages_SamePackage() {
        when(mockOpenApiConfig.scanPackages()).thenReturn(Set.of(dummyPackageB));
        when(mockOpenApiConfig.scanExcludePackages()).thenReturn(Set.of(dummyPackageB));

        verify(getEntry(dummyClassA), NOT_SCAN);
        verify(getEntry(dummyClassB), NOT_SCAN);
        verify(getEntry(dummyClassC), NOT_SCAN);
        verify(getEntry(dummyClassX), NOT_SCAN);
        verify(getEntry(dummyClassY), NOT_SCAN);
    }

    @Test
    public void testSpecifyScanPackagesAndScanExcludePackages_ExcludeSubPkg() {
        when(mockOpenApiConfig.scanPackages()).thenReturn(Set.of(dummyPackageB));
        when(mockOpenApiConfig.scanExcludePackages()).thenReturn(Set.of(dummyPackageC));

        verify(getEntry(dummyClassA), NOT_SCAN);
        verify(getEntry(dummyClassB), SCAN);
        verify(getEntry(dummyClassC), NOT_SCAN);
        verify(getEntry(dummyClassX), NOT_SCAN);
        verify(getEntry(dummyClassY), NOT_SCAN);
    }

    @Test
    public void testSpecifyScanPackagesAndScanExcludePackages_ExcludeParentPkg() {
        when(mockOpenApiConfig.scanPackages()).thenReturn(Set.of(dummyPackageB));
        when(mockOpenApiConfig.scanExcludePackages()).thenReturn(Set.of(dummyPackageA));

        verify(getEntry(dummyClassA), NOT_SCAN);
        verify(getEntry(dummyClassB), SCAN);
        verify(getEntry(dummyClassC), SCAN);
        verify(getEntry(dummyClassX), NOT_SCAN);
        verify(getEntry(dummyClassY), NOT_SCAN);
    }

    @Test
    public void testSpecifyScanPackagesAndScanExcludePackages_ExcludeOtherPkg() {
        when(mockOpenApiConfig.scanPackages()).thenReturn(Set.of(dummyPackageB));
        when(mockOpenApiConfig.scanExcludePackages()).thenReturn(Set.of(dummyPackageX));

        verify(getEntry(dummyClassA), NOT_SCAN);
        verify(getEntry(dummyClassB), SCAN);
        verify(getEntry(dummyClassC), SCAN);
        verify(getEntry(dummyClassX), NOT_SCAN);
        verify(getEntry(dummyClassY), NOT_SCAN);
    }

    @Test
    public void testSpecifyScanPackagesAndScanClasses() {
        when(mockOpenApiConfig.scanPackages()).thenReturn(Set.of(dummyPackageB));
        when(mockOpenApiConfig.scanClasses()).thenReturn(Set.of(dummyClassB, dummyClassX));

        verify(getEntry(dummyClassA), NOT_SCAN);
        verify(getEntry(dummyClassB), SCAN);
        verify(getEntry(dummyClassC), SCAN);
        verify(getEntry(dummyClassX), SCAN);
        verify(getEntry(dummyClassY), NOT_SCAN);
    }

    @Test
    public void testSpecifyScanPackagesAndScanExcludeClasses() {
        when(mockOpenApiConfig.scanPackages()).thenReturn(Set.of(dummyPackageB));
        when(mockOpenApiConfig.scanExcludeClasses()).thenReturn(Set.of(dummyClassB, dummyPackageX));

        verify(getEntry(dummyClassA), NOT_SCAN);
        verify(getEntry(dummyClassB), NOT_SCAN);
        verify(getEntry(dummyClassC), SCAN);
        verify(getEntry(dummyClassX), NOT_SCAN);
        verify(getEntry(dummyClassY), NOT_SCAN);
    }

    @Test
    public void testSpecifyScanPackagesAndScanExcludePackages_IncludeSubPkg_ExcludeSamePkg() {
        when(mockOpenApiConfig.scanPackages()).thenReturn(Set.of(dummyPackageA, dummyPackageC));
        when(mockOpenApiConfig.scanExcludePackages()).thenReturn(Set.of(dummyPackageA));

        verify(getEntry(dummyClassA), NOT_SCAN);
        verify(getEntry(dummyClassB), NOT_SCAN);
        verify(getEntry(dummyClassC), SCAN);
        verify(getEntry(dummyClassX), NOT_SCAN);
        verify(getEntry(dummyClassY), NOT_SCAN);
    }

    @Test
    public void testSpecifyScanPackagesAndScanExcludePackages_IncludeSubPkg_ExcludeSubPkg() {
        when(mockOpenApiConfig.scanPackages()).thenReturn(Set.of(dummyPackageA, dummyPackageC));
        when(mockOpenApiConfig.scanExcludePackages()).thenReturn(Set.of(dummyPackageB));

        verify(getEntry(dummyClassA), SCAN);
        verify(getEntry(dummyClassB), NOT_SCAN);
        verify(getEntry(dummyClassC), SCAN);
        verify(getEntry(dummyClassX), NOT_SCAN);
        verify(getEntry(dummyClassY), NOT_SCAN);
    }

    @Test
    public void testSpecifyScanPackagesAndScanExcludePackages_IncludeOtherPkg_ExcludeSamePkg() {
        when(mockOpenApiConfig.scanPackages()).thenReturn(Set.of(dummyPackageA, dummyPackageX));
        when(mockOpenApiConfig.scanExcludePackages()).thenReturn(Set.of(dummyPackageA));

        verify(getEntry(dummyClassA), NOT_SCAN);
        verify(getEntry(dummyClassB), NOT_SCAN);
        verify(getEntry(dummyClassC), NOT_SCAN);
        verify(getEntry(dummyClassX), SCAN);
        verify(getEntry(dummyClassY), SCAN);
    }

    @Test
    public void testSpecifyScanClassesAndScanExcludePackages() {
        when(mockOpenApiConfig.scanClasses()).thenReturn(Set.of(dummyClassB, dummyClassX));
        when(mockOpenApiConfig.scanExcludePackages()).thenReturn(Set.of(dummyPackageB));

        verify(getEntry(dummyClassA), NOT_SCAN);
        verify(getEntry(dummyClassB), SCAN);
        verify(getEntry(dummyClassC), NOT_SCAN);
        verify(getEntry(dummyClassX), SCAN);
        verify(getEntry(dummyClassY), NOT_SCAN);
    }

    @Test
    public void testSpecifyScanExcludePackagesAndScanExcludeClasses() {
        when(mockOpenApiConfig.scanExcludePackages()).thenReturn(Set.of(dummyPackageB));
        when(mockOpenApiConfig.scanExcludeClasses()).thenReturn(Set.of(dummyClassB, dummyClassX));

        verify(getEntry(dummyClassA), SCAN);
        verify(getEntry(dummyClassB), NOT_SCAN);
        verify(getEntry(dummyClassC), NOT_SCAN);
        verify(getEntry(dummyClassX), NOT_SCAN);
        verify(getEntry(dummyClassY), SCAN);
    }

    @Test
    public void testSpecifyScanClassesAndScanExcludeClasses_ExcludeMultiple() {
        when(mockOpenApiConfig.scanClasses()).thenReturn(Set.of(dummyClassB));
        when(mockOpenApiConfig.scanExcludeClasses()).thenReturn(Set.of(dummyClassB,dummyClassX));

        verify(getEntry(dummyClassA), NOT_SCAN);
        verify(getEntry(dummyClassB), NOT_SCAN);
        verify(getEntry(dummyClassC), NOT_SCAN);
        verify(getEntry(dummyClassX), NOT_SCAN);
        verify(getEntry(dummyClassY), NOT_SCAN);
    }

    @Test
    public void testSpecifyScanClassesAndScanExcludeClasses_IncludeMultiple() {
        when(mockOpenApiConfig.scanClasses()).thenReturn(Set.of(dummyClassB,dummyClassX));
        when(mockOpenApiConfig.scanExcludeClasses()).thenReturn(Set.of(dummyClassB));

        verify(getEntry(dummyClassA), NOT_SCAN);
        verify(getEntry(dummyClassB), NOT_SCAN);
        verify(getEntry(dummyClassC), NOT_SCAN);
        verify(getEntry(dummyClassX), SCAN);
        verify(getEntry(dummyClassY), NOT_SCAN);
    }

    @Test
    public void testSpecifyAll() {
        when(mockOpenApiConfig.scanPackages()).thenReturn(Set.of(dummyPackageA));
        when(mockOpenApiConfig.scanClasses()).thenReturn(Set.of(dummyClassB, dummyClassX));
        when(mockOpenApiConfig.scanExcludePackages()).thenReturn(Set.of(dummyPackageB));
        when(mockOpenApiConfig.scanExcludeClasses()).thenReturn(Set.of(dummyClassA, dummyClassX));

        verify(getEntry(dummyClassA), NOT_SCAN);
        verify(getEntry(dummyClassB), SCAN);
        verify(getEntry(dummyClassC), NOT_SCAN);
        verify(getEntry(dummyClassX), NOT_SCAN);
        verify(getEntry(dummyClassY), NOT_SCAN);
    }

    private void verify(String entry, boolean expected) {
        var result = openApiService.isClassToBeScanned(mockOpenApiConfig, entry);
        assertEquals(expected, result);
    }

    private String getEntry(String fqcn) {
        return fqcn.replaceAll("\\.", "/") + ".class";
    }
}
