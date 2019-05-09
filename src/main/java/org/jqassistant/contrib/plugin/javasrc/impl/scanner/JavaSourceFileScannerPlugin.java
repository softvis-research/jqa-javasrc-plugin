package org.jqassistant.contrib.plugin.javasrc.impl.scanner;

import com.buschmais.jqassistant.core.scanner.api.Scanner;
import com.buschmais.jqassistant.core.scanner.api.ScannerContext;
import com.buschmais.jqassistant.core.scanner.api.ScannerPlugin.Requires;
import com.buschmais.jqassistant.core.scanner.api.Scope;
import com.buschmais.jqassistant.plugin.common.api.model.FileDescriptor;
import com.buschmais.jqassistant.plugin.common.api.scanner.AbstractScannerPlugin;
import com.buschmais.jqassistant.plugin.common.api.scanner.filesystem.FileResource;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import org.jqassistant.contrib.plugin.javasrc.api.model.JavaSourceFileDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.scanner.JavaScope;
import org.jqassistant.contrib.plugin.javasrc.impl.scanner.visitor.TypeVisitor;
import org.jqassistant.contrib.plugin.javasrc.impl.scanner.visitor.VisitorHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * Plugin that scans Java source files.
 *
 * @author Dirk Mahler, Richard Mueller
 */
@Requires(FileDescriptor.class)
public class JavaSourceFileScannerPlugin extends AbstractScannerPlugin<FileResource, JavaSourceFileDescriptor> {
    private static final Logger LOGGER = LoggerFactory.getLogger(JavaSourceFileScannerPlugin.class);

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
            CompilationUnit cu = StaticJavaParser.parse(in);
            cu.getTypes().accept(new TypeVisitor(visitorHelper), null);
        } catch (JavaSourceException jse) {
            LOGGER.debug(jse.getClass().getSimpleName() + " " + jse.getMessage() + " in " + javaSourceFileDescriptor.getFileName());
        }
        visitorHelper.storeDependencies();
        return javaSourceFileDescriptor;
    }
}
