package org.unileipzig.jqassistant.plugin.parser.test;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

public class JavaParserTest {
    @Test
    public void parseHelloWorld() throws IOException {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        InputStream in = classloader.getResourceAsStream("HelloWorld.java");
        CompilationUnit cu = JavaParser.parse(in);
        System.out.println(cu);
    }
}
