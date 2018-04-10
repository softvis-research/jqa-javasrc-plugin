package org.jqassistant.contrib.plugin.javasrc.test.set.scanner;

import static org.hamcrest.CoreMatchers.anything;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.jqassistant.contrib.plugin.javasrc.test.matcher.AnnotationValueDescriptorMatcher.annotationValueDescriptor;
import static org.jqassistant.contrib.plugin.javasrc.test.matcher.FieldDescriptorMatcher.fieldDescriptor;
import static org.jqassistant.contrib.plugin.javasrc.test.matcher.MethodDescriptorMatcher.constructorDescriptor;
import static org.jqassistant.contrib.plugin.javasrc.test.matcher.MethodDescriptorMatcher.methodDescriptor;
import static org.jqassistant.contrib.plugin.javasrc.test.matcher.TypeDescriptorMatcher.typeDescriptor;
import static org.jqassistant.contrib.plugin.javasrc.test.matcher.ValueDescriptorMatcher.valueDescriptor;
import static org.jqassistant.contrib.plugin.javasrc.test.set.scanner.annotation.Enumeration.DEFAULT;
import static org.jqassistant.contrib.plugin.javasrc.test.set.scanner.annotation.Enumeration.NON_DEFAULT;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.buschmais.jqassistant.plugin.common.test.AbstractPluginIT;
import org.jqassistant.contrib.plugin.javasrc.api.model.AnnotationValueDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.FieldDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.JavaSourceDirectoryDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.MethodDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.TypeDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.scanner.JavaScope;
import org.jqassistant.contrib.plugin.javasrc.test.set.scanner.annotation.AnnotatedType;
import org.jqassistant.contrib.plugin.javasrc.test.set.scanner.annotation.Annotation;
import org.jqassistant.contrib.plugin.javasrc.test.set.scanner.annotation.AnnotationWithDefaultValue;
import org.jqassistant.contrib.plugin.javasrc.test.set.scanner.annotation.NestedAnnotation;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Contains test to verify correct scanning of annotations and annotated types.
 * 
 * @authors Dirk Mahler, Richard Müller
 */
public class AnnotationIT extends AbstractPluginIT {

    /**
     * Verifies an annotation on class level.
     * 
     * @throws IOException
     *             If the test fails.
     */
    @Test
    public void testAnnotatedClass() throws IOException, NoSuchFieldException {
        // verify annotation type
        final String TEST_DIRECTORY_PATH = "src/test/java/";
        final String FILE_DIRECTORY_PATH = "src/test/java/org/jqassistant/contrib/plugin/javasrc/test/set/scanner/annotation/";
        File directory = new File(FILE_DIRECTORY_PATH);
        store.beginTransaction();
        JavaSourceDirectoryDescriptor javaSourceDirectoryDescriptor = getScanner().scan(directory, TEST_DIRECTORY_PATH, JavaScope.SRC);
        TestResult testResult = query("MATCH (t:Type:Class)-[:ANNOTATED_BY]->(a:Value:Annotation)-[:OF_TYPE]->(at:Type:Annotation) RETURN t, a, at");
        assertThat(testResult.getRows().size(), equalTo(1));
        Map<String, Object> row = testResult.getRows().get(0);
        assertThat((TypeDescriptor) row.get("t"), typeDescriptor(AnnotatedType.class));
        assertThat((AnnotationValueDescriptor) row.get("a"), annotationValueDescriptor(Annotation.class, anything()));
        assertThat((TypeDescriptor) row.get("at"), typeDescriptor(Annotation.class));
        // verify values
        testResult = query("MATCH (t:Type:Class)-[:ANNOTATED_BY]->(a:Value:Annotation)-[:HAS]->(value:Value) RETURN value");
        assertThat(testResult.getRows().size(), equalTo(6));
        List<Object> values = testResult.getColumn("value");
        assertThat(values, hasItem(valueDescriptor("value", is("class"))));
        assertThat(values, hasItem(valueDescriptor("classValue", typeDescriptor(Number.class))));
        assertThat(values, hasItem(valueDescriptor("arrayValue", hasItems(valueDescriptor("[0]", is("a")), (valueDescriptor("[1]", is("b")))))));
        assertThat(values, hasItem(valueDescriptor("enumerationValue", fieldDescriptor(NON_DEFAULT))));
        assertThat(values, hasItem(valueDescriptor("nestedAnnotationValue", hasItem(valueDescriptor("value", is("nestedClass"))))));
        assertThat(values,
                hasItem(valueDescriptor("nestedAnnotationValues", hasItem(valueDescriptor("[0]", hasItem(valueDescriptor("value", is("nestedClasses"))))))));
        store.commitTransaction();
    }

