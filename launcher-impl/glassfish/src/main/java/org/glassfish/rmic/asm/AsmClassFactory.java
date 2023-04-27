/*
 * Copyright (c) 2018, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Eclipse Distribution License
 * v. 1.0 which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the Eclipse
 * Public License v. 2.0 are satisfied: GNU General Public License v2.0
 * w/Classpath exception which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR BSD-3-Clause OR GPL-2.0 WITH
 * Classpath-exception-2.0
 */

package org.glassfish.rmic.asm;

import org.glassfish.rmic.Names;
import org.glassfish.rmic.tools.java.ClassDeclaration;
import org.glassfish.rmic.tools.java.ClassDefinition;
import org.glassfish.rmic.tools.java.ClassDefinitionFactory;
import org.glassfish.rmic.tools.java.Environment;
import org.glassfish.rmic.tools.java.Identifier;
import org.glassfish.rmic.tools.java.MemberDefinition;
import org.glassfish.rmic.tools.java.Type;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * A factory for a class definition produced via ASM.
 */
public class AsmClassFactory implements ClassDefinitionFactory {
    // This field exists to allow unit testing of the case when ASM is not in the classpath.
    @SuppressWarnings("unused")
    private static final Boolean simulateMissingASM = false;

    private Map<Identifier, Identifier> outerClasses = new HashMap<>();

    public AsmClassFactory() {
        if (simulateMissingASM) throw new NoClassDefFoundError();
    }

    /**
     * Returns the latest API supported by the active version of ASM.
     * @return an integer value
     */
    static int getLatestVersion() {
        try {
            int latest = 0;
            for (Field field : Opcodes.class.getDeclaredFields()) {
                if (field.getName().startsWith("ASM") && field.getType().equals(int.class)
                    && field.getAnnotation(Deprecated.class) == null) {
                    latest = Math.max(latest, field.getInt(Opcodes.class));
                }
            }
            return latest;
        } catch (IllegalAccessException e) {
            return Opcodes.ASM9;
        }
    }

    /**
     * Returns the latest API supported by the active version of ASM.
     * @return an integer value
     */
    static int getLatestClassVersion() {
        try {
            int latest = 0;
            for (Field field : Opcodes.class.getDeclaredFields()) {
                if (!field.getName().equals("V1_1") && field.getName().startsWith("V") && field.getType().equals(int.class)) {
                    latest = Math.max(latest, field.getInt(Opcodes.class));
                }
            }
            return latest;
        } catch (IllegalAccessException e) {
            return Opcodes.V11;
        }
    }

    Identifier getOuterClassName(Identifier className) {
        if (isResolvedInnerClassName(className))
            className = Names.mangleClass(className);
        return outerClasses.get(className);
    }

    // This is needed to compensate for the hack described in Main.getClassIdentifier()
    private boolean isResolvedInnerClassName(Identifier className) {
        return className.toString().contains(". ");
    }

    @Override
    public ClassDefinition loadDefinition(InputStream is, Environment env) throws IOException {
        ClassDefinitionVisitor visitor = new ClassDefinitionVisitor(env);
        ClassReader classReader = new ClassReader(is);
        classReader.accept(visitor, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG);
        return visitor.getDefinition();
    }

    @Override
    public int getMaxClassVersion() {
        return getLatestClassVersion();
    }

    class ClassDefinitionVisitor extends ClassVisitor {
        private Environment env;
        private AsmClass asmClass;

        ClassDefinitionVisitor(Environment env) {
            super(getLatestVersion());
            this.env = env;
        }

        ClassDefinition getDefinition() {
            return asmClass;
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            asmClass = new AsmClass(AsmClassFactory.this, toSourceFileName(name), access, toClassDeclaration(name),
                                    toClassDeclaration(superName), toClassDeclarations(interfaces));
        }

        private String toSourceFileName(String name) {
            String className = toClassName(name);
            if (className.contains("$"))
                className = className.substring(0, className.indexOf("$"));
            return className + ".java";
        }

        private String toClassName(String name) {
            return name.substring(name.lastIndexOf('/') + 1);
        }

        private ClassDeclaration[] toClassDeclarations(String... names) {
            ClassDeclaration[] result = new ClassDeclaration[names.length];
            for (int i = 0; i < names.length; i++)
                result[i] = new ClassDeclaration(getIdentifier(names[i]));
            return result;
        }

        private ClassDeclaration toClassDeclaration(String name) {
            return name == null ? null : new ClassDeclaration(getIdentifier(name));
        }

        private Identifier getIdentifier(String name) {
            return Identifier.lookup(name.replace('/', '.'));
        }

        @Override
        public void visitInnerClass(String name, String outerName, String innerName, int access) {
            if (outerName != null)
                outerClasses.put(getIdentifier(name), getIdentifier(outerName));
        }

        @Override
        public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
            MemberDefinition definition = new AsmMemberDefinition(0, asmClass, access, TypeFactory.createType(desc), getIdentifier(name), value);
            asmClass.addMember(env, definition);
            return null;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            MemberDefinition definition = new AsmMemberDefinition(0, asmClass, access, createType(desc), getIdentifier(name), exceptions);
            asmClass.addMember(env, definition);
            return null;
        }

        private Type createType(String desc) {
            return TypeFactory.createMethodType(desc);
        }
    }
}
