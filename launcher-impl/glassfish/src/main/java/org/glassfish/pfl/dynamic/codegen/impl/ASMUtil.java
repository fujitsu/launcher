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

package org.glassfish.pfl.dynamic.codegen.impl;

import java.util.Properties ;
import java.util.HashSet ;
import java.util.HashMap ;

import java.io.IOException ;
import java.io.PrintStream ;
import java.io.FileOutputStream ;
import java.io.File ;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

// Imports for verify method
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.CheckClassAdapter;
// end of verify method imports

import org.glassfish.pfl.dynamic.codegen.spi.ImportList ;
import org.glassfish.pfl.dynamic.codegen.spi.Type ;
import org.glassfish.pfl.dynamic.codegen.spi.Variable ;
import org.glassfish.pfl.dynamic.codegen.spi.Wrapper ;
import java.io.PrintWriter;
import org.glassfish.pfl.basic.contain.Pair;
import org.glassfish.pfl.basic.func.NullaryFunction;

/** Simple class containing a few ASM-related utilities 
 * and dynamic attributes needs for the byte code generator.
 */
public class ASMUtil {
    public enum RequiredEmitterType { GETTER, SETTER, NONE } ;

    public static String bcName( Type type ) {
	return type.name().replace( '.', '/' ) ;
    }

    private static void displayNode( PrintStream ps, String msg, Node node ) {
	ps.println( ) ;
	ps.println( "=======================================================" ) ;
	ps.println( msg ) ;
	Util.display( node, ps ) ;	
	ps.println() ;
	Util.checkTree( node, ps ) ;
	ps.println() ;
	ps.println( "=======================================================" ) ;
    }

    public static void generateSourceCode( PrintStream ps, ClassGeneratorImpl cg,
	ImportList imports, Properties options ) throws IOException {

	TreeWalkerContext context = new TreeWalkerContext() ;
	Visitor visitor = new SourceStatementVisitor( context, 
	    imports, new CodegenPrinter( ps ) ) ;
	cg.accept( visitor ) ;
    }

    public static File getFile( String genDir, String className, 
	String suffix ) {

	Pair<String,String> names = Wrapper.splitClassName( className ) ;
	String pkgName = names.first().replace( '.', File.separatorChar ) ;
	File sdir = new File( genDir, pkgName ) ;
	sdir.mkdirs() ; // make sure the directory exists; may return false if already exists.
                        // Of course, it's not an error if the directory already exists.
	
	String sfname = names.second() + suffix ;

	File sfile = new File( sdir, sfname ) ;
	return sfile ;
    }

    public static void generateSourceCode( String sourceGenDir, ClassGeneratorImpl cg,
	ImportList imports, Properties options ) throws IOException {
	
	PrintStream ps = null ;

	try {
	    // Create a PrintStream for the source file.
	    File sfile = getFile( sourceGenDir, cg.name(), ".java" ) ;
	    ps = new PrintStream( sfile ) ;

	    // Write out the source code to the source file
	    generateSourceCode( ps, cg, imports, options ) ;
	} finally {
	    if (ps != null)
		ps.close() ;
	}
    }

/* Requires Apache constantpool package, which is not included in the ORB.
    private static final int CLASS_MAGIC = 0xCAFEBABE ;

    private static void readConstantPool( PrintStream ps, byte[] cldata ) {
	try {
	    ps.println( "*** Reading constant pool ***" ) ;
	    ConstantPool cp = new ConstantPool() ;
	    ByteArrayInputStream bos = new ByteArrayInputStream( cldata ) ;
	    DataInputStream dis = new DataInputStream( bos ) ;
	    int magic = dis.readInt() ;
	    ps.println( "Class magic = " + magic ) ;
	    if (magic != CLASS_MAGIC) {
		ps.println( "Bad magic" ) ;
		return ;
	    }

	    int minor = dis.readUnsignedShort() ;
	    int major = dis.readUnsignedShort() ;

	    ps.println( "Version: " + major + "." + minor ) ;
	    cp.read( dis, true ) ;
	    cp.resolve() ;
	} catch (Exception exc) {
	    ps.println( "Error in dumping constant pool: " + exc ) ;
	    exc.printStackTrace() ;
	}
    }
*/

    private static class FixStackSizeClassVisitor extends ClassVisitor {
        public FixStackSizeClassVisitor( final ClassVisitor cv ) {
            super(Opcodes.ASM9, cv);
        }

        @Override
        public MethodVisitor visitMethod( final int access, final String name,
            final String desc, final String signature, final String[] exceptions ) {
            MethodVisitor mv = cv.visitMethod( access, name, desc, signature, exceptions ) ;
            return mv == null ? null : new FixStackSizeMethodVisitor( mv ) ;
        }
    }

    private static class FixStackSizeMethodVisitor extends MethodVisitor {
        public FixStackSizeMethodVisitor( final MethodVisitor mv ) {
            super(Opcodes.ASM9, mv);
        }

        @Override
        public void visitMaxs( int maxStack, int maxLocals ) {
            // Make ASM calculate the stack size
            mv.visitMaxs( 0, 0 ) ;
        }
    }

