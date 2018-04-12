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
import org.jqassistant.contrib.plugin.javasrc.impl.scanner.visitor.VisitorHelper;

@Requires(FileDescriptor.class)
public class JavaSourceFileScannerPlugin extends AbstractScannerPlugin<FileResource, JavaSourceFileDescriptor> {

    @Override
    public boolean accepts(FileResource item, String path, Scope scope) throws IOException {
        return JavaScope.SRC.equals(scope) && path.toLowerCase().endsWith(".java");
    }

    @Override
    public JavaSourceFileDescriptor scan(FileResource item, String path, Scope scope, Scanner scanner) throws IOException {
        ScannerContext scannerContext = scanner.getContext();
        FileDescriptor fileDescriptor = scannerContext.getCurrentDescriptor();
        JavaSourceFileDescriptor javaSourceFileDescriptor = scannerContext.getStore().addDescriptorType(fileDescriptor, JavaSourceFileDescriptor.class);
        VisitorHelper visitorHelper = new VisitorHelper(scannerContext, javaSourceFileDescriptor);
        try (InputStream in = item.createStream()) {
            CompilationUnit cu = JavaParser.parse(in);
            cu.accept(new TypeVisitor(visitorHelper), javaSourceFileDescriptor);
        }
        visitorHelper.addDependencies();
        return javaSourceFileDescriptor;
    }
}
