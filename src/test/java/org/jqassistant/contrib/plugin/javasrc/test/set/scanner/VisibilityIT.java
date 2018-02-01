package org.jqassistant.contrib.plugin.javasrc.test.set.scanner;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.jqassistant.contrib.plugin.javasrc.test.matcher.FieldDescriptorMatcher.fieldDescriptor;
import static org.jqassistant.contrib.plugin.javasrc.test.matcher.MethodDescriptorMatcher.methodDescriptor;
import static org.jqassistant.contrib.plugin.javasrc.test.matcher.TypeDescriptorMatcher.typeDescriptor;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;

import org.jqassistant.contrib.plugin.javasrc.api.model.JavaSourceDirectoryDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.scanner.JavaScope;
import org.jqassistant.contrib.plugin.javasrc.test.set.scanner.visibility.Visibility;
import org.junit.Test;

import com.buschmais.jqassistant.plugin.common.test.AbstractPluginIT;

public class VisibilityIT extends AbstractPluginIT {

    @Test
    public void testPublicVisibility() throws IOException, NoSuchFieldException, NoSuchMethodException {
    	final String TEST_DIRECTORY_PATH = "src/test/java/";
    	final String FILE_DIRECTORY_PATH = "src/test/java/org/jqassistant/contrib/plugin/javasrc/test/set/scanner/visibility/";
        File directory = new File(FILE_DIRECTORY_PATH);
        store.beginTransaction();
        JavaSourceDirectoryDescriptor javaSourceDirectoryDescriptor = getScanner().scan(directory, TEST_DIRECTORY_PATH, JavaScope.CLASSPATH);
        assertThat(query("MATCH (t:Type) WHERE t.visibility='public' RETURN t").getColumn("t"), hasItem(typeDescriptor(Visibility.class)));
        assertThat(query("MATCH (f:Field) WHERE f.visibility='public' RETURN f").getColumn("f"), hasItem(fieldDescriptor(Visibility.class, "publicField")));
        assertThat(query("MATCH (m:Method) WHERE m.visibility='public' RETURN m").getColumn("m"), hasItem(methodDescriptor(Visibility.class, "publicMethod")));
        store.commitTransaction();
    }
    
    @Test
    public void testPrivateVisibility() throws IOException, NoSuchFieldException, NoSuchMethodException {
    	final String TEST_DIRECTORY_PATH = "src/test/java/";
    	final String FILE_DIRECTORY_PATH = "src/test/java/org/jqassistant/contrib/plugin/javasrc/test/set/scanner/visibility/";
        File directory = new File(FILE_DIRECTORY_PATH);
        store.beginTransaction();
        JavaSourceDirectoryDescriptor javaSourceDirectoryDescriptor = getScanner().scan(directory, TEST_DIRECTORY_PATH, JavaScope.CLASSPATH);
        assertThat(query("MATCH (f:Field) WHERE f.visibility='private' RETURN f").getColumn("f"), hasItem(fieldDescriptor(Visibility.class, "privateField")));
        assertThat(query("MATCH (m:Method) WHERE m.visibility='private' RETURN m").getColumn("m"), hasItem(methodDescriptor(Visibility.class, "privateMethod")));
        store.commitTransaction();
    }
    
    @Test
    public void testProtectedVisibility() throws IOException, NoSuchFieldException, NoSuchMethodException {
    	final String TEST_DIRECTORY_PATH = "src/test/java/";
    	final String FILE_DIRECTORY_PATH = "src/test/java/org/jqassistant/contrib/plugin/javasrc/test/set/scanner/visibility/";
         File directory = new File(FILE_DIRECTORY_PATH);
        store.beginTransaction();
        JavaSourceDirectoryDescriptor javaSourceDirectoryDescriptor = getScanner().scan(directory, TEST_DIRECTORY_PATH, JavaScope.CLASSPATH);
        assertThat(query("MATCH (f:Field) WHERE f.visibility='protected' RETURN f").getColumn("f"), hasItem(fieldDescriptor(Visibility.class, "protectedField")));
        assertThat(query("MATCH (m:Method) WHERE m.visibility='protected' RETURN m").getColumn("m"), hasItem(methodDescriptor(Visibility.class, "protectedMethod")));
        store.commitTransaction();
    }
    
    @Test
    public void testDefaultVisibility() throws IOException, NoSuchFieldException, NoSuchMethodException {
    	final String TEST_DIRECTORY_PATH = "src/test/java/";
    	final String FILE_DIRECTORY_PATH = "src/test/java/org/jqassistant/contrib/plugin/javasrc/test/set/scanner/visibility/";
        File directory = new File(FILE_DIRECTORY_PATH);
        store.beginTransaction();
        JavaSourceDirectoryDescriptor javaSourceDirectoryDescriptor = getScanner().scan(directory, TEST_DIRECTORY_PATH, JavaScope.CLASSPATH);
        assertThat(query("MATCH (f:Field) WHERE f.visibility='default' RETURN f").getColumn("f"), hasItem(fieldDescriptor(Visibility.class, "defaultField")));
        assertThat(query("MATCH (m:Method) WHERE m.visibility='default' RETURN m").getColumn("m"), hasItem(methodDescriptor(Visibility.class, "defaultMethod")));
        store.commitTransaction();
    }
}
