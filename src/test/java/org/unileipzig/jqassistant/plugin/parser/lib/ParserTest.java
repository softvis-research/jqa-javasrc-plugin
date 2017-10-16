package org.unileipzig.jqassistant.plugin.parser.lib;

import org.junit.Test;
import org.unileipzig.jqassistant.plugin.parser.example.ExampleLanguageParser;

public class ParserTest {
    @Test
    public void exampleLanguageTest1() throws Exception {
        ExampleLanguageParser p = new ExampleLanguageParser();
        System.out.println(p.symbols);
        System.out.println(p.parse("1+1*1"));
    }
}
