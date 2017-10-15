package org.unileipzig.jqassistant.plugin.parser.lib;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lexer aka Scanner aka Tokenizer
 */
public class Lexer {
    private Pattern regex;
    private Collection<Symbol> symbols;

    /**
     * Create a Lexer Instance by providing Token Definitions ("Types")
     *
     * @param types mapping of identifiers to regular expressions
     */
    public Lexer(Collection<Symbol> types) {
        StringBuilder b = new StringBuilder();
        for (Symbol symbol : types) {
            b.append(String.format("|(?<%s>%s)", symbol.id, symbol.re)); // doesn't work with numbers as IDs (!!)
        }
        regex = Pattern.compile(b.substring(1));
        symbols = types;
    }

    /**
     * Perform Lexicographic Analysis
     *
     * @param str Input
     * @return List of Token Objects
     */
    public List<Token> tokenize(String str) {
        Matcher matcher = regex.matcher(str);
        List<Token> tokens = new LinkedList<>();
        String match;
        while (matcher.find()) {
            for (Symbol symbol : symbols) {
                match = matcher.group(symbol.id);
                if (match != null) {
                    tokens.add(symbol.instantiate(match, matcher.start(), matcher.end()));
                    break;
                }
            }
        }
        return tokens;
    }
}
