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

import org.glassfish.pfl.basic.contain.SynchronizedHolder;
import java.util.HashSet;
import java.util.Set;
import org.glassfish.pfl.tf.spi.EnhancedClassData;
import org.glassfish.pfl.tf.spi.MethodMonitor;
import org.glassfish.pfl.tf.spi.Util;
import org.glassfish.pfl.tf.spi.annotation.TraceEnhanceLevel;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.commons.LocalVariablesSorter;

public class ClassTracer extends TFEnhanceAdapter {
    // Worst case: call to MethodMonitor.info requires 4 words on the stack.
    // MethodMonitor.enter requires creating an Object[],
    // which means the stack can be
    //
    // mm int Object[] Object[] int long
    //
    // during the construction of the call to enter. This adds at most
    // 7 words to the stack.
    private static final int MAX_EXTRA_STACK = 7 ;

    private void info( final int level, final String msg ) {
        util.info( level, "ClassTracer: " + msg ) ;
    }

    // Sequence to replace: ACONST_NULL, ICONST_0, INVOKESPECIAL to an
    // InfoMethod.

    public enum Input { ACONST_NULL_BC, ICONST_0_BC, INFO_METHOD_CALL, OTHER }

    public enum State {
        NULL1() {
            @Override
            public State transition( final Util util, final MethodVisitor mv,
                final Input input ) {

                info( util, 3, "State transition: NULL1 state, Input " + input ) ;
                switch (input) {
                    case ICONST_0_BC :
                        return State.NULL2 ;

                    case ACONST_NULL_BC :
                    case INFO_METHOD_CALL :
                    case OTHER :
                        info( util, 4, "Emitting 1 ACONST_NULL" ) ;
                        mv.visitInsn( Opcodes.ACONST_NULL ) ;
                        return State.NORMAL ;
                }
                return null ;
            }
        },

        NULL2() {
            @Override
            public State transition( final Util util, final MethodVisitor mv,
                final Input input ) {

                info( util, 3, "State transition: NULL2 state, Input " + input ) ;
                switch (input) {
                    case ICONST_0_BC :
                    case ACONST_NULL_BC :
                    case OTHER :
                        info( util, 4, "Emitting ACONST_NULL,ICONST_0" ) ;
                        mv.visitInsn( Opcodes.ACONST_NULL ) ;
                        mv.visitInsn( Opcodes.ICONST_0 ) ;
                        return State.NORMAL ;

                    case INFO_METHOD_CALL :
                        return State.NORMAL ;
                }
                return null ;
            }
        },

        NORMAL() {
            @Override
            public State transition( final Util util, final MethodVisitor mv,
                final Input input ) {

                info( util, 3, "State transition: NORMAL state, Input " + input ) ;
                switch (input) {
                    case ACONST_NULL_BC :
                        return State.NULL1 ;

                    case INFO_METHOD_CALL :
                    case ICONST_0_BC :
                    case OTHER :
                        return State.NORMAL ;
                }
                return null ;
            }
        } ;

	private static void info( final Util util, final int level,
            final String msg ) {

	    util.info( level, "ClassTracer.State: " + msg ) ;
	}


        public abstract State transition( Util util, MethodVisitor mv,
            Input input ) ;
    }

    private final Util util ;
    private final EnhancedClassData ecd ;

    private State current = State.NORMAL ;

    public ClassTracer( final Util util, final EnhancedClassData ecd,
        final ClassVisitor cv ) {

        super( cv, TraceEnhanceLevel.PHASE1, TraceEnhanceLevel.PHASE2, ecd ) ;
        this.util = util ;
        this.ecd = ecd ;
    }

    // - Scan method body:
    //   - for each return, add the finally body
    //   - for each call to an InfoMethod, add extra parameters to the
    //     end of the call (note that it is MUCH easier to recognize the
    //     end than the start of a method call, since nested calls and
    //     complex expressions make recognizing the start quite difficult)
    // - add preamble
    // - add outer exception handler
    private class MonitoredMethodEnhancer extends MethodVisitor {
        private final int access ;
        private final String name ;
        private final String desc ;
        private final MethodVisitor lmv ;
        private final int identVal ;

