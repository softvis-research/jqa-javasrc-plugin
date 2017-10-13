package org.unileipzig.jqassistant.plugin.parser.test;

import com.buschmais.jqassistant.core.scanner.api.DefaultScope;
import com.buschmais.jqassistant.core.scanner.api.Scanner;
import com.buschmais.jqassistant.plugin.common.test.AbstractPluginIT;
import org.unileipzig.jqassistant.plugin.parser.api.model.SourceCodeDescriptor;
import org.junit.Test;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;


public class SourceCodeScannerIT extends AbstractPluginIT {

    @Test
    public void validSourceCode() throws IOException {
        store.beginTransaction();
        File xmlFile = new File(getClassesDirectory(SourceCodeScannerIT.class), "/HelloWorld.java");
        Source source = new StreamSource(xmlFile);
        Scanner scanner = getScanner();
        SourceCodeDescriptor documentDescriptor = store.create(SourceCodeDescriptor.class);
        scanner.getContext().push(SourceCodeDescriptor.class, documentDescriptor);
        scanner.scan(source, xmlFile.getAbsolutePath(), DefaultScope.NONE);
        scanner.getContext().pop(SourceCodeDescriptor.class);
        // TODO: Test
        store.commitTransaction();
    }
}
