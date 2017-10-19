package org.unileipzig.jqassistant.plugin.parser.lib;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;

class Symbol {
    private static final AtomicInteger incrementID = new AtomicInteger(0);
    private static final Pattern reIdentifier = Pattern.compile("^[A-Z]+[A-Za-z0-9]*$");
    public String id;
    public String re;
    public int bp;
    public int skip; // flag whether to skip this token -> can be incremented/decremented temporarily / per context
    public int stop; // flag whether to stop when this token -> can be incremented/decremented temporarily / per context
    public Supplier<Object> nud;
    public Function<Object, Object> led;

    private static boolean validID(String identifier) {
        return reIdentifier.matcher(identifier).find();
    }

    private static String autoID() {
        return "GEN" + incrementID.incrementAndGet();
    }

    public Symbol(String re, int bp, Supplier<Object> nud, Function<Object, Object> led) {
        this.id = autoID();
        this.re = re;
        this.bp = bp;
        this.skip = 0;
        this.nud = nud;
        this.led = led;
    }

    public Symbol(String id, String re) throws Exception {
        if (!validID(id)) throw new Exception("Invalid Identifier: " + id);
        this.id = id;
        this.re = re;
        this.bp = 0;
        this.skip = 0;
        this.nud = null;
        this.led = null;
    }

    public Symbol(String id, String re, boolean skip) throws Exception {
        if (!validID(id)) throw new Exception("Invalid Identifier: " + id);
        this.id = id;
        this.re = re;
        this.bp = 0;
        this.skip = skip ? 1 : 0;
        this.nud = null;
        this.led = null;
    }

    public Token instantiate(String value, int from, int to) {
        return new Token(this, value, from, to);
    }
}
