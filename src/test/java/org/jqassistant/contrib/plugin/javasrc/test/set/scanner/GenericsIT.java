package org.jqassistant.contrib.plugin.javasrc.test.set.scanner;

import static org.hamcrest.Matchers.hasItem;
import static org.jqassistant.contrib.plugin.javasrc.test.matcher.MethodDescriptorMatcher.constructorDescriptor;
import static org.jqassistant.contrib.plugin.javasrc.test.matcher.TypeDescriptorMatcher.typeDescriptor;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;

import com.buschmais.jqassistant.plugin.common.test.AbstractPluginIT;
import org.jqassistant.contrib.plugin.javasrc.api.model.JavaSourceDirectoryDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.scanner.JavaScope;
import org.jqassistant.contrib.plugin.javasrc.test.set.scanner.generics.BoundGenericType;
import org.jqassistant.contrib.plugin.javasrc.test.set.scanner.generics.GenericType;
import org.jqassistant.contrib.plugin.javasrc.test.set.scanner.generics.NestedGenericMethod;
import org.jqassistant.contrib.plugin.javasrc.test.set.scanner.generics.NestedGenericType;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Contains tests to verify correct scanning generics.
 * 
 * @author Dirk Mahler, Richard Müller
 *
 */
public class GenericsIT extends AbstractPluginIT {

    @Test
    public void testGenericType() throws IOException, NoSuchMethodException {
        final String TEST_DIRECTORY_PATH = "src/test/java/";
        final String FILE_DIRECTORY_PATH = "src/test/java/org/jqassistant/contrib/plugin/javasrc/test/set/scanner/generics/";
        File directory = new File(FILE_DIRECTORY_PATH);
        store.beginTransaction();
        JavaSourceDirectoryDescriptor javaSourceDirectoryDescriptor = getScanner().scan(directory, TEST_DIRECTORY_PATH, JavaScope.SRC);
        // assertThat(query("MATCH (g:Type)-[:EXTENDS]->(s) RETURN
        // s").getColumn("s"), hasItem(typeDescriptor(Object.class)));
        assertThat(query("MATCH (g:Type) WHERE g.name='GenericType' RETURN g").getColumn("g"), hasItem(typeDescriptor(GenericType.class)));
        // TODO implicit default constructor is not scanned
        // assertThat(query("MATCH (g:Type)-[:DECLARES]->(c:Constructor) RETURN
        // c").getColumn("c"),
        // hasItem(constructorDescriptor(GenericType.class)));
        store.commitTransaction();
    }

    @Test
    public void testBoundGenericType() throws IOException, NoSuchMethodException {
        final String TEST_DIRECTORY_PATH = "src/test/java/";
        final String FILE_DIRECTORY_PATH = "src/test/java/org/jqassistant/contrib/plugin/javasrc/test/set/scanner/generics/";
        File directory = new File(FILE_DIRECTORY_PATH);
        store.beginTransaction();
        JavaSourceDirectoryDescriptor javaSourceDirectoryDescriptor = getScanner().scan(directory, TEST_DIRECTORY_PATH, JavaScope.SRC);
        // assertThat(query("MATCH (b:Type)-[:EXTENDS]->(s) RETURN
        // s").getColumn("s"), hasItem(typeDescriptor(Object.class)));
        assertThat(query("MATCH (g:Type) WHERE g.name='BoundGenericType' RETURN g").getColumn("g"), hasItem(typeDescriptor(BoundGenericType.class)));
        // assertThat(query("MATCH (b:Type)-[:DECLARES]->(c:Constructor) RETURN
        // c").getColumn("c"),
        // hasItem(constructorDescriptor(BoundGenericType.class)));
        store.commitTransaction();
    }

    @Test
    @Ignore
    public void testNestedGenericType() throws IOException, NoSuchMethodException {
        final String TEST_DIRECTORY_PATH = "src/test/java/";
        final String FILE_DIRECTORY_PATH = "src/test/java/org/jqassistant/contrib/plugin/javasrc/test/set/scanner/generics/";
        File directory = new File(FILE_DIRECTORY_PATH);
        store.beginTransaction();
        JavaSourceDirectoryDescriptor javaSourceDirectoryDescriptor = getScanner().scan(directory, TEST_DIRECTORY_PATH, JavaScope.SRC);
        assertThat(query("MATCH (n:Type)-[:EXTENDS]->(s) RETURN s").getColumn("s"), hasItem(typeDescriptor(Object.class)));
        assertThat(query("MATCH (n:Type)-[:DECLARES]->(c:Constructor) RETURN c").getColumn("c"), hasItem(constructorDescriptor(NestedGenericType.class)));
        assertThat(query("MATCH (n:Type)-[:DEPENDS_ON]->(d) RETURN d").getColumn("d"), hasItem(typeDescriptor(GenericType.class)));
    }

