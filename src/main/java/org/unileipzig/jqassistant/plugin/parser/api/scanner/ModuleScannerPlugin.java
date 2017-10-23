package org.unileipzig.jqassistant.plugin.parser.api.scanner;

import com.buschmais.jqassistant.core.scanner.api.Scanner;
import com.buschmais.jqassistant.core.scanner.api.ScannerPlugin.Requires;
import com.buschmais.jqassistant.core.scanner.api.Scope;
import com.buschmais.jqassistant.core.store.api.Store;
import com.buschmais.jqassistant.plugin.common.api.model.FileDescriptor;
import com.buschmais.jqassistant.plugin.common.api.scanner.AbstractScannerPlugin;
import com.buschmais.jqassistant.plugin.common.api.scanner.filesystem.FilePatternMatcher;
import com.buschmais.jqassistant.plugin.common.api.scanner.filesystem.FileResource;
import org.unileipzig.jqassistant.plugin.parser.api.model.ModuleDescriptor;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;

/**
 * Abstract base class for Source Code File scanners
 */
@Requires(FileDescriptor.class)
public abstract class ModuleScannerPlugin extends AbstractScannerPlugin<FileResource, ModuleDescriptor> {
    public static String PROPERTY_INCLUDE = "java.file.include";
    public static String PROPERTY_EXCLUDE = "java.file.exclude";
    protected FilePatternMatcher filePatternMatcher;

    @Override
    protected void configure() {
        filePatternMatcher = FilePatternMatcher.Builder.newInstance()
            .include(getStringProperty(PROPERTY_INCLUDE, ""))
            .exclude(getStringProperty(PROPERTY_EXCLUDE, null)).build();
    }

    @Override
    public boolean accepts(FileResource item, String path, Scope scope) throws IOException {
        return filePatternMatcher.accepts(path);
    }

    @Override
    public Class<? extends FileResource> getType() {
        return FileResource.class;
    }

    @Override
    public Class<ModuleDescriptor> getDescriptorType() {
        return getTypeParameter(ModuleScannerPlugin.class, 0);
    }

    @Override
    public ModuleDescriptor scan(final FileResource item, final String path, final Scope scope, final Scanner scanner) throws IOException {
        Store store = scanner.getContext().getStore();
        String input = new String(Files.readAllBytes(new File(path).toPath()), Charset.forName("UTF-8"));
        final ModuleDescriptor descriptor = store.create(ModuleDescriptor.class);
        descriptor.setFileName(path);
        this.read(descriptor, store, input);
        return descriptor;
    }

    /**
     * Setup Module Graph from input (e.g. via parsing)
     *
     * @param descriptor ModuleDescriptor already created but nothing other than the Filename has been set
     * @param store      Interface for storing the results
     * @param input      File Content
     */
    public abstract void read(ModuleDescriptor descriptor, Store store, String input);
}
