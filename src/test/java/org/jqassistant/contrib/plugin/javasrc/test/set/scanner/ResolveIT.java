/**
 * 
 */
package org.jqassistant.contrib.plugin.javasrc.test.set.scanner;

import static org.hamcrest.CoreMatchers.anything;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.jqassistant.contrib.plugin.javasrc.test.matcher.AnnotationValueDescriptorMatcher.annotationValueDescriptor;
import static org.jqassistant.contrib.plugin.javasrc.test.matcher.FieldDescriptorMatcher.fieldDescriptor;
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
import org.jqassistant.contrib.plugin.javasrc.test.set.scanner.annotation.Enumeration;
import org.jqassistant.contrib.plugin.javasrc.test.set.scanner.resolve.Annotation;
import org.jqassistant.contrib.plugin.javasrc.test.set.scanner.resolve.ExternalEnum;
import org.jqassistant.contrib.plugin.javasrc.test.set.scanner.resolve.GenericType;
import org.junit.Test;

/**
 * @author Richard Müller
 *
 */
public class ResolveIT extends AbstractPluginIT {

    @Test
    public void testResolveExternalEnum() throws IOException, NoSuchFieldException, NoSuchMethodException {
        final String TEST_DIRECTORY_PATH = "src/test/java/";
        final String FILE_DIRECTORY_PATH = "src/test/java/org/jqassistant/contrib/plugin/javasrc/test/set/scanner/resolve/";
        File directory = new File(FILE_DIRECTORY_PATH);
        store.beginTransaction();
        JavaSourceDirectoryDescriptor javaSourceDirectoryDescriptor = getScanner().scan(directory, TEST_DIRECTORY_PATH, JavaScope.SRC);
        TestResult testResult = query("MATCH (f:Field)-[:ANNOTATED_BY]->(a:Value:Annotation)-[:OF_TYPE]->(at:Type:Annotation) RETURN f, a, at");
        assertThat(testResult.getRows().size(), equalTo(1));
        Map<String, Object> row = testResult.getRows().get(0);
        assertThat((FieldDescriptor) row.get("f"), fieldDescriptor(ExternalEnum.class, "id"));
        assertThat((AnnotationValueDescriptor) row.get("a"), annotationValueDescriptor(Annotation.class, anything()));
        assertThat((TypeDescriptor) row.get("at"), typeDescriptor(Annotation.class));
        // verify values
        testResult = query("MATCH (f:Field)-[:ANNOTATED_BY]->(a:Value:Annotation)-[:HAS]->(value:Value) RETURN value");
        assertThat(testResult.getRows().size(), equalTo(1));
        List<Object> values = testResult.getColumn("value");
        assertThat(values, hasItem(valueDescriptor("enumerationValue", fieldDescriptor(Enumeration.NON_DEFAULT))));
        store.commitTransaction();
    }

    @Test
    public void testResolveWildcardBounded() throws IOException, NoSuchFieldException, NoSuchMethodException {
        final String TEST_DIRECTORY_PATH = "src/test/java/";
        final String FILE_DIRECTORY_PATH = "src/test/java/org/jqassistant/contrib/plugin/javasrc/test/set/scanner/resolve/";
        File directory = new File(FILE_DIRECTORY_PATH);
        store.beginTransaction();
        JavaSourceDirectoryDescriptor javaSourceDirectoryDescriptor = getScanner().scan(directory, TEST_DIRECTORY_PATH, JavaScope.SRC);
        TestResult testResult = query("MATCH (t:Type)-[:DEPENDS_ON]->(dependents:Type) WHERE t.name = 'WildcardParameterBounded' RETURN dependents");
        List<Object> dependents = testResult.getColumn("dependents");
        assertThat(dependents.size(), equalTo(3));
        assertThat(dependents, hasItem(typeDescriptor(String.class)));
        assertThat(dependents, hasItem(typeDescriptor(GenericType.class)));
        assertThat(dependents, hasItem(typeDescriptor(void.class)));
        store.commitTransaction();
    }

    @Test
    public void testResolveWildcardUnBounded() throws IOException, NoSuchFieldException, NoSuchMethodException {
        final String TEST_DIRECTORY_PATH = "src/test/java/";
        final String FILE_DIRECTORY_PATH = "src/test/java/org/jqassistant/contrib/plugin/javasrc/test/set/scanner/resolve/";
        File directory = new File(FILE_DIRECTORY_PATH);
        store.beginTransaction();
        JavaSourceDirectoryDescriptor javaSourceDirectoryDescriptor = getScanner().scan(directory, TEST_DIRECTORY_PATH, JavaScope.SRC);
        TestResult testResult = query("MATCH (t:Type)-[:DEPENDS_ON]->(dependents:Type) WHERE t.name = 'WildcardParameterUnbounded' RETURN dependents");
        List<Object> dependents = testResult.getColumn("dependents");
        assertThat(dependents.size(), equalTo(3));
        assertThat(dependents, hasItem(typeDescriptor(GenericType.class)));
        assertThat(dependents, hasItem(typeDescriptor(void.class)));
        store.commitTransaction();
    }
}