        private final Set<Integer> returnOpcodes = new HashSet<Integer>() ;

        private final Label start = new Label() ;
        private final LabelNode startNode = new LabelNode( start ) ;

        private final Label excHandler = new Label() ;
        private final LabelNode excHandlerNode = new LabelNode( excHandler ) ;

        private final Label end = new Label() ;
        private final LabelNode endNode = new LabelNode( end ) ;

        private final Label afterExcStore = new Label() ;
        private final LabelNode afterExcStoreNode = new LabelNode( end ) ;

        // Values must be set in setLocalVariablesSorter.
        private LocalVariablesSorter lvs = null ;
        private LocalVariableNode __result = null ;
        private LocalVariableNode __mm = null ;
        private LocalVariableNode __exc = null ;

        public void setLocalVariablesSorter( final LocalVariablesSorter lvs ) {
            this.lvs = lvs ;

            Type type = Type.getReturnType( desc ) ;

            if (!type.equals( Type.VOID_TYPE)) {
                __result = new LocalVariableNode( "__$result$__",
                    type.getDescriptor(),
                    null, startNode, endNode, lvs.newLocal(type) ) ;
            } else {
                __result = null ;
            }

            type = Type.getType( MethodMonitor.class );
            __mm = new LocalVariableNode( "__$mm$__",
                type.getDescriptor(),
                null, startNode, endNode, lvs.newLocal(type) ) ;

            type = Type.getType( Throwable.class ) ;
            __exc = new LocalVariableNode( "__$exc$__",
                type.getDescriptor(), 
                null, excHandlerNode, endNode, lvs.newLocal(type) ) ;
        }

        public MonitoredMethodEnhancer( final int access, final String name,
            final String desc, final MethodVisitor mv ) {
            super(Opcodes.ASM9, mv);
            this.access = access ;
            this.name = name ;
            this.desc = desc ;
            this.lmv = mv ;
            this.identVal = ecd.getMethodIndex(name) ;

            returnOpcodes.add( Opcodes.RETURN ) ;
            returnOpcodes.add( Opcodes.IRETURN ) ;
            returnOpcodes.add( Opcodes.ARETURN ) ;
            returnOpcodes.add( Opcodes.LRETURN ) ;
            returnOpcodes.add( Opcodes.FRETURN ) ;
            returnOpcodes.add( Opcodes.DRETURN ) ;
        }

        /*
        private Object getTypeForStackMap( Type type ) {
            switch (type.getSort()) {
                case Type.VOID :
                    return null ;
                case Type.BOOLEAN :
                case Type.CHAR :
                case Type.BYTE :
                case Type.SHORT :
                case Type.INT :
                    return Opcodes.INTEGER ;
                case Type.LONG :
                    return Opcodes.LONG ;
                case Type.FLOAT :
                    return Opcodes.FLOAT ;
                case Type.DOUBLE :
                    return Opcodes.DOUBLE ;
                case Type.ARRAY :
                case Type.OBJECT :
                    return type.getInternalName() ;
            }

            return null ;
        }
         */

