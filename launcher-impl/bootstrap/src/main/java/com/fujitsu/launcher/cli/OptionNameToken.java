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

public class OptionNameToken extends Token {
    public OptionNameToken(String value) {
        super(value);
    }

    @Override
    public String toString() {
        return "OptionNameToken[" + get() + "]";
    }
}
