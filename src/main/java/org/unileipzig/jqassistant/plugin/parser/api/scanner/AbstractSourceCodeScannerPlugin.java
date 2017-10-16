package org.unileipzig.jqassistant.plugin.parser.api.scanner;

import com.buschmais.jqassistant.core.scanner.api.Scanner;
import com.buschmais.jqassistant.core.scanner.api.ScannerPlugin.Requires;
import com.buschmais.jqassistant.core.scanner.api.Scope;
import com.buschmais.jqassistant.plugin.common.api.model.FileDescriptor;
import com.buschmais.jqassistant.plugin.common.api.scanner.AbstractScannerPlugin;
import com.buschmais.jqassistant.plugin.common.api.scanner.filesystem.FileResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unileipzig.jqassistant.plugin.parser.api.model.SourceCodeDescriptor;

import java.io.IOException;

/**
 * Abstract base class for Source Code scanners
 *
 * @param <D> The descriptor type.
 */
@Requires(FileDescriptor.class)
public abstract class AbstractSourceCodeScannerPlugin<D extends SourceCodeDescriptor> extends
    AbstractScannerPlugin<FileResource, D> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSourceCodeScannerPlugin.class);

    @Override
    public Class<? extends FileResource> getType() {
        return FileResource.class;
    }

    @Override
    public Class<D> getDescriptorType() {
        return getTypeParameter(AbstractSourceCodeScannerPlugin.class, 0);
    }

    @Override
    public D scan(FileResource item, String path, Scope scope, Scanner scanner) throws IOException {
        FileDescriptor fileDescriptor = scanner.getContext().getCurrentDescriptor();
        Class<D> descriptorType = getDescriptorType();
        D sourceFileDescriptor = scanner.getContext().getStore().addDescriptorType(fileDescriptor, descriptorType);
        // TODO
        return sourceFileDescriptor;
    }

    public abstract D scan(FileResource item, D descriptor, String path, Scope scope, Scanner scanner) throws IOException;
}
