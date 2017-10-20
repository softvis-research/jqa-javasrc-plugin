package org.unileipzig.jqassistant.plugin.parser.custom;

public class SyntaxError extends Exception {

    private static int line(String s, int until) {
        int lines = 1;
        for (int i = 0; i < until; i++) {
            char c = s.charAt(i);
            if (c == '\n') lines++;
        }
        return lines;
    }

    private static int column(String s, int until) {
        int column = 0;
        for (int i = until; i > 0; i--) {
            char c = s.charAt(i);
            if (c == '\n') break;
            column++;
        }
        return column;
    }

    /**
     * @param input Input given to Lexer::tokenize()
     * @param from  Position within the Input from where on there was no matching Token Definition
     * @param to    Position within the Input until which there was no matching Token Definition
     */
    public SyntaxError(String input, int from, int to) {
        super(String.format("No suitable Token Definition for Input on line %s column %s: '%s'",
            line(input, from),
            column(input, from),
            input.substring(from, to)
        ));
    }
}
