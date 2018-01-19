package org.unileipzig.jqassistant.plugin.parser.test.set.scanner;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.assertThat;
import static org.unileipzig.jqassistant.plugin.parser.test.matcher.MethodDescriptorMatcher.methodDescriptor;
import static org.unileipzig.jqassistant.plugin.parser.test.matcher.TypeDescriptorMatcher.typeDescriptor;

import java.io.File;

import org.junit.Test;
import org.unileipzig.jqassistant.plugin.parser.api.model.JavaSourceDirectoryDescriptor;
import org.unileipzig.jqassistant.plugin.parser.api.scanner.JavaScope;
import org.unileipzig.jqassistant.plugin.parser.test.set.scanner.method.Method;
import org.unileipzig.jqassistant.plugin.parser.test.set.scanner.visibility.Visibility;

import com.buschmais.jqassistant.plugin.common.test.AbstractPluginIT;

/**
 * Contains test to verify correct scanning of methods.
 * 
 * @author Richard Müller
 *
 */
public class MethodIT extends AbstractPluginIT {

	@Test
	public void testReturnType() throws NoSuchMethodException {
		final String TEST_DIRECTORY_PATH = "src/test/java/";
		final String FILE_DIRECTORY_PATH = "src/test/java/org/unileipzig/jqassistant/plugin/parser/test/set/scanner/method/";
		File directory = new File(FILE_DIRECTORY_PATH);
		store.beginTransaction();
		JavaSourceDirectoryDescriptor javaSourceDirectoryDescriptor = getScanner().scan(directory, TEST_DIRECTORY_PATH, JavaScope.CLASSPATH);
		assertThat(query("MATCH (m:Method) WHERE m.signature='int returningPrimitiveType()' RETURN m").getColumn("m"), hasItem(methodDescriptor(Method.class, "returningPrimitiveType")));
		assertThat(query("MATCH (m:Method) WHERE m.signature='void returningVoid()' RETURN m").getColumn("m"), hasItem(methodDescriptor(Method.class, "returningVoid")));
		assertThat(query("MATCH (m:Method) WHERE m.signature='java.lang.String returningType()' RETURN m").getColumn("m"), hasItem(methodDescriptor(Method.class, "returningType")));
		store.commitTransaction();
	}

	@Test
	public void testParameter() throws NoSuchMethodException {
		final String TEST_DIRECTORY_PATH = "src/test/java/";
		final String FILE_DIRECTORY_PATH = "src/test/java/org/unileipzig/jqassistant/plugin/parser/test/set/scanner/method/";
		File directory = new File(FILE_DIRECTORY_PATH);
		store.beginTransaction();
		JavaSourceDirectoryDescriptor javaSourceDirectoryDescriptor = getScanner().scan(directory, TEST_DIRECTORY_PATH, JavaScope.CLASSPATH);
//		assertThat(query("MATCH (m:Method) WHERE m.signature='int returningPrimitiveType()' RETURN m").getColumn("m"), hasItem(methodDescriptor(Method.class, "returningPrimitiveType")));
//		assertThat(query("MATCH (m:Method) WHERE m.signature='void returningVoid()' RETURN m").getColumn("m"), hasItem(methodDescriptor(Method.class, "returningVoid")));
//		assertThat(query("MATCH (m:Method) WHERE m.signature='java.lang.String returningType()' RETURN m").getColumn("m"), hasItem(methodDescriptor(Method.class, "returningType")));
		store.commitTransaction();
	}
}
