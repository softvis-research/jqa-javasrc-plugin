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
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.resolution.Resolvable;
import com.github.javaparser.resolution.declarations.*;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
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
     */
    private TypeDescriptor handleType(Object resolved) {
        if (Utils.whichSolverWasUsed(resolved) == Utils.SolverType.JavaParserSolver) {
            if (resolved instanceof ResolvedClassDeclaration) {
                return this.handleClass((ResolvedClassDeclaration) resolved);
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
            System.out.println("TODO: Need to handle builtin or unresolvable types"
                + ", e.g.: " + resolved
                + " - used resolver: " + Utils.whichSolverWasUsed(resolved));
        }

        return null; // throw?
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
            methodDescriptor.setVisibility(m.accessSpecifier().toString()); // TODO: may need to adjust
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
        for (ResolvedFieldDeclaration fD : resolvedClass.getDeclaredFields()) {
            System.out.println("Resolved FieldDeclaration: " + fD);
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
