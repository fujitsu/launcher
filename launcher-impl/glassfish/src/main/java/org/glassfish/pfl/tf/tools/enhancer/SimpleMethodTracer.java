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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.glassfish.pfl.tf.spi.Util;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 *
 * @author ken
 */
public class SimpleMethodTracer extends MethodVisitor {
    final Util util ;
    final Set<Label> visitedLables = new HashSet<Label>() ;

    private void msg( String str ) {
        util.info( 2, "---" + str ) ;
    }

    public SimpleMethodTracer( MethodVisitor mv, Util util ) {
        super(Opcodes.ASM9, mv);
        this.util = util ;
    }

    public AnnotationVisitor visitAnnotationDefault() {
        msg( "visitAnnotationDefault") ;
        return mv.visitAnnotationDefault() ;
    }

    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        msg( "visitAnnotation(desc=" + desc + ",visible=" + visible + ")" ) ;
        return mv.visitAnnotation(desc, visible) ;
    }

    public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
        msg( "visitParameterAnnotation(parameter=" + parameter
            + ".desc=" + desc + ",visible=" + visible + ")" ) ;
        return mv.visitParameterAnnotation(parameter, desc, visible) ;
    }

    public void visitAttribute(Attribute attr) {
        msg( "visitAttribute(attr=" + attr + ")" ) ;
        mv.visitAttribute( attr ) ;
    }

    public void visitCode() {
        msg( "visitCode" ) ;
        mv.visitCode() ;
    }

    private String getFrameType( int type ) {
        switch (type) {
            case Opcodes.F_APPEND : return "APPEND" ;
            case Opcodes.F_CHOP : return "CHOP" ;
            case Opcodes.F_FULL : return "FULL" ;
            case Opcodes.F_NEW  : return "NEW" ;
            case Opcodes.F_SAME : return "SAME" ;
            case Opcodes.F_SAME1 : return "SAME1" ;
        }
        return "BAD_FRAME_TYPE" ;
    }


    public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
        msg( "visitFrame(type=" + getFrameType(type)
            + ",nLocal=" + nLocal + ",local=" + Arrays.asList( local )
            + ",nStack=" + nStack + ",stack=" + Arrays.asList( stack ) + ")" ) ;

        mv.visitFrame( type, nLocal, local, nStack, stack ) ;
    }

    public void visitInsn(int opcode) {
        msg( "visitInsn(opcode=" + Util.opcodeToString(opcode) + ")" ) ;
        mv.visitInsn( opcode ) ;
    }

    public void visitIntInsn(int opcode, int operand) {
        msg( "visitIntInsn(opcode=" + Util.opcodeToString(opcode)
            + ",operand=" + operand + ")" ) ;
        mv.visitIntInsn( opcode, operand ) ;
    }

    public void visitVarInsn(int opcode, int var) {
        msg( "visitVarInsn(opcode=" + Util.opcodeToString(opcode)
            + ",var=" + var + ")" ) ;
        mv.visitVarInsn(opcode, var);
    }

    public void visitTypeInsn(int opcode, String type) {
        msg( "visitTypeInsn(opcode=" + Util.opcodeToString(opcode)
            + ",type=" + type + ")" ) ;
        mv.visitTypeInsn(opcode, type);
    }

    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        msg( "visitFieldInsn(opcode=" + Util.opcodeToString(opcode) 
            + ",owner=" + owner + ",name=" + name + ",desc=" + desc + ")" ) ;
        mv.visitFieldInsn(opcode, owner, name, desc);
    }

    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
        msg( "visitMethodInsn(opcode=" + Util.opcodeToString(opcode)
            + ",owner=" + owner + ",name=" + name + ",desc=" + desc + ")" ) ;
        mv.visitMethodInsn(opcode, owner, name, desc);
    }

    public void visitJumpInsn(int opcode, Label label) {
        msg( "visitJumpInsn(opcode=" + Util.opcodeToString(opcode)
            + ",label=" + label + ")" ) ;
        mv.visitJumpInsn(opcode, label);
    }

    public void visitLabel(Label label) {
        msg( "visitLabel(label=" + label + ")" ) ;
        visitedLables.add( label ) ;
        mv.visitLabel(label);
    }

    public void visitLdcInsn(Object cst) {
        msg( "visitLdcInsn(cst=" + cst + ")" ) ;
        mv.visitLdcInsn(cst);
    }

    public void visitIincInsn(int var, int increment) {
        msg( "visitIincInsn(var=" + var
            + ",increment=" + increment + ")" ) ;
        mv.visitIincInsn(var, increment);
    }

    public void visitTableSwitchInsn(int min, int max, Label dflt, Label[] labels) {
        msg( "visitTableSwitchInsn" ) ;
        mv.visitTableSwitchInsn(min, max, dflt, labels);
    }

    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        msg( "visitLookupSwitchInsn" ) ;
        mv.visitLookupSwitchInsn(dflt, keys, labels);
    }

    public void visitMultiANewArrayInsn(String desc, int dims) {
        msg( "visitMultiANewArrayInsn(desc=" + desc + ",dims=" + dims + ")" ) ;
        mv.visitMultiANewArrayInsn(desc, dims);
    }

    public void visitTryCatchBlock(Label start, Label end, Label handler,
        String type) {

        /* Ignore these rules: it seems that ASM doesn't really follow them,
         * and this causes what appears to be a spurious failure.
         *
        if (visitedLables.contains(start)) {
            throw new RuntimeException(
                "visiting try-catch block: start label has already been visited" ) ;
        }

        if (visitedLables.contains(end)) {
            throw new RuntimeException(
                "visiting try-catch block: end label has already been visited" ) ;
        }

        if (visitedLables.contains(handler)) {
            throw new RuntimeException(
                "visiting try-catch block: handler label has already been visited" ) ;
        }
        */

        msg( "visitTryCatchBlock(start=" + start + ",end=" + end
            + ",handler=" + handler + ",type=" + type + ")" ) ;
        mv.visitTryCatchBlock(start, end, handler, type);
    }

    public void visitLocalVariable(String name, String desc, String signature,
        Label start, Label end, int index) {

        if (!visitedLables.contains( start )) {
            throw new RuntimeException(
                "visitLocalVariable: start label has not been visited" ) ;
        }

        if (!visitedLables.contains( end )) {
            throw new RuntimeException(
                "visitLocalVariable: end label has not been visited" ) ;
        }

        msg( "visitLocalVariable(name=" + name + ",desc=" + desc
            + ",signature=" + signature + ",start=" + start + ",end=" + end
            + ",index=" + index + ")" ) ;
        mv.visitLocalVariable(name, desc, signature, start, end, index);
    }

    public void visitLineNumber(int line, Label start) {
        msg( "visitLineNumber(line=" + line + ",start=" + start + ")" ) ;
        mv.visitLineNumber(line, start);
    }

    public void visitMaxs(int maxStack, int maxLocals) {
        msg( "visitMaxs(maxStack=" + maxStack 
            + ",maxLocals=" + maxLocals + ")" ) ;
        mv.visitMaxs(maxStack, maxLocals);
    }

    public void visitEnd() {
        msg( "visitEnd" ) ;
        mv.visitEnd();
    }
}
