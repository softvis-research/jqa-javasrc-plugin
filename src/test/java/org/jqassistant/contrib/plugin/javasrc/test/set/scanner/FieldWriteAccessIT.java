package org.jqassistant.contrib.plugin.javasrc.test.set.scanner;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.jqassistant.contrib.plugin.javasrc.test.matcher.FieldDescriptorMatcher.fieldDescriptor;
import static org.junit.Assert.assertThat;

import java.io.File;

import com.buschmais.jqassistant.plugin.common.test.AbstractPluginIT;
import org.jqassistant.contrib.plugin.javasrc.api.model.JavaSourceDirectoryDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.scanner.JavaScope;
import org.jqassistant.contrib.plugin.javasrc.test.set.scanner.write.FieldWriteAccess;
import org.junit.Test;

/**
 * Contains tests to verify correct scanning of write access of field.
 * 
 * @author Richard Müller
 *
 */
public class FieldWriteAccessIT extends AbstractPluginIT {

    @Test
    public void testFieldWriteAccess() throws NoSuchMethodException, NoSuchFieldException {
        final String TEST_DIRECTORY_PATH = "src/test/java/";
        final String FILE_DIRECTORY_PATH = "src/test/java/org/jqassistant/contrib/plugin/javasrc/test/set/scanner/write/";
        File directory = new File(FILE_DIRECTORY_PATH);
        store.beginTransaction();
        JavaSourceDirectoryDescriptor javaSourceDirectoryDescriptor = getScanner().scan(directory, TEST_DIRECTORY_PATH, JavaScope.CLASSPATH);
        // verify fields
        TestResult testResult = query("MATCH (m:Method)-[WRITES]->(f:Field) RETURN f, m");
        assertThat(testResult.getColumn("f").size(), equalTo(2));
        assertThat(testResult.getColumn("f"), hasItems(fieldDescriptor(FieldWriteAccess.class, "a"), fieldDescriptor(FieldWriteAccess.class, "b")));
        // verify line numbers
        assertThat(query("MATCH (:Method)-[w:WRITES]->(field:Field) WHERE field.name='a' RETURN w.lineNumber as lineNumber").getColumn("lineNumber").get(0),
                equalTo(8));
        assertThat(query("MATCH (:Method)-[w:WRITES]->(field:Field) WHERE field.name='b' RETURN w.lineNumber as lineNumber").getColumn("lineNumber").get(0),
                equalTo(9));
    }

}
