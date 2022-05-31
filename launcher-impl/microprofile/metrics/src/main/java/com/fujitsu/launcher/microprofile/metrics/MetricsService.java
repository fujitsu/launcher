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
package com.fujitsu.launcher.microprofile.metrics;

import java.io.IOException;

import jakarta.annotation.PostConstruct;

import org.glassfish.api.StartupRunLevel;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import io.smallrye.metrics.setup.JmxRegistrar;

@Service(name = "metrics-service")
@RunLevel(StartupRunLevel.VAL)
public class MetricsService {

    @PostConstruct
    public void postConstruct() {
        try {
            new JmxRegistrar().init();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
