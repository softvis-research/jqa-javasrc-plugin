package org.unileipzig.jqassistant.plugin.parser.impl.scanner;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.unileipzig.jqassistant.plugin.parser.api.model.ClassTypeDescriptor;
import org.unileipzig.jqassistant.plugin.parser.api.model.JavaSourceFileDescriptor;
import org.unileipzig.jqassistant.plugin.parser.api.model.TypeDescriptor;
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
        // parse files and determine concrete types (i.e. class, interface, annotation or enum)
        TypeResolver typeResolver = context.peek(TypeResolver.class); // JQA (probably redundant)
        TypeSolver typeSolver = new CombinedTypeSolver( // JavaSymbolResolver
            new JavaParserTypeSolver(new File("/")), // resolves types in the same path (FIXME: configure path)
            new ReflectionTypeSolver() // resolves builtin types, e.g. java.lang.Object
        );
        JavaParser.setStaticConfiguration(new ParserConfiguration().setSymbolResolver(new JavaSymbolSolver(typeSolver)));
        try (InputStream in = item.createStream()) {
            CompilationUnit cu = JavaParser.parse(in);
            System.out.println("CU: " + cu);
            Optional<PackageDeclaration> packageDeclaration = cu.getPackageDeclaration();
            String packageName = packageDeclaration.isPresent() ? packageDeclaration.get().getNameAsString() : "";
            for (TypeDeclaration<?> typeDeclaration : cu.getTypes()) {
                ClassTypeDescriptor typeDescriptor = typeResolver.createType(packageName + typeDeclaration.getNameAsString(), javaSourceFileDescriptor, ClassTypeDescriptor.class, context);
                if (typeDeclaration.isClassOrInterfaceDeclaration()) {
                    for (ClassOrInterfaceType superClassType : typeDeclaration.asClassOrInterfaceDeclaration().getExtendedTypes()) {
                        try {
                            System.out.println(superClassType.resolve());
                        } catch (Exception e) { // FIXME: where to import UnsolvedSymbolException from?
                            System.out.println(e);
                        }
                        //String fqn = superClassType.();
                        //TypeDescriptor superClassDescriptor = typeResolver.resolveType("com.acme.MySuperClass", context);
                        //typeDescriptor.setExtends(superClassDescriptor);
                    }
                }

            }
        }
        return javaSourceFileDescriptor;

    }
}
