/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017 Fujitsu Limited and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the term of the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/fujitsu/launcher/blob/master/LICENSE.txt
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]" 
 */
package com.fujitsu.launcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 *
 * @author Tsuyoshi Yoshitomi
 */
public class DeployProperties extends Properties {

    public void setContextRoot(String contextroot) {
        setProperty("--contextroot", contextroot);
    }

    public void setLibraries(String libraries) {
        setProperty("--libraries", libraries);
    }

    public String[] getDeployOptions() {
        List<String> deployOptions = new ArrayList<>();
        deployOptions.add("--name=launcher_application");

        if (this.getProperty("--contextroot") != null) {
            deployOptions.add("--contextroot=" + getProperty("--contextroot"));
        } else {
            deployOptions.add("--contextroot=");
        }

        if (this.getProperty("--libraries") != null) {
            deployOptions.add("--libraries=" + getProperty("--libraries"));
        }

        return deployOptions.toArray(new String[deployOptions.size()]);
    }
}
