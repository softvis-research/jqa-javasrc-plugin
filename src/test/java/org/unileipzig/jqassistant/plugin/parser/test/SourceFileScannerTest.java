package org.unileipzig.jqassistant.plugin.parser.test;

import com.buschmais.jqassistant.core.scanner.api.DefaultScope;
import com.buschmais.jqassistant.core.scanner.api.ScannerContext;
import com.buschmais.jqassistant.plugin.common.api.scanner.filesystem.FileResource;
import com.buschmais.jqassistant.plugin.common.test.scanner.MapBuilder;
import org.unileipzig.jqassistant.plugin.parser.impl.scanner.JavaModuleScannerPlugin;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class SourceFileScannerTest {

    @Mock
    private ScannerContext context;

    @Mock
    private FileResource fileResource;

    private JavaModuleScannerPlugin scannerPlugin = new JavaModuleScannerPlugin();

    @Test
    public void includeFilePattern() throws IOException {
        configure("*.java", null);
        assertThat(scannerPlugin.accepts(fileResource, "test.txt", DefaultScope.NONE), equalTo(false));
        assertThat(scannerPlugin.accepts(fileResource, "test.java", DefaultScope.NONE), equalTo(true));
    }

    @Test
    public void includeAndExcludeFilePattern() throws IOException {
        configure("HelloWorld.java", "test.java");
        assertThat(scannerPlugin.accepts(fileResource, "HelloWorld.java", DefaultScope.NONE), equalTo(true));
        assertThat(scannerPlugin.accepts(fileResource, "test.java", DefaultScope.NONE), equalTo(false));
    }

    private void configure(String includes, String excludes) {
        Map<String, Object> properties = MapBuilder.<String, Object>create(JavaModuleScannerPlugin.PROPERTY_INCLUDE, includes)
            .put(JavaModuleScannerPlugin.PROPERTY_EXCLUDE, excludes).get();
        scannerPlugin.configure(context, properties);
    }
}
