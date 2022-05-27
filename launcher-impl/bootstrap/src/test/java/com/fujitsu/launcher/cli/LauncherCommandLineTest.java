/*
 * Copyright (c) 2021 Fujitsu Limited and/or its affiliates. All rights
 * reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package com.fujitsu.launcher.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.fujitsu.launcher.LauncherConfig;
import com.fujitsu.launcher.Operation;

public class LauncherCommandLineTest {

    @Test
    public void testDeploy() throws CommandLineException {
        LauncherCommandLine commandLine = new LauncherCommandLine();
        LauncherConfig config = commandLine.parseCommandLine(new String[] {
                "--config-file", "mydomain.xml",
                "--contextroot", "/mycontextroot",
                "--deploy", "myapp.war",
                "--http-listener", "28080",
                "--https-listener", "28181",
                "--libraries", "mylib1.jar,mylib2.jar",
                "--precompilejsp", "true"
        });

        assertEquals(Operation.DEPLOY, config.getOperation());
        assertEquals("mydomain.xml", config.getConfigFile());
        assertEquals("/mycontextroot", config.getContextRoot());
        assertEquals("myapp.war", config.getDeploy());
        assertEquals(28080, config.getHttpListener());
        assertEquals(28181, config.getHttpsListener());
        assertEquals("mylib1.jar,mylib2.jar", config.getLibraries());
        assertEquals(true, config.isPrecompilejsp());
    }

    @Test
    public void testGenerate() throws CommandLineException {
        LauncherCommandLine commandLine = new LauncherCommandLine();
        LauncherConfig config = commandLine.parseCommandLine(new String[] {
                "--deploy", "myapp.war",
                "--force", "true",
                "--generate", "myuber.jar"
        });

        assertEquals(Operation.GENERATE, config.getOperation());
        assertEquals("myapp.war", config.getDeploy());
        assertEquals(true, config.isForce());
        assertEquals("myuber.jar", config.getGenerate());
    }

    @Test
    public void testExecute() throws CommandLineException {
        LauncherCommandLine commandLine = new LauncherCommandLine();
        LauncherConfig config = commandLine.parseCommandLine(new String[] {
                "--execute", "create-file-user", "--passwordfile", "password.txt", "user1"
        });

        assertEquals(Operation.EXECUTE, config.getOperation());
        assertEquals("create-file-user", config.getExecute());
        assertEquals(Arrays.asList("--passwordfile", "password.txt", "user1"), config.getSubcommandArguments());
    }

    @Test
    public void testConflictingOptions() {
        LauncherCommandLine commandLine = new LauncherCommandLine();

        Exception exception = assertThrows(
                CommandLineException.class, () -> {
                    commandLine.parseCommandLine(new String[] { "--deploy", "myapp.war", "--execute", "create-file-user" });
                });

        assertEquals("Options '--deploy' and '--execute' are conflicting.", exception.getMessage());
    }

    @Test
    public void testMandatoryOptionNotFound() {
        LauncherCommandLine commandLine = new LauncherCommandLine();

        Exception exception = assertThrows(
                CommandLineException.class, () -> {
                    commandLine.parseCommandLine(new String[] { "--config-file", "mydomain.xml" });
                });

        assertEquals("Neither option '--deploy' nor '--execute' is specified.", exception.getMessage());
    }

    @Test
    public void testUnexpectedArgument() {
        LauncherCommandLine commandLine = new LauncherCommandLine();

        Exception exception = assertThrows(
                CommandLineException.class, () -> {
                    commandLine.parseCommandLine(new String[] { "--deploy", "myapp.war", "foo" });
                });

        assertEquals("Unexpected argument: foo", exception.getMessage());
    }

    @Test
    public void testForbiddenOptionForDeploy() {
        LauncherCommandLine commandLine = new LauncherCommandLine();

        Exception exception = assertThrows(
                CommandLineException.class, () -> {
                    commandLine.parseCommandLine(new String[] { "--deploy", "myapp.war", "--force" });
                });

        assertEquals("Option '--force' must be used with option '--generate'.", exception.getMessage());
    }

    @Test
    public void testForbiddenOptionForExecute() {
        LauncherCommandLine commandLine = new LauncherCommandLine();

        Exception exception = assertThrows(
                CommandLineException.class, () -> {
                    commandLine.parseCommandLine(new String[] { "--execute", "create-file-user", "--contextroot", "/mycontextroot" });
                });

        assertEquals("Option '--contextroot' cannot be used with option '--execute'.", exception.getMessage());
    }

}
