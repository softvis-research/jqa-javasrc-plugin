package org.jqassistant.contrib.plugin.javasrc.test.set.scanner;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.jqassistant.contrib.plugin.javasrc.test.matcher.MethodDescriptorMatcher.methodDescriptor;
import static org.junit.Assert.assertThat;

import java.io.File;

import com.buschmais.jqassistant.plugin.common.test.AbstractPluginIT;
import org.jqassistant.contrib.plugin.javasrc.api.model.JavaSourceDirectoryDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.scanner.JavaScope;
import org.jqassistant.contrib.plugin.javasrc.test.set.scanner.method.Method;
import org.junit.Test;

/**
 * Contains test to verify correct scanning of methods.
 * 
 * @author Richard Müller
 *
 */
public class MethodIT extends AbstractPluginIT {

    @Test
    public void testReturnType() throws NoSuchMethodException {
        final String TEST_DIRECTORY_PATH = "src/test/java/";
        final String FILE_DIRECTORY_PATH = "src/test/java/org/jqassistant/contrib/plugin/javasrc/test/set/scanner/method/";
        File directory = new File(FILE_DIRECTORY_PATH);
        store.beginTransaction();
        JavaSourceDirectoryDescriptor javaSourceDirectoryDescriptor = getScanner().scan(directory, TEST_DIRECTORY_PATH, JavaScope.CLASSPATH);
        assertThat(query("MATCH (m:Method) WHERE m.signature='int returningPrimitiveType()' RETURN m").getColumn("m"),
                hasItem(methodDescriptor(Method.class, "returningPrimitiveType")));
        assertThat(query("MATCH (m:Method) WHERE m.signature='void returningVoid()' RETURN m").getColumn("m"),
                hasItem(methodDescriptor(Method.class, "returningVoid")));
        assertThat(query("MATCH (m:Method) WHERE m.signature='java.lang.String returningType()' RETURN m").getColumn("m"),
                hasItem(methodDescriptor(Method.class, "returningType")));
        store.commitTransaction();
    }

    @Test
    public void testParameter() throws NoSuchMethodException {
        final String TEST_DIRECTORY_PATH = "src/test/java/";
        final String FILE_DIRECTORY_PATH = "src/test/java/org/jqassistant/contrib/plugin/javasrc/test/set/scanner/method/";
        File directory = new File(FILE_DIRECTORY_PATH);
        store.beginTransaction();
        JavaSourceDirectoryDescriptor javaSourceDirectoryDescriptor = getScanner().scan(directory, TEST_DIRECTORY_PATH, JavaScope.CLASSPATH);
        // assertThat(query("MATCH (m:Method) WHERE m.signature='int
        // returningPrimitiveType()' RETURN m").getColumn("m"),
        // hasItem(methodDescriptor(Method.class, "returningPrimitiveType")));
        // assertThat(query("MATCH (m:Method) WHERE m.signature='void
        // returningVoid()' RETURN m").getColumn("m"),
        // hasItem(methodDescriptor(Method.class, "returningVoid")));
        // assertThat(query("MATCH (m:Method) WHERE
        // m.signature='java.lang.String returningType()' RETURN
        // m").getColumn("m"), hasItem(methodDescriptor(Method.class,
        // "returningType")));
        store.commitTransaction();
    }
}
