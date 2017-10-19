package org.unileipzig.jqassistant.plugin.parser.lib;

import java.util.*;

/**
 * Implementation of a Parsing Strategy outlined in [Laurent, 2017]
 * "Red Shift: Procedural Shift-Reduce Parsing"
 * https://conf.researchr.org/event/sle-2017/sle-2017-papers-red-shift-procedural-shift-reduce-parsing
 * http://norswap.com/pubs/sle2017.pdf
 */
public class RedShiftParser implements Parser {
    public Map<String, Symbol> symbols = new LinkedHashMap<>();
    public List<RedShiftReducer> reducers = new ArrayList<>();
    List<Object> stack = new LinkedList<>();

    public void define(RedShiftReducer r) {
        reducers.add(r);
    }

    @Override
    public Object parse(String input) throws Exception {
        List<Token> tokens = (new Lexer(symbols.values())).tokenize(input);
        int i = 0;
        int n = tokens.size();
        while (i < n) {
            stack.add(tokens.get(i++));
            outer:
            while (true) {
                for (RedShiftReducer reducer : reducers) {
                    if (reducer.reduce(stack, tokens)) {
                        continue outer;
                    }
                }
                break;
            }
        }
        return new Object();
    }
}
