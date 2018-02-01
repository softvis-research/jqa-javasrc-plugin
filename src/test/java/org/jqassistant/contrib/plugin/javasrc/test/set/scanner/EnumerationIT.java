package org.jqassistant.contrib.plugin.javasrc.test.set.scanner;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.jqassistant.contrib.plugin.javasrc.test.matcher.FieldDescriptorMatcher.fieldDescriptor;
import static org.jqassistant.contrib.plugin.javasrc.test.matcher.TypeDescriptorMatcher.typeDescriptor;
import static org.junit.Assert.assertThat;

import java.io.File;

import org.jqassistant.contrib.plugin.javasrc.api.model.JavaSourceDirectoryDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.scanner.JavaScope;
import org.jqassistant.contrib.plugin.javasrc.test.set.scanner.enumeration.EnumerationType;
import org.junit.Test;

import com.buschmais.jqassistant.plugin.common.test.AbstractPluginIT;

/**
 * Contains test which verify correct scanning of constructors.
 */
public class EnumerationIT extends AbstractPluginIT {

    
	
    @Test
    public void testEnumerationType() throws NoSuchFieldException {
        final String TEST_DIRECTORY_PATH = "src/test/java/";
    	final String TYPE_DIRECTORY_PATH = "src/test/java/org/jqassistant/contrib/plugin/javasrc/test/set/scanner/enumeration/";
    	File directory = new File(TYPE_DIRECTORY_PATH);
        store.beginTransaction();
        JavaSourceDirectoryDescriptor javaSourceDirectoryDescriptor = getScanner().scan(directory, TEST_DIRECTORY_PATH, JavaScope.CLASSPATH);
        assertThat(query("MATCH (e:Type:Enum) RETURN e").getColumn("e"), hasItem(typeDescriptor(EnumerationType.class)));
        assertThat(query("MATCH (e:Type:Enum)-[:DECLARES]->(f:Field) WHERE f.name = 'A' RETURN f").getColumn("f"), hasItem(fieldDescriptor(EnumerationType.class, "A")));
        assertThat(query("MATCH (e:Type:Enum)-[:DECLARES]->(f:Field) WHERE f.name = 'B' RETURN f").getColumn("f"), hasItem(fieldDescriptor(EnumerationType.class, "B")));
        assertThat(query("MATCH (e:Type:Enum)-[:DECLARES]->(f:Field) WHERE f.name = 'value' RETURN f").getColumn("f"), hasItem(fieldDescriptor(EnumerationType.class, "value")));
  //      System.out.println(query("MATCH (e:Type:Enum)-[:DECLARES]->(c:Constructor) RETURN c").getColumn("c"));
        store.commitTransaction();
    }
	
	/**
     * Verifies scanning of {@link EnumerationType}.
     * 
     * @throws java.io.IOException
     *             If the test fails.
     * @throws NoSuchMethodException
     *             If the test fails.
     */
//    @Test
//    public void implicitDefaultConstructor() throws IOException, NoSuchMethodException, NoSuchFieldException {
//        scanClasses(EnumerationType.class);
//        store.beginTransaction();
//        assertThat(query("MATCH (e:Type:Enum) RETURN e").getColumn("e"), hasItem(typeDescriptor(EnumerationType.class)));
//        assertThat(query("MATCH (e:Type:Enum)-[:EXTENDS]->(s) RETURN s").getColumn("s"), hasItem(typeDescriptor(Enum.class)));
//        assertThat(query("MATCH (e:Type:Enum)-[:DECLARES]->(f:Field) RETURN f").getColumn("f"), CoreMatchers.allOf(
//                hasItem(fieldDescriptor(EnumerationType.class, "A")), hasItem(fieldDescriptor(EnumerationType.class, "B")),
//                hasItem(fieldDescriptor(EnumerationType.class, "value"))));
//        assertThat(query("MATCH (e:Type:Enum)-[:DECLARES]->(c:Constructor) RETURN c").getColumn("c"),
//                hasItem(MethodDescriptorMatcher.constructorDescriptor(EnumerationType.class, String.class, int.class, boolean.class)));
//        store.commitTransaction();
//    }
    
    
}
