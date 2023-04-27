/*
 * Copyright (c) 2010, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package com.sun.enterprise.admin.cli.schemadoc;

import org.objectweb.asm.AnnotationVisitor;
import org.glassfish.api.admin.config.PropertiesDesc;
import org.glassfish.api.admin.config.PropertyDesc;
import org.jvnet.hk2.config.Attribute;
import org.objectweb.asm.Opcodes;

public class AttributeMethodVisitor extends EmptyVisitor {
    private ClassDef def;
    private String name;
    private String type;
    private boolean duckTyped;

    public AttributeMethodVisitor(ClassDef classDef, String method, String aggType) {
        super(Opcodes.ASM9);
        def = classDef;
        name = method;
        type = aggType;
        def.addAttribute(name, null);
    }

    @Override
    public String toString() {
        return "AttributeMethodVisitor{" + "def=" + def + ", name='" + name + '\'' + ", type='" + type + '\'' + ", duckTyped=" + duckTyped
                + '}';
    }

    /**
     * Visits an annotation of this method.
     *
     * @param desc the class descriptor of the annotation class.
     * @param visible <tt>true</tt> if the annotation is visible at runtime.
     *
     * @return a visitor to visit the annotation values, or <tt>null</tt> if this visitor is not interested in visiting this
     * annotation.
     */
    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        duckTyped |= "Lorg/jvnet/hk2/config/DuckTyped;".equals(desc);
        AnnotationVisitor visitor = null;
        if ("Lorg/jvnet/hk2/config/Attribute;".equals(desc) || "Lorg/jvnet/hk2/config/Element;".equals(desc)) {
            try {
                final Class<?> configurable = Thread.currentThread().getContextClassLoader().loadClass(def.getDef());
                final Attribute annotation = configurable.getMethod(name).getAnnotation(Attribute.class);
                def.addAttribute(name, annotation);
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }

        } else if ("Lorg/glassfish/api/admin/config/PropertiesDesc;".equals(desc)) {
            try {
                final Class<?> configurable = Thread.currentThread().getContextClassLoader().loadClass(def.getDef());
                final PropertiesDesc annotation = configurable.getMethod(name).getAnnotation(PropertiesDesc.class);
                final PropertyDesc[] propertyDescs = annotation.props();
                for (PropertyDesc prop : propertyDescs) {
                    def.addProperty(prop);
                }
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
        return visitor;
    }

    @Override
    public void visitEnd() {
        if (!duckTyped) {
            if (!isSimpleType(type)) {
                def.addAggregatedType(name, type);
                def.removeAttribute(name);
            }
        } else {
            def.removeAttribute(name);
        }
    }

    private boolean isSimpleType(String type) {
        return type.startsWith("java");
    }
}
