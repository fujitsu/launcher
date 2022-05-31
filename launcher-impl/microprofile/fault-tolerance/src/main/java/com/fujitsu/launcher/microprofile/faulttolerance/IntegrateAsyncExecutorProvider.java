/*
 * Copyright (c) 2021-2022 Fujitsu Limited and/or its affiliates. All rights
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

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.smallrye.faulttolerance.AsyncExecutorProvider;
import jakarta.annotation.Priority;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

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
