package org.unileipzig.jqassistant.plugin.parser.lib;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class Parser {
    private Map<String, Symbol> symbols = new LinkedHashMap<>();
    private List<Symbol> skipped = new LinkedList<>();
    private List<Token> tokens;
    private int pos;

    private void define(String re, int bp, Supplier<Object> nud, Function<Object, Object> led) throws Exception {
        Symbol s = symbols.get(re);
        if (s != null) {
            if (bp >= s.bp) s.bp = bp;
            if (nud != null) {
                if (s.nud == null) s.nud = nud;
                else throw new Exception("AlreadyDefinedNud");
            }
            if (led != null) {
                if (s.led == null) s.led = led;
                else throw new Exception("AlreadyDefinedLed");
            }
        } else {
            symbols.put(re, new Symbol(re, bp, nud, led));
        }
    }

    public void literal(String re, Function<Object, Object> f) throws Exception {
        define(re, 0, () -> f.apply(tokens.get(pos - 1).value), null);
    }

    public void skip(String re) {
        skipped.add(new Symbol(re, 0, null, null));
    }

    public void binaryLeftAssociative(String re, int bp, BiFunction<Object, Object, Object> f) throws Exception {
        define(re, bp, null, (left) -> f.apply(left, parseExpression(bp)));
    }

    public void binaryRightAssociative(String re, int bp, BiFunction<Object, Object, Object> f) throws Exception {
        define(re, bp, null, (left) -> f.apply(left, parseExpression(bp - 1)));
    }

    public void unaryPrefix(String re, int bp, Function<Object, Object> f) throws Exception {
        define(re, bp, () -> f.apply(parseExpression(bp)), null);
    }

    public void unaryPostfix(String re, int bp, Function<Object, Object> f) throws Exception {
        define(re, bp, null, (left) -> f.apply(parseExpression(bp - 1)));
    }

    private Object parseExpression(int bp) {
        Object left = tokens.get(pos++).symbol.nud.get();
        while (bp < tokens.get(pos).symbol.bp) {
            left = tokens.get(pos++).symbol.led.apply(left);
        }
        return left;
    }

    public Object parse(String input) {
        pos = 0;
        tokens = (new Lexer(symbols.values(), skipped)).tokenize(input);
        return parseExpression(0);
    }
}
