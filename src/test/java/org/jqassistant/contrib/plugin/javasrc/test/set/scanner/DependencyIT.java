package org.jqassistant.contrib.plugin.javasrc.test.set.scanner;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.jqassistant.contrib.plugin.javasrc.test.matcher.TypeDescriptorMatcher.typeDescriptor;
import static org.junit.Assert.assertThat;

import java.io.File;

import com.buschmais.jqassistant.plugin.common.test.AbstractPluginIT;
import org.jqassistant.contrib.plugin.javasrc.api.model.JavaSourceDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.JavaSourceDirectoryDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.scanner.JavaScope;
import org.jqassistant.contrib.plugin.javasrc.test.set.scanner.dependency.Dependency;
import org.junit.Test;

/**
 * Contains tests to verify correct scanning of dependencies.
 * 
 * @author Richard Müller
 *
 */
public class DependencyIT extends AbstractPluginIT {

    @Test
    public void testInternalDependency() throws NoSuchMethodException, NoSuchFieldException {
        final String TEST_DIRECTORY_PATH = "src/test/java/";
        final String FILE_DIRECTORY_PATH = "src/test/java/org/jqassistant/contrib/plugin/javasrc/test/set/scanner/dependency/";
        File directory = new File(FILE_DIRECTORY_PATH);
        store.beginTransaction();
        JavaSourceDirectoryDescriptor javaSourceDirectoryDescriptor = getScanner().scan(directory, TEST_DIRECTORY_PATH, JavaScope.SRC);
        assertThat(query("MATCH (dependent:Type)-[:DEPENDS_ON]->(dependency:Type) WHERE dependent.name = 'Dependent' RETURN dependency").getColumn("dependency")
                .size(), equalTo(2));
        assertThat(
                query("MATCH (dependent:Type)-[:DEPENDS_ON]->(dependency:Type) WHERE dependent.name = 'Dependent' RETURN dependency").getColumn("dependency"),
                hasItems(typeDescriptor(Dependency.class), typeDescriptor(void.class)));
        TestResult testResult = query("MATCH (dependent:Type)-[:DEPENDS_ON]->(dependency:Type) WHERE dependent.name = 'Dependent' RETURN dependency.fqn");
        store.commitTransaction();
    }

    @Test
    public void testExternalDependency() throws NoSuchMethodException, NoSuchFieldException {
        final String TEST_DIRECTORY_PATH = "src/test/java/";
        final String FILE_DIRECTORY_PATH = "src/test/java/org/jqassistant/contrib/plugin/javasrc/test/set/scanner/dependency/";
        File directory = new File(FILE_DIRECTORY_PATH);
        store.beginTransaction();
        JavaSourceDirectoryDescriptor javaSourceDirectoryDescriptor = getScanner().scan(directory, TEST_DIRECTORY_PATH, JavaScope.SRC);
        assertThat(query("MATCH (dependent:Type)-[:DEPENDS_ON]->(dependency:Type) WHERE dependent.name = 'ExternalDependency' RETURN dependency")
                .getColumn("dependency").size(), equalTo(1));
        assertThat(query("MATCH (dependent:Type)-[:DEPENDS_ON]->(dependency:Type) WHERE dependent.name = 'ExternalDependency' RETURN dependency")
                .getColumn("dependency"), hasItem(typeDescriptor(JavaSourceDescriptor.class)));
        store.commitTransaction();
    }
}
