package org.unileipzig.jqassistant.plugin.parser.lib;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

class Symbol {
    private static final AtomicInteger incrementID = new AtomicInteger(0);
    public String id;
    public String re;
    public int bp;
    public Supplier<Object> nud;
    public Function<Object, Object> led;

    public Symbol(String re, int bp, Supplier<Object> nud, Function<Object, Object> led) {
        this.id = "GEN" + incrementID.incrementAndGet(); // Autoincrement-ID
        this.re = re;
        this.bp = bp;
        this.nud = nud;
        this.led = led;
    }

    public Symbol(String id, String re) {
        this.id = id; // TODO: check whether ID is OK
        this.re = re;
        this.bp = 0;
        this.nud = null;
        this.led = null;
    }

    public Token instantiate(String value, int from, int to) {
        return new Token(this, value, from, to);
    }
}
