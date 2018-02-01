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
    public void testParameter() throws NoSuchMethodException {
        final String TEST_DIRECTORY_PATH = "src/test/java/";
        final String FILE_DIRECTORY_PATH = "src/test/java/org/jqassistant/contrib/plugin/javasrc/test/set/scanner/method/";
        File directory = new File(FILE_DIRECTORY_PATH);
        store.beginTransaction();
        JavaSourceDirectoryDescriptor javaSourceDirectoryDescriptor = getScanner().scan(directory, TEST_DIRECTORY_PATH, JavaScope.CLASSPATH);
        assertThat(query("MATCH (m:Method) WHERE m.name='parameterType' RETURN m").getColumn("m"),
                hasItem(methodDescriptor(Method.class, "parameterType", String.class)));
        assertThat(query("MATCH (m:Method) WHERE m.name='parameterPrimitiveType' RETURN m").getColumn("m"),
                hasItem(methodDescriptor(Method.class, "parameterPrimitiveType", int.class)));
        assertThat(query("MATCH (m:Method) WHERE m.name='parameterArray' RETURN m").getColumn("m"),
                hasItem(methodDescriptor(Method.class, "parameterArray", boolean[].class)));
        store.commitTransaction();
    }

    @Test
    public void testReturnType() throws NoSuchMethodException {
        final String TEST_DIRECTORY_PATH = "src/test/java/";
        final String FILE_DIRECTORY_PATH = "src/test/java/org/jqassistant/contrib/plugin/javasrc/test/set/scanner/method/";
        File directory = new File(FILE_DIRECTORY_PATH);
        store.beginTransaction();
        JavaSourceDirectoryDescriptor javaSourceDirectoryDescriptor = getScanner().scan(directory, TEST_DIRECTORY_PATH, JavaScope.CLASSPATH);
        assertThat(query("MATCH (m:Method) WHERE m.name='returnPrimitiveType' RETURN m").getColumn("m"),
                hasItem(methodDescriptor(Method.class, "returnPrimitiveType")));
        assertThat(query("MATCH (m:Method) WHERE m.name='returnVoid' RETURN m").getColumn("m"),
                hasItem(methodDescriptor(Method.class, "returnVoid")));
        assertThat(query("MATCH (m:Method) WHERE m.name='returnType' RETURN m").getColumn("m"),
                hasItem(methodDescriptor(Method.class, "returnType")));
        assertThat(query("MATCH (m:Method) WHERE m.name='returnArray' RETURN m").getColumn("m"),
                hasItem(methodDescriptor(Method.class, "returnArray")));
        store.commitTransaction();
    }
}
