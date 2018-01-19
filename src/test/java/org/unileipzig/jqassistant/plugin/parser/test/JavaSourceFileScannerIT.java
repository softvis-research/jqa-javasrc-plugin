package org.unileipzig.jqassistant.plugin.parser.test;

import java.io.File;

import org.junit.Test;
import org.unileipzig.jqassistant.plugin.parser.api.scanner.JavaScope;

import com.buschmais.jqassistant.plugin.common.api.model.FileDescriptor;
import com.buschmais.jqassistant.plugin.common.test.AbstractPluginIT;

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
