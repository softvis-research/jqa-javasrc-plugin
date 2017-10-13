package org.unileipzig.jqassistant.plugin.parser.lib;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lexer aka Scanner aka Tokenizer
 */
public class Lexer {
    private LinkedHashMap<String, String> types;
    private Pattern regex;

    /**
     * Create a Lexer Instance by providing Token Definitions ("Types")
     *
     * @param tokenDefinitions mapping of identifiers to regular expressions
     */
    public Lexer(LinkedHashMap<String, String> tokenDefinitions) {
        StringBuffer s = new StringBuffer();
        types = tokenDefinitions;
        types.forEach((identifier, pattern) -> {
            s.append(String.format("|(?<%s>%s)", identifier, pattern));
        });
        regex = Pattern.compile(s.substring(1));
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
            for (String identifier : types.keySet()) {
                match = matcher.group(identifier);
                if (match != null) {
                    tokens.add(new Token(identifier, match, matcher.start(), matcher.end()));
                    break;
                }
            }
        }
        return tokens;
    }
}
