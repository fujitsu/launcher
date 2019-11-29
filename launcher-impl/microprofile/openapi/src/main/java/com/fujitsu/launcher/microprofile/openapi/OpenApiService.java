/*
 * Copyright (c) 2019 Fujitsu Limited and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package com.fujitsu.launcher.microprofile.openapi;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.eclipse.microprofile.config.ConfigProvider;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.Events;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.runlevel.RunLevel;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.internal.deployment.Deployment;
import org.glassfish.web.deployment.descriptor.WebBundleDescriptorImpl;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;
import org.jvnet.hk2.annotations.Service;

import io.smallrye.openapi.api.OpenApiConfig;
import io.smallrye.openapi.api.OpenApiConfigImpl;
import io.smallrye.openapi.api.OpenApiDocument;
import io.smallrye.openapi.runtime.OpenApiProcessor;
import io.smallrye.openapi.runtime.OpenApiStaticFile;
import io.smallrye.openapi.runtime.io.OpenApiSerializer.Format;
/**
 *
 * @author Katsuhiro Kunisada
 * @author Takahiro Nagao
 */
@Service(name = "openapi-service")
@RunLevel(StartupRunLevel.VAL)
public class OpenApiService implements PostConstruct, EventListener {
    private static final String WEB_INF_CLASSES_PREFIX = "WEB-INF/classes/";
    private static final String CLASS_SUFFIX = ".class";
    private static final String JAR_SUFFIX = ".jar";

    @Inject
    private Events events;

