package org.jqassistant.contrib.plugin.javasrc.test.set.scanner;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.jqassistant.contrib.plugin.javasrc.test.matcher.FieldDescriptorMatcher.fieldDescriptor;
import static org.jqassistant.contrib.plugin.javasrc.test.matcher.TypeDescriptorMatcher.typeDescriptor;
import static org.junit.Assert.assertThat;

import java.io.File;

import com.buschmais.jqassistant.plugin.common.test.AbstractPluginIT;
import org.jqassistant.contrib.plugin.javasrc.api.model.JavaSourceDirectoryDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.scanner.JavaScope;
import org.jqassistant.contrib.plugin.javasrc.test.set.scanner.enumeration.EnumerationType;
import org.junit.Test;

/**
 * Contains tests to verify correct scanning of enums.
 * 
 * @author Richard Müller
 * 
 */
public class EnumerationIT extends AbstractPluginIT {

    @Test
    public void testEnumerationType() throws NoSuchFieldException {
        final String TEST_DIRECTORY_PATH = "src/test/java/";
        final String TYPE_DIRECTORY_PATH = "src/test/java/org/jqassistant/contrib/plugin/javasrc/test/set/scanner/enumeration/";
        File directory = new File(TYPE_DIRECTORY_PATH);
        store.beginTransaction();
        JavaSourceDirectoryDescriptor javaSourceDirectoryDescriptor = getScanner().scan(directory, TEST_DIRECTORY_PATH, JavaScope.SRC);
        assertThat(query("MATCH (e:Type:Enum) RETURN e").getColumn("e"), hasItem(typeDescriptor(EnumerationType.class)));
        assertThat(query("MATCH (e:Type:Enum)-[:DECLARES]->(f:Field) WHERE f.name = 'A' RETURN f").getColumn("f"),
                hasItem(fieldDescriptor(EnumerationType.class, "A")));
        assertThat(query("MATCH (e:Type:Enum)-[:DECLARES]->(f:Field) WHERE f.name = 'B' RETURN f").getColumn("f"),
                hasItem(fieldDescriptor(EnumerationType.class, "B")));
        assertThat(query("MATCH (e:Type:Enum)-[:DECLARES]->(f:Field) WHERE f.name = 'value' RETURN f").getColumn("f"),
                hasItem(fieldDescriptor(EnumerationType.class, "value")));
        store.commitTransaction();
    }
}
