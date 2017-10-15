package org.unileipzig.jqassistant.plugin.parser.example;

public enum BinaryOperatorType {
    /* Logical */ AND, OR,
    /* Comparison */ EQUALS, NOTEQUALS, GREATERTHAN, LOWERTHAN, LOWERTHANOREQUAL, GREATERTHANOREQUAL,
    /* Mathematical */ ADD, SUBTRACT, MULTIPY, DIVIDE, MODULO,
    /* Assignment*/ ASSIGN, ADDASSIGN, SUBTRACTASSIGN, MULTIPLYASSIGN, DIVIDEASSIGN, MODULOASSIGN
}
