package org.jqassistant.contrib.plugin.javasrc.test.set.scanner;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.jqassistant.contrib.plugin.javasrc.test.matcher.MethodDescriptorMatcher.methodDescriptor;
import static org.jqassistant.contrib.plugin.javasrc.test.matcher.TypeDescriptorMatcher.typeDescriptor;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.Map;

import com.buschmais.jqassistant.plugin.common.test.AbstractPluginIT;
import org.jqassistant.contrib.plugin.javasrc.api.model.MethodDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.TypeDescriptor;
import org.jqassistant.contrib.plugin.javasrc.test.set.scanner.innerclass.AnonymousInnerClass;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Contains test on relations between outer and inner classes.
 * 
 * authors Dirk Mahler, Richard Müller
 */
public class AnonymousInnerClassIT extends AbstractPluginIT {

    private static final String INNERCLASS_NAME = AnonymousInnerClass.class.getName() + "$1";

    /**
     * Asserts that the outer class can be fetched and contains a relation to
     * the inner class.
     */
    @Test
    @Ignore
    public void assertOuterClassContainsInnerClass() throws NoSuchMethodException {
        // TODO get this test working
        final String TEST_DIRECTORY_PATH = "src/test/java/";
        final String FILE_DIRECTORY_PATH = "src/test/java/org/jqassistant/contrib/plugin/javasrc/test/set/scanner/innerclass/";
        File directory = new File(FILE_DIRECTORY_PATH);
        store.beginTransaction();
        TestResult testResult = query(
                "MATCH (outerClass:Type)-[:DECLARES]->(innerClass:Type)<-[:DECLARES]-(method:Method)<-[:DECLARES]-(outerClass) RETURN outerClass, innerClass, method");
        assertThat(testResult.getRows().size(), equalTo(1));
        Map<String, Object> row = testResult.getRows().get(0);
        TypeDescriptor outerClass = (TypeDescriptor) row.get("outerClass");
        assertThat(outerClass, typeDescriptor(AnonymousInnerClass.class));
        TypeDescriptor innerClass = (TypeDescriptor) row.get("innerClass");
        assertThat(innerClass, typeDescriptor(INNERCLASS_NAME));
        MethodDescriptor method = (MethodDescriptor) row.get("method");
        assertThat(method, methodDescriptor(AnonymousInnerClass.class, "iterator"));
        store.commitTransaction();
    }

}