    /**
     * Verifies an annotation on method level.
     *
     * @throws IOException
     *             If the test fails.
     */
    @Test
    public void testAnnotatedMethod() throws IOException, NoSuchFieldException, NoSuchMethodException {
        // verify annotation type on method level
        final String TEST_DIRECTORY_PATH = "src/test/java/";
        final String FILE_DIRECTORY_PATH = "src/test/java/org/jqassistant/contrib/plugin/javasrc/test/set/scanner/annotation/";
        File directory = new File(FILE_DIRECTORY_PATH);
        store.beginTransaction();
        JavaSourceDirectoryDescriptor javaSourceDirectoryDescriptor = getScanner().scan(directory, TEST_DIRECTORY_PATH, JavaScope.SRC);
        TestResult testResult = query("MATCH (m:Method)-[:ANNOTATED_BY]->(a:Value:Annotation)-[:OF_TYPE]->(at:Type:Annotation) RETURN m, a, at");
        assertThat(testResult.getRows().size(), equalTo(1));
        Map<String, Object> row = testResult.getRows().get(0);
        assertThat((MethodDescriptor) row.get("m"), methodDescriptor(AnnotatedType.class, "annotatedMethod", String.class));
        assertThat((AnnotationValueDescriptor) row.get("a"), annotationValueDescriptor(Annotation.class, anything()));
        assertThat((TypeDescriptor) row.get("at"), typeDescriptor(Annotation.class));
        // verify values on method level
        testResult = query("MATCH (m:Method)-[:ANNOTATED_BY]->(a:Value:Annotation)-[:HAS]->(value:Value) RETURN value");
        assertThat(testResult.getRows().size(), equalTo(1));
        List<Object> values = testResult.getColumn("value");
        assertThat(values, hasItem(valueDescriptor("value", is("method"))));
        store.commitTransaction();
    }

    /**
     * Verifies an annotation on method parameter level.
     *
     * @throws IOException
     *             If the test fails.
     */
    @Test
    public void testAnnotatedMethodParameter() throws IOException, NoSuchFieldException, NoSuchMethodException {
        // verify annotation type on method parameter level
        final String TEST_DIRECTORY_PATH = "src/test/java/";
        final String FILE_DIRECTORY_PATH = "src/test/java/org/jqassistant/contrib/plugin/javasrc/test/set/scanner/annotation/";
        File directory = new File(FILE_DIRECTORY_PATH);
        store.beginTransaction();
        JavaSourceDirectoryDescriptor javaSourceDirectoryDescriptor = getScanner().scan(directory, TEST_DIRECTORY_PATH, JavaScope.SRC);
        TestResult testResult = query(
                "MATCH (m:Method)-[:HAS]->(p:Parameter)-[:ANNOTATED_BY]->(a:Value:Annotation)-[:OF_TYPE]->(at:Type:Annotation) WHERE m.name = 'annotatedMethod' RETURN m, a, at");
        assertThat(testResult.getRows().size(), equalTo(1));
        Map<String, Object> row = testResult.getRows().get(0);
        assertThat((MethodDescriptor) row.get("m"), methodDescriptor(AnnotatedType.class, "annotatedMethod", String.class));
        assertThat((AnnotationValueDescriptor) row.get("a"), annotationValueDescriptor(Annotation.class, anything()));
        assertThat((TypeDescriptor) row.get("at"), typeDescriptor(Annotation.class));
        // verify values on method parameter level
        testResult = query(
                "MATCH (m:Method)-[:HAS]->(p:Parameter)-[:ANNOTATED_BY]->(a:Value:Annotation)-[:HAS]->(value:Value) WHERE m.name = 'annotatedMethod' RETURN value");
        assertThat(testResult.getRows().size(), equalTo(1));
        List<Object> values = testResult.getColumn("value");
        assertThat(values, hasItem(valueDescriptor("value", is("parameter"))));
        store.commitTransaction();
    }

