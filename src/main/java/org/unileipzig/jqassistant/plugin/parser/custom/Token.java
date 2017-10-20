package org.unileipzig.jqassistant.plugin.parser.custom;

class Token {
    public Symbol symbol;
    public String value;
    public int from;
    public int to;

    Token(Symbol symbol, String value, int from, int to) {
        this.symbol = symbol;
        this.value = value;
        this.from = from;
        this.to = to;
    }
}
