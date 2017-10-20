package org.unileipzig.jqassistant.plugin.parser.custom.example;

import java.util.Arrays;
import java.util.List;

public class BinaryOperator extends Expression {
    public List<Object> operands;
    public BinaryOperatorType type;

    public BinaryOperator(BinaryOperatorType type, Object left, Object right) {
        this.operands = Arrays.asList(left, right);
        this.type = type;
    }

    @Override
    public String toString() {
        String s = operands.toString();
        s = s.substring(1, s.length() - 1);
        return String.format("(%s %s)", type, s);
    }
}
