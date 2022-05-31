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

import java.util.concurrent.ThreadFactory;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.concurrent.ManagedThreadFactory;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class ThreadFactoryProvider {
    private ManagedThreadFactory factory;

    @Inject
    public ThreadFactoryProvider(
        @ConfigProperty(name = "com.fujitsu.launcher.microprofile.faulttolerance.managedThreadFactory", defaultValue="java:comp/DefaultManagedThreadFactory") String factoryName
    ){
        try{
            InitialContext ic = new InitialContext();
            factory = (ManagedThreadFactory) ic.lookup(factoryName);
        } catch (NamingException e){
            throw new RuntimeException(e);
        }
    }

    public ThreadFactory get(){
        return factory;
    }
}