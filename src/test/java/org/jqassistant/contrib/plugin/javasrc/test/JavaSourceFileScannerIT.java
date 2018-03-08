package org.jqassistant.contrib.plugin.javasrc.test;

import java.io.File;

import com.buschmais.jqassistant.plugin.common.api.model.FileDescriptor;
import com.buschmais.jqassistant.plugin.common.test.AbstractPluginIT;
import org.jqassistant.contrib.plugin.javasrc.api.scanner.JavaScope;
import org.junit.Test;

public class JavaSourceFileScannerIT extends AbstractPluginIT {

    @Test
    public void testScanJavaFile() {
        final String TEST_FILE_PATH = "src/test/java/org/unileipzig/jqassistant/plugin/parser/test/set/scanner/TypeIT.java";
        File file = new File(TEST_FILE_PATH);
        store.beginTransaction();
        FileDescriptor fileDescriptor = getScanner().scan(file, file.getPath(), JavaScope.SRC);
        store.commitTransaction();
    }

}
