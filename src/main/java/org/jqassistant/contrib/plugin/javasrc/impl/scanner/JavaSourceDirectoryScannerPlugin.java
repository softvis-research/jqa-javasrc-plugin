package org.jqassistant.contrib.plugin.javasrc.impl.scanner;

import java.io.File;
import java.io.IOException;

import com.buschmais.jqassistant.core.scanner.api.ScannerContext;
import com.buschmais.jqassistant.core.scanner.api.Scope;
import com.buschmais.jqassistant.plugin.common.api.scanner.AbstractDirectoryScannerPlugin;
import org.jqassistant.contrib.plugin.javasrc.api.model.JavaSourceDirectoryDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.scanner.JavaScope;

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
        if (scannerContext.peekOrDefault(TypeResolver.class, null) == null) {
            scannerContext.push(TypeResolver.class, new TypeResolver(containerDescriptor.getFileName(), scannerContext));
        }
    }

    @Override
    protected void leaveContainer(File container, JavaSourceDirectoryDescriptor containerDescriptor, ScannerContext scannerContext) throws IOException {
        scannerContext.pop(TypeResolver.class);
    }
}
