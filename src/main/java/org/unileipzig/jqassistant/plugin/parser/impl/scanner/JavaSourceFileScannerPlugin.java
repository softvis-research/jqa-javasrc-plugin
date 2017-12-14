package org.unileipzig.jqassistant.plugin.parser.impl.scanner;

import com.buschmais.jqassistant.core.scanner.api.Scanner;
import com.buschmais.jqassistant.core.scanner.api.ScannerContext;
import com.buschmais.jqassistant.core.scanner.api.ScannerPlugin.Requires;
import com.buschmais.jqassistant.core.scanner.api.Scope;
import com.buschmais.jqassistant.core.store.api.Store;
import com.buschmais.jqassistant.plugin.common.api.model.FileDescriptor;
import com.buschmais.jqassistant.plugin.common.api.scanner.AbstractScannerPlugin;
import com.buschmais.jqassistant.plugin.common.api.scanner.filesystem.FileResource;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.resolution.Resolvable;
import com.github.javaparser.resolution.declarations.*;
import com.github.javaparser.resolution.types.*;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserFieldDeclaration;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserMethodDeclaration;
import org.unileipzig.jqassistant.plugin.parser.api.model.*;
import org.unileipzig.jqassistant.plugin.parser.api.scanner.JavaScope;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

@Requires(FileDescriptor.class)
public class JavaSourceFileScannerPlugin extends AbstractScannerPlugin<FileResource, JavaSourceFileDescriptor> {
    private Resolver resolver;
    private Store store;

    @Override
    public boolean accepts(FileResource item, String path, Scope scope) throws IOException {
        return JavaScope.CLASSPATH.equals(scope) && path.toLowerCase().endsWith(".java");
    }

    @Override
    public JavaSourceFileDescriptor scan(FileResource item, String path, Scope scope, Scanner scanner) throws IOException {
        ScannerContext context = scanner.getContext();
        store = context.getStore();
        resolver = context.peek(Resolver.class); // get it from context, it should be the same object throughout
        FileDescriptor fileDescriptor = context.getCurrentDescriptor();
        JavaSourceFileDescriptor javaSourceFileDescriptor = store.addDescriptorType(fileDescriptor, JavaSourceFileDescriptor.class);
        // parse files and determine concrete types (i.e. class, interface, annotation or enum)
        try (InputStream in = item.createStream()) {
            CompilationUnit cu = JavaParser.parse(in);
            Optional<PackageDeclaration> packageDeclaration = cu.getPackageDeclaration();
            String packageName = packageDeclaration.isPresent() ? packageDeclaration.get().getNameAsString() : "";
            List<TypeDescriptor> typeDescriptors = javaSourceFileDescriptor.getTypes();
            for (TypeDeclaration<?> typeDeclaration : cu.getTypes()) {
                TypeDescriptor typeDescriptor = this.handleType(typeDeclaration);
                if (typeDescriptor != null) typeDescriptors.add(typeDescriptor);
                else System.out.println("WARNING: couldn't handle type in " + cu);
            }
        }
        return javaSourceFileDescriptor;
    }

    /**
     * Create TypeDescriptor (XO) from TypeDeclaration (JavaParser)
     * ... it needs to be (and most probably is) something that implements Resolvable
     */
    private TypeDescriptor handleType(TypeDeclaration typeDeclaration) {
        TypeDescriptor typeDescriptor = null;
        if (typeDeclaration instanceof Resolvable) {
            try {
                Object resolved = ((Resolvable) typeDeclaration).resolve();
                typeDescriptor = this.handleType(resolved);
                //System.out.println("was able to resolve " + resolved);
            } catch (RuntimeException e) { // actually is UnsolvedSymbolException
                System.out.println("could not resolve " + typeDeclaration + " e: " + e);
                throw e;
                // TODO: return some replacement/placeholder ("external dependency")
            }
        } else {
            System.out.println("!!! Unexpected Type that doesn't implement Resolvable: " + typeDeclaration);
        }
        return typeDescriptor;
    }

