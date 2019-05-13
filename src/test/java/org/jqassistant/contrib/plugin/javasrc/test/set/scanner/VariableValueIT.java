package org.jqassistant.contrib.plugin.javasrc.test.set.scanner;

import com.buschmais.jqassistant.plugin.common.test.AbstractPluginIT;
import org.jqassistant.contrib.plugin.javasrc.api.model.JavaSourceDirectoryDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.scanner.JavaScope;
import org.jqassistant.contrib.plugin.javasrc.test.set.scanner.variablevalue.SubClass;
import org.jqassistant.contrib.plugin.javasrc.test.set.scanner.variablevalue.SuperClass;
import org.junit.Test;

import java.io.File;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.jqassistant.contrib.plugin.javasrc.test.matcher.TypeDescriptorMatcher.typeDescriptor;
import static org.junit.Assert.assertThat;

/**
 * Contains tests to verify correct scanning variable class values.
 *
 * @author Richard Mueller
 */
public class VariableValueIT extends AbstractPluginIT {

    @Test
    public void testTypeAndValue() {
        final String TEST_DIRECTORY_PATH = "src/test/java/";
        final String TYPE_DIRECTORY_PATH = "src/test/java/org/jqassistant/contrib/plugin/javasrc/test/set/scanner/variablevalue/";
        File directory = new File(TYPE_DIRECTORY_PATH);
        store.beginTransaction();
        JavaSourceDirectoryDescriptor javaSourceDirectoryDescriptor = getScanner().scan(directory, TEST_DIRECTORY_PATH, JavaScope.SRC);
        assertThat(query("MATCH (method:Method)-[:DECLARES]->(variable:Variable)-[:IS]->(dynamicType:Type) WHERE variable.name = 'variableWithDifferentStaticAndDynamicType' RETURN dynamicType").getColumn("dynamicType"),
            hasItems(typeDescriptor(SubClass.class)));
        assertThat(query("MATCH (method:Method)-[:DECLARES]->(variable:Variable)-[:OF_TYPE]->(staticType:Type) WHERE variable.name = 'variableWithDifferentStaticAndDynamicType' RETURN staticType").getColumn("staticType"),
            hasItems(typeDescriptor(SuperClass.class)));
        store.commitTransaction();
    }
}
