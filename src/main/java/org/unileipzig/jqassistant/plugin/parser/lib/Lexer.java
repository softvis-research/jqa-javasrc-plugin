package org.unileipzig.jqassistant.plugin.parser.lib;

import com.google.common.collect.Iterables;

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
    private Collection<Symbol> skipped;

    /**
     * Create a Lexer Instance by providing Token Definitions ("Types")
     *
     * @param types mapping of identifiers to regular expressions
     */
    public Lexer(Collection<Symbol> types, Collection<Symbol> skipped) {
        this.symbols = types;
        this.skipped = skipped;
        StringBuilder b = new StringBuilder();
        for (Symbol symbol : Iterables.concat(skipped, types)) {
            b.append(String.format("|(?<%s>%s)", symbol.id, symbol.re)); // doesn't work with numbers as IDs (!!)
        }
        this.regex = Pattern.compile(b.substring(1));
    }

    /**
     * Perform Lexicographic Analysis
     *
     * @param str Input
     * @return List of Token Objects
     */
    public List<Token> tokenize(String str) throws Exception {
        Matcher matcher = regex.matcher(str);
        List<Token> tokens = new LinkedList<>();
        String match;
        int pos = 0;
        outer:
        while (matcher.find()) {
            for (Symbol skipped : skipped) {
                if (matcher.group(skipped.id) != null) {
                    pos = matcher.end();
                    continue outer;
                }
            }
            for (Symbol symbol : symbols) {
                match = matcher.group(symbol.id);
                if (match != null) {
                    pos = matcher.end();
                    tokens.add(symbol.instantiate(match, matcher.start(), matcher.end()));
                    break;
                }
            }
        }
        if (pos < str.length()) {
            throw new Exception("No suitable Token Definition for Input at " + pos + ": " + str.substring(pos));
        }
        return tokens;
    }
}
