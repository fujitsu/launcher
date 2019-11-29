/*
 * Copyright (c) 2019 Fujitsu Limited and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.glassfish.weld;

import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.JarFile;

import org.glassfish.api.deployment.DeployCommandParameters;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.archive.ReadableArchive;

/**
*
* @author Katsuhiro Kunisada
*/
public class ParamLibrariesResolver {

    /**
     * Checks jars specified by '--libraries' option and returns them if they are
     * required by 'Extension-List' of the given archive.
     *
     * @throws IOException
     */
    public static Set<String> getLibraries(DeploymentContext context, ReadableArchive archive) throws IOException {
        Set<String> libs = new HashSet<String>();
        DeployCommandParameters params = context.getCommandParameters(DeployCommandParameters.class);
        String librariesOption = Optional.ofNullable(params.libraries()).orElse("");
        if (archive.getManifest() == null || librariesOption.isEmpty()) {
            return libs;
        }

        Attributes archiveAttrs = archive.getManifest().getMainAttributes();
        String archiveExtList = getAttributeString(archiveAttrs, Name.EXTENSION_LIST);
        for (String file : librariesOption.split(",")) {
            try (JarFile jar = new JarFile(file)) {
                if (jar.getManifest() == null) {
                    continue;
                }
                Attributes jarAttrs = jar.getManifest().getMainAttributes();
                String jarExtName = getAttributeString(jarAttrs, Name.EXTENSION_NAME);
                String jarExtVer = getAttributeString(jarAttrs, Name.SPECIFICATION_VERSION);
                if (jarExtName.isEmpty() || jarExtVer.isEmpty()) {
                    continue;
                }
                for (String s : archiveExtList.split("\\p{javaWhitespace}+")) {
                    if (s.isEmpty()) {
                        continue;
                    }
                    String name = getAttributeString(archiveAttrs, s + "-" + Name.EXTENSION_NAME);
                    String version = getAttributeString(archiveAttrs, s + "-" + Name.SPECIFICATION_VERSION);
                    if (jarExtName.equals(name) && jarExtVer.equals(version)) {
                        libs.add(jar.getName());
                    }
                }
            }
        }
        return libs;
    }

    private static String getAttributeString(Attributes attr, String name) {
        return Optional.ofNullable(attr.getValue(name)).orElse("").trim();
    }

    private static String getAttributeString(Attributes attr, Name name) {
        return getAttributeString(attr, name.toString());
    }
}
