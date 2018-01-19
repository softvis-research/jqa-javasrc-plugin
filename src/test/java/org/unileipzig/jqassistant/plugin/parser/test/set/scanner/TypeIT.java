package org.unileipzig.jqassistant.plugin.parser.test.set.scanner;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.assertThat;
import static org.unileipzig.jqassistant.plugin.parser.test.matcher.TypeDescriptorMatcher.typeDescriptor;

import java.io.File;
import java.io.IOException;

import org.junit.Test;
import org.unileipzig.jqassistant.plugin.parser.api.model.ClassTypeDescriptor;
import org.unileipzig.jqassistant.plugin.parser.api.model.JavaSourceDirectoryDescriptor;
import org.unileipzig.jqassistant.plugin.parser.api.model.JavaSourceFileDescriptor;
import org.unileipzig.jqassistant.plugin.parser.api.model.TypeDescriptor;
import org.unileipzig.jqassistant.plugin.parser.api.scanner.JavaScope;
import org.unileipzig.jqassistant.plugin.parser.test.set.scanner.type.Type;
import org.unileipzig.jqassistant.plugin.parser.test.set.scanner.visibility.Visibility;

import com.buschmais.jqassistant.plugin.common.api.model.FileDescriptor;
import com.buschmais.jqassistant.plugin.common.test.AbstractPluginIT;


/**
 * Contains test to verify correct scanning of types.
 * 
 * @author Richard Müller
 *
 */
public class TypeIT extends AbstractPluginIT {
	
    @Test
    public void testTypeDescriptor() {
        final String TEST_DIRECTORY_PATH = "src/test/java/";
    	final String FILE_DIRECTORY_PATH = "src/test/java/org/unileipzig/jqassistant/plugin/parser/test/set/scanner/type/";
    	File directory = new File(FILE_DIRECTORY_PATH);
        store.beginTransaction();
        JavaSourceDirectoryDescriptor javaSourceDirectoryDescriptor = getScanner().scan(directory, TEST_DIRECTORY_PATH, JavaScope.CLASSPATH);
        assertThat(query("MATCH (t:Type) RETURN t").getColumn("t"), hasItem(typeDescriptor(Type.class)));
        store.commitTransaction();
    }
    
    @Test
    public void testAccessModifier() {
        final String TEST_DIRECTORY_PATH = "src/test/java/";
    	final String FILE_DIRECTORY_PATH = "src/test/java/org/unileipzig/jqassistant/plugin/parser/test/set/scanner/type/";
        File directory = new File(FILE_DIRECTORY_PATH);
        store.beginTransaction();
        JavaSourceDirectoryDescriptor javaSourceDirectoryDescriptor = getScanner().scan(directory, TEST_DIRECTORY_PATH, JavaScope.CLASSPATH);
        for (FileDescriptor fileDescriptor : javaSourceDirectoryDescriptor.getContains()) {
            for (TypeDescriptor typeDescriptor : ((JavaSourceFileDescriptor) fileDescriptor).getTypes()) {               
            	assertTrue(((ClassTypeDescriptor)typeDescriptor).isFinal());
            	assertTrue(!((ClassTypeDescriptor)typeDescriptor).isStatic());
            	assertTrue(!((ClassTypeDescriptor)typeDescriptor).isAbstract());
            }
        }
        store.commitTransaction();
    }
	

    
}
