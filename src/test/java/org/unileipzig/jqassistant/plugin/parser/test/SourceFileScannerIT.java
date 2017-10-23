package org.unileipzig.jqassistant.plugin.parser.test;

import com.buschmais.jqassistant.core.scanner.api.DefaultScope;
import com.buschmais.jqassistant.core.scanner.api.Scanner;
import com.buschmais.jqassistant.plugin.common.test.AbstractPluginIT;
import com.buschmais.jqassistant.plugin.common.test.scanner.MapBuilder;
import org.junit.Test;
import org.unileipzig.jqassistant.plugin.parser.api.model.ModuleDescriptor;
import org.unileipzig.jqassistant.plugin.parser.api.scanner.JavaScope;

import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.IOException;
import java.util.Map;


public class SourceFileScannerIT extends AbstractPluginIT {
    private Scanner getJavaFileScanner() {
        Map<String, Object> properties = MapBuilder.<String, Object>create("java.file.include", "*.ANY").get();
        return getScanner(properties);
    }

    @Test
    public void validOutput() throws IOException {
        File f = new File(getClassesDirectory(SourceFileScannerIT.class), "/HelloWorld.ANY");
        store.beginTransaction();
        Scanner scanner = getJavaFileScanner();
        ModuleDescriptor descriptor = store.create(ModuleDescriptor.class);
        scanner.getContext().push(ModuleDescriptor.class, descriptor);
        scanner.scan(new StreamSource(f), f.getAbsolutePath(), DefaultScope.NONE);
        scanner.getContext().pop(ModuleDescriptor.class);
        //verifyDocument(descriptor);
        store.commitTransaction();
    }

    @Test
    public void validOutput2() throws IOException {
        File f = new File(getClassesDirectory(SourceFileScannerIT.class), "/HelloWorld.java");
        store.beginTransaction();
        ModuleDescriptor descriptor = getJavaFileScanner().scan(f, f.getAbsolutePath(), JavaScope.SRC);
        store.commitTransaction();
    }
}
