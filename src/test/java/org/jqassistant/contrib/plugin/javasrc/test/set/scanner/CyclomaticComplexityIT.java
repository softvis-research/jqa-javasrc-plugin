package org.jqassistant.contrib.plugin.javasrc.test.set.scanner;

import static java.lang.Integer.valueOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.buschmais.jqassistant.plugin.common.test.AbstractPluginIT;
import org.jqassistant.contrib.plugin.javasrc.api.model.JavaSourceDirectoryDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.MethodDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.scanner.JavaScope;
import org.junit.Test;

/**
 * Contains tests to verify correct scanning of complexity metrics of methods.
 * 
 * @authors Dirk Mahler, Richard Müller
 *
 */
public class CyclomaticComplexityIT extends AbstractPluginIT {

    @Test
    public void testCyclomaticComplexity() throws IOException {
        Map<String, Integer> expectedComplexities = new HashMap<>();
        expectedComplexities.put("<init>", valueOf(1));
        expectedComplexities.put("ifStatement", valueOf(2));
        expectedComplexities.put("nestedIfStatement", valueOf(4));
        expectedComplexities.put("caseStatement", valueOf(5));
        expectedComplexities.put("baseComplexity", valueOf(1));
        expectedComplexities.put("highComplexity", valueOf(11));
        final String TEST_DIRECTORY_PATH = "src/test/java/";
        final String FILE_DIRECTORY_PATH = "src/test/java/org/jqassistant/contrib/plugin/javasrc/test/set/scanner/metric/";
        File directory = new File(FILE_DIRECTORY_PATH);
        store.beginTransaction();
        JavaSourceDirectoryDescriptor javaSourceDirectoryDescriptor = getScanner().scan(directory, TEST_DIRECTORY_PATH, JavaScope.SRC);
        List<MethodDescriptor> methods = query("Match (:Class)-[:DECLARES]->(m:Method) return m").getColumn("m");
        assertThat(methods.size(), equalTo(6));
        for (MethodDescriptor methodDescriptor : methods) {
            String name = methodDescriptor.getName();
            int cyclomaticComplexity = methodDescriptor.getCyclomaticComplexity();
            Integer expectedComplexity = expectedComplexities.get(name);
            assertThat(expectedComplexity, notNullValue());
            assertThat(cyclomaticComplexity, equalTo(expectedComplexity));
        }
        store.commitTransaction();
    }

}
