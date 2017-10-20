package org.unileipzig.jqassistant.plugin.parser.custom;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class LexerTest {
    @Test
    public void simpleCase1() throws Exception {
        Lexer l = new Lexer(Arrays.asList(
            new Symbol("WS", "[ \t\r\n\f]"),
            new Symbol("HEX", "x[0-9]+(\\.[0-9]+)?"),
            new Symbol("FLOAT", "[0-9]+\\.[0-9]+"),
            new Symbol("INTEGER", "[0-9]+")
        ));
        List<Token> tokens = l.tokenize("x5.0 0.9 192");
        Assert.assertEquals("HEX", tokens.get(0).symbol.id);
        Assert.assertEquals("x5.0", tokens.get(0).value);
        Assert.assertEquals(0, tokens.get(0).from);
        Assert.assertEquals(4, tokens.get(0).to);
        Assert.assertEquals("WS", tokens.get(1).symbol.id);
        Assert.assertEquals(" ", tokens.get(1).value);
        Assert.assertEquals(4, tokens.get(1).from);
        Assert.assertEquals(5, tokens.get(1).to);
        Assert.assertEquals("FLOAT", tokens.get(2).symbol.id);
        Assert.assertEquals("0.9", tokens.get(2).value);
        Assert.assertEquals(5, tokens.get(2).from);
        Assert.assertEquals(8, tokens.get(2).to);
        Assert.assertEquals("WS", tokens.get(3).symbol.id);
        Assert.assertEquals(" ", tokens.get(3).value);
        Assert.assertEquals(8, tokens.get(3).from);
        Assert.assertEquals(9, tokens.get(3).to);
        Assert.assertEquals("INTEGER", tokens.get(4).symbol.id);
        Assert.assertEquals("192", tokens.get(4).value);
        Assert.assertEquals(9, tokens.get(4).from);
        Assert.assertEquals(12, tokens.get(4).to);
    }

    @Test
    public void simpleCase2() throws Exception {
        Lexer l = new Lexer(Arrays.asList(
            new Symbol("WS", "[ \t\r\n\f]", true),
            new Symbol("HEX", "x[0-9]+(\\.[0-9]+)?"),
            new Symbol("FLOAT", "[0-9]+\\.[0-9]+"),
            new Symbol("INTEGER", "[0-9]+")
        ));
        List<Token> tokens = l.tokenize("x5.0 0.9 192");
        Assert.assertEquals("HEX", tokens.get(0).symbol.id);
        Assert.assertEquals("x5.0", tokens.get(0).value);
        Assert.assertEquals(0, tokens.get(0).from);
        Assert.assertEquals(4, tokens.get(0).to);
        Assert.assertEquals("FLOAT", tokens.get(1).symbol.id);
        Assert.assertEquals("0.9", tokens.get(1).value);
        Assert.assertEquals(5, tokens.get(1).from);
        Assert.assertEquals(8, tokens.get(1).to);
        Assert.assertEquals("INTEGER", tokens.get(2).symbol.id);
        Assert.assertEquals("192", tokens.get(2).value);
        Assert.assertEquals(9, tokens.get(2).from);
        Assert.assertEquals(12, tokens.get(2).to);
    }
}
