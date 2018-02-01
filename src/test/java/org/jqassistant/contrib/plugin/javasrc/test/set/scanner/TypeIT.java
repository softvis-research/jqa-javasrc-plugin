package org.jqassistant.contrib.plugin.javasrc.test.set.scanner;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.jqassistant.contrib.plugin.javasrc.test.matcher.TypeDescriptorMatcher.typeDescriptor;
import static org.junit.Assert.assertThat;

import java.io.File;

import com.buschmais.jqassistant.plugin.common.api.model.FileDescriptor;
import com.buschmais.jqassistant.plugin.common.test.AbstractPluginIT;
import org.jqassistant.contrib.plugin.javasrc.api.model.ClassTypeDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.JavaSourceDirectoryDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.JavaSourceFileDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.TypeDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.scanner.JavaScope;
import org.jqassistant.contrib.plugin.javasrc.test.set.scanner.type.Type;
import org.junit.Test;

/**
 * Contains test to verify correct scanning of types.
 * 
 * @author Richard Müller
 *
 */
public class TypeIT extends AbstractPluginIT {

    @Test
    public void testTypeDescriptor() {
        final String TEST_DIRECTORY_PATH = "src/test/java/";
        final String FILE_DIRECTORY_PATH = "src/test/java/org/jqassistant/contrib/plugin/javasrc/test/set/scanner/type/";
        File directory = new File(FILE_DIRECTORY_PATH);
        store.beginTransaction();
        JavaSourceDirectoryDescriptor javaSourceDirectoryDescriptor = getScanner().scan(directory, TEST_DIRECTORY_PATH, JavaScope.CLASSPATH);
        assertThat(query("MATCH (t:Type) RETURN t").getColumn("t"), hasItem(typeDescriptor(Type.class)));
        store.commitTransaction();
    }

    @Test
    public void testAccessModifier() {
        final String TEST_DIRECTORY_PATH = "src/test/java/";
        final String FILE_DIRECTORY_PATH = "src/test/java/org/jqassistant/contrib/plugin/javasrc/test/set/scanner/type/";
        File directory = new File(FILE_DIRECTORY_PATH);
        store.beginTransaction();
        JavaSourceDirectoryDescriptor javaSourceDirectoryDescriptor = getScanner().scan(directory, TEST_DIRECTORY_PATH, JavaScope.CLASSPATH);
        for (FileDescriptor fileDescriptor : javaSourceDirectoryDescriptor.getContains()) {
            for (TypeDescriptor typeDescriptor : ((JavaSourceFileDescriptor) fileDescriptor).getTypes()) {
                assertTrue(((ClassTypeDescriptor) typeDescriptor).isFinal());
                assertTrue(!((ClassTypeDescriptor) typeDescriptor).isStatic());
                assertTrue(!((ClassTypeDescriptor) typeDescriptor).isAbstract());
            }
        }
        store.commitTransaction();
    }

}
