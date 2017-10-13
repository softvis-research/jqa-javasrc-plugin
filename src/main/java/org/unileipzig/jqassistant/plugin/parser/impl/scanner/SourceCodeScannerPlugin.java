package org.unileipzig.jqassistant.plugin.parser.impl.scanner;

import com.buschmais.jqassistant.core.scanner.api.Scanner;
import com.buschmais.jqassistant.core.scanner.api.ScannerPlugin.Requires;
import com.buschmais.jqassistant.core.scanner.api.Scope;
import com.buschmais.jqassistant.plugin.common.api.model.FileDescriptor;
import com.buschmais.jqassistant.plugin.common.api.scanner.filesystem.FilePatternMatcher;
import com.buschmais.jqassistant.plugin.common.api.scanner.filesystem.FileResource;
import org.unileipzig.jqassistant.plugin.parser.api.model.SourceCodeDescriptor;
import org.unileipzig.jqassistant.plugin.parser.api.scanner.AbstractSourceCodeScannerPlugin;

import java.io.IOException;

@Requires(FileDescriptor.class)
public class SourceCodeScannerPlugin extends AbstractSourceCodeScannerPlugin<SourceCodeDescriptor> {

    public static final String PROPERTY_INCLUDE = "xml.file.include";
    public static final String PROPERTY_EXCLUDE = "xml.file.exclude";

    private FilePatternMatcher filePatternMatcher;

    @Override
    protected void configure() {
        filePatternMatcher = FilePatternMatcher.Builder.newInstance().include(getStringProperty(PROPERTY_INCLUDE, ""))
            .exclude(getStringProperty(PROPERTY_EXCLUDE, null)).build();
    }

    @Override
    public boolean accepts(FileResource item, String path, Scope scope) throws IOException {
        return filePatternMatcher.accepts(path);
    }

    @Override
    public SourceCodeDescriptor scan(FileResource item, SourceCodeDescriptor descriptor, String path, Scope scope, Scanner scanner) throws IOException {
        return descriptor;
    }

}
