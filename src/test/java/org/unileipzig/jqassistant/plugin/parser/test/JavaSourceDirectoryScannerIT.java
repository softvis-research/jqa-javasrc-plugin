package org.unileipzig.jqassistant.plugin.parser.test;

import com.buschmais.jqassistant.plugin.common.api.model.FileDescriptor;
import org.junit.Ignore;
import org.junit.Test;
import org.unileipzig.jqassistant.plugin.parser.api.model.JavaSourceDirectoryDescriptor;
import org.unileipzig.jqassistant.plugin.parser.api.model.JavaSourceFileDescriptor;
import org.unileipzig.jqassistant.plugin.parser.api.model.TypeDescriptor;
import org.unileipzig.jqassistant.plugin.parser.api.scanner.JavaScope;

import java.io.File;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

public class JavaSourceDirectoryScannerIT extends com.buschmais.jqassistant.plugin.common.test.AbstractPluginIT {

    @Test
    @Ignore
    public void scanHelloWorld() {
        File f = new File("src/test/java/samples1");
        store.beginTransaction();
        JavaSourceDirectoryDescriptor dirDescriptor = getScanner().scan(f, "src/test/java", JavaScope.CLASSPATH);
        for (FileDescriptor fileDescriptor : dirDescriptor.getContains()) {
            assertTrue(fileDescriptor instanceof JavaSourceFileDescriptor);
            for (TypeDescriptor typeDescriptor : ((JavaSourceFileDescriptor) fileDescriptor).getTypes()) {
                assertTrue(typeDescriptor.getFullQualifiedName().startsWith("samples1.HelloWorld"));
                //System.out.println(typeDescriptor.getFullQualifiedName() + ": " + typeDescriptor.getDeclaredMembers());
                assertTrue(typeDescriptor.getDeclaredMembers().size() >= 1);
                assertTrue(typeDescriptor.getDeclaredMethods().size() >= 1);
            }
        }
        store.commitTransaction();
    }

    @Test
    @Ignore
    public void scanInnerClasses() {
        File f = new File("src/test/java/samples2");
        store.beginTransaction();
        JavaSourceDirectoryDescriptor dirDescriptor = getScanner().scan(f, "src/test/java", JavaScope.CLASSPATH);
        for (FileDescriptor fileDescriptor : dirDescriptor.getContains()) {
            for (TypeDescriptor typeDescriptor : ((JavaSourceFileDescriptor) fileDescriptor).getTypes()) {
                if (typeDescriptor.getFullQualifiedName().equals("samples2.InnerClasses")) {
                    assertEquals(2, typeDescriptor.getDeclaredInnerClasses().size());
                }
            }
        }
        store.commitTransaction();
    }

}
