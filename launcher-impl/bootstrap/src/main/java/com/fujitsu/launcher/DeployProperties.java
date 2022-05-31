/*
 * Copyright (c) 2017-2021 Fujitsu Limited and/or its affiliates. All rights
 * reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package com.fujitsu.launcher;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 *
 * @author Tsuyoshi Yoshitomi
 */
public class DeployProperties extends Properties {

    private static boolean relativeToApplibsDirectory = Boolean
            .getBoolean("com.fujitsu.launcher.deploy.libraries.relativeToApplibsDirectory");

    public void setContextRoot(String contextroot) {
        setProperty("--contextroot", contextroot);
    }

    public void setLibraries(String libraries) {
        setProperty("--libraries", libraries);
    }

    public void setPrecompilejsp(boolean precompilejsp) {
        setProperty("--precompilejsp", Boolean.toString(precompilejsp));
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
            String libraries = getProperty("--libraries");

            // Convert relative paths to absolute ones by taking them to be
            // relative to the current working directory.
            // By default GlassFish regards relative paths as relative to
            // instance-root/lib/applibs.
            if (!relativeToApplibsDirectory) {
                libraries = absolutizeDeployParamLibraries(libraries);
            }

            deployOptions.add("--libraries=" + libraries);
        }

        if (this.getProperty("--precompilejsp") != null) {
            deployOptions.add("--precompilejsp=" + getProperty("--precompilejsp"));
        }

        return deployOptions.toArray(new String[deployOptions.size()]);
    }

    private static String absolutizeDeployParamLibraries(String libraries) {
        String[] split = libraries.split(",");

        for (int i = 0; i < split.length; i++) {
            File f = new File(split[i]);
            split[i] = f.getAbsolutePath();
        }

        return String.join(",", split);
    }
}
