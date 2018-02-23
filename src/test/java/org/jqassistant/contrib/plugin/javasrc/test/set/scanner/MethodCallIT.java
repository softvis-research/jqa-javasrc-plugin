package org.jqassistant.contrib.plugin.javasrc.test.set.scanner;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import com.buschmais.jqassistant.plugin.common.test.AbstractPluginIT;
import org.jqassistant.contrib.plugin.javasrc.api.model.JavaSourceDirectoryDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.JavaSourceFileDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.MethodDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.scanner.JavaScope;
import org.junit.Test;

/**
 * Contains tests to verify correct scanning of methods calls.
 * 
 * @author Cornelius Wilhelm, Richard Müller
 *
 */
public class MethodCallIT extends AbstractPluginIT {

    @Test
    public void testMethodCall() throws NoSuchMethodException {
        // TODO refactor test
        final String TEST_DIRECTORY_PATH = "src/test/java/";
        final String FILE_DIRECTORY_PATH = "src/test/java/org/jqassistant/contrib/plugin/javasrc/test/set/scanner/invoke/";
        File directory = new File(FILE_DIRECTORY_PATH);
        store.beginTransaction();
        JavaSourceDirectoryDescriptor javaSourceDirectoryDescriptor = getScanner().scan(directory, TEST_DIRECTORY_PATH, JavaScope.CLASSPATH);
        JavaSourceFileDescriptor fileDescriptor = (JavaSourceFileDescriptor) javaSourceDirectoryDescriptor.getContains().get(0);
        fileDescriptor.getTypes().forEach((type) -> {
            // assertEquals("MethodCall", type.getName());
            for (Object o : type.getDeclaredMethods()) {
                if (o instanceof MethodDescriptor) {
                    MethodDescriptor method = (MethodDescriptor) o;
                    // System.out.println(method.getSignature()); // why is this
                    // redundantly in type.getDeclaredMethods()?
                    // -> solved: the redundancy came from adding methods and
                    // fields (also) to the List of memberDescriptors
                    // assertEquals("MethodCall",
                    // method.getDeclaringType().getName());
                    Set<String> signaturesOfCalledMethods = new HashSet<>();
                    Set<String> expectedSignaturesOfCalledMethods = new HashSet<>();
                    expectedSignaturesOfCalledMethods.add("void calledMethod0()");
                    expectedSignaturesOfCalledMethods.add("void calledMethod1()");
                    expectedSignaturesOfCalledMethods.add("void calledMethod2()");
                    if (method.getName().equals("callingMethod")) {
                        method.getInvokes().forEach(invoke -> {
                            signaturesOfCalledMethods.add(invoke.getInvokedMethod().getSignature());
                        });
                        assertEquals(expectedSignaturesOfCalledMethods, signaturesOfCalledMethods);
                    }
                }
            }
        });
    }

}
