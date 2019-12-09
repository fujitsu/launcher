/*
 * Copyright (c) 2017-2019 Fujitsu Limited and/or its affiliates. All rights
 * reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package com.fujitsu.launcher;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.embeddable.GlassFish;
import org.glassfish.embeddable.GlassFishProperties;
import org.glassfish.embeddable.GlassFishRuntime;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Globals;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import com.sun.enterprise.glassfish.bootstrap.Constants;
import com.sun.enterprise.module.bootstrap.StartupContext;
import com.sun.enterprise.server.logging.GFFileHandler;

/**
 *
 * @author Tsuyoshi Yoshitomi
 */
public class LauncherMain {

    @Option(name = "--config-file")
    private String configFile;

    @Option(name = "--http-listener")
    private int httpListener = 8080;

    @Option(name = "--https-listener")
    private int httpsListener = 8181;

    @Option(name = "--deploy")
    private String inputWar;

    @Option(name = "--contextroot")
    private String contextRoot = "";

    @Option(name = "--libraries")
    private String libraries;

    @Option(name = "--generate")
    private String outputJar;

    @Option(name = "--force")
    private boolean isForce = false;

    private GlassFishProperties glassfishProperties = new GlassFishProperties();
    private DeployProperties deployProperties = new DeployProperties();

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        new LauncherMain(args).start();
    }

    private LauncherMain(String[] args) {
        try {
            new CmdLineParser(this).parseArgument(args);
        } catch (CmdLineException ex) {
            throw new IllegalArgumentException(ex);
        }
        if (inputWar == null) {
            throw new IllegalArgumentException("No --deploy is given.");
        }

        if (configFile != null) {
            glassfishProperties.setConfigFileReadOnly(true);
            if (outputJar == null) {
                glassfishProperties.setConfigFileURI(new File(configFile).toURI().normalize().toString());
            }
        } else {
            glassfishProperties.setConfigFileReadOnly(false);
            glassfishProperties.setPort("http-listener", httpListener);
            glassfishProperties.setPort("https-listener", httpsListener);
        }

        if (contextRoot != null) {
            deployProperties.setContextRoot(contextRoot);
        }
        if (libraries != null) {
            deployProperties.setLibraries(libraries);
        }
    }

    private void start() {
        if (outputJar != null) {
            generate();
        } else {
            launch();
        }
    }

    private void generate() {
        try {
            if (isForce) {
                Files.deleteIfExists(Paths.get(outputJar));
            }
            Files.copy(getTemplate(), Paths.get(outputJar));

            Map<String, Object> env = new HashMap<>();
            env.put("create", "true");
            URI uri = URI.create("jar:" + new File(outputJar).toURI()).normalize();
            try (
                    FileSystem zipfs = FileSystems.newFileSystem(uri, env);
                    InputStream wis = new FileInputStream(inputWar);
                    BufferedWriter gbw = Files.newBufferedWriter(zipfs.getPath("uber-jar_glassfish.properties"));
                    BufferedWriter dbw = Files.newBufferedWriter(zipfs.getPath("uber-jar_deploy.properties"));
                    InputStream mis = this.getClass().getClassLoader().getResourceAsStream("com/fujitsu/launcher/uber-jar_MANIFEST.MF")) {

                Files.copy(wis, zipfs.getPath("uber-jar_application.war"));
                glassfishProperties.getProperties().store(gbw, this.getClass().getName());
                deployProperties.store(dbw, this.getClass().getName());
                Files.delete(zipfs.getPath("META-INF/MANIFEST.MF"));
                Files.copy(mis, zipfs.getPath("META-INF/MANIFEST.MF"));
            }
            if (configFile != null) {
                try (
                        FileSystem zipfs = FileSystems.newFileSystem(uri, env);
                        InputStream cis = new FileInputStream(configFile)) {

                    Files.copy(cis, zipfs.getPath("uber-jar_domain.xml"));
                }
            }
        } catch (Throwable th) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Uber-jar can't be generated.", th);
            System.exit(1);
        }
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Uber-jar was generated. {0}", outputJar);
    }

    private void launch() {
        try {
            Thread preInitShutdownHook = createPreInitShutdownHook();
            Runtime.getRuntime().addShutdownHook(preInitShutdownHook);

            GlassFish glassfish = GlassFishRuntime.bootstrap().newGlassFish(glassfishProperties);

            Thread postInitShutdownHook = createPostInitShutdownHook(glassfish);
            Runtime.getRuntime().addShutdownHook(postInitShutdownHook);
            Runtime.getRuntime().removeShutdownHook(preInitShutdownHook);

            glassfish.start();
            glassfish.getDeployer().deploy(new File(inputWar), deployProperties.getDeployOptions());
        } catch (Throwable th) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Server was stopped.", th);
            cleanInstanceRoot();
            System.exit(1);
        }
    }

    private Path getTemplate() throws IOException {
        String resourceName = this.getClass().getCanonicalName().replaceAll("\\.", "/") + ".class";
        URL url = this.getClass().getClassLoader().getResource(resourceName);
        if (url == null) {
            throw new FileNotFoundException(resourceName + " has not found.");
        }
        JarURLConnection jarURLConnection = (JarURLConnection) url.openConnection();
        return Paths.get(jarURLConnection.getJarFile().getName());
    }

    public static Thread createPreInitShutdownHook() {
        return new Thread() {
            public void run() {
                cleanInstanceRoot();
            }
        };
    }

    public static Thread createPostInitShutdownHook(GlassFish glassfish) {
        return new Thread() {
            public void run() {
                try {
                    glassfish.stop();
                    // work-around for deleting server.log on disposal
                    forceCloseLog();
                    glassfish.dispose();
                } catch (Exception e) {
                    // fall through;
                }
            }

            private void forceCloseLog() {
                GFFileHandler h = Globals.get(GFFileHandler.class);
                h.preDestroy();
                h.close();
            }
        };
    }

    public static void cleanInstanceRoot() {
        String instanceRootProp = getInstanceRoot();
        if (instanceRootProp != null) {
            File instanceRoot = new File(instanceRootProp);
            deleteRecursively(instanceRoot);
        }
    }

    private static String getInstanceRoot() {
        Properties arguments = getStartupContextArguments();
        if (arguments == null) {
            return System.getProperty(Constants.INSTANCE_ROOT_PROP_NAME);
        } else {
            return arguments.getProperty(Constants.INSTANCE_ROOT_PROP_NAME);
        }
    }

    private static Properties getStartupContextArguments() {
        ServiceLocator habitat = Globals.getDefaultHabitat();
        if (habitat == null) {
            return null;
        }
        StartupContext startupContext = habitat.getService(StartupContext.class);
        if (startupContext == null) {
            return null;
        }
        return startupContext.getArguments();
    }

    private static boolean deleteRecursively(File file) {
        if (file.isDirectory()) {
            for (File f : file.listFiles()) {
                deleteRecursively(f);
            }
        }
        return file.delete();
    }
}
