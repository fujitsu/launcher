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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public class ExceptionUtil {
    public static String toStackStaceString(Throwable throwable) {
        try (StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw)) {
            throwable.printStackTrace(pw);
            return sw.toString();
        } catch (IOException e) {
            throw new RuntimeException("Error while processing stack trace: " + e);
        }
    }
}
