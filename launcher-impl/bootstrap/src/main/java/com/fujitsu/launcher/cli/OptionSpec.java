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

import java.util.HashMap;
import java.util.Map;

public class OptionSpec {

    private Map<String, Class<? extends Option<?>>> map = new HashMap<>();

    public void define(String name, Class<? extends Option<?>> type) {
        map.put(name, type);
    }

    public Class<? extends Option<?>> getType(String name) {
        return map.get(name);
    }

    public boolean isDefined(String name) {
        return map.containsKey(name);
    }

    @Override
    public String toString() {
        return map.toString();
    }
}