        @Override
        public void visitCode() {
            info( 2, "visitCode" ) ;

            // visit try-catch block BEFORE visiting start label!
            // But that "requirement" (violated in Kuleshov's AOSD 07 paper)
            // makes it very hard to generate finally handlers in the correct order.
            // Moving the visitTryCatchBlock calls to visitMaxs.
            //
            // lmv.visitTryCatchBlock( start, end, excHandler, null );
            // lmv.visitTryCatchBlock( excHandler, afterExcStore, excHandler, null );

/*            final Object rt = getTypeForStackMap( Type.getReturnType( desc ) )  ;
            final Object[] locals = (rt == null)
                ? new Object[] { ecd.getClassName(),
                    EnhancedClassData.OBJECT_NAME, EnhancedClassData.MM_NAME }
                : new Object[] { ecd.getClassName(), rt,
                    EnhancedClassData.OBJECT_NAME, EnhancedClassData.MM_NAME } ;

            Object[] stack = new Type[] { } ;
            lmv.visitFrame(Opcodes.F_NEW, locals.length, locals, 
                stack.length, stack) ;
*/
            // __result = null or 0 (type specific, omitted if void return)
            if (__result != null) {
                util.initLocal( lmv, __result ) ;
            }

            // final MethodMonitor __mm = __mmXX.content() ;
            // (for the appropriate XX for this method)
            final String fullDesc = util.getFullMethodDescriptor(name,desc) ;
            info( 2, "fullDesc = " + fullDesc ) ;

            final String fname = ecd.getHolderName( fullDesc );

            lmv.visitFieldInsn( Opcodes.GETSTATIC, ecd.getClassName(),
                fname, Type.getDescriptor( SynchronizedHolder.class ));
            lmv.visitMethodInsn( Opcodes.INVOKEVIRTUAL,
                EnhancedClassData.SH_NAME, "content",
                "()Ljava/lang/Object;" );
            lmv.visitTypeInsn( Opcodes.CHECKCAST,
                EnhancedClassData.MM_NAME );
            lmv.visitVarInsn( Opcodes.ASTORE, __mm.index );

            // if (__mm != null) {
            lmv.visitVarInsn( Opcodes.ALOAD, __mm.index ) ;
            lmv.visitJumpInsn( Opcodes.IFNULL, start );

            // __mm.enter( __ident, <array of wrapped args> ) ;
            lmv.visitVarInsn( Opcodes.ALOAD, __mm.index )  ;
            util.emitIntConstant( lmv, identVal ) ;

            util.wrapArgs( lmv, access, desc ) ;

            lmv.visitMethodInsn( Opcodes.INVOKEINTERFACE,
                EnhancedClassData.MM_NAME, "enter",
                "(I[Ljava/lang/Object;)V" ) ;

            // }
            lmv.visitLabel( start ) ;
        }

        private void emitExceptionReport( final int excIndex ) {
            info( 2, "emitExceptionReport called" ) ;
            final Label skipLabel = new Label() ;
            lmv.visitVarInsn( Opcodes.ALOAD, __mm.index ) ;
            lmv.visitJumpInsn( Opcodes.IFNULL, skipLabel ) ;

            lmv.visitVarInsn( Opcodes.ALOAD, __mm.index ) ;
            util.emitIntConstant( lmv, identVal ) ;
            lmv.visitVarInsn( Opcodes.ALOAD, excIndex ) ;

            lmv.visitMethodInsn( Opcodes.INVOKEINTERFACE,
                EnhancedClassData.MM_NAME, "exception",
                "(ILjava/lang/Throwable;)V" ) ;

            lmv.visitLabel( skipLabel ) ;
        }

        private void emitFinally() {
            info( 2, "emitFinally called" ) ;
            final Label skipLabel = new Label() ;
            lmv.visitVarInsn( Opcodes.ALOAD, __mm.index ) ;
            lmv.visitJumpInsn( Opcodes.IFNULL, skipLabel ) ;

            lmv.visitVarInsn( Opcodes.ALOAD, __mm.index ) ;
            util.emitIntConstant(lmv, identVal ) ;

            final Type rtype = Type.getReturnType( desc ) ;
            if (rtype.equals( Type.VOID_TYPE )) {
                lmv.visitMethodInsn( Opcodes.INVOKEINTERFACE,
                    EnhancedClassData.MM_NAME, "exit",
                    "(I)V" ) ;
            } else {
                util.wrapArg( lmv, __result.index,
                    Type.getType( __result.desc ) ) ;

                lmv.visitMethodInsn( Opcodes.INVOKEINTERFACE,
                    EnhancedClassData.MM_NAME, "exit",
                    "(ILjava/lang/Object;)V" ) ;
            }

            lmv.visitLabel( skipLabel ) ;
        }

