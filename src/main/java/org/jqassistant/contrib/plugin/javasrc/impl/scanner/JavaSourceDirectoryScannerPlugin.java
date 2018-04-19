package org.jqassistant.contrib.plugin.javasrc.impl.scanner;

import java.io.File;
import java.io.IOException;

import com.buschmais.jqassistant.core.scanner.api.ScannerContext;
import com.buschmais.jqassistant.core.scanner.api.Scope;
import com.buschmais.jqassistant.plugin.common.api.scanner.AbstractDirectoryScannerPlugin;
import org.jqassistant.contrib.plugin.javasrc.api.model.JavaSourceDirectoryDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.scanner.JavaScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JavaSourceDirectoryScannerPlugin extends AbstractDirectoryScannerPlugin<JavaSourceDirectoryDescriptor> {
    private static final Logger LOGGER = LoggerFactory.getLogger(JavaSourceDirectoryScannerPlugin.class);
    private final String JQASSISTANT_PLUGIN_JAVASRC_JAR_DIRNAME = "jqassistant.plugin.javasrc.jar.dirname";
    private String jarDirName = "src/test/resources";

    @Override
    protected Scope getRequiredScope() {
        return JavaScope.SRC;
    }

    @Override
    protected void configure() {
        super.configure();
        if (getProperties().containsKey(JQASSISTANT_PLUGIN_JAVASRC_JAR_DIRNAME)) {
            jarDirName = (String) getProperties().get(JQASSISTANT_PLUGIN_JAVASRC_JAR_DIRNAME);
        }

        LOGGER.info("Java Source Parser plugin looks for jar files in directory '{}'", jarDirName);
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
            scannerContext.push(JavaTypeSolver.class, new JavaTypeSolver(containerDescriptor.getFileName(), this.jarDirName));
        }
    }

    @Override
    protected void leaveContainer(File container, JavaSourceDirectoryDescriptor containerDescriptor, ScannerContext scannerContext) throws IOException {
        scannerContext.pop(JavaTypeResolver.class);
        scannerContext.pop(JavaTypeSolver.class);
    }
}
