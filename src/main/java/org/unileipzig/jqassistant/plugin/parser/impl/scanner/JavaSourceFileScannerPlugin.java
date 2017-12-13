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
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.declarations.ResolvedClassDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedArrayType;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
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
                String fqn = packageName + "." + typeDeclaration.getName();
                TypeDescriptor typeDescriptor = this.handleType(fqn, typeDeclaration);
                typeDescriptors.add(typeDescriptor);
            }
        }
        return javaSourceFileDescriptor;
    }

    /**
     * Create TypeDescriptor (XO) from TypeDeclaration (JavaParser)
     */
    private TypeDescriptor handleType(String fullyQualifiedName, TypeDeclaration typeDeclaration) {
        if (typeDeclaration.isClassOrInterfaceDeclaration()) {
            if (!resolver.has(fullyQualifiedName)) {
                return this.handleClass(fullyQualifiedName, typeDeclaration.asClassOrInterfaceDeclaration());
            } else {
                return resolver.get(fullyQualifiedName, ClassFileDescriptor.class);
            }
        } else if (typeDeclaration.isEnumDeclaration()) {
            // TODO
            System.out.println("ENUM found");
        }
        return null; // throw?
    }

    /**
     * Create TypeDescriptor (XO) from Type (or rather ResolvedType) (JavaParser)
     */
    private TypeDescriptor handleType(Type type) {
        //if (resolver.has(fullyQualifiedName)) { // TODO: need to construct fullyQualifiedName from some information
        //    return resolver.get(fullyQualifiedName, TypeDescriptor.class); // more concrete validation?
        //}
        try {
            ResolvedType resolvedType = type.resolve();
            if (resolvedType.isArray()) {
                ResolvedArrayType resolvedArrayType = resolvedType.asArrayType();
                System.out.println("ARRAY" + resolvedArrayType);
            } else if (resolvedType.isReferenceType()) {
                ResolvedReferenceType resolvedReferenceType = resolvedType.asReferenceType();
                ResolvedReferenceTypeDeclaration resolvedDeclaration = resolvedReferenceType.getTypeDeclaration();
                if (resolvedDeclaration.isClass()) {
                    // typecast to ResolvedClassDeclaration?
                    // need to differentiate between JavaParserClassDeclaration and ReflectionClassDeclaration
                    // ReflectionClassDeclaration refers to a builtin type, such as String
                } else if (resolvedDeclaration.isEnum()) {
                    // TODO
                } else if (resolvedDeclaration.isInterface()) {
                    // TODO
                }
            }
        } catch (RuntimeException e) { // actually is UnsolvedSymbolException
            System.out.println("could not resolve " + type + " e: " + e);
            // TODO: return some replacement/placeholder ("external dependency")
        }
        return null; // throw?
    }

    /**
     * Create ClassFileDescriptor (XO) from ClassOrInterfaceDeclaration (JavaParser)
     */
    private ClassFileDescriptor handleClass(String fullyQualifiedName, ClassOrInterfaceDeclaration typeDeclaration) {
        ClassFileDescriptor classFileDescriptor = resolver.create(fullyQualifiedName, ClassFileDescriptor.class);
        List<MemberDescriptor> memberDescriptors = classFileDescriptor.getDeclaredMembers();
        List<MethodDescriptor> methodDescriptors = classFileDescriptor.getDeclaredMethods();
        List<FieldDescriptor> fieldDescriptors = classFileDescriptor.getDeclaredFields();
        // get methods and fields
        for (MethodDeclaration mD : typeDeclaration.getMethods()) {
            String fullyQualifiedMethodName = fullyQualifiedName + "." + mD.getName();
            System.out.println("MethodDeclaration: " + fullyQualifiedMethodName);
            MethodDescriptor methodDescriptor = resolver.create(fullyQualifiedMethodName, MethodDescriptor.class);
            methodDescriptor.setName(mD.getName().toString());
            methodDescriptor.setSignature(mD.getSignature().toString());
            memberDescriptors.add(methodDescriptor);
            methodDescriptors.add(methodDescriptor);
            for (Modifier m : mD.getModifiers()) {
                switch (m) { // TODO: STRICTFP, VOLATILE, etc. pp.
                    case PUBLIC:
                    case PRIVATE:
                    case PROTECTED:
                    case DEFAULT:
                        methodDescriptor.setVisibility(m.toString());
                        break;
                    case FINAL:
                        methodDescriptor.setFinal(true);
                        break;
                    case NATIVE:
                        methodDescriptor.setNative(true);
                        break;
                    case STATIC:
                        methodDescriptor.setStatic(true);
                        break;
                    case ABSTRACT:
                        methodDescriptor.setAbstract(true);
                        break;
                }
            }
            int parameterIndex = 0;
            List<ParameterDescriptor> parameterDescriptors = methodDescriptor.getParameters();
            for (Parameter p : mD.getParameters()) {
                ParameterDescriptor parameterDescriptor = store.create(ParameterDescriptor.class);
                //parameterDescriptor.setType(resolver.getOrResolve(p.getType()));
                parameterDescriptor.setType(this.handleType(p.getType()));
                parameterDescriptor.setIndex(parameterIndex++);
                parameterDescriptors.add(parameterDescriptor);
            }
            mD.getBody().ifPresent((body) -> {
                for (Statement statement : body.getStatements()) {
                    System.out.println("MethodBodyStatement: " + statement);
                }
            });
        }
        for (FieldDeclaration fD : typeDeclaration.getFields()) {
            System.out.println("FieldDeclaration: " + fD);
        }
        // get superclasses / implemented interfaces
        for (ClassOrInterfaceType superClassType : typeDeclaration.asClassOrInterfaceDeclaration().getExtendedTypes()) {
            // FIXME: since it is a list, does getExtendedTypes() also include interfaces?? -> in that case we need to filter
            // TODO: explore using ClassOrInterfaceDeclaration.resolve() at the very beginning to have only one API to take care of!
            TypeDescriptor superClassDescriptor = this.handleType(superClassType);
            classFileDescriptor.setSuperClass(superClassDescriptor);
        }
        List<TypeDescriptor> interfaces = classFileDescriptor.getInterfaces();
        for (ClassOrInterfaceType interfaceType : typeDeclaration.getImplementedTypes()) {
            TypeDescriptor interfaceDescriptor = this.handleType(interfaceType);
            interfaces.add(interfaceDescriptor);
        }
        return classFileDescriptor;
    }

    /**
     * Create ClassFileDescriptor (XO) from ResolvedClassDeclaration (JavaParser)
     * ResolvedClassDeclaration seems to be somewhat similar to ClassOrInterfaceDeclaration, but is everything but...
     */
    private ClassFileDescriptor handleClass(String fullyQualifiedName, ResolvedClassDeclaration typeDeclaration) {
        ClassFileDescriptor classFileDescriptor = resolver.create(fullyQualifiedName, ClassFileDescriptor.class);
        // get methods and fields
        for (ResolvedMethodDeclaration mD : typeDeclaration.getDeclaredMethods()) {
            System.out.println("Resolved MethodDeclaration: " + mD);
        }
        for (ResolvedFieldDeclaration fD : typeDeclaration.getDeclaredFields()) {
            System.out.println("Resolved FieldDeclaration: " + fD);
        }
        // ...
        return classFileDescriptor;
    }
}
