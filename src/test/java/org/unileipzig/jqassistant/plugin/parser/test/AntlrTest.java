package org.unileipzig.jqassistant.plugin.parser.test;

import java8.Java8Lexer;
import java8.Java8Parser;
import org.antlr.v4.runtime.*;
import org.junit.Test;

import java.io.InputStream;
import java.nio.file.Files;

public class AntlrTest {
    private static InputStream readFileFromRessourcesFolder(String filename) {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        return classloader.getResourceAsStream(filename);
    };

    @Test
    public void testExploratoryString() throws Exception {
        CharStream inputCharStream = new ANTLRInputStream(readFileFromRessourcesFolder("HelloWorld.java"));
        TokenSource tokenSource = new Java8Lexer(inputCharStream);
        TokenStream inputTokenStream = new CommonTokenStream(tokenSource);
        Java8Parser parser = new Java8Parser(inputTokenStream);

        Java8Parser.CompilationUnitContext c = parser.compilationUnit();
        System.out.println("OUTPUT" + c);
    }
}
