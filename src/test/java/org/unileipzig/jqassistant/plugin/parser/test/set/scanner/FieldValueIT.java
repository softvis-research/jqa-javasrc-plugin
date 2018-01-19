package org.unileipzig.jqassistant.plugin.parser.test.set.scanner;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.unileipzig.jqassistant.plugin.parser.api.model.JavaSourceDirectoryDescriptor;
import org.unileipzig.jqassistant.plugin.parser.api.scanner.JavaScope;

import com.buschmais.jqassistant.plugin.common.test.AbstractPluginIT;
import com.buschmais.jqassistant.plugin.common.test.scanner.MapBuilder;

/**
 * Contains test which verify correct scanning static field values.
 */
public class FieldValueIT extends AbstractPluginIT {

   
    @Test
    public void testfieldValue() {
        final String TEST_DIRECTORY_PATH = "src/test/java/";
    	final String TYPE_DIRECTORY_PATH = "src/test/java/org/unileipzig/jqassistant/plugin/parser/test/set/scanner/fieldvalue/";
    	File directory = new File(TYPE_DIRECTORY_PATH);
        store.beginTransaction();
        JavaSourceDirectoryDescriptor javaSourceDirectoryDescriptor = getScanner().scan(directory, TEST_DIRECTORY_PATH, JavaScope.CLASSPATH);
        verifyValue("stringValue", "StringValue");
        verifyValue("intValue", "1");
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
