package org.unileipzig.jqassistant.plugin.parser.lib;

import java.util.function.Function;
import java.util.function.Supplier;

class Symbol {
    public String id;
    public String re;
    public int bp;
    public Supplier<Object> nud;
    public Function<Object, Object> led;

    public Symbol(String re, int bp, Supplier<Object> nud, Function<Object, Object> led) {
        this.id = null;
        this.re = re;
        this.bp = bp;
        this.nud = nud;
        this.led = led;
    }

    public Symbol(String id, String re) {
        this.id = id;
        this.re = re;
        this.bp = 0;
        this.nud = null;
        this.led = null;
    }

    public Token instantiate(String value, int from, int to) {
        return new Token(this, value, from, to);
    }
}
