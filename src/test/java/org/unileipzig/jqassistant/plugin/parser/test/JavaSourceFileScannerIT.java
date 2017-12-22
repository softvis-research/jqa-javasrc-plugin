package org.unileipzig.jqassistant.plugin.parser.test;

import com.buschmais.jqassistant.plugin.common.api.model.FileDescriptor;
import org.junit.Ignore;
import org.junit.Test;
import org.unileipzig.jqassistant.plugin.parser.api.model.ClassTypeDescriptor;
import org.unileipzig.jqassistant.plugin.parser.api.model.JavaSourceDirectoryDescriptor;
import org.unileipzig.jqassistant.plugin.parser.api.model.JavaSourceFileDescriptor;
import org.unileipzig.jqassistant.plugin.parser.api.model.MethodDescriptor;
import org.unileipzig.jqassistant.plugin.parser.api.scanner.JavaScope;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
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
    @Ignore
    public void scanConstructors() {
        scanFileHelper("src/test/java/samples3/ConstructorExample.java", (fileDescriptor) -> {
            fileDescriptor.getTypes().forEach((type) -> {
                //System.out.println("scanned " + fileDescriptor.getFileName() + " " + type);
                assertEquals("ConstructorExample", type.getName());
            });
        });
    }

    @Test
    public void scanMethodCalls() {
        scanFileHelper("src/test/java/samples3/MethodCallExample.java", (fileDescriptor) -> {
            fileDescriptor.getTypes().forEach((type) -> {
                assertEquals("MethodCallExample", type.getName());
                for (Object o : type.getDeclaredMethods()) {
                    if (o instanceof MethodDescriptor) {
                        MethodDescriptor method = (MethodDescriptor) o;
                        //System.out.println(method.getSignature()); // why is this redundantly in type.getDeclaredMethods()?
                        //   -> solved: the redundancy came from adding methods and fields (also) to the List of memberDescriptors
                        Set<String> signaturesOfCalledMethods = new HashSet<>();
                        Set<String> expectedSignaturesOfCalledMethods = new HashSet<>();
                        expectedSignaturesOfCalledMethods.add("calledMethod0()");
                        expectedSignaturesOfCalledMethods.add("calledMethod1()");
                        expectedSignaturesOfCalledMethods.add("calledMethod2()");
                        if (method.getName().equals("callingMethod")) {
                            method.getInvokes().forEach(invoke -> {
                                System.out.println(method.getSignature() + " calls " + invoke.getInvokedMethod().getSignature());
                                signaturesOfCalledMethods.add(invoke.getInvokedMethod().getSignature());
                            });
                            assertEquals(expectedSignaturesOfCalledMethods, signaturesOfCalledMethods);
                        }
                    } else if (o instanceof ClassTypeDescriptor) {
                        System.out.println("How did this get here: " + ((ClassTypeDescriptor) o).getFullQualifiedName());
                    } else {
                        throw new RuntimeException("...");
                    }
                }
                ;
            });
        });
    }

}