    /**
     * Create TypeDescriptor (XO) from a Resolved* Object (JavaParser)
     * need to differentiate e.g. between JavaParserClassDeclaration and ReflectionClassDeclaration
     * From jqa-java-plugin docs:
     * # *NOTE* The full set of information is only available for class files which
     * # have actually been scanned. Types which are only referenced (i.e. from
     * # external libraries not included in the scan) are represented by `:Type` nodes with a
     * # property `fqn` and `DECLARES` relations to their members. These are `:Field` or
     * # `:Method` labeled nodes which only provide the property `signature`.
     */
    private TypeDescriptor handleType(Object resolved) {
        TypeDescriptor descriptor = null;
        if (resolved instanceof ResolvedArrayType) {
            // how array are currently handled in jqa-java-plugin: see SignatureHelper#getType(org.objectweb.asm.Type)
            // "case Type.ARRAY: return getType(t.getElementType());"
            // thus doing the same, here!
            resolved = ((ResolvedArrayType) resolved).getComponentType();
        }
        if (Utils.whichSolverWasUsed(resolved) == Utils.SolverType.JavaParserSolver) {
            if (resolved instanceof ResolvedClassDeclaration) {
                descriptor = this.handleClass((ResolvedClassDeclaration) resolved);
            } else if (resolved instanceof ResolvedInterfaceDeclaration) {
                // TODO
            } else if (resolved instanceof ResolvedEnumDeclaration) {
                // TODO
            } else if (resolved instanceof ResolvedAnnotationDeclaration) {
                // TODO
            } else {
                System.out.println("!!! Unexpected Resolvable: " + resolved);
            }
        } else {
            // get the fullyQualifiedName, create a TypeDescriptor class implying it is to be treated as a dependency
            String fullyQualifiedName = "";
            if (resolved instanceof ResolvedVoidType) {
                fullyQualifiedName = Void.class.getCanonicalName();
            } else if (resolved instanceof ResolvedPrimitiveType) {
                // (INT, BOOLEAN, LONG, CHAR, FLOAT, DOUBLE, SHORT, BYTE)
                fullyQualifiedName = ((ResolvedPrimitiveType) resolved).getBoxTypeQName();
            } else if (resolved instanceof ResolvedReferenceType) {
                ResolvedReferenceType resolvedReferenceType = (ResolvedReferenceType) resolved;
                fullyQualifiedName = resolvedReferenceType.getQualifiedName();
            } else {
                throw new RuntimeException("Unexpected Resolvable: " + resolved);
            }
            // HANDLE DEPENDENCIES -> THUS NEED TO KNOW WHICH IS THE THING THAT DEPENDS ON THIS (+1 PARAMETER)
            if (resolver.has(fullyQualifiedName)) {
                System.out.println("resolver has " + fullyQualifiedName);
            } else {
                System.out.println("will need to add TypeDescriptor (as dependency) Object for " + fullyQualifiedName);
                System.out.println(resolver.descriptorCache.keySet());
            }
        }
        // handle possibly added descriptors
        if (descriptor != null && resolver.hasDependencies(descriptor)) {
            System.out.println(descriptor.getFullQualifiedName() + " has dependencies");
            resolver.storeDependencies(descriptor);
        }
        return descriptor;
    }

