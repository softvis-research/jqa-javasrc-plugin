package org.jqassistant.contrib.plugin.javasrc.test.set.scanner;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.jqassistant.contrib.plugin.javasrc.test.matcher.TypeDescriptorMatcher.typeDescriptor;
import static org.junit.Assert.assertThat;

import java.io.File;

import com.buschmais.jqassistant.plugin.common.test.AbstractPluginIT;
import org.jqassistant.contrib.plugin.javasrc.api.model.JavaSourceDirectoryDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.scanner.JavaScope;
import org.jqassistant.contrib.plugin.javasrc.test.set.scanner.inheritance.SuperInterface;
import org.junit.Test;

/**
 * Contains tests to verify correct scanning of implements and extends
 * relations.
 * 
 * @author Richard Müller
 *
 */
public class InheritanceIT extends AbstractPluginIT {

    @Test
    public void testInterfaceExtendsInterface() {
        final String TEST_DIRECTORY_PATH = "src/test/java/";
        final String FILE_DIRECTORY_PATH = "src/test/java/org/jqassistant/contrib/plugin/javasrc/test/set/scanner/inheritance/";
        File directory = new File(FILE_DIRECTORY_PATH);
        store.beginTransaction();
        JavaSourceDirectoryDescriptor javaSourceDirectoryDescriptor = getScanner().scan(directory, TEST_DIRECTORY_PATH, JavaScope.CLASSPATH);
        assertThat(query("MATCH (sub:Type:Interface)-[:EXTENDS]->(super:Type:Interface) RETURN super").getColumn("super"),
                hasItem(typeDescriptor(SuperInterface.class)));
        store.commitTransaction();
    }

    @Test
    public void testClassImplementsInterface() {
        final String TEST_DIRECTORY_PATH = "src/test/java/";
        final String FILE_DIRECTORY_PATH = "src/test/java/org/jqassistant/contrib/plugin/javasrc/test/set/scanner/inheritance/";
        File directory = new File(FILE_DIRECTORY_PATH);
        store.beginTransaction();
        JavaSourceDirectoryDescriptor javaSourceDirectoryDescriptor = getScanner().scan(directory, TEST_DIRECTORY_PATH, JavaScope.CLASSPATH);
        assertThat(query("MATCH (t:Type)-[:IMPLEMENTS]->(i:Type:Interface) RETURN i").getColumn("i"), hasItem(typeDescriptor(SuperInterface.class)));
        store.commitTransaction();
    }

    @Test
    public void testClassExtendsClass() {
        final String TEST_DIRECTORY_PATH = "src/test/java/";
        final String FILE_DIRECTORY_PATH = "src/test/java/org/jqassistant/contrib/plugin/javasrc/test/set/scanner/inheritance/";
        File directory = new File(FILE_DIRECTORY_PATH);
        store.beginTransaction();
        JavaSourceDirectoryDescriptor javaSourceDirectoryDescriptor = getScanner().scan(directory, TEST_DIRECTORY_PATH, JavaScope.CLASSPATH);
        assertThat(query("MATCH (t:Type)-[:EXTENDS]->(i:Type) RETURN i").getColumn("i"), hasItem(typeDescriptor(Exception.class)));
        store.commitTransaction();
    }
}
