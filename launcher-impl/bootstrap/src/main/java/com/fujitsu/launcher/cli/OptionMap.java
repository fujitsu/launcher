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

public class OptionMap {

    private Map<String, Option<?>> map = new HashMap<>();

    public void put(Option<?> option) {
        map.put(option.getName(), option);
    }

    public Option<?> get(String name) {
        return map.get(name);
    }

    @SuppressWarnings("unchecked")
    public <T extends Option<?>> T get(String name, Class<T> clazz) {
        return (T) map.get(name);
    }

    public Map<String, Option<?>> getMap() {
        return map;
    }

    @Override
    public String toString() {
        return "OptionMap[" + map.toString() + "]";
    }
}