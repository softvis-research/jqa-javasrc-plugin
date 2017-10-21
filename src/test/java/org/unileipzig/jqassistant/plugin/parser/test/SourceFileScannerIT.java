package org.unileipzig.jqassistant.plugin.parser.test;

import com.buschmais.jqassistant.core.scanner.api.Scanner;
import com.buschmais.jqassistant.plugin.common.test.AbstractPluginIT;
import com.buschmais.jqassistant.plugin.common.test.scanner.MapBuilder;
import org.junit.Test;
import org.unileipzig.jqassistant.plugin.parser.api.model.SourceFileDescriptor;
import org.unileipzig.jqassistant.plugin.parser.api.scanner.JavaScope;

import java.io.File;
import java.io.IOException;
import java.util.Map;


public class SourceFileScannerIT extends AbstractPluginIT {
    private Scanner getJavaFileScanner() {
        Map<String, Object> properties = MapBuilder.<String, Object>create("java.file.include", "*.java").get();
        return getScanner(properties);
    }

    @Test
    public void validOutput() throws IOException {
        File f = new File(getClassesDirectory(SourceFileScannerIT.class), "/HelloWorld.java");
        System.out.println(f.getAbsolutePath());
        store.beginTransaction();
        SourceFileDescriptor descriptor = getJavaFileScanner().scan(f, f.getAbsolutePath(), JavaScope.CLASSPATH);
        //verifyDocument(descriptor);
        store.commitTransaction();
    }
}
