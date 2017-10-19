package org.unileipzig.jqassistant.plugin.parser.lib;

import org.junit.Assert;
import org.junit.Test;
import org.unileipzig.jqassistant.plugin.parser.example.ExampleLanguagePrattParser;

public class PrattParserTest {
    @Test
    public void emptyInputTest() throws Exception {
        //ExampleLanguagePrattParser p = new ExampleLanguagePrattParser();
        //Assert.assertEquals("", p.parse("").toString());
    }

    @Test
    public void exampleLanguageTest1() throws Exception {
        ExampleLanguagePrattParser p = new ExampleLanguagePrattParser();
        Assert.assertEquals("(ADD 1, (MULTIPLY 1, 1))", p.parse("1 + 1 * 1").toString());
        Assert.assertEquals("(ADD 1, (MULTIPLY 1, 1))", p.parse("(1 + 1) * 1").toString());
    }
}
