package org.unileipzig.jqassistant.plugin.parser.example;

import org.unileipzig.jqassistant.plugin.parser.lib.Parser;

public class ExampleLanguageParser extends Parser {
    public ExampleLanguageParser() {
        try {
            // Binary Operators - Logical
            binaryLeftAssociative("[&][&]", 40, (left, right) -> new BinaryOperator(BinaryOperatorType.AND, left, right));
            binaryLeftAssociative("[/][/]", 40, (left, right) -> new BinaryOperator(BinaryOperatorType.OR, left, right));
            // Binary Operators - Mathematical
            binaryLeftAssociative("[+]", 110, (left, right) -> new BinaryOperator(BinaryOperatorType.ADD, left, right));
            binaryLeftAssociative("[-]", 110, (left, right) -> new BinaryOperator(BinaryOperatorType.SUBTRACT, left, right));
            binaryLeftAssociative("[*]", 120, (left, right) -> new BinaryOperator(BinaryOperatorType.MULTIPY, left, right));
            binaryLeftAssociative("[/]", 120, (left, right) -> new BinaryOperator(BinaryOperatorType.DIVIDE, left, right));
            binaryLeftAssociative("[%]", 120, (left, right) -> new BinaryOperator(BinaryOperatorType.MODULO, left, right));
            // Binary Operators - Comparison
            binaryLeftAssociative("[=][=]", 60, (left, right) -> new BinaryOperator(BinaryOperatorType.EQUALS, left, right));
            binaryLeftAssociative("[!][=]", 60, (left, right) -> new BinaryOperator(BinaryOperatorType.NOTEQUALS, left, right));
            binaryLeftAssociative("[<]", 60, (left, right) -> new BinaryOperator(BinaryOperatorType.LOWERTHAN, left, right));
            binaryLeftAssociative("[>]", 60, (left, right) -> new BinaryOperator(BinaryOperatorType.GREATERTHAN, left, right));
            binaryLeftAssociative("[<][=]", 60, (left, right) -> new BinaryOperator(BinaryOperatorType.LOWERTHANOREQUAL, left, right));
            binaryLeftAssociative("[>][=]", 60, (left, right) -> new BinaryOperator(BinaryOperatorType.GREATERTHANOREQUAL, left, right));
            // Binary Operators - Assignment
            binaryRightAssociative("[=]", 10, (left, right) -> new BinaryOperator(BinaryOperatorType.ASSIGN, left, right));
            binaryRightAssociative("[+][=]", 10, (left, right) -> new BinaryOperator(BinaryOperatorType.ADDASSIGN, left, right));
            binaryRightAssociative("[-][=]", 10, (left, right) -> new BinaryOperator(BinaryOperatorType.SUBTRACTASSIGN, left, right));
            binaryRightAssociative("[*][=]", 10, (left, right) -> new BinaryOperator(BinaryOperatorType.MULTIPLYASSIGN, left, right));
            binaryRightAssociative("[/][=]", 10, (left, right) -> new BinaryOperator(BinaryOperatorType.DIVIDEASSIGN, left, right));
            binaryRightAssociative("[%][=]", 10, (left, right) -> new BinaryOperator(BinaryOperatorType.MODULOASSIGN, left, right));
            // Unary Operators
            unaryPrefix("[!]", 10, (operand) -> new UnaryOperator(UnaryOperatorType.NOT, operand));
            unaryPrefix("[+]", 130, (operand) -> new UnaryOperator(UnaryOperatorType.PREFIXPLUS, operand));
            unaryPrefix("[-]", 130, (operand) -> new UnaryOperator(UnaryOperatorType.PREFIXMINUS, operand));
            unaryPrefix("[+][+]", 130, (operand) -> new UnaryOperator(UnaryOperatorType.PREFIXINCREMENT, operand));
            unaryPrefix("[-][-]", 130, (operand) -> new UnaryOperator(UnaryOperatorType.PREFIXDECREMENT, operand));
        } catch (Exception e) {
            System.out.println("Error when constructing Parser: " + e.getMessage());
        }
    }

    public static void main(final String[] args) throws Exception { // need a main() for debugging
        (new ExampleLanguageParser()).parse("1+1");
    }
}
