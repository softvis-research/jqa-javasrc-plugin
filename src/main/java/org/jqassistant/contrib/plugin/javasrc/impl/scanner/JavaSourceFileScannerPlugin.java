package org.jqassistant.contrib.plugin.javasrc.impl.scanner;

import java.io.IOException;
import java.io.InputStream;

import com.buschmais.jqassistant.core.scanner.api.Scanner;
import com.buschmais.jqassistant.core.scanner.api.ScannerContext;
import com.buschmais.jqassistant.core.scanner.api.ScannerPlugin.Requires;
import com.buschmais.jqassistant.core.scanner.api.Scope;
import com.buschmais.jqassistant.plugin.common.api.model.FileDescriptor;
import com.buschmais.jqassistant.plugin.common.api.scanner.AbstractScannerPlugin;
import com.buschmais.jqassistant.plugin.common.api.scanner.filesystem.FileResource;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import org.jqassistant.contrib.plugin.javasrc.api.model.JavaSourceFileDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.scanner.JavaScope;
import org.jqassistant.contrib.plugin.javasrc.impl.scanner.visitor.TypeVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Requires(FileDescriptor.class)
public class JavaSourceFileScannerPlugin extends AbstractScannerPlugin<FileResource, JavaSourceFileDescriptor> {
    private TypeResolver typeResolver;
    private static final Logger LOGGER = LoggerFactory.getLogger(JavaSourceFileScannerPlugin.class);

    @Override
    public boolean accepts(FileResource item, String path, Scope scope) throws IOException {
        LOGGER.warn(" " + JavaScope.SRC.equals(scope) + " " + scope.getClass() + " " + path.toLowerCase().endsWith(".java"));
        LOGGER.warn("path: " + path.toLowerCase());
        return JavaScope.SRC.equals(scope) && path.toLowerCase().endsWith(".java");
    }

    @Override
    public JavaSourceFileDescriptor scan(FileResource item, String path, Scope scope, Scanner scanner) throws IOException {
        LOGGER.warn(item + " " + path + " " + scope);
        ScannerContext context = scanner.getContext();
        typeResolver = context.peek(TypeResolver.class);
        FileDescriptor fileDescriptor = context.getCurrentDescriptor();
        JavaSourceFileDescriptor javaSourceFileDescriptor = context.getStore().addDescriptorType(fileDescriptor, JavaSourceFileDescriptor.class);
        try (InputStream in = item.createStream()) {
            CompilationUnit cu = JavaParser.parse(in);
            cu.accept(new TypeVisitor(typeResolver), javaSourceFileDescriptor);
        }
        typeResolver.addDependencies();
        return javaSourceFileDescriptor;
    }
}
