package org.unileipzig.jqassistant.plugin.parser.test.set.scanner;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.unileipzig.jqassistant.plugin.parser.test.matcher.TypeDescriptorMatcher.typeDescriptor;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.hamcrest.Matcher;
import org.junit.Test;
import org.unileipzig.jqassistant.plugin.parser.api.model.JavaSourceDirectoryDescriptor;
import org.unileipzig.jqassistant.plugin.parser.api.scanner.JavaScope;
import org.unileipzig.jqassistant.plugin.parser.test.set.scanner.innerclass.NestedInnerClasses;

import com.buschmais.jqassistant.plugin.common.test.AbstractPluginIT;

/**
 * Contains test on relations between outer and inner classes.
 */
public class NestedInnerClassesIT extends AbstractPluginIT {

    /**
     * Scans an outer class.
     * 
     * @throws IOException
     *             If the test fails.
     */
    @Test
    public void nestedInnerClasses() throws IOException {
		final String TEST_DIRECTORY_PATH = "src/test/java/";
		final String FILE_DIRECTORY_PATH = "src/test/java/org/unileipzig/jqassistant/plugin/parser/test/set/scanner/innerclass/";
		File directory = new File(FILE_DIRECTORY_PATH);
		store.beginTransaction();
		JavaSourceDirectoryDescriptor javaSourceDirectoryDescriptor = getScanner().scan(directory, TEST_DIRECTORY_PATH, JavaScope.CLASSPATH);
		List<Map<String, Object>> rows = query("MATCH (t1:Type)-[:DECLARES]->(t2:Type)-[:DECLARES]->(t3:Type) RETURN t1, t2.name, t3.name").getRows();
        assertThat(rows.size(), equalTo(1));
        Map<String, Object> row = rows.get(0);
        assertThat(row.get("t1"), (Matcher<? super Object>) typeDescriptor(NestedInnerClasses.class));
        assertEquals(row.get("t2.name").toString(), "FirstLevel");
        assertEquals(row.get("t3.name").toString(), "SecondLevel");
        store.commitTransaction();
    }
}
