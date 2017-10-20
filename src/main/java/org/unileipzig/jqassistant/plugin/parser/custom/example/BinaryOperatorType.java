package org.unileipzig.jqassistant.plugin.parser.custom.example;

public enum BinaryOperatorType {
    /* Logical */ AND, OR,
    /* Comparison */ EQUALS, NOTEQUALS, GREATERTHAN, LOWERTHAN, LOWERTHANOREQUAL, GREATERTHANOREQUAL,
    /* Mathematical */ ADD, SUBTRACT, MULTIPLY, DIVIDE, MODULO,
    /* Assignment*/ ASSIGN, ADDASSIGN, SUBTRACTASSIGN, MULTIPLYASSIGN, DIVIDEASSIGN, MODULOASSIGN
}
