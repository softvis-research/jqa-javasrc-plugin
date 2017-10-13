package org.unileipzig.jqassistant.plugin.parser.api.scanner;

import com.buschmais.jqassistant.plugin.common.api.scanner.filesystem.FileResource;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;

public class ExampleTest {
    @Test
    public void unitTestsWorking() throws IOException {
        ByteArrayInputStream stream = new ByteArrayInputStream("<a><a></<a></a>".getBytes());
        FileResource resource = Mockito.mock(FileResource.class);

        doReturn(stream).when(resource).createStream();

        assertThat(true, is(true));
    }
}