    /**
     * Verifies an annotation on constructor parameter level.
     *
     * @throws IOException
     *             If the test fails.
     */
    @Test
    public void testAnnotatedConstructorParameter() throws IOException, NoSuchFieldException, NoSuchMethodException {
        // verify annotation type on method parameter level
        final String TEST_DIRECTORY_PATH = "src/test/java/";
        final String FILE_DIRECTORY_PATH = "src/test/java/org/jqassistant/contrib/plugin/javasrc/test/set/scanner/annotation/";
        File directory = new File(FILE_DIRECTORY_PATH);
        store.beginTransaction();
        JavaSourceDirectoryDescriptor javaSourceDirectoryDescriptor = getScanner().scan(directory, TEST_DIRECTORY_PATH, JavaScope.SRC);
        TestResult testResult = query(
                "MATCH (c:Method:Constructor)-[:HAS]->(p:Parameter)-[:ANNOTATED_BY]->(a:Value:Annotation)-[:OF_TYPE]->(at:Type:Annotation) RETURN c, a, at");
        assertThat(testResult.getRows().size(), equalTo(1));
        Map<String, Object> row = testResult.getRows().get(0);
        assertThat((AnnotationValueDescriptor) row.get("a"), annotationValueDescriptor(Annotation.class, anything()));
        assertThat((TypeDescriptor) row.get("at"), typeDescriptor(Annotation.class));
        // verify values on method parameter level
        testResult = query("MATCH (c:Method:Constructor)-[:HAS]->(p:Parameter)-[:ANNOTATED_BY]->(a:Value:Annotation)-[:HAS]->(value:Value) RETURN value");
        assertThat(testResult.getRows().size(), equalTo(1));
        List<Object> values = testResult.getColumn("value");
        assertThat(values, hasItem(valueDescriptor("value", is("parameter"))));
        store.commitTransaction();
    }

    /**
     * Verifies an annotation on field level.
     *
     * @throws IOException
     *             If the test fails.
     */
    @Test
    public void testAnnotatedField() throws IOException, NoSuchFieldException, NoSuchMethodException {
        // verify annotation type
        final String TEST_DIRECTORY_PATH = "src/test/java/";
        final String FILE_DIRECTORY_PATH = "src/test/java/org/jqassistant/contrib/plugin/javasrc/test/set/scanner/annotation/";
        File directory = new File(FILE_DIRECTORY_PATH);
        store.beginTransaction();
        JavaSourceDirectoryDescriptor javaSourceDirectoryDescriptor = getScanner().scan(directory, TEST_DIRECTORY_PATH, JavaScope.SRC);
        TestResult testResult = query("MATCH (f:Field)-[:ANNOTATED_BY]->(a:Value:Annotation)-[:OF_TYPE]->(at:Type:Annotation) RETURN f, a, at");
        assertThat(testResult.getRows().size(), equalTo(1));
        Map<String, Object> row = testResult.getRows().get(0);
        assertThat((FieldDescriptor) row.get("f"), fieldDescriptor(AnnotatedType.class, "annotatedField"));
        assertThat((AnnotationValueDescriptor) row.get("a"), annotationValueDescriptor(Annotation.class, anything()));
        assertThat((TypeDescriptor) row.get("at"), typeDescriptor(Annotation.class));
        // verify values
        testResult = query("MATCH (f:Field)-[:ANNOTATED_BY]->(a:Value:Annotation)-[:HAS]->(value:Value) RETURN value");
        assertThat(testResult.getRows().size(), equalTo(1));
        List<Object> values = testResult.getColumn("value");
        assertThat(values, hasItem(valueDescriptor("value", is("field"))));
        store.commitTransaction();
    }

