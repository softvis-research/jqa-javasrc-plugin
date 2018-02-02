package org.jqassistant.contrib.plugin.javasrc.impl.scanner;

import java.io.IOException;
import java.io.InputStream;

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
import org.jqassistant.contrib.plugin.javasrc.api.model.JavaSourceFileDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.scanner.JavaScope;
import org.jqassistant.contrib.plugin.javasrc.impl.scanner.visitor.AnnotationVisitor;
import org.jqassistant.contrib.plugin.javasrc.impl.scanner.visitor.FieldVisitor;
import org.jqassistant.contrib.plugin.javasrc.impl.scanner.visitor.MethodVisitor;
import org.jqassistant.contrib.plugin.javasrc.impl.scanner.visitor.TypeVisitor;

@Requires(FileDescriptor.class)
public class JavaSourceFileScannerPlugin extends AbstractScannerPlugin<FileResource, JavaSourceFileDescriptor> {
    private TypeResolver typeResolver;
    private Store store;

    @Override
    public boolean accepts(FileResource item, String path, Scope scope) throws IOException {
        return JavaScope.CLASSPATH.equals(scope) && path.toLowerCase().endsWith(".java");
    }

    @Override
    public JavaSourceFileDescriptor scan(FileResource item, String path, Scope scope, Scanner scanner) throws IOException {
        ScannerContext context = scanner.getContext();
        store = context.getStore();
        typeResolver = context.peek(TypeResolver.class); // get it from context,
                                                         // it should be the
                                                         // same object
                                                         // throughout
        FileDescriptor fileDescriptor = context.getCurrentDescriptor();
        JavaSourceFileDescriptor javaSourceFileDescriptor = store.addDescriptorType(fileDescriptor, JavaSourceFileDescriptor.class);
        try (InputStream in = item.createStream()) {
            CompilationUnit cu = JavaParser.parse(in);
            cu.accept(new TypeVisitor(typeResolver), javaSourceFileDescriptor);
            cu.accept(new FieldVisitor(typeResolver), javaSourceFileDescriptor);
            cu.accept(new MethodVisitor(typeResolver), javaSourceFileDescriptor);
            cu.accept(new AnnotationVisitor(typeResolver), javaSourceFileDescriptor);
        }
        return javaSourceFileDescriptor;
    }
}
