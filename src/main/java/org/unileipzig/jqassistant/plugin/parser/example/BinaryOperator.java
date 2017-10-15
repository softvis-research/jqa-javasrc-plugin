package org.unileipzig.jqassistant.plugin.parser.example;

import java.util.Arrays;
import java.util.List;

public class BinaryOperator {
    public List<Object> operands;
    public BinaryOperatorType type;

    public BinaryOperator(BinaryOperatorType type, Object left, Object right) {
        this.operands = Arrays.asList(left, right);
        this.type = type;
    }
}
