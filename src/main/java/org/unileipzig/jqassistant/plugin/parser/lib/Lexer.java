package org.unileipzig.jqassistant.plugin.parser.lib;

import com.google.common.collect.Iterables;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lexer aka Scanner aka Tokenizer
 */
public class Lexer {
    private Pattern regex;
    private Collection<Symbol> symbols;
    private Collection<Symbol> skip;

    /**
     * Create a Lexer Instance by providing Token Definitions ("Types")
     *
     * @param types Descriptions of Tokens that should end up in the list of lexed Tokens
     * @param skip  Descriptions of Tokens that should NOT end up in the list of lexed Tokens
     */
    public Lexer(Collection<Symbol> types, Collection<Symbol> skip) {
        this.symbols = types;
        this.skip = skip;
        StringBuilder b = new StringBuilder();
        for (Symbol symbol : Iterables.concat(skip, types)) {
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
        List<Token> tokens = new ArrayList<>();
        String match;
        int pos = 0;
        outer:
        while (matcher.find()) {
            if (matcher.start() > pos) {
                throw new SyntaxError(str, pos, matcher.start());
            }
            for (Symbol symbol : skip) {
                if (matcher.group(symbol.id) != null) {
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
            throw new SyntaxError(str, pos, str.length());
        }
        return tokens;
    }
}
