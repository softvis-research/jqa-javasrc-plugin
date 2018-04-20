package org.jqassistant.contrib.plugin.javasrc.test.set.scanner;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.jqassistant.contrib.plugin.javasrc.test.matcher.MethodDescriptorMatcher.methodDescriptor;
import static org.jqassistant.contrib.plugin.javasrc.test.matcher.TypeDescriptorMatcher.typeDescriptor;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.Map;

import com.buschmais.jqassistant.plugin.common.test.AbstractPluginIT;
import org.jqassistant.contrib.plugin.javasrc.api.model.JavaSourceDirectoryDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.MethodDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.TypeDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.scanner.JavaScope;
import org.jqassistant.contrib.plugin.javasrc.test.set.scanner.innerclass.AnonymousInnerClass;
import org.junit.Test;

/**
 * Contains tests on relations between outer and inner classes.
 * 
 * @authors Dirk Mahler, Richard Mueller
 */
public class AnonymousInnerClassIT extends AbstractPluginIT {

    private static final String INNERCLASS_NAME = AnonymousInnerClass.class.getName() + "$1";

    @Test
    public void testOuterClassContainsInnerClass() throws NoSuchMethodException {
        final String TEST_DIRECTORY_PATH = "src/test/java/";
        final String FILE_DIRECTORY_PATH = "src/test/java/org/jqassistant/contrib/plugin/javasrc/test/set/scanner/innerclass/";
        File directory = new File(FILE_DIRECTORY_PATH);
        store.beginTransaction();
        JavaSourceDirectoryDescriptor javaSourceDirectoryDescriptor = getScanner().scan(directory, TEST_DIRECTORY_PATH, JavaScope.SRC);
        TestResult testResult = query(
                "MATCH (outerClass:Class)-[:DECLARES]->(innerClass:Type)<-[:DECLARES]-(method:Method)<-[:DECLARES]-(outerClass) WHERE outerClass.name='AnonymousInnerClass' RETURN outerClass, innerClass, method");
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

    @Test
    public void testAnonymousClassField() throws NoSuchMethodException {
        final String TEST_DIRECTORY_PATH = "src/test/java/";
        final String FILE_DIRECTORY_PATH = "src/test/java/org/jqassistant/contrib/plugin/javasrc/test/set/scanner/innerclass/";
        File directory = new File(FILE_DIRECTORY_PATH);
        store.beginTransaction();
        JavaSourceDirectoryDescriptor javaSourceDirectoryDescriptor = getScanner().scan(directory, TEST_DIRECTORY_PATH, JavaScope.SRC);
        TestResult testResult = query(
                "MATCH (anonymousClass:Class)-[:DECLARES]->(field:Field) WHERE anonymousClass.fqn='" + INNERCLASS_NAME + "' RETURN field");
        assertThat(testResult.getRows().size(), equalTo(1));
        store.commitTransaction();
    }

    @Test
    public void testAnonymousClassMethod() throws NoSuchMethodException {
        final String TEST_DIRECTORY_PATH = "src/test/java/";
        final String FILE_DIRECTORY_PATH = "src/test/java/org/jqassistant/contrib/plugin/javasrc/test/set/scanner/innerclass/";
        File directory = new File(FILE_DIRECTORY_PATH);
        store.beginTransaction();
        JavaSourceDirectoryDescriptor javaSourceDirectoryDescriptor = getScanner().scan(directory, TEST_DIRECTORY_PATH, JavaScope.SRC);
        TestResult testResult = query(
                "MATCH (anonymousClass:Class)-[:DECLARES]->(method:Method) WHERE anonymousClass.fqn='" + INNERCLASS_NAME + "' RETURN method");
        assertThat(testResult.getRows().size(), equalTo(3));
        store.commitTransaction();
    }

    @Test
    public void testTwoAnonymousClasses() throws NoSuchMethodException {
        final String TEST_DIRECTORY_PATH = "src/test/java/";
        final String FILE_DIRECTORY_PATH = "src/test/java/org/jqassistant/contrib/plugin/javasrc/test/set/scanner/innerclass/";
        File directory = new File(FILE_DIRECTORY_PATH);
        store.beginTransaction();
        JavaSourceDirectoryDescriptor javaSourceDirectoryDescriptor = getScanner().scan(directory, TEST_DIRECTORY_PATH, JavaScope.SRC);
        TestResult testResult = query(
                "MATCH (outerClass:Class)-[:DECLARES]->(method:Method)-[:DECLARES]->(anonymousClass:Class) WHERE outerClass.name='AnonymousInnerClasses'  RETURN anonymousClass");
        assertThat(testResult.getRows().size(), equalTo(2));
        store.commitTransaction();
    }
}
