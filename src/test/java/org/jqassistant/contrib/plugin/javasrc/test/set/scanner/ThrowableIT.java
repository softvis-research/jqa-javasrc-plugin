package org.jqassistant.contrib.plugin.javasrc.test.set.scanner;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.jqassistant.contrib.plugin.javasrc.test.matcher.TypeDescriptorMatcher.typeDescriptor;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;

import com.buschmais.jqassistant.plugin.common.test.AbstractPluginIT;
import org.jqassistant.contrib.plugin.javasrc.api.model.JavaSourceDirectoryDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.scanner.JavaScope;
import org.jqassistant.contrib.plugin.javasrc.test.set.scanner.throwable.MyException;
import org.junit.Test;

/**
 * Contains tests to verify correct scanning of thrown exceptions by methods and
 * constructors.
 * 
 * @author Richard Müller
 *
 */
public class ThrowableIT extends AbstractPluginIT {

    @Test
    public void testMethodExceptions() throws NoSuchMethodException, NoSuchFieldException {
        final String TEST_DIRECTORY_PATH = "src/test/java/";
        final String FILE_DIRECTORY_PATH = "src/test/java/org/jqassistant/contrib/plugin/javasrc/test/set/scanner/throwable/";
        File directory = new File(FILE_DIRECTORY_PATH);
        store.beginTransaction();
        JavaSourceDirectoryDescriptor javaSourceDirectoryDescriptor = getScanner().scan(directory, TEST_DIRECTORY_PATH, JavaScope.CLASSPATH);
        assertThat(query("MATCH (m:Method)-[:THROWS]->(exception:Type) WHERE m.name = 'method1' RETURN exception").getColumn("exception"),
                hasItem(typeDescriptor(IOException.class)));
        assertThat(query("MATCH (m:Method)-[:THROWS]->(exception:Type) WHERE m.name = 'method2' RETURN exception").getColumn("exception"),
                hasItems(typeDescriptor(RuntimeException.class), typeDescriptor(MyException.class)));
        store.commitTransaction();
    }

    @Test
    public void testConstructorExceptions() throws NoSuchMethodException, NoSuchFieldException {
        final String TEST_DIRECTORY_PATH = "src/test/java/";
        final String FILE_DIRECTORY_PATH = "src/test/java/org/jqassistant/contrib/plugin/javasrc/test/set/scanner/throwable/";
        File directory = new File(FILE_DIRECTORY_PATH);
        store.beginTransaction();
        JavaSourceDirectoryDescriptor javaSourceDirectoryDescriptor = getScanner().scan(directory, TEST_DIRECTORY_PATH, JavaScope.CLASSPATH);
        assertThat(query("MATCH (c:Method:Constructor)-[:THROWS]->(exception:Type) WHERE c.name = 'Throwable' RETURN exception").getColumn("exception"),
                hasItem(typeDescriptor(NoSuchMethodError.class)));
        store.commitTransaction();
    }
}
