package org.unileipzig.jqassistant.plugin.parser.lib;

import org.junit.Assert;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.List;

public class LexerTest {
    @Test
    public void simpleCase1() {
        LinkedHashMap<String, String> types = new LinkedHashMap<>(); // no map-literal in java >:(
        types.put("WS", "[ \t\r\n\f]");
        types.put("HEX", "x[0-9]+(\\.[0-9]+)?");
        types.put("FLOAT", "[0-9]+\\.[0-9]+");
        types.put("INTEGER", "[0-9]+");
        Lexer l = new Lexer(types);
        List<Token> tokens = l.tokenize("x5.0 0.9 192"); // thus no deep-equal in junit >:(
        Assert.assertEquals("HEX", tokens.get(0).getName());
        Assert.assertEquals("x5.0", tokens.get(0).getValue());
        Assert.assertEquals(0, tokens.get(0).getFrom());
        Assert.assertEquals(4, tokens.get(0).getTo());
        Assert.assertEquals("WS", tokens.get(1).getName());
        Assert.assertEquals(" ", tokens.get(1).getValue());
        Assert.assertEquals(4, tokens.get(1).getFrom());
        Assert.assertEquals(5, tokens.get(1).getTo());
        Assert.assertEquals("FLOAT", tokens.get(2).getName());
        Assert.assertEquals("0.9", tokens.get(2).getValue());
        Assert.assertEquals(5, tokens.get(2).getFrom());
        Assert.assertEquals(8, tokens.get(2).getTo());
        Assert.assertEquals("WS", tokens.get(3).getName());
        Assert.assertEquals(" ", tokens.get(3).getValue());
        Assert.assertEquals(8, tokens.get(3).getFrom());
        Assert.assertEquals(9, tokens.get(3).getTo());
        Assert.assertEquals("INTEGER", tokens.get(4).getName());
        Assert.assertEquals("192", tokens.get(4).getValue());
        Assert.assertEquals(9, tokens.get(4).getFrom());
        Assert.assertEquals(12, tokens.get(4).getTo());
    }
}