        @Override
        public void visitInsn(final int opcode) {
            info( 2, "visitInsn[" + Util.opcodeToString(opcode) + "] called") ;
            if (opcode == Opcodes.ACONST_NULL) {
                current = current.transition( util, mv, Input.ACONST_NULL_BC ) ;

                if (current == State.NORMAL) {
                    lmv.visitInsn(opcode);
                }
            } else if (opcode == Opcodes.ICONST_0) {
                current = current.transition( util, mv, Input.ICONST_0_BC ) ;

                if (current == State.NORMAL) {
                    lmv.visitInsn(opcode);
                }
            } else {
                current = current.transition( util, mv, Input.OTHER ) ;

                if (opcode == Opcodes.ATHROW) {
                    info( 2, "handling throw" ) ;
                    final int exc = lvs.newLocal(
                        Type.getType(Throwable.class)) ;

                    lmv.visitVarInsn( Opcodes.ASTORE, exc) ;

                    emitExceptionReport( exc ) ;

                    // restore exception from local for following ATHROW
                    // (this will be caught in the finally exception handler,
                    // which will handle calling MethodMonitor.exit).
                    lmv.visitVarInsn( Opcodes.ALOAD, exc ) ;
                } else if (returnOpcodes.contains(opcode)) {
                    info( 2, "handling return" ) ;
                    util.storeFromXReturn( lmv, opcode, __result ) ;

                    emitFinally() ;

                    util.loadFromXReturn( lmv, opcode, __result ) ;
                } 

                lmv.visitInsn(opcode);
            }
        }

        @Override
        public void visitMethodInsn( final int opcode, final String owner,
            final String name, final String desc ) {
            info( 2, "MM method: visitMethodInsn["
                + Util.opcodeToString(opcode) + "]: " + owner
                + "." + name + desc ) ;

            // If opcode is INVOKESPECIAL, owner is this class, and name/desc
            // are in the infoMethodDescs set, update the desc for the call
            // and add the extra parameters to the end of the call.
            final String fullDesc = util.getFullMethodDescriptor( name, desc ) ;
            if ((opcode == Opcodes.INVOKESPECIAL)
                && (owner.equals( ecd.getClassName() )
                && (ecd.classifyMethod(fullDesc)
                    == EnhancedClassData.MethodType.INFO_METHOD))) {

                info( 2, "rewriting method call" ) ;
                current = current.transition( util, lmv,
                    Input.INFO_METHOD_CALL ) ;

                lmv.visitVarInsn( Opcodes.ALOAD, __mm.index ) ;
                util.emitIntConstant(lmv, identVal );

                lmv.visitMethodInsn(opcode, owner, name, desc );
            } else {
                current = current.transition( util, lmv, Input.OTHER ) ;

                lmv.visitMethodInsn(opcode, owner, name, desc );
            }
        }

        @Override
        public void visitMaxs( final int maxStack, final int maxLocals ) {
            info( 2, "MM method: visitMaxs" ) ;
            lmv.visitLabel( end  ) ;
            lmv.visitLabel( excHandler  ) ;

            // Here these finally blocks will be generated AFTER any 
            // finally blocks in the wrapper code.
            lmv.visitTryCatchBlock( start, end, excHandler, null );
            lmv.visitTryCatchBlock( excHandler, afterExcStore, excHandler, null );
            
            // Store the exception
            lmv.visitVarInsn( Opcodes.ASTORE, __exc.index ) ;

            lmv.visitLabel( afterExcStore ) ;

            emitFinally() ;

            // throw the exception
            lmv.visitVarInsn( Opcodes.ALOAD, __exc.index ) ;
            lmv.visitInsn( Opcodes.ATHROW ) ;

            // visit local variables AFTER visiting labels!
            if (__result != null) {
                __result.accept( lmv ) ;
            }

            __mm.accept( lmv ) ;
            __exc.accept( lmv ) ;

            lmv.visitMaxs( maxStack + MAX_EXTRA_STACK, maxLocals ) ;
        }

        @Override
        public void visitIntInsn(final int opcode, final int operand) {
            info( 2, "visitIntInsn[" + Util.opcodeToString(opcode)
                + "] operand=" + operand ) ;
            current = current.transition( util, mv, Input.OTHER ) ;
            lmv.visitIntInsn(opcode, operand);
        }

