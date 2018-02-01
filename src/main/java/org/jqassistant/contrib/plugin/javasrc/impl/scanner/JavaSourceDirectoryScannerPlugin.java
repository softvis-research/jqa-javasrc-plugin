package org.jqassistant.contrib.plugin.javasrc.impl.scanner;

import java.io.File;
import java.io.IOException;

import org.jqassistant.contrib.plugin.javasrc.api.model.JavaSourceDirectoryDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.scanner.JavaScope;

import com.buschmais.jqassistant.core.scanner.api.ScannerContext;
import com.buschmais.jqassistant.core.scanner.api.Scope;
import com.buschmais.jqassistant.plugin.common.api.scanner.AbstractDirectoryScannerPlugin;

public class JavaSourceDirectoryScannerPlugin extends AbstractDirectoryScannerPlugin<JavaSourceDirectoryDescriptor> {
    //private static Resolver resolver = null;

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
       if (scannerContext.peekOrDefault(TypeResolver.class, null) == null) {
           //TODO find a solution for javasymbolsolver issue: https://github.com/javaparser/javasymbolsolver/issues/353	   
    	   final String path = "src/test/java";
           scannerContext.push(TypeResolver.class, new TypeResolver(path, scannerContext));
        }
    }

    @Override
    protected void leaveContainer(File container, JavaSourceDirectoryDescriptor containerDescriptor, ScannerContext scannerContext) throws IOException {
       scannerContext.pop(TypeResolver.class);
    }
}
