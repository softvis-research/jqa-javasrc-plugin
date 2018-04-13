package org.jqassistant.contrib.plugin.javasrc.test.set.scanner;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.List;
import java.util.Map;

import com.buschmais.jqassistant.plugin.common.test.AbstractPluginIT;
import com.buschmais.jqassistant.plugin.common.test.scanner.MapBuilder;
import org.jqassistant.contrib.plugin.javasrc.api.model.JavaSourceDirectoryDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.scanner.JavaScope;
import org.junit.Test;

/**
 * Contains tests to verify correct scanning static field values.
 * 
 * @author Richard Mueller
 * 
 */
public class FieldValueIT extends AbstractPluginIT {

    @Test
    public void testFieldValue() {
        final String TEST_DIRECTORY_PATH = "src/test/java/";
        final String TYPE_DIRECTORY_PATH = "src/test/java/org/jqassistant/contrib/plugin/javasrc/test/set/scanner/fieldvalue/";
        File directory = new File(TYPE_DIRECTORY_PATH);
        store.beginTransaction();
        JavaSourceDirectoryDescriptor javaSourceDirectoryDescriptor = getScanner().scan(directory, TEST_DIRECTORY_PATH, JavaScope.SRC);
        verifyValue("stringValue", "StringValue");
        verifyValue("intValue", 1);
        store.commitTransaction();
    }

    private <V> void verifyValue(String fieldName, V expectedValue) {
        Map<String, Object> params = MapBuilder.<String, Object> create("fieldName", fieldName).get();
        TestResult testResult = query("MATCH (:Type)-[:DECLARES]->(f:Field)-[:HAS]->(v:Value:Primitive) WHERE f.name={fieldName} RETURN v.value as value",
                params);
        List<Map<String, Object>> rows = testResult.getRows();
        assertThat(rows.size(), equalTo(1));
        Map<String, Object> row = rows.get(0);
        V value = (V) row.get("value");
        assertThat(value, equalTo(expectedValue));
    }
}
