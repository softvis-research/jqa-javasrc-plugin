package org.unileipzig.jqassistant.plugin.parser.lib;

import org.junit.Assert;
import org.junit.Test;
import org.unileipzig.jqassistant.plugin.parser.example.ExampleLanguageParser;

public class ParserTest {
    @Test
    public void exampleLanguageTest1() throws Exception {
        ExampleLanguageParser p = new ExampleLanguageParser();
        Assert.assertEquals("(ADD 1, (MULTIPLY 1, 1))", p.parse("1 + 1 * 1").toString());
        Assert.assertEquals("(MULTIPLY 1, (ADD 1, 1))", p.parse("1 * 1 + 1").toString());
        Assert.assertEquals("(MULTIPLY 1, (ADD 1, 1))", p.parse("(1 + 1) + 1").toString());
    }
}