    private static byte[] fixStackSize( byte[] code ) {
        // Debugging code: try to read/write it again to see what happens with
        // max stack size
        ClassReader cr = new ClassReader( code );
        ClassWriter cw = new ClassWriter( ClassWriter.COMPUTE_MAXS ) ;
        ClassVisitor visitor = new FixStackSizeClassVisitor( cw ) ;
        cr.accept( visitor, 0 ) ;
        return cw.toByteArray() ;
    }

    /** Given a completed ClassGeneratorImpl, use ASM to construct
     * the byte array representing the compiled class.
     */
    public static byte[] generate( ClassLoader cl, ClassGeneratorImpl cg,
	ImportList imports, Properties options, PrintStream debugOutput ) {

	// Make sure that ClassLoader cl is used where required (mainly in the
	// implementation of Type.classInfo, which is used in several places).
	CurrentClassLoader.set( cl ) ;
	
	// get options
	boolean dumpConstantPool = false ;
	boolean dumpAfterSetupVisitor = false ;
	boolean traceByteCodeGeneration = false ;
	boolean useAsmVerifier = false ;
	String classGenDir = null ; 
	String sourceGenDir = null ; 

	if (options != null) {
	    dumpConstantPool = Boolean.parseBoolean(
		options.getProperty( Wrapper.DUMP_CONSTANT_POOL )) ;
	    dumpAfterSetupVisitor = Boolean.parseBoolean(
		options.getProperty( Wrapper.DUMP_AFTER_SETUP_VISITOR )) ;
	    traceByteCodeGeneration = Boolean.parseBoolean(
		options.getProperty( Wrapper.TRACE_BYTE_CODE_GENERATION )) ;
	    useAsmVerifier = Boolean.parseBoolean( 
		options.getProperty( Wrapper.USE_ASM_VERIFIER )) ;
	    classGenDir = options.getProperty( 
		Wrapper.CLASS_GENERATION_DIRECTORY )  ;
	    sourceGenDir = options.getProperty( 
		Wrapper.SOURCE_GENERATION_DIRECTORY )  ;
	}

	if (sourceGenDir != null) {
	    try {
		generateSourceCode( sourceGenDir, cg, imports, options ) ;
	    } catch (IOException exc) {
		throw new IllegalArgumentException( 
		    "Could not generate source code for class " 
		    + cg.name(), exc ) ;
	    }
	}

        // have ASM compute max stack size
	ClassWriter cw = new ClassWriter( ClassWriter.COMPUTE_MAXS ) ; 

	// Prepare the tree for byte code generation.  We use a fresh
	// TreeWalker context for each pass with a visitor.
	TreeWalkerContext twc = new TreeWalkerContext() ;
	Visitor v1 = new ASMSetupVisitor( twc ) ;
	cg.accept( v1 ) ;
	if (dumpAfterSetupVisitor)
	    displayNode( debugOutput, "Contents of AST after SetupVisitor", cg ) ;

	// generate byte code
	twc = new TreeWalkerContext() ;
	Visitor v2 = new ASMByteCodeVisitor( twc, cw, traceByteCodeGeneration, 
	    debugOutput ) ;
	cg.accept( v2 ) ;

	byte[] result = fixStackSize( cw.toByteArray() ) ;

	if (dumpConstantPool) {
	    // readConstantPool( debugOutput, result ) ;
	}

	if (classGenDir != null) {
	    // Dump the generate bytecode to a directory for debugging.
	    File cfile = getFile( classGenDir, cg.name(), ".class" ) ;
	    FileOutputStream fos = null ;
	    try {
		fos = new FileOutputStream( cfile ) ;
		fos.write( result, 0, result.length ) ;
	    } catch (IOException exc) {
		throw new IllegalArgumentException( 
		    "Could not dump generated bytecode to file "
		    + cfile ) ;
	    } finally {
		if (fos != null)
		    try {
			fos.close() ;
		    } catch (IOException exc) {
			// ignore this
		    }
	    }
	}

	if (useAsmVerifier) {
	    debugOutput.println( "*** Using ASM verifier ***" ) ;
	    verify( debugOutput, result ) ;
	}

	return result ;
    }

    private static void verify( final PrintStream ps, byte[] classData ) {
	ClassReader cr = new ClassReader( classData ) ;
        PrintWriter pw = new PrintWriter( ps ) ;
        CheckClassAdapter.verify( cr, true, pw ) ;
        // pw.close();
    }

    // Function used to initialize Attribute<MyLabel> instances.
    private static NullaryFunction<MyLabel> makeLabel = 
	new NullaryFunction<MyLabel>() {
	    public MyLabel evaluate() {
		return new MyLabel() ;
	    }
	} ;

    // All attributes are package private so that they can be 
    // used in other parts of the codegen implementation.
    
    // Attribute on MethodGenerator that defines the label on the
    // return instruction at the end of the method.
    static Attribute<MyLabel> returnLabel = new Attribute<MyLabel>(
	MyLabel.class, "returnLabel", makeLabel ) ;

