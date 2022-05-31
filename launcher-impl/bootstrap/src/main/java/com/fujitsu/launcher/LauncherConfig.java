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
package com.fujitsu.launcher;

import java.util.List;

public class LauncherConfig {

    private Operation operation;
    private String configFile;
    private String contextRoot = "/";
    private String deploy;
    private String execute;
    private boolean force;
    private String generate;
    private int httpListener = 8080;
    private int httpsListener = 8181;
    private String libraries;
    private boolean precompilejsp;
    private List<String> subcommandArguments;

    public void setOperation(Operation operation) {
        this.operation = operation;
    }

    public Operation getOperation() {
        return operation;
    }

    public String getConfigFile() {
        return configFile;
    }

    public void setConfigFile(String configFile) {
        this.configFile = configFile;
    }

    public String getContextRoot() {
        return contextRoot;
    }

    public void setContextRoot(String contextRoot) {
        this.contextRoot = contextRoot;
    }

    public String getDeploy() {
        return deploy;
    }

    public void setDeploy(String deploy) {
        this.deploy = deploy;
    }

    public String getExecute() {
        return execute;
    }

    public void setExecute(String execute) {
        this.execute = execute;
    }

    public boolean isForce() {
        return force;
    }

    public void setForce(boolean force) {
        this.force = force;
    }

    public String getGenerate() {
        return generate;
    }

    public void setGenerate(String generate) {
        this.generate = generate;
    }

    public int getHttpListener() {
        return httpListener;
    }

    public void setHttpListener(int httpListener) {
        this.httpListener = httpListener;
    }

    public int getHttpsListener() {
        return httpsListener;
    }

    public void setHttpsListener(int httpsListener) {
        this.httpsListener = httpsListener;
    }

    public String getLibraries() {
        return libraries;
    }

    public void setLibraries(String libraries) {
        this.libraries = libraries;
    }

    public boolean isPrecompilejsp() {
        return precompilejsp;
    }

    public void setPrecompilejsp(boolean precompilejsp) {
        this.precompilejsp = precompilejsp;
    }

    public List<String> getSubcommandArguments() {
        return subcommandArguments;
    }

    public void setSubcommandArguments(List<String> subcommandArguments) {
        this.subcommandArguments = subcommandArguments;
    }
}
