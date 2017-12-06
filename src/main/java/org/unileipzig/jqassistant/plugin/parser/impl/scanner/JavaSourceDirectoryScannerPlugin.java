package org.unileipzig.jqassistant.plugin.parser.impl.scanner;

import java.io.File;
import java.io.IOException;

import org.unileipzig.jqassistant.plugin.parser.api.model.JavaSourceDirectoryDescriptor;
import org.unileipzig.jqassistant.plugin.parser.api.scanner.JavaScope;

import com.buschmais.jqassistant.core.scanner.api.ScannerContext;
import com.buschmais.jqassistant.core.scanner.api.Scope;
import com.buschmais.jqassistant.plugin.common.api.scanner.AbstractDirectoryScannerPlugin;

public class JavaSourceDirectoryScannerPlugin extends AbstractDirectoryScannerPlugin<JavaSourceDirectoryDescriptor> {

    @Override
    protected Scope getRequiredScope() {
        return JavaScope.CLASSPATH;
    }

    @Override
    protected JavaSourceDirectoryDescriptor getContainerDescriptor(File container, ScannerContext scannerContext) {
        return scannerContext.getStore().create(JavaSourceDirectoryDescriptor.class);
    }

    @Override
    protected void enterContainer(File container, JavaSourceDirectoryDescriptor containerDescriptor, ScannerContext scannerContext) throws IOException {
        //do sth. when entering a directory
    }

    @Override
    protected void leaveContainer(File container, JavaSourceDirectoryDescriptor containerDescriptor, ScannerContext scannerContext) throws IOException {
        // do sth. when leaving a directory
    }
}
