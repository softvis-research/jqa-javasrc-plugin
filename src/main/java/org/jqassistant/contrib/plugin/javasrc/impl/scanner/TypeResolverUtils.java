package org.jqassistant.contrib.plugin.javasrc.impl.scanner;

import java.util.EnumSet;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.resolution.types.ResolvedType;
import org.jqassistant.contrib.plugin.javasrc.api.model.VisibilityModifier;

/**
 * This utility class provides often used methods related to parsing and
 * solving.
 * 
 * @author Richard Müller
 *
 */
public class TypeResolverUtils {

    public static final String CONSTRUCTOR_METHOD = "void <init>";

    /**
     * Returns the VisibilityModifier for an EnumSet<Modifier> from java parser.
     * 
     * @param modifiers
     * @return VisibilityModifier
     */
    public static VisibilityModifier getAccessSpecifier(EnumSet<Modifier> modifiers) {
        if (modifiers.contains(Modifier.PUBLIC)) {
            return VisibilityModifier.PUBLIC;
        } else if (modifiers.contains(Modifier.PROTECTED)) {
            return VisibilityModifier.PROTECTED;
        } else if (modifiers.contains(Modifier.PRIVATE)) {
            return VisibilityModifier.PRIVATE;
        } else {
            return VisibilityModifier.DEFAULT;
        }
    }

    /**
     * Returns the full qualified name of a resolved type.
     * 
     * @param resolvedType
     * @return full qualified name
     */
    public static String getQualifiedName(ResolvedType resolvedType) {
        String fqn = "";
        if (resolvedType.isVoid()) {
            fqn = resolvedType.describe();
        } else if (resolvedType.isPrimitive()) {
            fqn = resolvedType.asPrimitive().describe();
        } else if (resolvedType.isReferenceType()) {
            fqn = resolvedType.asReferenceType().getTypeDeclaration().getQualifiedName();
        } else if (resolvedType.isArray()) {
            fqn = resolvedType.asArrayType().describe();
        } else if (resolvedType.isTypeVariable()) {
            fqn = resolvedType.asTypeVariable().qualifiedName();
        } else {
            throw new RuntimeException("Type could not be resolved: " + resolvedType.toString());
        }
        return fqn;
    }

    /**
     * Returns the value of a literal expression as Object.
     * 
     * @param expression
     * @return value as Object
     */
    public static Object getLiteralExpressionValue(Expression expression) {
        if (expression.isBooleanLiteralExpr()) {
            return expression.asBooleanLiteralExpr().getValue();
        } else if (expression.isStringLiteralExpr()) {
            return expression.asStringLiteralExpr().getValue();
        } else if (expression.isLiteralStringValueExpr()) {
            return expression.asLiteralStringValueExpr().getValue();
        } else if (expression.isDoubleLiteralExpr()) {
            return expression.asDoubleLiteralExpr().getValue();
        } else if (expression.isLongLiteralExpr()) {
            return expression.asLongLiteralExpr().getValue();
        } else if (expression.isCharLiteralExpr()) {
            return expression.asCharLiteralExpr().getValue();
        } else if (expression.isIntegerLiteralExpr()) {
            return expression.asIntegerLiteralExpr().getValue();
        } else if (expression.isNullLiteralExpr()) {
            return null;
        } else {
            throw new RuntimeException("Expression value could not be resolved: " + expression.toString());
        }
    }
}
