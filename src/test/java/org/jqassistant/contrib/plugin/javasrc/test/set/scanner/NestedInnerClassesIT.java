package org.jqassistant.contrib.plugin.javasrc.test.set.scanner;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.jqassistant.contrib.plugin.javasrc.test.matcher.TypeDescriptorMatcher.typeDescriptor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.buschmais.jqassistant.plugin.common.test.AbstractPluginIT;
import org.hamcrest.Matcher;
import org.jqassistant.contrib.plugin.javasrc.api.model.JavaSourceDirectoryDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.scanner.JavaScope;
import org.jqassistant.contrib.plugin.javasrc.test.set.scanner.innerclass.EnumWithInnerClass;
import org.jqassistant.contrib.plugin.javasrc.test.set.scanner.innerclass.NestedInnerClasses;
import org.junit.Test;

/**
 * Contains tests to verify correct scanning of relations between outer and
 * inner classes.
 * 
 * @authors Dirk Mahler, Richard Mueller
 *
 */
public class NestedInnerClassesIT extends AbstractPluginIT {

    @Test
    public void testNestedInnerClasses() throws IOException {
        final String TEST_DIRECTORY_PATH = "src/test/java/";
        final String FILE_DIRECTORY_PATH = "src/test/java/org/jqassistant/contrib/plugin/javasrc/test/set/scanner/innerclass/";
        File directory = new File(FILE_DIRECTORY_PATH);
        store.beginTransaction();
        JavaSourceDirectoryDescriptor javaSourceDirectoryDescriptor = getScanner().scan(directory, TEST_DIRECTORY_PATH, JavaScope.SRC);
        List<Map<String, Object>> rows = query("MATCH (t1:Type)-[:DECLARES]->(t2:Type)-[:DECLARES]->(t3:Type) RETURN t1, t2.name, t3.name").getRows();
        assertThat(rows.size(), equalTo(1));
        Map<String, Object> row = rows.get(0);
        assertThat(row.get("t1"), (Matcher<? super Object>) typeDescriptor(NestedInnerClasses.class));
        assertEquals(row.get("t2.name").toString(), "FirstLevel");
        assertEquals(row.get("t3.name").toString(), "SecondLevel");
        store.commitTransaction();
    }

    @Test
    public void testEnumWithInnerClass() throws IOException {
        final String TEST_DIRECTORY_PATH = "src/test/java/";
        final String FILE_DIRECTORY_PATH = "src/test/java/org/jqassistant/contrib/plugin/javasrc/test/set/scanner/innerclass/";
        File directory = new File(FILE_DIRECTORY_PATH);
        store.beginTransaction();
        JavaSourceDirectoryDescriptor javaSourceDirectoryDescriptor = getScanner().scan(directory, TEST_DIRECTORY_PATH, JavaScope.SRC);
        assertThat(query("MATCH (e:Enum)-[:DECLARES]->(innerClass:Class) RETURN innerClass").getColumn("innerClass"),
                hasItem(typeDescriptor(EnumWithInnerClass.Innerclass.class)));
        store.commitTransaction();
    }
}
