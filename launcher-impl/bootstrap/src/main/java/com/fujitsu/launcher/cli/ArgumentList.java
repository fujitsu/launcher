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

import java.util.ArrayList;
import java.util.List;

public class ArgumentList {
    private List<String> arguments = new ArrayList<>();

    public void add(String token) {
        arguments.add(token);
    }

    public List<String> getList() {
        return arguments;
    }

    @Override
    public String toString() {
        return "ArgumentList[" + arguments.toString() + "]";
    }
}
