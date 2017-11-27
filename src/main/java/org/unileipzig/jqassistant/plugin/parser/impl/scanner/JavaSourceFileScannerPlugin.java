package org.unileipzig.jqassistant.plugin.parser.impl.scanner;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import org.unileipzig.jqassistant.plugin.parser.api.model.ClassTypeDescriptor;
import org.unileipzig.jqassistant.plugin.parser.api.model.JavaSourceFileDescriptor;
import org.unileipzig.jqassistant.plugin.parser.api.scanner.JavaScope;
import org.unileipzig.jqassistant.plugin.parser.api.scanner.TypeResolver;

import com.buschmais.jqassistant.core.scanner.api.Scanner;
import com.buschmais.jqassistant.core.scanner.api.ScannerContext;
import com.buschmais.jqassistant.core.scanner.api.ScannerPlugin.Requires;
import com.buschmais.jqassistant.core.scanner.api.Scope;
import com.buschmais.jqassistant.plugin.common.api.model.FileDescriptor;
import com.buschmais.jqassistant.plugin.common.api.scanner.AbstractScannerPlugin;
import com.buschmais.jqassistant.plugin.common.api.scanner.filesystem.FileResource;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

@Requires(FileDescriptor.class)
public class JavaSourceFileScannerPlugin extends AbstractScannerPlugin<FileResource, JavaSourceFileDescriptor> {

    @Override
    public boolean accepts(FileResource item, String path, Scope scope) throws IOException {
        return JavaScope.CLASSPATH.equals(scope) && path.toLowerCase().endsWith(".java");
    }

    @Override
    public JavaSourceFileDescriptor scan(FileResource item, String path, Scope scope, Scanner scanner) throws IOException {
        ScannerContext context = scanner.getContext();
        FileDescriptor fileDescriptor = context.getCurrentDescriptor();
        JavaSourceFileDescriptor javaSourceFileDescriptor = context.getStore().addDescriptorType(fileDescriptor, JavaSourceFileDescriptor.class);
        // parse files and determine concrete types (i.e. class, interface, annotation
        // or
        // enum)
        TypeResolver typeResolver = context.peek(TypeResolver.class);
        try (InputStream in = item.createStream()) {
            CompilationUnit cu = JavaParser.parse(in);
            System.out.println("CU: " + cu);
            Optional<PackageDeclaration> packageDeclaration = cu.getPackageDeclaration();
            String packageName = packageDeclaration.isPresent() ? packageDeclaration.get().getNameAsString() : "";
            for (TypeDeclaration<?> typeDeclaration : cu.getTypes()) {
                String fqn = packageName + typeDeclaration.getNameAsString();
                ClassTypeDescriptor typeDescriptor = typeResolver.createType(fqn, javaSourceFileDescriptor, ClassTypeDescriptor.class, context);
                if (typeDeclaration.isClassOrInterfaceDeclaration()) {
                    for (ClassOrInterfaceType superClassType : typeDeclaration.asClassOrInterfaceDeclaration().getExtendedTypes()) {
                        // TypeDescriptor superClassDescriptor =
                        // typeResolver.resolveType("com.acme.MySuperClass", context);
                        // typeDescriptor.setExtends(superClassDescriptor);
                    }
                }

            }
        }
        return javaSourceFileDescriptor;

    }
}
