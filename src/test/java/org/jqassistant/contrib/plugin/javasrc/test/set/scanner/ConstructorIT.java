package org.jqassistant.contrib.plugin.javasrc.test.set.scanner;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.jqassistant.contrib.plugin.javasrc.test.matcher.MethodDescriptorMatcher.constructorDescriptor;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;

import org.jqassistant.contrib.plugin.javasrc.api.model.JavaSourceDirectoryDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.scanner.JavaScope;
import org.jqassistant.contrib.plugin.javasrc.test.set.scanner.constructor.OverloadedConstructor;
import org.junit.Test;

import com.buschmais.jqassistant.plugin.common.test.AbstractPluginIT;

/**
 * Contains test which verify correct scanning of constructors.
 */
public class ConstructorIT extends AbstractPluginIT {

    @Test
    public void testOverloadedConstructor() throws IOException, NoSuchMethodException {
       	final String TEST_DIRECTORY_PATH = "src/test/java/";
		final String FILE_DIRECTORY_PATH = "src/test/java/org/jqassistant/contrib/plugin/javasrc/test/set/scanner/constructor/";
		File directory = new File(FILE_DIRECTORY_PATH);
		store.beginTransaction();
		JavaSourceDirectoryDescriptor javaSourceDirectoryDescriptor = getScanner().scan(directory, TEST_DIRECTORY_PATH, JavaScope.CLASSPATH);
		assertThat(query("MATCH (c:Method:Constructor) WHERE c.signature='void <init>()' RETURN c").getColumn("c"), hasItem(constructorDescriptor(OverloadedConstructor.class)));
		assertThat(query("MATCH (c:Method:Constructor) WHERE c.signature='void <init>(java.lang.String)' RETURN c").getColumn("c"), hasItem(constructorDescriptor(OverloadedConstructor.class, String.class)));
        store.commitTransaction();
    }
}