    /**
     * Create ClassFileDescriptor (XO) from ResolvedClassDeclaration (JavaParser)
     * TODO: need to differentiate between JavaParserClassDeclaration and ReflectionClassDeclaration
     * - e.g. for a JavaParserClassDeclaration, we WANT TO know everything really detailed
     * - e.g. for a ReflectionClassDeclaration, we DON'T, since it's a builtin class
     */
    private ClassFileDescriptor handleClass(ResolvedClassDeclaration resolvedClass) {
        String fullyQualifiedName = resolvedClass.getQualifiedName();
        if (resolver.has(fullyQualifiedName)) return resolver.get(fullyQualifiedName, ClassFileDescriptor.class);
        ClassFileDescriptor classDescriptor = resolver.create(fullyQualifiedName, ClassFileDescriptor.class);
        List<MemberDescriptor> memberDescriptors = classDescriptor.getDeclaredMembers();
        List<MethodDescriptor> methodDescriptors = classDescriptor.getDeclaredMethods();
        List<FieldDescriptor> fieldDescriptors = classDescriptor.getDeclaredFields();
        // get methods and fields
        for (ResolvedMethodDeclaration m : resolvedClass.getDeclaredMethods()) {
            MethodDescriptor methodDescriptor = resolver.create(m.getQualifiedName(), MethodDescriptor.class);
            memberDescriptors.add(methodDescriptor);
            methodDescriptors.add(methodDescriptor);
            methodDescriptor.setName(m.getName());
            methodDescriptor.setSignature(m.getSignature());
            methodDescriptor.setVisibility(Utils.modifierToString(m.accessSpecifier()));
            methodDescriptor.setStatic(m.isStatic());
            methodDescriptor.setAbstract(m.isAbstract());
            // handle parameters
            List<ParameterDescriptor> parameterDescriptors = methodDescriptor.getParameters();
            for (int i = 0; i < m.getNumberOfParams(); i++) {
                ResolvedParameterDeclaration p = m.getParam(i);
                ParameterDescriptor parameterDescriptor = store.create(ParameterDescriptor.class);
                parameterDescriptor.setIndex(i);
                TypeDescriptor parameterTypeDescriptor = this.handleType(p.getType());
                parameterDescriptor.setType(parameterTypeDescriptor);
                // name seems to (currently!) not be relevant for ParameterDescriptor
                // interesting: hasVariadicParameter ...  seems also currently not to be relevant
                parameterDescriptors.add(parameterDescriptor);
            }
            // handle (specified) return type (it would also be possible to be retrieved from body statements)
            TypeDescriptor returnTypeDescriptor = this.handleType(m.getReturnType());
            methodDescriptor.setReturns(returnTypeDescriptor);
            // handle (specified) thrown exceptions (otherwise needs to be retrieved from body statements)
            for (ResolvedType e : m.getSpecifiedExceptions()) {
                System.out.println("TODO: resolve exception" + e);
            }
            // some information we can only get from JavaParserMethodDeclaration.wrappedNode (MethodDeclaration)
            // for example, ResolvedMethodDeclaration itself has no reference in any way to the method's body
            if (m instanceof JavaParserMethodDeclaration) { // should always be true since we wouldn't want any such details above for builtin types
                MethodDeclaration mD = ((JavaParserMethodDeclaration) m).getWrappedNode();
                for (Modifier modifier : mD.getModifiers()) {
                    switch (modifier) { // we need only to handle those that where not possible to compute above
                        case FINAL:
                            methodDescriptor.setFinal(true);
                            break;
                        case NATIVE:
                            methodDescriptor.setNative(true);
                            break;
                    }
                }
                // handle body statements
                mD.getBody().ifPresent((body) -> {
                    for (Statement statement : body.getStatements()) {
                        if (statement instanceof ReturnStmt) {
                            // TODO...
                        }
                        // TODO...
                    }
                });
            }
        }
        for (ResolvedFieldDeclaration f : resolvedClass.getDeclaredFields()) {
            String fullyQualifiedFieldName = f.declaringType().getQualifiedName() + "." + f.getName();
            FieldDescriptor fieldDescriptor = resolver.create(fullyQualifiedFieldName, FieldDescriptor.class);
            fieldDescriptors.add(fieldDescriptor);
            TypeDescriptor fieldTypeDescriptor = this.handleType(f.getType());
            fieldDescriptor.setType(fieldTypeDescriptor);
            fieldDescriptor.setVisibility(Utils.modifierToString(f.accessSpecifier()));
            fieldDescriptor.setStatic(f.isStatic());
            // some information we can only get from JavaParserFieldDeclaration.wrappedNode (FieldDeclaration)
            if (f instanceof JavaParserFieldDeclaration) {
                //fieldDescriptor.setSynthetic();//?fieldDescriptor.setSignature();//?
                FieldDeclaration fD = ((JavaParserFieldDeclaration) f).getWrappedNode();
                fieldDescriptor.setTransient(fD.isTransient());
                fieldDescriptor.setFinal(fD.isFinal());
            }
            //System.out.println("Resolved FieldDeclaration: " + fullyQualifiedFieldName + " type: " + f.getType());
        }
        // get superclasses / implemented interfaces
        TypeDescriptor superClassDescriptor = this.handleType(resolvedClass.getSuperClass());
        classDescriptor.setSuperClass(superClassDescriptor);
        List<TypeDescriptor> interfaces = classDescriptor.getInterfaces();
        for (ResolvedReferenceType interfaceType : resolvedClass.getInterfaces()) {
            TypeDescriptor interfaceDescriptor = this.handleType(interfaceType);
            interfaces.add(interfaceDescriptor);
        }
        return classDescriptor;
    }
}
