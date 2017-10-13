package org.unileipzig.jqassistant.plugin.parser.lib;

class Token {
    private String name;
    private String value;
    private int from;
    private int to;

    Token(String name, String value, int from, int to) {
        this.name = name;
        this.value = value;
        this.from = from;
        this.to = to;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public int getFrom() {
        return from;
    }

    public int getTo() {
        return to;
    }
}
