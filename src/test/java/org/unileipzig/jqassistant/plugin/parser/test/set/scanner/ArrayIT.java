package org.unileipzig.jqassistant.plugin.parser.test.set.scanner;


import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.unileipzig.jqassistant.plugin.parser.test.matcher.FieldDescriptorMatcher.fieldDescriptor;
import static org.unileipzig.jqassistant.plugin.parser.test.matcher.MethodDescriptorMatcher.methodDescriptor;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.unileipzig.jqassistant.plugin.parser.api.model.JavaSourceDirectoryDescriptor;
import org.unileipzig.jqassistant.plugin.parser.api.scanner.JavaScope;
import org.unileipzig.jqassistant.plugin.parser.test.set.scanner.array.Array;

import com.buschmais.jqassistant.plugin.common.test.AbstractPluginIT;


public class ArrayIT extends AbstractPluginIT {

    @Test
    public void testFieldType() throws IOException, NoSuchFieldException, NoSuchMethodException {
    	final String TEST_DIRECTORY_PATH = "src/test/java/";
		final String FILE_DIRECTORY_PATH = "src/test/java/org/unileipzig/jqassistant/plugin/parser/test/set/scanner/array/";
		File directory = new File(FILE_DIRECTORY_PATH);
		store.beginTransaction();
		JavaSourceDirectoryDescriptor javaSourceDirectoryDescriptor = getScanner().scan(directory, TEST_DIRECTORY_PATH, JavaScope.CLASSPATH);
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("className", Array.class.getName());
        TestResult testResult = query("MATCH (t:Type)-[:DECLARES]->(f:Field) WHERE t.fqn={className} RETURN f", parameters);
        assertThat(testResult.getColumn("f"), hasItem(fieldDescriptor(Array.class, "stringArray")));
        store.commitTransaction();
    }
    
    @Test
	public void testMethodParameterType() throws IOException, NoSuchFieldException, NoSuchMethodException {
		final String TEST_DIRECTORY_PATH = "src/test/java/";
		final String FILE_DIRECTORY_PATH = "src/test/java/org/unileipzig/jqassistant/plugin/parser/test/set/scanner/array/";
		File directory = new File(FILE_DIRECTORY_PATH);
		store.beginTransaction();
		JavaSourceDirectoryDescriptor javaSourceDirectoryDescriptor = getScanner().scan(directory, TEST_DIRECTORY_PATH, JavaScope.CLASSPATH);
	    Map<String, Object> parameters = new HashMap<String, Object>();
	    parameters.put("className", Array.class.getName());
	    TestResult testResult = query("MATCH (t:Type)-[:DECLARES]->(m:Method) WHERE t.fqn={className} AND m.name='setStringArray' RETURN m.signature", parameters);
	    assertEquals(testResult.getColumn("m.signature").get(0), "void setStringArray(java.lang.String[])");
	    store.commitTransaction();
	}

	@Test
    public void testMethodReturnType() throws IOException, NoSuchFieldException, NoSuchMethodException {
    	final String TEST_DIRECTORY_PATH = "src/test/java/";
		final String FILE_DIRECTORY_PATH = "src/test/java/org/unileipzig/jqassistant/plugin/parser/test/set/scanner/array/";
		File directory = new File(FILE_DIRECTORY_PATH);
		store.beginTransaction();
		JavaSourceDirectoryDescriptor javaSourceDirectoryDescriptor = getScanner().scan(directory, TEST_DIRECTORY_PATH, JavaScope.CLASSPATH);
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("className", Array.class.getName());
        TestResult testResult = query("MATCH (t:Type)-[:DECLARES]->(m:Method) WHERE t.fqn={className} AND m.name='getStringArray' RETURN m", parameters);
        assertThat(testResult.getColumn("m"), hasItem(methodDescriptor(Array.class, "getStringArray")));
        store.commitTransaction();
    }
}
