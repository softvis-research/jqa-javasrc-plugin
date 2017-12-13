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
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.resolution.declarations.ResolvedClassDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
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
            //System.out.println("CU: " + cu);
            Optional<PackageDeclaration> packageDeclaration = cu.getPackageDeclaration();
            String packageName = packageDeclaration.isPresent() ? packageDeclaration.get().getNameAsString() : "";
            for (TypeDeclaration<?> typeDeclaration : cu.getTypes()) {
                String fqn = packageName + "." + typeDeclaration.getName();
                if (typeDeclaration.isClassOrInterfaceDeclaration()) {
                    if (!resolver.has(fqn)) this.handleClass(fqn, typeDeclaration.asClassOrInterfaceDeclaration());
                } else if (typeDeclaration.isEnumDeclaration()) {
                    // TODO
                    System.out.println("ENUM found");
                }
            }
        }
        return javaSourceFileDescriptor;
    }

    /**
     * Create ClassFileDescriptor (XO) from ClassOrInterfaceDeclaration (JavaParser)
     */
    private void handleClass(String fullyQualifiedName, ClassOrInterfaceDeclaration typeDeclaration) {
        ClassFileDescriptor classFileDescriptor = resolver.create(fullyQualifiedName, ClassFileDescriptor.class);
        List<MemberDescriptor> members = classFileDescriptor.getDeclaredMembers();
        List<MethodDescriptor> methods = classFileDescriptor.getDeclaredMethods();
        List<FieldDescriptor> fields = classFileDescriptor.getDeclaredFields();
        // get methods and fields
        for (MethodDeclaration mD : typeDeclaration.getMethods()) {
            String fullyQualifiedMethodName = fullyQualifiedName + "." + mD.getName();
            System.out.println("MethodDeclaration: " + fullyQualifiedMethodName);
            MethodDescriptor methodDescriptor = resolver.create(fullyQualifiedMethodName, MethodDescriptor.class);
            methodDescriptor.setName(mD.getName().toString());
            methodDescriptor.setSignature(mD.getSignature().toString());
            members.add(methodDescriptor);
            methods.add(methodDescriptor);
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
            try {
                ResolvedReferenceType x = superClassType.resolve();
                //javaParserFacade.solve(ClassOrInterfaceDeclaration.class);
                //ResolvedType y = javaParserFacade.solve(superClassType);
                System.out.println("Superclass: " + superClassType + " resolved " + x.getTypeDeclaration());
            } catch (RuntimeException e) { // actually is UnsolvedSymbolException
                System.out.println("could not resolve " + superClassType.getName() + " e: " + e);
            }
            //String fqn = superClassType.();
            //TypeDescriptor superClassDescriptor = resolver.resolveType("com.acme.MySuperClass", context);
            //classFileDescriptor.setExtends(superClassDescriptor);
        }
    }

    /**
     * Create ClassFileDescriptor (XO) from ResolvedClassDeclaration (JavaParser)
     * ResolvedClassDeclaration seems to be somewhat similar to ClassOrInterfaceDeclaration, but is everything but...
     */
    private void handleClass(String fullyQualifiedName, ResolvedClassDeclaration typeDeclaration) {
        ClassFileDescriptor classFileDescriptor = resolver.create(fullyQualifiedName, ClassFileDescriptor.class);
        // get methods and fields
        for (ResolvedMethodDeclaration mD : typeDeclaration.getDeclaredMethods()) {
            System.out.println("Resolved MethodDeclaration: " + mD);
        }
        for (ResolvedFieldDeclaration fD : typeDeclaration.getDeclaredFields()) {
            System.out.println("Resolved FieldDeclaration: " + fD);
        }
        // ...
    }
}
