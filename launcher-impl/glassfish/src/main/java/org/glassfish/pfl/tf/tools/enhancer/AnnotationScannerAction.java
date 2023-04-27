/*
 * Copyright (c) 1997, 2018 Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020 Payara Services Ltd.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.pfl.tf.tools.enhancer;

import org.glassfish.pfl.basic.tools.file.FileWrapper;
import org.glassfish.pfl.basic.tools.file.Scanner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.glassfish.pfl.tf.timer.spi.TimingInfoProcessor;
import org.glassfish.pfl.tf.spi.Util;
import org.glassfish.pfl.tf.spi.annotation.MethodMonitorGroup;
import org.objectweb.asm.AnnotationVisitor;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/** Scan all classes looking for annotations annotated with @MethodMonitorGroup,
 * and saves the internal names of any such annotations.
 *
 * @author ken
 */
public class AnnotationScannerAction implements Scanner.Action {
    private static Class<MethodMonitorGroup> MMG_CLASS =
        MethodMonitorGroup.class ;

    private static String MMG_DESCRIPTOR =
        Type.getType(MMG_CLASS).getDescriptor() ;

    private final Util util ;
    private final TimingInfoProcessor tip ;

    // NOTE: this is a set of annotation class names in INTERNAL format.
    private Set<String> annotationNames = new HashSet<String>() ;
    private String currentClass ;

    AnnotationScannerAction(Util util, TimingInfoProcessor tip) {
        this.util = util ;
        this.tip = tip ;
    }

    public Set<String> getAnnotationNames() {
        return annotationNames ;
    }

    private class ClassScanner extends ClassVisitor {
        
        private boolean visitingAnnotation = false;
        private String timerGroupDescription ;
        private String timerGroupName ;
        private List<Type> timerGroupMembers ;
        
        private ClassScanner() {
            super(Opcodes.ASM9);
        }
        
        @Override
        public void visit( int version, int access, String name, String signature,
            String superName, String[] interfaces ) {
            util.info( 3, "Visiting class " + name ) ;

            if ((access & Opcodes.ACC_ANNOTATION) == Opcodes.ACC_ANNOTATION) {
                util.info( 2, "\t(Annotation)") ;
                // We are only interested in classes that are annotations
                currentClass = name ;
            }
        }
        
         @Override
        public AnnotationVisitor visitAnnotation( String desc,
            boolean visible ) {

            util.info( 3, "\tVisiting annotation " + desc ) ;

            if (desc.equals( MMG_DESCRIPTOR )) {
                // Leave name in internal form.
                annotationNames.add( currentClass  );
                visitingAnnotation = true ;
                timerGroupName = getGroupName( currentClass ) ;
                timerGroupDescription = "TimerGroup for Annotation "
                    + timerGroupName ;
                timerGroupMembers = new ArrayList<>() ;

                return new AnnoScanner(timerGroupDescription, timerGroupName, timerGroupMembers);
            }

            return null ;
        }
        
        private String getGroupName(String desc) {
            String result = desc ;
            final int index = desc.lastIndexOf('/') ;

            if (index >= 0) {
                result = desc.substring( index + 1 ) ;
            }

            return result ;
        }
        
        // Don't visit fields or methods: we don't need to look at their 
        // annotations in this visitor.
        @Override
        public MethodVisitor visitMethod( final int access, final String name,
            final String desc, final String signature, 
            final String[] exceptions ) {

            return null ;
        }
        
        @Override
        public FieldVisitor visitField( final int access, final String name,
            final String desc, final String signature,
            final Object value ) {

            return null ;
        }
    }
    
    private class AnnoScanner extends AnnotationVisitor {
        private boolean visitingAnnotation;
        private String annotationValueName = null ;
        private String timerGroupDescription ;
        private final String timerGroupName ;
        private List<Type> timerGroupMembers ;
        
        private AnnoScanner(String timerGroupDesc, String timeGroupName, List<Type> timerGroupMembers) {
            super(Opcodes.ASM9);
            visitingAnnotation = true;
            this.timerGroupDescription = timerGroupDesc;
            this.timerGroupName = timeGroupName;
            this.timerGroupMembers = timerGroupMembers;
        }

        @Override 
        // visit an Annotation member
        public void visit( String name, Object value ) {
            if (name == null) {
                // This is called after visitArray
                if (annotationValueName.equals( "value" )) {
                    timerGroupMembers.add( (Type)value ) ;
                }
            } else if(name.equals("description")) {
                if (value instanceof String) { 
                    timerGroupDescription = (String)value ;
                }
            } else if (name.equals( "value")) {
                if (value instanceof Type[]) {
                    // This normally doesn't happen: it seems that ASM
                    // always visits the array elements.
                    timerGroupMembers = Arrays.asList( (Type[])value ) ;
                }
            }
        }

        @Override
        public AnnotationVisitor visitArray( String name ) {
            annotationValueName = name ;
            return this ;
        }

        @Override
        // Only interested in the visitEnd on an Annotation!
        public void visitEnd() {
            if (visitingAnnotation) {
                visitingAnnotation = false ;

                tip.addTimerGroup( timerGroupName, timerGroupDescription ) ;
                for (Type type : timerGroupMembers) {
                    String name = type.getClassName() ;
                    final int index = name.lastIndexOf('.') ;
                    if (index >= 0) {
                        name = name.substring(index + 1) ;
                    }
                    tip.contains( name ) ;
                }
            }
        }
    }

    @Override
    public boolean evaluate(FileWrapper fw) {
        try {
            byte[] inputData = fw.readAll();
            ClassReader cr = new ClassReader( inputData ) ;
            ClassVisitor as = new ClassScanner();
            cr.accept( as, 0 );
        } catch (IOException ex) {
            return true ; // ignore things we can't read
        }

        return true ;
    }
}

