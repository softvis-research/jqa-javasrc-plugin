package org.jqassistant.contrib.plugin.javasrc.impl.scanner;

import java.io.File;
import java.io.IOException;

import com.buschmais.jqassistant.core.scanner.api.ScannerContext;
import com.buschmais.jqassistant.core.scanner.api.Scope;
import com.buschmais.jqassistant.plugin.common.api.scanner.AbstractDirectoryScannerPlugin;
import org.jqassistant.contrib.plugin.javasrc.api.model.JavaSourceDirectoryDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.scanner.JavaScope;
import org.jqassistant.contrib.plugin.javasrc.impl.scanner.solver.JavaTypeSolver;

public class JavaSourceDirectoryScannerPlugin extends AbstractDirectoryScannerPlugin<JavaSourceDirectoryDescriptor> {

    @Override
    protected Scope getRequiredScope() {
        return JavaScope.SRC;
    }

    @Override
    protected JavaSourceDirectoryDescriptor getContainerDescriptor(File container, ScannerContext scannerContext) {
        return scannerContext.getStore().create(JavaSourceDirectoryDescriptor.class);
    }

    @Override
    protected void enterContainer(File container, JavaSourceDirectoryDescriptor containerDescriptor, ScannerContext scannerContext) throws IOException {
        if (scannerContext.peekOrDefault(JavaTypeResolver.class, null) == null) {
            scannerContext.push(JavaTypeResolver.class, new JavaTypeResolver(scannerContext));
        }
        if (scannerContext.peekOrDefault(JavaTypeSolver.class, null) == null) {
            scannerContext.push(JavaTypeSolver.class, new JavaTypeSolver(containerDescriptor.getFileName()));
        }
    }

    @Override
    protected void leaveContainer(File container, JavaSourceDirectoryDescriptor containerDescriptor, ScannerContext scannerContext) throws IOException {
        scannerContext.pop(JavaTypeResolver.class);
        scannerContext.pop(JavaTypeSolver.class);
    }
}
