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
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import org.unileipzig.jqassistant.plugin.parser.api.model.ClassFileDescriptor;
import org.unileipzig.jqassistant.plugin.parser.api.model.JavaSourceFileDescriptor;
import org.unileipzig.jqassistant.plugin.parser.api.scanner.JavaScope;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

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
        Store store = context.getStore();
        JavaSourceFileDescriptor javaSourceFileDescriptor = store.addDescriptorType(fileDescriptor, JavaSourceFileDescriptor.class);
        // parse files and determine concrete types (i.e. class, interface, annotation or enum)
        Resolver resolver = context.peek(Resolver.class); // get it from context, it should be the same object throughout
        JavaParser.setStaticConfiguration(new ParserConfiguration().setSymbolResolver(new JavaSymbolSolver(resolver.typeSolver)));
        JavaParserFacade javaParserFacade = JavaParserFacade.get(resolver.typeSolver);
        try (InputStream in = item.createStream()) {
            CompilationUnit cu = JavaParser.parse(in);
            //System.out.println("CU: " + cu);
            Optional<PackageDeclaration> packageDeclaration = cu.getPackageDeclaration();
            String packageName = packageDeclaration.isPresent() ? packageDeclaration.get().getNameAsString() : "";
            for (TypeDeclaration<?> typeDeclaration : cu.getTypes()) {
                String fqn = packageName + typeDeclaration.getNameAsString();
                ClassFileDescriptor typeDescriptor = resolver.getOrCreate(fqn, ClassFileDescriptor.class);
                if (typeDeclaration.isClassOrInterfaceDeclaration()) {
                    // get methods and fields
                    for (MethodDeclaration mD : typeDeclaration.getMethods()) {
                        System.out.println("MethodDeclaration: " + mD.getName());
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
                            System.out.println("Superclass: " + superClassType + " resolved " + x.getQualifiedName());
                        } catch (UnsolvedSymbolException e) {
                            System.out.println("correctly catching " + e);
                        } catch (RuntimeException e) {
                            System.out.println("should've been catching UnsolvedSymbolException " + e);
                            System.out.println("could not resolve " + superClassType.getName());
                        }
                        //String fqn = superClassType.();
                        //TypeDescriptor superClassDescriptor = resolver.resolveType("com.acme.MySuperClass", context);
                        //typeDescriptor.setExtends(superClassDescriptor);
                    }
                }

            }
        }
        return javaSourceFileDescriptor;

    }
}