    @Test
    @Ignore
    public void testNestedGenericMethod() throws IOException, NoSuchMethodException {
        final String TEST_DIRECTORY_PATH = "src/test/java/";
        final String FILE_DIRECTORY_PATH = "src/test/java/org/jqassistant/contrib/plugin/javasrc/test/set/scanner/generics/";
        File directory = new File(FILE_DIRECTORY_PATH);
        store.beginTransaction();
        JavaSourceDirectoryDescriptor javaSourceDirectoryDescriptor = getScanner().scan(directory, TEST_DIRECTORY_PATH, JavaScope.SRC);
        assertThat(query("MATCH (n:Type)-[:EXTENDS]->(s) RETURN s").getColumn("s"), hasItem(typeDescriptor(Object.class)));
        assertThat(query("MATCH (n:Type)-[:DECLARES]->(c:Constructor) RETURN c").getColumn("c"), hasItem(constructorDescriptor(NestedGenericMethod.class)));
        store.commitTransaction();
    }

    @Test
    public void testExtendsGenericClass() throws IOException, NoSuchMethodException {
        final String TEST_DIRECTORY_PATH = "src/test/java/";
        final String FILE_DIRECTORY_PATH = "src/test/java/org/jqassistant/contrib/plugin/javasrc/test/set/scanner/generics/";
        File directory = new File(FILE_DIRECTORY_PATH);
        store.beginTransaction();
        JavaSourceDirectoryDescriptor javaSourceDirectoryDescriptor = getScanner().scan(directory, TEST_DIRECTORY_PATH, JavaScope.SRC);
        assertThat(query("MATCH (e:Type)-[:EXTENDS]->(s) RETURN s").getColumn("s"), hasItem(typeDescriptor(GenericType.class)));
        assertThat(query("MATCH (e:Type)-[:DEPENDS_ON]->(d) RETURN d").getColumn("d"), hasItem(typeDescriptor(Number.class)));
        // assertThat(query("MATCH (e:Type)-[:DECLARES]->(c:Constructor) RETURN
        // c").getColumn("c"),
        // hasItem(constructorDescriptor(ExtendsGenericClass.class)));
        store.commitTransaction();
    }

    @Test
    public void testImplementsGenericInterface() throws IOException, NoSuchMethodException {
        final String TEST_DIRECTORY_PATH = "src/test/java/";
        final String FILE_DIRECTORY_PATH = "src/test/java/org/jqassistant/contrib/plugin/javasrc/test/set/scanner/generics/";
        File directory = new File(FILE_DIRECTORY_PATH);
        store.beginTransaction();
        JavaSourceDirectoryDescriptor javaSourceDirectoryDescriptor = getScanner().scan(directory, TEST_DIRECTORY_PATH, JavaScope.SRC);
        assertThat(query("MATCH (igi:Type)-[:IMPLEMENTS]->(i) RETURN i").getColumn("i"), hasItem(typeDescriptor(Iterable.class)));
        assertThat(query("MATCH (igi:Type)-[:DEPENDS_ON]->(d) RETURN d").getColumn("d"), hasItem(typeDescriptor(Number.class)));
        // assertThat(query("MATCH (igi:Type)-[:DECLARES]->(c:Constructor)
        // RETURN c").getColumn("c"),
        // hasItem(constructorDescriptor(ImplementsGenericInterface.class)));
        store.commitTransaction();
    }

    @Test
    public void testGenericMembers() throws IOException, NoSuchMethodException, NoSuchFieldException {
        final String TEST_DIRECTORY_PATH = "src/test/java/";
        final String FILE_DIRECTORY_PATH = "src/test/java/org/jqassistant/contrib/plugin/javasrc/test/set/scanner/generics/";
        File directory = new File(FILE_DIRECTORY_PATH);
        store.beginTransaction();
        JavaSourceDirectoryDescriptor javaSourceDirectoryDescriptor = getScanner().scan(directory, TEST_DIRECTORY_PATH, JavaScope.SRC);
        TestResult result = query("MATCH (gm:Type)-[:DEPENDS_ON]->(tv) RETURN tv");
        assertThat(result.getColumn("tv"), hasItem(typeDescriptor(Integer.class)));
        assertThat(result.getColumn("tv"), hasItem(typeDescriptor(Number.class)));
        assertThat(result.getColumn("tv"), hasItem(typeDescriptor(Double.class)));
        store.commitTransaction();
    }
}
