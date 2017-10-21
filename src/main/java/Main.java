import java8.Java8Lexer;
import java8.Java8Parser;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;

public class Main {
    private static CharStream readFile(String path) throws Exception {
        return new ANTLRInputStream(new String(Files.readAllBytes(new File(path).toPath()), Charset.forName("UTF-8")));
    }

    public static void print(RuleContext ctx) {
        explore(ctx, 0);
    }

    private static void explore(RuleContext ctx, int indentation) {
        // source: https://github.com/ftomassetti/python-ast
        String ruleName = Java8Parser.ruleNames[ctx.getRuleIndex()];
        for (int i = 0; i < indentation; i++) {
            System.out.print("  ");
        }
        System.out.println(ruleName);
        for (int i = 0; i < ctx.getChildCount(); i++) {
            ParseTree element = ctx.getChild(i);
            if (element instanceof RuleContext) {
                explore((RuleContext) element, indentation + 1);
            }
        }
    }

    public static void main(final String[] args) throws Exception { // need a main() for debugging
        TokenSource tokenSource = new Java8Lexer(readFile("src/test/resources/HelloWorld.java"));
        TokenStream inputTokenStream = new CommonTokenStream(tokenSource);
        Java8Parser parser = new Java8Parser(inputTokenStream);
        explore(parser.compilationUnit(), 2);
    }
}
