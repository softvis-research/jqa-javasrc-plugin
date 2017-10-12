package com.buschmais.jqassistant.plugin.parser.test;

import com.buschmais.jqassistant.core.scanner.api.DefaultScope;
import com.buschmais.jqassistant.core.scanner.api.ScannerContext;
import com.buschmais.jqassistant.plugin.common.api.scanner.filesystem.FileResource;
import com.buschmais.jqassistant.plugin.common.test.scanner.MapBuilder;
import com.buschmais.jqassistant.plugin.parser.impl.scanner.SourceCodeScannerPlugin;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class SourceCodeScannerTest { // TODO: change everything to test Java instead of XML

    @Mock
    private ScannerContext context;

    @Mock
    private FileResource fileResource;

    private SourceCodeScannerPlugin scannerPlugin = new SourceCodeScannerPlugin();

    @Test
    public void noFilePattern() throws IOException {
        configure(null, null);
        assertThat(scannerPlugin.accepts(fileResource, "test.txt", DefaultScope.NONE), equalTo(false));
        assertThat(scannerPlugin.accepts(fileResource, "test.xml", DefaultScope.NONE), equalTo(false));
    }

    @Test
    public void includeFilePattern() throws IOException {
        configure("*.xml", null);
        assertThat(scannerPlugin.accepts(fileResource, "test.txt", DefaultScope.NONE), equalTo(false));
        assertThat(scannerPlugin.accepts(fileResource, "test.xml", DefaultScope.NONE), equalTo(true));
    }

    @Test
    public void includeAndExcludeFilePattern() throws IOException {
        configure("test.*", "*.xml");
        assertThat(scannerPlugin.accepts(fileResource, "test.txt", DefaultScope.NONE), equalTo(true));
        assertThat(scannerPlugin.accepts(fileResource, "test.xml", DefaultScope.NONE), equalTo(false));
    }

    private void configure(String includes, String excludes) {
        Map<String, Object> properties = MapBuilder.<String, Object>create(SourceCodeScannerPlugin.PROPERTY_INCLUDE, includes)
            .put(SourceCodeScannerPlugin.PROPERTY_EXCLUDE, excludes).get();
        scannerPlugin.configure(context, properties);
    }
}
