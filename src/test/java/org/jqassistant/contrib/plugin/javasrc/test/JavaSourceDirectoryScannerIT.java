package org.jqassistant.contrib.plugin.javasrc.test;

import java.io.File;

import com.buschmais.jqassistant.plugin.common.test.AbstractPluginIT;
import org.jqassistant.contrib.plugin.javasrc.api.model.JavaSourceDirectoryDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.scanner.JavaScope;
import org.junit.Test;

/**
 * Contains test to verify correct scanning of java source directories.
 * 
 * @author Richard Mueller
 *
 */
public class JavaSourceDirectoryScannerIT extends AbstractPluginIT {

    @Test
    public void testScanJavaDirectory() {
        final String TEST_DIRECTORY_PATH = "src/test/java";
        final String FILE_DIRECTORY_PATH = "src/test/java/org/jqassistant/contrib/plugin/javasrc/test/set/scanner/type/";
        File directory = new File(FILE_DIRECTORY_PATH);
        store.beginTransaction();
        JavaSourceDirectoryDescriptor javaSourceDirectoryDescriptor = getScanner().scan(directory, TEST_DIRECTORY_PATH, JavaScope.SRC);
        store.commitTransaction();
    }

}
