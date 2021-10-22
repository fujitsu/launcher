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
package com.fujitsu.launcher.microprofile.faulttolerance;

import java.util.concurrent.ExecutorService;

import javax.annotation.Priority;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.inject.Alternative;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.smallrye.faulttolerance.AsyncExecutorProvider;

@Singleton
@Alternative
@Priority(1)
public class IntegrateAsyncExecutorProvider implements AsyncExecutorProvider {
    private ManagedExecutorService executor;

    @Inject
    public IntegrateAsyncExecutorProvider(
        @ConfigProperty(name = "com.fujitsu.launcher.microprofile.faulttolerance.managedExecutorService", defaultValue="java:comp/DefaultManagedExecutorService") String executorName
    ){
        try{
            InitialContext ic = new InitialContext();
            executor = (ManagedExecutorService) ic.lookup(executorName);
        } catch (NamingException e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public ExecutorService get() {
        return executor;
    }
}