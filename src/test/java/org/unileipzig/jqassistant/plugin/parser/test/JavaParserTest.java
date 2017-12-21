package org.unileipzig.jqassistant.plugin.parser.test;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

public class JavaParserTest {
    Object exampleField;
    @Test
    public void parseHelloWorld() throws IOException {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        CompilationUnit root = JavaParser.parse(classloader.getResourceAsStream("samples/HelloWorld.java.skip"));
        for (ImportDeclaration iD : root.getImports()) {
            System.out.println("ImportDeclaration: " + iD);
        }
        for (TypeDeclaration tD : root.getTypes()) {
            System.out.println("TypeDeclaration: " + tD.getName());
            for (MethodDeclaration mD : (List<MethodDeclaration>) tD.getMethods()) {
                System.out.println("MethodDeclaration: " + mD.getName());
            }
        }
    }
}
