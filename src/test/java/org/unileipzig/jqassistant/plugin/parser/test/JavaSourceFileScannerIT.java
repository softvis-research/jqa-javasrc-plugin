package org.unileipzig.jqassistant.plugin.parser.test;

import com.buschmais.jqassistant.plugin.common.api.model.FileDescriptor;
import org.junit.Test;
import org.unileipzig.jqassistant.plugin.parser.api.model.JavaSourceDirectoryDescriptor;
import org.unileipzig.jqassistant.plugin.parser.api.model.JavaSourceFileDescriptor;
import org.unileipzig.jqassistant.plugin.parser.api.scanner.JavaScope;

import java.io.File;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;

public class JavaSourceFileScannerIT extends com.buschmais.jqassistant.plugin.common.test.AbstractPluginIT {
    private void scanFileHelper(String path, Consumer<JavaSourceFileDescriptor> then) {
        File folder = new File(path).getParentFile(), parent = folder.getParentFile();
        store.beginTransaction();
        JavaSourceDirectoryDescriptor dirDescriptor = getScanner().scan(folder, parent.getPath(), JavaScope.CLASSPATH);
        for (FileDescriptor fileDescriptor : dirDescriptor.getContains()) {
            if (path.contains(fileDescriptor.getFileName()))
                then.accept((JavaSourceFileDescriptor) fileDescriptor);
        }
        store.commitTransaction();
    }

    @Test
    public void scanConstructors() {
        scanFileHelper("src/test/java/samples3/ConstructorExample.java", (fileDescriptor) -> {
            fileDescriptor.getTypes().forEach((type) -> {
                //System.out.println("scanned " + fileDescriptor.getFileName() + " " + type);
                assertEquals("ConstructorExample", type.getName());
            });
        });
    }

}
