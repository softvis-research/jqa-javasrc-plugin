package org.unileipzig.jqassistant.plugin.parser.impl.scanner;

import com.buschmais.jqassistant.core.scanner.api.Scanner;
import com.buschmais.jqassistant.core.scanner.api.ScannerPlugin.Requires;
import com.buschmais.jqassistant.core.scanner.api.Scope;
import com.buschmais.jqassistant.plugin.common.api.model.FileDescriptor;
import com.buschmais.jqassistant.plugin.common.api.scanner.filesystem.FilePatternMatcher;
import com.buschmais.jqassistant.plugin.common.api.scanner.filesystem.FileResource;
import org.unileipzig.jqassistant.plugin.parser.api.model.SourceFileDescriptor;
import org.unileipzig.jqassistant.plugin.parser.api.scanner.SourceFileScannerPlugin;

import java.io.IOException;

@Requires(FileDescriptor.class)
public class JavaFileScannerPlugin extends SourceFileScannerPlugin<SourceFileDescriptor> {

    public static final String PROPERTY_INCLUDE = "java.file.include";
    public static final String PROPERTY_EXCLUDE = "java.file.exclude";

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
    public SourceFileDescriptor scan(FileResource item, SourceFileDescriptor descriptor, String path, Scope scope, Scanner scanner) throws IOException {
        return descriptor;
    }
}