    // Attribute on Statement nodes that labels the start of 
    // the statement.
    static Attribute<MyLabel> statementStartLabel = new Attribute<MyLabel>(
	MyLabel.class, "statementStartLabel", makeLabel ) ;

    static Attribute<MyLabel> statementEndLabel = new Attribute<MyLabel>(
	MyLabel.class, "statementEndLabel", makeLabel ) ;

    // Attribute on BlockStatements in TryStatements used to label
    // the end of the Block.  Needed for generating exception table.
    static Attribute<MyLabel> throwEndLabel = new Attribute<MyLabel>(
	MyLabel.class, "throwEndLabel", makeLabel ) ;

    // Attribute on all Statement nodes that gives the start of the
    // sequentially next statement immediately after the current
    // statement if any.  This is only set if the parent node has
    // a local next statement (e.g. BlockStatement).
    static Attribute<Node> next = new Attribute<Node>(
	Node.class, "next", (Node)null ) ;

    static Attribute<Variable> returnVariable = new Attribute<Variable>(
	Variable.class, "returnVariable", (Variable)null ) ;

    // Variable attributes
    
    // All local Variable definitions have this attribute which defines where
    // they are allocated in the stack frame.
    static Attribute<Integer> stackFrameSlot = new Attribute<Integer>(
	Integer.class, "stackFrameSlot", 0 ) ;

    // All Variable definitions have a getEmitter attribute which defines
    // how to get the value of the Variable.
    static Attribute<EmitterFactory.Emitter> getEmitter = new Attribute<EmitterFactory.Emitter>(
	EmitterFactory.Emitter.class, "getEmitter", (EmitterFactory.Emitter)null ) ;

    // All Variable definitions have a getEmitter attribute which defines
    // how to set the value of the Variable.
    static Attribute<EmitterFactory.Emitter> setEmitter = new Attribute<EmitterFactory.Emitter>(
	EmitterFactory.Emitter.class, "setEmitter", (EmitterFactory.Emitter)null ) ;

    // All assignable expression nodes have an emitter attribute which defines
    // what operation (load or store) is needed when that reference is visited.
    static Attribute<EmitterFactory.Emitter> emitter = new Attribute<EmitterFactory.Emitter>(
	EmitterFactory.Emitter.class, "emitter", (EmitterFactory.Emitter)null ) ;

    // Indicates whether a variable needs to emit a setter, a getter, or no
    // code at all when visited for code generation.
    static Attribute<RequiredEmitterType> requiredEmitterType = new Attribute<RequiredEmitterType>(
	RequiredEmitterType.class, "requiredEmitterType", RequiredEmitterType.GETTER ) ;

    // Used in ASMByteCodeVisitor to track the last statement visited in a
    // BlockStatement
    static Attribute<Statement> lastStatement = new Attribute<Statement>(
	Statement.class, "lastStatement", (Statement)null ) ;

    // Used to hold the exception for the uncaught exception handler when
    // generating code for a try statement with a finally block.
    static Attribute<Variable> uncaughtException = new Attribute<Variable>(
	Variable.class, "uncaughtException", (Variable)null ) ;

    // Used to hold the local variable that holds the return address for
    // a finally block.
    static Attribute<Variable> returnAddress = new Attribute<Variable>(
	Variable.class, "returnAddress", (Variable)null ) ;

    // Used to track the last BlockStatement visited while generating
    // bytecode for a TryStatement.
    static Attribute<BlockStatement> lastBlock = new Attribute<BlockStatement>(
	BlockStatement.class, "lastBlock", (BlockStatement)null ) ;

    // Used to label the start of the uncaught exception handler.
    static Attribute<MyLabel> uncaughtExceptionHandler = new Attribute<MyLabel>(
	MyLabel.class, "uncaughtExceptionHandler", makeLabel ) ;

    static Attribute<Integer> ctr = new Attribute<Integer>(
	Integer.class, "ctr", 0 ) ;

    public static class LineNumberTable extends HashMap<MyLabel,Integer> {
	public LineNumberTable() {
	    super() ;
	}
    }

    private static NullaryFunction<LineNumberTable> tableMaker = 
	new NullaryFunction<LineNumberTable>() {
	    public LineNumberTable evaluate() {
		return new LineNumberTable() ;
	    }
	} ;

    // Attribute on MethodGenerator that contains the LineNumberTable.
    static Attribute<LineNumberTable> lineNumberTable = new Attribute<LineNumberTable>(
	LineNumberTable.class, "lineNumberTable", tableMaker ) ;

    public static class VariablesInMethod extends HashSet<Variable> {
	public VariablesInMethod() {
	    super() ;
	}
    }

    private static NullaryFunction<VariablesInMethod> vmMaker = 
	new NullaryFunction<VariablesInMethod>() {
	    public VariablesInMethod evaluate() {
		return new VariablesInMethod() ;
	    }
	} ;

    static Attribute<VariablesInMethod> variablesInMethod = new Attribute<VariablesInMethod>(
	VariablesInMethod.class, "variablesInMethod", vmMaker ) ;
}
