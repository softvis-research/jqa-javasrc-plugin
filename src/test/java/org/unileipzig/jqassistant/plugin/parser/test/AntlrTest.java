package org.unileipzig.jqassistant.plugin.parser.test;

import java8.Java8Lexer;
import java8.Java8Parser;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.Test;

public class AntlrTest {
    private CharStream readFileFromRessourcesFolder(String filename) throws Exception {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        return new ANTLRInputStream(classloader.getResourceAsStream(filename));
    }

    @Test
    public void testExploratoryString() throws Exception {
        TokenSource tokenSource = new Java8Lexer(readFileFromRessourcesFolder("HelloWorld.java"));
        TokenStream inputTokenStream = new CommonTokenStream(tokenSource);
        Java8Parser parser = new Java8Parser(inputTokenStream);
        Java8Parser.CompilationUnitContext c = parser.compilationUnit();
        System.out.println(c);
    }
}
