/**
 * 
 */
package org.jqassistant.contrib.plugin.javasrc.test.set.scanner;

import static org.hamcrest.CoreMatchers.anything;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.jqassistant.contrib.plugin.javasrc.test.matcher.AnnotationValueDescriptorMatcher.annotationValueDescriptor;
import static org.jqassistant.contrib.plugin.javasrc.test.matcher.FieldDescriptorMatcher.fieldDescriptor;
import static org.jqassistant.contrib.plugin.javasrc.test.matcher.MethodDescriptorMatcher.methodDescriptor;
import static org.jqassistant.contrib.plugin.javasrc.test.matcher.TypeDescriptorMatcher.typeDescriptor;
import static org.jqassistant.contrib.plugin.javasrc.test.matcher.ValueDescriptorMatcher.valueDescriptor;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.buschmais.jqassistant.plugin.common.test.AbstractPluginIT;
import org.jqassistant.contrib.plugin.javasrc.api.model.AnnotationValueDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.FieldDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.JavaSourceDirectoryDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.TypeDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.scanner.JavaScope;
import org.jqassistant.contrib.plugin.javasrc.impl.scanner.ExternalClass;
import org.jqassistant.contrib.plugin.javasrc.impl.scanner.ExternalEnumeration;
import org.jqassistant.contrib.plugin.javasrc.test.set.scanner.resolve.Annotation;
import org.jqassistant.contrib.plugin.javasrc.test.set.scanner.resolve.ResolveExternalEnumeration;
import org.jqassistant.contrib.plugin.javasrc.test.set.scanner.resolve.ResolveMethodWithExternalParameter;
import org.junit.Test;

/**
 * @author Richard Mueller
 *
 */
public class ResolveIT extends AbstractPluginIT {
    // TODO implement tests for external annotation, this, 2 x fieldaccess
    @Test
    public void testResolveExternalEnum() throws IOException, NoSuchFieldException, NoSuchMethodException {
        final String TEST_DIRECTORY_PATH = "src/test/java";
        final String FILE_DIRECTORY_PATH = "src/test/java/org/jqassistant/contrib/plugin/javasrc/test/set/scanner/resolve/";
        File directory = new File(FILE_DIRECTORY_PATH);
        store.beginTransaction();
        JavaSourceDirectoryDescriptor javaSourceDirectoryDescriptor = getScanner().scan(directory, TEST_DIRECTORY_PATH, JavaScope.SRC);
        TestResult testResult = query("MATCH (f:Field)-[:ANNOTATED_BY]->(a:Value:Annotation)-[:OF_TYPE]->(at:Type:Annotation) RETURN f, a, at");
        assertThat(testResult.getRows().size(), equalTo(1));
        Map<String, Object> row = testResult.getRows().get(0);
        assertThat((FieldDescriptor) row.get("f"), fieldDescriptor(ResolveExternalEnumeration.class, "id"));
        assertThat((AnnotationValueDescriptor) row.get("a"), annotationValueDescriptor(Annotation.class, anything()));
        assertThat((TypeDescriptor) row.get("at"), typeDescriptor(Annotation.class));
        // verify values
        testResult = query("MATCH (f:Field)-[:ANNOTATED_BY]->(a:Value:Annotation)-[:HAS]->(value:Value) RETURN value");
        assertThat(testResult.getRows().size(), equalTo(1));
        List<Object> values = testResult.getColumn("value");
        assertThat(values, hasItem(valueDescriptor("enumerationValue", fieldDescriptor(ExternalEnumeration.NON_DEFAULT))));
        store.commitTransaction();
    }

    @Test
    public void testResolveExternalStaticMethodCall() throws NoSuchMethodException {
        final String TEST_DIRECTORY_PATH = "src/test/java";
        final String FILE_DIRECTORY_PATH = "src/test/java/org/jqassistant/contrib/plugin/javasrc/test/set/scanner/resolve/";
        File directory = new File(FILE_DIRECTORY_PATH);
        store.beginTransaction();
        JavaSourceDirectoryDescriptor javaSourceDirectoryDescriptor = getScanner().scan(directory, TEST_DIRECTORY_PATH, JavaScope.SRC);
        TestResult testResult = query("MATCH (caller:Method)-[INVOKES]->(callee:Method) WHERE caller.name='callExternalStaticMethod' RETURN callee");
        // verify methods
        assertThat(testResult.getColumn("callee").size(), equalTo(1));
        assertThat(testResult.getColumn("callee"), hasItem(methodDescriptor(ExternalClass.class, "externalStaticMethod", String.class)));
    }

    @Test
    public void testResolveMethodWithExternalParameter() throws NoSuchMethodException {
        final String TEST_DIRECTORY_PATH = "src/test/java";
        final String FILE_DIRECTORY_PATH = "src/test/java/org/jqassistant/contrib/plugin/javasrc/test/set/scanner/resolve/";
        File directory = new File(FILE_DIRECTORY_PATH);
        store.beginTransaction();
        JavaSourceDirectoryDescriptor javaSourceDirectoryDescriptor = getScanner().scan(directory, TEST_DIRECTORY_PATH, JavaScope.SRC);
        assertThat(query("MATCH (m:Method) WHERE m.name='methodWithExternalParamter' RETURN m").getColumn("m"),
                hasItem(methodDescriptor(ResolveMethodWithExternalParameter.class, "methodWithExternalParamter", ExternalEnumeration.class)));
    }
}