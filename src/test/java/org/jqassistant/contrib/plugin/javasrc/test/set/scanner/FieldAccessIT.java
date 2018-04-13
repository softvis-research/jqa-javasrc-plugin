package org.jqassistant.contrib.plugin.javasrc.test.set.scanner;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.jqassistant.contrib.plugin.javasrc.test.matcher.FieldDescriptorMatcher.fieldDescriptor;
import static org.jqassistant.contrib.plugin.javasrc.test.matcher.MethodDescriptorMatcher.methodDescriptor;
import static org.junit.Assert.assertThat;

import java.io.File;

import com.buschmais.jqassistant.plugin.common.test.AbstractPluginIT;
import org.jqassistant.contrib.plugin.javasrc.api.model.JavaSourceDirectoryDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.scanner.JavaScope;
import org.jqassistant.contrib.plugin.javasrc.test.set.scanner.access.FieldAccess;
import org.junit.Test;

/**
 * Contains tests to verify correct scanning of read and write access of a
 * field.
 * 
 * @author Richard Mueller
 *
 */
public class FieldAccessIT extends AbstractPluginIT {

    @Test
    public void testReadAccess() throws NoSuchMethodException, NoSuchFieldException {
        final String TEST_DIRECTORY_PATH = "src/test/java/";
        final String FILE_DIRECTORY_PATH = "src/test/java/org/jqassistant/contrib/plugin/javasrc/test/set/scanner/access/";
        File directory = new File(FILE_DIRECTORY_PATH);
        store.beginTransaction();
        JavaSourceDirectoryDescriptor javaSourceDirectoryDescriptor = getScanner().scan(directory, TEST_DIRECTORY_PATH, JavaScope.SRC);
        TestResult testResult = query("MATCH (m:Method)-[:READS]->(f:Field) RETURN f, m");
        // verify methods
        assertThat(testResult.getColumn("m").size(), equalTo(4));
        assertThat(testResult.getColumn("m"), hasItems(methodDescriptor(FieldAccess.class, "getA"), methodDescriptor(FieldAccess.class, "getB"),
                methodDescriptor(FieldAccess.class, "setA", int.class), methodDescriptor(FieldAccess.class, "setB", int.class)));
        // verify fields
        assertThat(testResult.getColumn("f"), hasItems(fieldDescriptor(FieldAccess.class, "a"), fieldDescriptor(FieldAccess.class, "b")));
        // verify line numbers
        assertThat(query("MATCH (m:Method)-[r:READS]->(field:Field) WHERE m.name='getA' RETURN r.lineNumber").getColumn("r.lineNumber").get(0), equalTo(8));
        assertThat(query("MATCH (m:Method)-[r:READS]->(field:Field) WHERE m.name='setA' RETURN r.lineNumber").getColumn("r.lineNumber").get(0), equalTo(12));
        assertThat(query("MATCH (m:Method)-[r:READS]->(field:Field) WHERE m.name='getB' RETURN r.lineNumber").getColumn("r.lineNumber").get(0), equalTo(17));
        assertThat(query("MATCH (m:Method)-[r:READS]->(field:Field) WHERE m.name='setB' RETURN r.lineNumber").getColumn("r.lineNumber").get(0), equalTo(21));
        store.commitTransaction();
    }

    @Test
    public void testWriteAccess() throws NoSuchMethodException, NoSuchFieldException {
        final String TEST_DIRECTORY_PATH = "src/test/java/";
        final String FILE_DIRECTORY_PATH = "src/test/java/org/jqassistant/contrib/plugin/javasrc/test/set/scanner/access/";
        File directory = new File(FILE_DIRECTORY_PATH);
        store.beginTransaction();
        JavaSourceDirectoryDescriptor javaSourceDirectoryDescriptor = getScanner().scan(directory, TEST_DIRECTORY_PATH, JavaScope.SRC);
        TestResult testResult = query("MATCH (m:Method)-[:WRITES]->(f:Field) RETURN f, m");
        // verify methods
        assertThat(testResult.getColumn("m").size(), equalTo(2));
        assertThat(testResult.getColumn("m"),
                hasItems(methodDescriptor(FieldAccess.class, "setA", int.class), methodDescriptor(FieldAccess.class, "setB", int.class)));
        // // verify fields
        assertThat(testResult.getColumn("f").size(), equalTo(2));
        assertThat(testResult.getColumn("f"), hasItems(fieldDescriptor(FieldAccess.class, "a"), fieldDescriptor(FieldAccess.class, "b")));
        // verify line numbers
        assertThat(query("MATCH (:Method)-[w:WRITES]->(field:Field) WHERE field.name='a' RETURN w.lineNumber").getColumn("w.lineNumber").get(0), equalTo(12));
        assertThat(query("MATCH (:Method)-[w:WRITES]->(field:Field) WHERE field.name='b' RETURN w.lineNumber").getColumn("w.lineNumber").get(0), equalTo(21));
        store.commitTransaction();
    }
}
