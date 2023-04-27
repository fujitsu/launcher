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

import org.glassfish.pfl.tf.spi.EnhancedClassData;
import org.glassfish.pfl.tf.spi.TraceEnhancementException;
import org.glassfish.pfl.tf.spi.annotation.TFEnhanced;
import org.glassfish.pfl.tf.spi.annotation.TraceEnhanceLevel;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 *
 * @author ken
 */
public class TFEnhanceAdapter extends ClassVisitor {
    private static final String TFENHANCED_ANNO_DESC =
        Type.getDescriptor( TFEnhanced.class ) ;
    private static final String TRACE_ENHANCE_LEVEL_DESC =
        Type.getDescriptor( TraceEnhanceLevel.class ) ;

    private boolean firstCall = true ;


    // Holder for actual value of TFEnhanced annotation (NONE if no
    // annotation present.
    private final TraceEnhanceLevel[] present = new TraceEnhanceLevel[1] ;

    private final TraceEnhanceLevel required ; // required for next phase to run
    private final TraceEnhanceLevel result ;   // if at required level, resulting level

    private final EnhancedClassData ecd ;

    public TFEnhanceAdapter( ClassVisitor cv, TraceEnhanceLevel required,
        TraceEnhanceLevel result, EnhancedClassData ecd ) {
        super(Opcodes.ASM9, cv);
        this.required = required ;
        this.result = result ;
        this.ecd = ecd ;
        present[0] = TraceEnhanceLevel.NONE ;
    }

    private void checkForTFEnhanceAnnotation() {
        if (firstCall) {
            firstCall = false ;
            if (present[0] != required) {
                throw new TraceEnhancementException(
                    "Class " + ecd.getClassName()
                    + " has trace enhancement level " + present[0] 
                    + " but " + required + " is required.") ;
            }

            // Write out annotation with result level.
            AnnotationVisitor av = super.visitAnnotation( TFENHANCED_ANNO_DESC,
                true ) ;
            av.visitEnum( "stage", TRACE_ENHANCE_LEVEL_DESC, result.name() ) ;
            av.visitEnd() ;
        }
    }

    @Override
    public void visitInnerClass( String name,
        String outerName, String innerName, int access ) {

        checkForTFEnhanceAnnotation();
        super.visitInnerClass( name, outerName, innerName, access ) ;
    }

    @Override
    public FieldVisitor visitField( int access, String name, String desc,
        String signature, Object value ) {

        checkForTFEnhanceAnnotation();
        return super.visitField(access, name, desc, signature, value) ;
    }

    @Override
    public MethodVisitor visitMethod( int access, String name, String desc,
        String signature, String[] exceptions ) {

        checkForTFEnhanceAnnotation();
        return super.visitMethod(access, name, desc, signature, exceptions) ;
    }

    @Override
    public AnnotationVisitor visitAnnotation( String desc, boolean isVisible ) {
        if (desc.equals( TFENHANCED_ANNO_DESC )) {
            // Consume the TFEnhanced annotation here.  We'll write out a new
            // one above.
            return new AnnotationVisitor(Opcodes.ASM9) {
                public void visit(String name, Object value) {
                }

                public void visitEnum(String name, String desc, String value) {
                    if (name.equals( "stage")) {
                        present[0] = Enum.valueOf( TraceEnhanceLevel.class, 
                            value ) ;
                    }
                }

                public AnnotationVisitor visitAnnotation(String name, 
                    String desc) {

                    return null ;
                }

                public AnnotationVisitor visitArray(String name) {
                    return null ;
                }

                public void visitEnd() {
                }
            } ;
        } else {
            final AnnotationVisitor av = super.visitAnnotation( desc, isVisible ) ;
            return av ;
        }
    }
}
