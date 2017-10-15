package org.unileipzig.jqassistant.plugin.parser.example;

public class UnaryOperator {
    public Object operand;
    public UnaryOperatorType type;

    public UnaryOperator(UnaryOperatorType type, Object operand) {
        this.operand = operand;
        this.type = type;
    }
}
