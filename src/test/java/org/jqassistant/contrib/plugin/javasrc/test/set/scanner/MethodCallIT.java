package org.jqassistant.contrib.plugin.javasrc.test.set.scanner;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.jqassistant.contrib.plugin.javasrc.test.matcher.MethodDescriptorMatcher.methodDescriptor;
import static org.junit.Assert.assertThat;

import java.io.File;

import com.buschmais.jqassistant.plugin.common.test.AbstractPluginIT;
import org.jqassistant.contrib.plugin.javasrc.api.model.JavaSourceDirectoryDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.scanner.JavaScope;
import org.jqassistant.contrib.plugin.javasrc.test.set.scanner.call.Callee;
import org.jqassistant.contrib.plugin.javasrc.test.set.scanner.call.Caller;
import org.junit.Test;

/**
 * Contains tests to verify correct scanning of methods calls.
 * 
 * @author Richard Müller
 *
 */
public class MethodCallIT extends AbstractPluginIT {

    @Test
    public void testCallsFromMethod() throws NoSuchMethodException {
        final String TEST_DIRECTORY_PATH = "src/test/java/";
        final String FILE_DIRECTORY_PATH = "src/test/java/org/jqassistant/contrib/plugin/javasrc/test/set/scanner/call/";
        File directory = new File(FILE_DIRECTORY_PATH);
        store.beginTransaction();
        JavaSourceDirectoryDescriptor javaSourceDirectoryDescriptor = getScanner().scan(directory, TEST_DIRECTORY_PATH, JavaScope.CLASSPATH);
        TestResult testResult = query("MATCH (caller:Method)-[INVOKES]->(callee:Method) WHERE caller.name='callingMethod' RETURN callee");
        // verify methods
        assertThat(testResult.getColumn("callee").size(), equalTo(3));
        assertThat(testResult.getColumn("callee"), hasItems(methodDescriptor(Caller.class, "calledMethod0"), methodDescriptor(Caller.class, "calledMethod1"),
                methodDescriptor(Callee.class, "calledMethod2")));
        // verify line numbers
        assertThat(query("MATCH (:Method)-[i:INVOKES]->(callee:Method) WHERE callee.name='calledMethod0' RETURN i.lineNumber as lineNumber")
                .getColumn("lineNumber").get(0), equalTo(10));
        assertThat(query("MATCH (:Method)-[i:INVOKES]->(callee:Method) WHERE callee.name='calledMethod1' RETURN i.lineNumber as lineNumber")
                .getColumn("lineNumber").get(0), equalTo(11));
        assertThat(query("MATCH (:Method)-[i:INVOKES]->(callee:Method) WHERE callee.name='calledMethod2' RETURN i.lineNumber as lineNumber")
                .getColumn("lineNumber").get(0), equalTo(13));
        store.commitTransaction();
    }

    @Test
    public void testCallsFromConstructor() throws NoSuchMethodException {
        final String TEST_DIRECTORY_PATH = "src/test/java/";
        final String FILE_DIRECTORY_PATH = "src/test/java/org/jqassistant/contrib/plugin/javasrc/test/set/scanner/call/";
        File directory = new File(FILE_DIRECTORY_PATH);
        store.beginTransaction();
        JavaSourceDirectoryDescriptor javaSourceDirectoryDescriptor = getScanner().scan(directory, TEST_DIRECTORY_PATH, JavaScope.CLASSPATH);
        TestResult testResult = query("MATCH (caller:Method:Constructor)-[INVOKES]->(callee:Method) RETURN callee");
        // verify methods
        assertThat(testResult.getColumn("callee").size(), equalTo(1));
        assertThat(testResult.getColumn("callee"), hasItem(methodDescriptor(Caller.class, "calledMethod3")));
        // verify line numbers
        assertThat(query("MATCH (:Method)-[i:INVOKES]->(callee:Method) WHERE callee.name='calledMethod0' RETURN i.lineNumber as lineNumber")
                .getColumn("lineNumber").get(0), equalTo(10));
        store.commitTransaction();
    }

}
