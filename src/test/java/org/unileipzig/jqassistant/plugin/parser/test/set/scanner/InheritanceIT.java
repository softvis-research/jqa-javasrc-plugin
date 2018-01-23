package org.unileipzig.jqassistant.plugin.parser.test.set.scanner;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.assertThat;
import static org.unileipzig.jqassistant.plugin.parser.test.matcher.TypeDescriptorMatcher.typeDescriptor;

import java.io.File;

import org.junit.Test;
import org.unileipzig.jqassistant.plugin.parser.api.model.JavaSourceDirectoryDescriptor;
import org.unileipzig.jqassistant.plugin.parser.api.scanner.JavaScope;
import org.unileipzig.jqassistant.plugin.parser.test.set.scanner.inheritance.SuperInterface;

import com.buschmais.jqassistant.plugin.common.test.AbstractPluginIT;

public class InheritanceIT extends AbstractPluginIT {

   
    @Test
    public void testInterfaceExtendsInterface() {
        final String TEST_DIRECTORY_PATH = "src/test/java/";
    	final String FILE_DIRECTORY_PATH = "src/test/java/org/unileipzig/jqassistant/plugin/parser/test/set/scanner/inheritance/";
    	File directory = new File(FILE_DIRECTORY_PATH);
        store.beginTransaction();
        JavaSourceDirectoryDescriptor javaSourceDirectoryDescriptor = getScanner().scan(directory, TEST_DIRECTORY_PATH, JavaScope.CLASSPATH);
		assertThat(query("MATCH (sub:Type:Interface)-[:EXTENDS]->(super:Type:Interface) RETURN super").getColumn("super"), hasItem(typeDescriptor(SuperInterface.class)));
        store.commitTransaction();
    }
    
    @Test
    public void testClassImplementsInterface() {
        final String TEST_DIRECTORY_PATH = "src/test/java/";
    	final String FILE_DIRECTORY_PATH = "src/test/java/org/unileipzig/jqassistant/plugin/parser/test/set/scanner/inheritance/";
    	File directory = new File(FILE_DIRECTORY_PATH);
        store.beginTransaction();
        JavaSourceDirectoryDescriptor javaSourceDirectoryDescriptor = getScanner().scan(directory, TEST_DIRECTORY_PATH, JavaScope.CLASSPATH);
        assertThat(query("MATCH (t:Type)-[:IMPLEMENTS]->(i:Type:Interface) RETURN i").getColumn("i"), hasItem(typeDescriptor(SuperInterface.class)));
        store.commitTransaction();
    }
}
