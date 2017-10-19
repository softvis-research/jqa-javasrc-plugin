package org.unileipzig.jqassistant.plugin.parser.lib;

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

    /**
     * Create a Lexer Instance by providing Token Definitions ("Types")
     *
     * @param types Descriptions of Tokens that should end up in the list of lexed Tokens
     */
    public Lexer(Collection<Symbol> types) {
        this.symbols = types;
        StringBuilder b = new StringBuilder();
        for (Symbol symbol : types) {
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
        while (matcher.find()) {
            if (matcher.start() > pos) {
                throw new SyntaxError(str, pos, matcher.start());
            }
            for (Symbol symbol : symbols) {
                match = matcher.group(symbol.id);
                if (match != null) {
                    pos = matcher.end();
                    if (symbol.skip == 0) {
                        tokens.add(symbol.instantiate(match, matcher.start(), matcher.end()));
                    }
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
