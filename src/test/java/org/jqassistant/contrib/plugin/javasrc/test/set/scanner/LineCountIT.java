package org.jqassistant.contrib.plugin.javasrc.test.set.scanner;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.File;

import com.buschmais.jqassistant.plugin.common.test.AbstractPluginIT;
import org.jqassistant.contrib.plugin.javasrc.api.model.JavaSourceDirectoryDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.scanner.JavaScope;
import org.junit.Test;

/**
 * Contains tests to verify correct scanning of LOC of methods and constructors.
 * 
 * @author Richard Müller
 *
 */
public class LineCountIT extends AbstractPluginIT {

    @Test
    public void testMethodLineCount() throws NoSuchMethodException, NoSuchFieldException {
        final String TEST_DIRECTORY_PATH = "src/test/java/";
        final String FILE_DIRECTORY_PATH = "src/test/java/org/jqassistant/contrib/plugin/javasrc/test/set/scanner/loc/";
        File directory = new File(FILE_DIRECTORY_PATH);
        store.beginTransaction();
        JavaSourceDirectoryDescriptor javaSourceDirectoryDescriptor = getScanner().scan(directory, TEST_DIRECTORY_PATH, JavaScope.SRC);
        assertThat(query("MATCH (m:Method) WHERE m.name = 'add' RETURN m.effectiveLineCount").getColumn("m.effectiveLineCount").get(0), equalTo(4));
        assertThat(query("MATCH (m:Method) WHERE m.name = 'emptyMethod' RETURN m.effectiveLineCount").getColumn("m.effectiveLineCount").get(0), equalTo(2));
        store.commitTransaction();
    }

    @Test
    public void testConstructorLineCount() throws NoSuchMethodException, NoSuchFieldException {
        final String TEST_DIRECTORY_PATH = "src/test/java/";
        final String FILE_DIRECTORY_PATH = "src/test/java/org/jqassistant/contrib/plugin/javasrc/test/set/scanner/loc/";
        File directory = new File(FILE_DIRECTORY_PATH);
        store.beginTransaction();
        JavaSourceDirectoryDescriptor javaSourceDirectoryDescriptor = getScanner().scan(directory, TEST_DIRECTORY_PATH, JavaScope.SRC);
        assertThat(query("MATCH (c:Method:Constructor) RETURN c.effectiveLineCount").getColumn("c.effectiveLineCount").get(0), equalTo(3));

        store.commitTransaction();
    }
}
