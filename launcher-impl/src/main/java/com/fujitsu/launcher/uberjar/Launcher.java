/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017 Fujitsu Limited and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the term of the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/fujitsu/Launcher/blob/master/LICENSE.txt
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
package com.fujitsu.launcher.uberjar;

import com.fujitsu.launcher.DeployProperties;
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

            GlassFish glassfish = GlassFishRuntime.bootstrap().newGlassFish(glassfishProperties);
            glassfish.start();
            glassfish.getDeployer().deploy(wis, deployProperties.getDeployOptions());
        } catch (Throwable th) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Server was stopped.", th);
            System.exit(1);
        }
    }
}
