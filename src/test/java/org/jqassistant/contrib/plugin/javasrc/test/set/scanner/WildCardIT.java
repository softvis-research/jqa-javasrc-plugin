/**
 * 
 */
package org.jqassistant.contrib.plugin.javasrc.test.set.scanner;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.jqassistant.contrib.plugin.javasrc.test.matcher.TypeDescriptorMatcher.typeDescriptor;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.buschmais.jqassistant.plugin.common.test.AbstractPluginIT;
import org.jqassistant.contrib.plugin.javasrc.api.model.JavaSourceDirectoryDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.scanner.JavaScope;
import org.jqassistant.contrib.plugin.javasrc.test.set.scanner.wildcard.GenericType;
import org.junit.Test;

/**
 * @author Richard Müller
 *
 */
public class WildCardIT extends AbstractPluginIT {

    @Test
    public void testResolveWildcardBounded() throws IOException, NoSuchFieldException, NoSuchMethodException {
        final String TEST_DIRECTORY_PATH = "src/test/java/";
        final String FILE_DIRECTORY_PATH = "src/test/java/org/jqassistant/contrib/plugin/javasrc/test/set/scanner/wildcard/";
        File directory = new File(FILE_DIRECTORY_PATH);
        store.beginTransaction();
        JavaSourceDirectoryDescriptor javaSourceDirectoryDescriptor = getScanner().scan(directory, TEST_DIRECTORY_PATH, JavaScope.SRC);
        TestResult testResult = query("MATCH (t:Type)-[:DEPENDS_ON]->(dependents:Type) WHERE t.name = 'WildcardParameterBounded' RETURN dependents");
        List<Object> dependents = testResult.getColumn("dependents");
        assertThat(dependents.size(), equalTo(3));
        assertThat(dependents, hasItem(typeDescriptor(String.class)));
        assertThat(dependents, hasItem(typeDescriptor(GenericType.class)));
        assertThat(dependents, hasItem(typeDescriptor(void.class)));
        store.commitTransaction();
    }

    @Test
    public void testResolveWildcardUnbounded() throws IOException, NoSuchFieldException, NoSuchMethodException {
        final String TEST_DIRECTORY_PATH = "src/test/java/";
        final String FILE_DIRECTORY_PATH = "src/test/java/org/jqassistant/contrib/plugin/javasrc/test/set/scanner/wildcard/";
        File directory = new File(FILE_DIRECTORY_PATH);
        store.beginTransaction();
        JavaSourceDirectoryDescriptor javaSourceDirectoryDescriptor = getScanner().scan(directory, TEST_DIRECTORY_PATH, JavaScope.SRC);
        TestResult testResult = query("MATCH (t:Type)-[:DEPENDS_ON]->(dependents:Type) WHERE t.name = 'WildcardParameterUnbounded' RETURN dependents");
        List<Object> dependents = testResult.getColumn("dependents");
        assertThat(dependents.size(), equalTo(3));
        assertThat(dependents, hasItem(typeDescriptor(GenericType.class)));
        assertThat(dependents, hasItem(typeDescriptor(void.class)));
        store.commitTransaction();
    }
}