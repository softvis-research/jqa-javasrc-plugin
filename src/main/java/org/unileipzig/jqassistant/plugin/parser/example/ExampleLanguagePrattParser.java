package org.unileipzig.jqassistant.plugin.parser.example;

import org.unileipzig.jqassistant.plugin.parser.lib.PrattParser;

public class ExampleLanguagePrattParser extends PrattParser {
    public ExampleLanguagePrattParser() {
        try {
            // Binary Operators - Logical
            binaryLeftAssociative("[&][&]", 40, (left, right) -> new BinaryOperator(BinaryOperatorType.AND, left, right));
            binaryLeftAssociative("[/][/]", 40, (left, right) -> new BinaryOperator(BinaryOperatorType.OR, left, right));
            // Binary Operators - Mathematical
            binaryLeftAssociative("[+]", 110, (left, right) -> new BinaryOperator(BinaryOperatorType.ADD, left, right));
            binaryLeftAssociative("[-]", 110, (left, right) -> new BinaryOperator(BinaryOperatorType.SUBTRACT, left, right));
            binaryLeftAssociative("[*]", 120, (left, right) -> new BinaryOperator(BinaryOperatorType.MULTIPLY, left, right));
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
            unaryPostfix("[+][+]", 130, (operand) -> new UnaryOperator(UnaryOperatorType.POSTFIXINCREMENT, operand));
            unaryPostfix("[-][-]", 130, (operand) -> new UnaryOperator(UnaryOperatorType.POSTFIXDECREMENT, operand));
            // Literals
            literal("[0-9]+", Integer::parseInt); // INTEGER
            skip("[ \t\r\n\f]");
            //////////////////////////////////////////////////////////////////////////////
            //////////////////////////////////////////////////////////////////////////////
            //////////////////////////////////////////////////////////////////////////////
            /*
             * Function Declaration
             *
             * Grammar Rule (EMFText):
             *    members.ClassMethod ::=
             *      annotationsAndModifiers*
             *      ("<" typeParameters ("," typeParameters)* ">")?
             *      (typeReference arrayDimensionsBefore*)
             *      name[]
             *      "(" (parameters ("," parameters)* )? ")"
             *      arrayDimensionsAfter*
             *      ("throws" exceptions ("," exceptions)*)?
             *      "{" (!1 statements)* "}"
             *      ;
             *
             * How to map that to the Pratt PrattParser Strategy:
             *  - collect all implicitly used TOKENS (Lexer must know/handle them in advance)
             *  - a whole lot of language features can have annotations, for example, so we need to put
             *    all of them into a common data structure so we can avoid backtracking
             *  - in case of that particular rule, we know ftom the Java.ecore, that annotationsAndModifiers is of type
             *    AnnotationInstance so we can parse that first and check what can be followed on from that
             */
            this.literal("function", (v) -> {
                return new Object();
            });
            /*
             * Brackets
             *
             * Grammar Rule (ANTLR):
             *    parExpression: '(' expression ')';
             *
             * Grammar Rule (EMFText):
             *    @Operator(type="primitive", weight="20", superclass="OclExpression")
             *    BraceExp ::= "(" exp ")";
             *
             */
            this.block("[(]", "[)]", 140, () -> {
                return "(";
            });
        } catch (Exception e) {
            System.out.println("Error when constructing PrattParser: " + e.getMessage());
        }
    }

    public static void main(final String[] args) throws Exception { // need a main() for debugging
        System.out.println((new ExampleLanguagePrattParser()).parse("1 + 1"));
    }
}