    /**
     * Verifies dependencies generated by default values of annotation methods.
     *
     * @throws IOException
     *             If the test fails.
     */
    @Test
    public void testAnnotationDefaultValues() throws IOException, NoSuchFieldException, NoSuchMethodException {
        final String TEST_DIRECTORY_PATH = "src/test/java/";
        final String FILE_DIRECTORY_PATH = "src/test/java/org/jqassistant/contrib/plugin/javasrc/test/set/scanner/annotation/";
        File directory = new File(FILE_DIRECTORY_PATH);
        store.beginTransaction();
        JavaSourceDirectoryDescriptor javaSourceDirectoryDescriptor = getScanner().scan(directory, TEST_DIRECTORY_PATH, JavaScope.SRC);
        assertThat(query("MATCH (t:Type:Annotation) RETURN t").getColumn("t"), hasItem(typeDescriptor(AnnotationWithDefaultValue.class)));
        assertThat(query("MATCH (t:Type:Annotation)-[:DECLARES]->(m:Method)-[:HAS_DEFAULT]->(v:Value) WHERE m.name='classValue' RETURN v").getColumn("v"),
                hasItem(valueDescriptor(null, typeDescriptor(Number.class))));
        assertThat(query("MATCH (t:Type:Annotation)-[:DECLARES]->(m:Method)-[:HAS_DEFAULT]->(v:Value) WHERE m.name='enumerationValue' RETURN v").getColumn("v"),
                hasItem(valueDescriptor(null, fieldDescriptor(DEFAULT))));
        assertThat(query("MATCH (t:Type:Annotation)-[:DECLARES]->(m:Method)-[:HAS_DEFAULT]->(v:Value) WHERE m.name='primitiveValue' RETURN v").getColumn("v"),
                hasItem(valueDescriptor(null, is(0d))));
        assertThat(query("MATCH (t:Type:Annotation)-[:DECLARES]->(m:Method)-[:HAS_DEFAULT]->(v:Value) WHERE m.name='arrayValue' RETURN v").getColumn("v"),
                hasItem(valueDescriptor(null, hasItem(valueDescriptor("[0]", typeDescriptor(Integer.class))))));
        assertThat(query("MATCH (t:Type:Annotation)-[:DECLARES]->(m:Method)-[:HAS_DEFAULT]->(v:Value) WHERE m.name='annotationValue' RETURN v").getColumn("v"),
                hasItem(annotationValueDescriptor(NestedAnnotation.class, hasItem(valueDescriptor("value", is("test"))))));
        store.commitTransaction();
    }

    /**
     * Verifies dependencies generated by default values of annotation methods.
     *
     * @throws IOException
     *             If the test fails.
     */
    @Test
    @Ignore("Scanning the constructor of generic type fails if a parameter annotation is present.")
    public void testAnnotatedInnerClass() throws IOException, NoSuchFieldException, NoSuchMethodException {
        // verify annotation type on method parameter level
        final String TEST_DIRECTORY_PATH = "src/test/java/";
        final String FILE_DIRECTORY_PATH = "src/test/java/org/jqassistant/contrib/plugin/javasrc/test/set/scanner/annotation/";
        File directory = new File(FILE_DIRECTORY_PATH);
        store.beginTransaction();
        JavaSourceDirectoryDescriptor javaSourceDirectoryDescriptor = getScanner().scan(directory, TEST_DIRECTORY_PATH, JavaScope.SRC);
        TestResult testResult = query("MATCH (c:Constructor)-[:HAS]->(:Parameter)-[:ANNOTATED_BY]->()-[:OF_TYPE]->(Type:Annotation) RETURN c");
        assertThat(testResult.getRows().size(), equalTo(1));
        assertThat(testResult.getColumn("c"), hasItem(constructorDescriptor(AnnotatedType.GenericInnerAnnotatedType.class, AnnotatedType.class, Object.class)));
        store.commitTransaction();
    }
}
