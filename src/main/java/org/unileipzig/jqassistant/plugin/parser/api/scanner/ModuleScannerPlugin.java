package org.unileipzig.jqassistant.plugin.parser.api.scanner;

import com.buschmais.jqassistant.core.scanner.api.Scanner;
import com.buschmais.jqassistant.core.scanner.api.ScannerPlugin.Requires;
import com.buschmais.jqassistant.core.scanner.api.Scope;
import com.buschmais.jqassistant.plugin.common.api.model.FileDescriptor;
import com.buschmais.jqassistant.plugin.common.api.scanner.AbstractScannerPlugin;
import com.buschmais.jqassistant.plugin.common.api.scanner.filesystem.FileResource;
import org.unileipzig.jqassistant.plugin.parser.api.model.ModuleDescriptor;

import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;

/**
 * Abstract base class for Source Code File scanners
 *
 * @param <D> The descriptor type.
 */
@Requires(FileDescriptor.class)
public abstract class ModuleScannerPlugin<D extends ModuleDescriptor> extends AbstractScannerPlugin<FileResource, D> {
    @Override
    public Class<? extends FileResource> getType() {
        return FileResource.class;
    }

    @Override
    public Class<D> getDescriptorType() {
        return getTypeParameter(ModuleScannerPlugin.class, 0);
    }

    @Override
    public D scan(FileResource item, String path, Scope scope, Scanner scanner) throws IOException {
        FileDescriptor fileDescriptor = scanner.getContext().getCurrentDescriptor();
        Class<D> descriptorType = getDescriptorType();
        D descriptor = scanner.getContext().getStore().addDescriptorType(fileDescriptor, descriptorType);
        scanner.getContext().push(ModuleDescriptor.class, descriptor); // ?
        scanner.scan(new String(Files.readAllBytes(new File(path).toPath()), Charset.forName("UTF-8")), path, scope);
        scanner.getContext().pop(ModuleDescriptor.class); // ?
        return descriptor;
    }

    public abstract D scan(String input, D descriptor, Scope scope, Scanner scanner) throws IOException;
}
