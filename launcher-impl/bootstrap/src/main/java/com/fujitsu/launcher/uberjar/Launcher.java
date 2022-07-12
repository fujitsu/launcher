/*
 * Copyright (c) 2017-2022 Fujitsu Limited and/or its affiliates. All rights
 * reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package com.fujitsu.launcher.uberjar;

import com.fujitsu.launcher.DeployProperties;
import com.fujitsu.launcher.LauncherMain;

import java.io.InputStream;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.embeddable.GlassFish;
import org.glassfish.embeddable.GlassFishProperties;
import org.glassfish.embeddable.GlassFishRuntime;

/**
 *
 * @author Tsuyoshi Yoshitomi
 */
public class Launcher {

    public static void main(String[] args) {
        new Launcher().start();
    }

    private void start() {
        try (
                InputStream gpis = this.getClass().getClassLoader().getResourceAsStream("uber-jar_glassfish.properties");
                InputStream dpis = this.getClass().getClassLoader().getResourceAsStream("uber-jar_deploy.properties");
                InputStream wis = this.getClass().getClassLoader().getResourceAsStream("uber-jar_application.war")) {

            GlassFishProperties glassfishProperties = new GlassFishProperties();
            glassfishProperties.getProperties().load(gpis);
            URL url = this.getClass().getClassLoader().getResource("uber-jar_domain.xml");
            if (url != null) {
                glassfishProperties.setConfigFileURI(url.toURI().toString());
            }

            DeployProperties deployProperties = new DeployProperties();
            deployProperties.load(dpis);

            LauncherMain.initProductName();

            Thread preInitShutdownHook = LauncherMain.createPreInitShutdownHook();
            Runtime.getRuntime().addShutdownHook(preInitShutdownHook);

            GlassFish glassfish = GlassFishRuntime.bootstrap().newGlassFish(glassfishProperties);

            Thread postInitShutdownHook = LauncherMain.createPostInitShutdownHook(glassfish);
            Runtime.getRuntime().addShutdownHook(postInitShutdownHook);
            Runtime.getRuntime().removeShutdownHook(preInitShutdownHook);

            glassfish.start();
            glassfish.getDeployer().deploy(wis, deployProperties.getDeployOptions());
        } catch (Throwable th) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Server was stopped.", th);
            LauncherMain.cleanInstanceRoot();
            System.exit(1);
        }
    }
}
