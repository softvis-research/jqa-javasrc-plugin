package org.jqassistant.contrib.plugin.javasrc.test.set.scanner;

import com.buschmais.jqassistant.plugin.common.test.AbstractPluginIT;
import org.jqassistant.contrib.plugin.javasrc.api.model.JavaSourceDirectoryDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.scanner.JavaScope;
import org.jqassistant.contrib.plugin.javasrc.test.set.scanner.argument.SubClass;
import org.junit.Test;

import java.io.File;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.jqassistant.contrib.plugin.javasrc.test.matcher.TypeDescriptorMatcher.typeDescriptor;
import static org.junit.Assert.assertThat;

/**
 * Contains tests to verify correct scanning of methods calls.
 *
 * @author Richard Mueller
 */
public class ArgumentIT extends AbstractPluginIT {

    @Test
    public void testCallArgument() throws NoSuchMethodException {
        final String TEST_DIRECTORY_PATH = "src/test/java/";
        final String FILE_DIRECTORY_PATH = "src/test/java/org/jqassistant/contrib/plugin/javasrc/test/set/scanner/argument/";
        File directory = new File(FILE_DIRECTORY_PATH);
        store.beginTransaction();
        JavaSourceDirectoryDescriptor javaSourceDirectoryDescriptor = getScanner().scan(directory, TEST_DIRECTORY_PATH, JavaScope.SRC);
        TestResult testResult = query("MATCH (caller:Method)-[INVOKES]->(callee:Method) WHERE caller.name='callingMethod' RETURN callee");
        assertThat(query("MATCH (:Method)-[i:INVOKES]->(callee:Method) WHERE callee.name='calledMethodWithArgument' RETURN i.lineNumber as lineNumber")
            .getColumn("lineNumber").get(0), equalTo(7));
        assertThat(
            query("MATCH (dependent:Type)-[:DEPENDS_ON]->(dependency:Type) WHERE dependent.name = 'Caller' RETURN dependency").getColumn("dependency"),
            hasItems(typeDescriptor(String.class), typeDescriptor(SubClass.class)));
        store.commitTransaction();
    }

}
