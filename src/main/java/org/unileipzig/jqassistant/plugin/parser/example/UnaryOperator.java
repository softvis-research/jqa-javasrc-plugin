package org.unileipzig.jqassistant.plugin.parser.example;

public class UnaryOperator extends Expression {
    public Object operand;
    public UnaryOperatorType type;

    public UnaryOperator(UnaryOperatorType type, Object operand) {
        this.operand = operand;
        this.type = type;
    }

    @Override
    public String toString() {
        return String.format("(%s %s)", type, operand);
    }
}
