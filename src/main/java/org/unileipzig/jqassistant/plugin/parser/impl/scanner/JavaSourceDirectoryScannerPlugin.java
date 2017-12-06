package org.unileipzig.jqassistant.plugin.parser.impl.scanner;

import com.buschmais.jqassistant.core.scanner.api.ScannerContext;
import com.buschmais.jqassistant.core.scanner.api.Scope;
import com.buschmais.jqassistant.plugin.common.api.scanner.AbstractDirectoryScannerPlugin;
import org.unileipzig.jqassistant.plugin.parser.api.model.JavaSourceDirectoryDescriptor;
import org.unileipzig.jqassistant.plugin.parser.api.scanner.JavaScope;

import java.io.File;
import java.io.IOException;

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
        //do sth. when entering a directory
        //-> cannot override scan() cause it's marked as *final*, and when the constructor is called we don't have the path information, so it seems it has to be done here of all places...
        //if (JavaSourceDirectoryScannerPlugin.resolver == null) resolver = new Resolver(container.getPath(), scannerContext.getStore());
        // instead of global/static access, put it into scannerContext -> make shure this is done only once as constructor of Resolver will recursively create JavaParserTypeSolver instances!
        if(scannerContext.peekOrDefault(Resolver.class, null) == null) {
            String path = container.getParentFile().getParentFile().getPath(); // for the HelloWorld Files, container.getPath() was not enough (!)
            scannerContext.push(Resolver.class, new Resolver(path, scannerContext.getStore()));
        }
    }

    @Override
    protected void leaveContainer(File container, JavaSourceDirectoryDescriptor containerDescriptor, ScannerContext scannerContext) throws IOException {
        // do sth. when leaving a directory
    }
}
