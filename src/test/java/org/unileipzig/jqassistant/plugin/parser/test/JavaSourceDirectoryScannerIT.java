package org.unileipzig.jqassistant.plugin.parser.test;

import java.io.File;

import org.junit.Test;
import org.unileipzig.jqassistant.plugin.parser.api.model.JavaSourceDirectoryDescriptor;
import org.unileipzig.jqassistant.plugin.parser.api.scanner.JavaScope;

import com.buschmais.jqassistant.plugin.common.test.AbstractPluginIT;

public class JavaSourceDirectoryScannerIT extends com.buschmais.jqassistant.plugin.common.test.AbstractPluginIT {

    @Test
    public void scanTestJava() {
        File srcTestJava = new File("src/test/java/samples");
        store.beginTransaction();
        JavaSourceDirectoryDescriptor sourceDirectoryDescriptor = getScanner().scan(srcTestJava, "src/test/java", JavaScope.CLASSPATH);
        store.commitTransaction();
    }

}
