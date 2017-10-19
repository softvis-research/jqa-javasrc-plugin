package org.unileipzig.jqassistant.plugin.parser.lib;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Implementation of a Parsing Strategy outlined in [Pratt, 1973]
 *  "Top down operator precedence" https://doi.org/10.1145/512927.512931
 * Other Articles about that Parsing Strategy:
 *  - http://javascript.crockford.com/tdop/tdop.html
 *  - http://effbot.org/zone/simple-top-down-parsing.htm
 *  Discussion about combining Pratt with other Parsing Strategies, such as PEGParser/Packrat:
 *  - https://news.ycombinator.com/item?id=10731002
 */
public class PrattParser implements Parser {
    public Map<String, Symbol> symbols = new LinkedHashMap<>();
    public List<Token> tokens;
    private int i;
    private int n;

    private Symbol define(String re, int bp, Supplier<Object> nud, Function<Object, Object> led) throws Exception {
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
            s = new Symbol(re, bp, nud, led);
            symbols.put(re, s);
        }
        return s;
    }

    public Symbol skip(String re) {
        Symbol s = symbols.get(re);
        if (s == null) {
            s = new Symbol(re, 0, null, null);
            symbols.put(re, s);
        }
        ++s.skip;
        return s;
    }

    public Symbol stop(String re) {
        Symbol s = symbols.get(re);
        if (s == null) {
            s = new Symbol(re, 0, null, null);
            symbols.put(re, s);
        }
        ++s.stop;
        return s;
    }

    public void literal(String re, Function<String, Object> f) throws Exception {
        define(re, 0, () -> f.apply(tokens.get(i - 1).value), null);
    }

    public void block(String reStart, String reEnd, int bp, Supplier<Object> f) throws Exception {
        Symbol end = define(reEnd, bp, f, null);
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
        Object left = tokens.get(i++).symbol.nud.get();
        while (i < n && bp < tokens.get(i).symbol.bp) {
            left = tokens.get(i++).symbol.led.apply(left);
        }
        return left;
    }

    @Override
    public Object parse(String input) throws Exception {
        tokens = (new Lexer(symbols.values())).tokenize(input);
        i = 0;
        n = tokens.size();
        if (n == 0) return null; // TODO: properly handle empty input
        return parseExpression(0);
    }
}