        @Override
        public void visitVarInsn(final int opcode, final int var) {
            info( 2, "visitVarInsn[" + Util.opcodeToString(opcode)
                + "] var=" + var ) ;
            current = current.transition( util, mv, Input.OTHER ) ;
            lmv.visitVarInsn(opcode, var);
        }

        @Override
        public void visitTypeInsn(final int opcode, final String type) {
            info( 2, "visitTypeInsn[" + Util.opcodeToString(opcode)
                + "] type=" + type ) ;
            current = current.transition( util, mv, Input.OTHER ) ;
            lmv.visitTypeInsn(opcode, type);
        }

        @Override
        public void visitFieldInsn(
            final int opcode,
            final String owner,
            final String name,
            final String desc)
        {
            info( 2, "visitFieldInsn[" + Util.opcodeToString(opcode)
                + "] " + owner + "." + name + desc ) ;
            current = current.transition( util, mv, Input.OTHER ) ;
            lmv.visitFieldInsn(opcode, owner, name, desc);
        }

        @Override
        public void visitJumpInsn(final int opcode, final Label label) {
            info( 2, "visitTypeInsn[" + Util.opcodeToString(opcode)
                + "] label=" + label ) ;
            current = current.transition( util,  mv, Input.OTHER ) ;
            lmv.visitJumpInsn(opcode, label);
        }

        @Override
        public void visitLdcInsn(final Object cst) {
            info( 2, "visitLdcInsn " + cst ) ;
            current = current.transition( util,  mv, Input.OTHER ) ;
            lmv.visitLdcInsn(cst);
        }

        @Override
        public void visitIincInsn(final int var, final int increment) {
            info( 2, "visitIincInsn " + " var=" + var
                + " increment=" + increment ) ;
            current = current.transition( util, mv, Input.OTHER ) ;
            lmv.visitIincInsn(var, increment);
        }

        @Override
        public void visitTableSwitchInsn(
            final int min,
            final int max,
            final Label dflt,
            final Label[] labels)
        {
            current = current.transition( util, mv, Input.OTHER ) ;
            lmv.visitTableSwitchInsn(min, max, dflt, labels);
        }

        @Override
        public void visitLookupSwitchInsn(
            final Label dflt,
            final int[] keys,
            final Label[] labels)
        {
            current = current.transition( util, mv, Input.OTHER ) ;
            lmv.visitLookupSwitchInsn(dflt, keys, labels);
        }

        @Override
        public void visitMultiANewArrayInsn(final String desc, final int dims) {
            current = current.transition( util, mv, Input.OTHER ) ;
            lmv.visitMultiANewArrayInsn(desc, dims);
        }
    } // end of MonitoredMethodEnhancer

    @Override
    public MethodVisitor visitMethod( final int access, final String name,
        final String desc, final String sig, final String[] exceptions ) {
        info( 2, "visitMethod: " + name + desc ) ;
        // Enhance the class first (part 1).
        // - Modify all of the @InfoMethod methods with extra arguments
        // - Modify all calls to @InfoMethod methods to add the extra arguments
        //   or to flag an error if NOT called from an MM method.

        final String fullDesc = util.getFullMethodDescriptor(name, desc) ;
        final EnhancedClassData.MethodType mtype =
            ecd.classifyMethod(fullDesc) ;

        MethodVisitor mv = super.visitMethod( access, name, desc,
            sig, exceptions ) ;
        if (util.getDebug()) {
            mv = new SimpleMethodTracer(mv, util) ;
        }

        switch (mtype) {
            case STATIC_INITIALIZER :
            case INFO_METHOD :
            case NORMAL_METHOD :
                return mv ;

            case MONITORED_METHOD :
                final MonitoredMethodEnhancer mme = new MonitoredMethodEnhancer(
                    access, name, desc, mv ) ;
                // AnalyzerAdapter aa = new AnalyzerAdapter( ecd.getClassName(),
                    // access, name, desc, mme ) ;
                final LocalVariablesSorter lvs = new LocalVariablesSorter( access,
                    desc, mme ) ;
                mme.setLocalVariablesSorter(lvs);

                return lvs ;
        }

        return null ;
    }
} // end of ClassTracer