    @Override
    public void postConstruct() {
        events.register(this);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void event(Event event) {
        if (!event.is(Deployment.APPLICATION_LOADED)) {
            return;
        }
        ApplicationInfo appInfo = (ApplicationInfo) event.hook();
        if (appInfo.getMetaData(WebBundleDescriptorImpl.class) == null) {
            return;
        }
        openapi(appInfo);
    }

    private void openapi(ApplicationInfo appInfo) {
        OpenApiConfig config = new OpenApiConfigImpl(ConfigProvider.getConfig(appInfo.getAppClassLoader()));
        ReadableArchive gfArchive = appInfo.getSource();
        ClassLoader appClassLoader = appInfo.getAppClassLoader();
        IndexView index = getIndexForArchive(config, gfArchive, appClassLoader);
        OpenApiDocument doc = OpenApiDocument.INSTANCE;
        if (doc.isSet()) {
            return;
        }
        OpenApiStaticFile staticFile = getOpenApiStaticFile(appClassLoader);
        doc.config(config);
        doc.modelFromStaticFile(OpenApiProcessor.modelFromStaticFile(staticFile));
        doc.modelFromAnnotations(OpenApiProcessor.modelFromAnnotations(config, index));
        doc.modelFromReader(OpenApiProcessor.modelFromReader(config, getContextClassLoader()));
        doc.filter(OpenApiProcessor.getFilter(config, appClassLoader));
        doc.initialize();
    }

    private ClassLoader getContextClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

    private IndexView getIndexForArchive(OpenApiConfig config, ReadableArchive archive, ClassLoader classLoader) {
        try {
            Indexer indexer = new Indexer();
            index(indexer, "io/smallrye/openapi/runtime/scanner/CollectionStandin.class", classLoader);
            index(indexer, "io/smallrye/openapi/runtime/scanner/MapStandin.class", classLoader);
            indexArchive(config, indexer, archive, classLoader);
            return indexer.complete();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void index(Indexer indexer, String resourceName, ClassLoader classLoader) throws IOException {
        URL resource = classLoader.getResource(resourceName.replaceAll(WEB_INF_CLASSES_PREFIX, ""));
        try (InputStream is = resource.openConnection().getInputStream()) {
            indexer.index(is);
        }
    }

    private void indexArchive(OpenApiConfig config, Indexer indexer, ReadableArchive archive, ClassLoader classLoader)
            throws IOException {
        for (String entry : Collections.list(archive.entries())) {
            if (isClassEntry(entry, classLoader) && isClassToBeScanned(config, entry)) {
                index(indexer, entry, classLoader);
            } else if (isJarEntry(entry) && isJarToBeScanned(config, entry)) {
                indexArchive(config, indexer, archive.getSubArchive(entry), classLoader);
            }
        }
    }

    private boolean isClassEntry(String entry, ClassLoader appClassLoader) {
        if (!entry.endsWith(CLASS_SUFFIX)) {
            return false;
        }
        String name = entry.replace(WEB_INF_CLASSES_PREFIX, "").replace(CLASS_SUFFIX, "").replace("/", ".");

        Class<?> loadedClass = null;
        try {
            loadedClass = appClassLoader.loadClass(name);
        } catch (Throwable t) {
            // ignore
        }
        if (loadedClass == null) {
            return false;
        }
        try {
            loadedClass.getDeclaredFields();
            loadedClass.getDeclaredMethods();
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private boolean isJarEntry(String entry) {
        return entry.toLowerCase().endsWith(JAR_SUFFIX);
    }

    // These config settings are SmallRye's extensions.
    private boolean isJarToBeScanned(OpenApiConfig config, String entry) {
        if (config.scanDependenciesDisable()) {
            return false;
        }
        Set<String> jars = config.scanDependenciesJars();
        if (jars.isEmpty()) {
            return true;
        }
        return jars.contains(Paths.get(entry).getFileName().toString());
    }

    private boolean isClassToBeScanned(OpenApiConfig config, String entry) {
        Set<String> scanClasses = config.scanClasses();
        Set<String> scanPackages = config.scanPackages();
        Set<String> scanExcludeClasses = config.scanExcludeClasses();
        Set<String> scanExcludePackages = config.scanExcludePackages();

        if (entry == null) {
            return false;
        }

        String fqcn = getFqcn(entry);
        String packageName = getPackageName(fqcn);
        boolean ret;

        if (scanClasses.isEmpty() && scanPackages.isEmpty()) {
            ret = true;
        } else if (!scanClasses.isEmpty() && scanPackages.isEmpty()) {
            ret = scanClasses.contains(fqcn);
        } else if (scanClasses.isEmpty() && !scanPackages.isEmpty()) {
            ret = scanPackages.contains(packageName);
        } else {
            ret = scanClasses.contains(fqcn) || scanPackages.contains(packageName);
        }
        if (scanExcludeClasses.contains(fqcn) || scanExcludePackages.contains(packageName)) {
            ret = false;
        }
        return ret;
    }

    private String getFqcn(String className) {
        if (className.startsWith(WEB_INF_CLASSES_PREFIX)) {
            className = className.substring(WEB_INF_CLASSES_PREFIX.length());
        }

        String fqcn = className.replaceAll("/", ".").substring(0, className.lastIndexOf(CLASS_SUFFIX));
        return fqcn;
    }

    private String getPackageName(String fqcn) {
        String packageName = "";
        if (fqcn.contains(".")) {
            packageName = fqcn.substring(0, fqcn.lastIndexOf("."));
        }
        return packageName;
    }

    private OpenApiStaticFile getOpenApiStaticFile(ClassLoader classLoader) {
        try {
            List<OpenApiFile> candidates = new ArrayList<>();
            candidates.add(new OpenApiFile("META-INF/openapi.yaml", Format.YAML));
            candidates.add(new OpenApiFile("META-INF/openapi.yml", Format.YAML));
            candidates.add(new OpenApiFile("META-INF/openapi.json", Format.JSON));
            candidates.add(new OpenApiFile("../../META-INF/openapi.yaml", Format.YAML));
            candidates.add(new OpenApiFile("../../META-INF/openapi.yml", Format.YAML));
            candidates.add(new OpenApiFile("../../META-INF/openapi.json", Format.JSON));

            for (OpenApiFile f : candidates) {
                URL resource = classLoader.getResource(f.getPath());
                if (resource == null) {
                    continue;
                }
                OpenApiStaticFile result = new OpenApiStaticFile();
                result.setFormat(f.getFormat());
                result.setContent(resource.openConnection().getInputStream());
                return result;
            }
            return null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private class OpenApiFile {
        private final String path;
        private final Format format;

        public OpenApiFile(String path, Format format) {
            this.path = path;
            this.format = format;
        }

        public String getPath() {
            return path;
        }

        public Format getFormat() {
            return format;
        }
    }
}
