package org.jqassistant.contrib.plugin.javasrc.impl.scanner.visitor;

import java.util.EnumSet;

import com.buschmais.jqassistant.core.store.api.model.Descriptor;
import com.buschmais.jqassistant.plugin.common.api.model.ArrayValueDescriptor;
import com.buschmais.jqassistant.plugin.common.api.model.ValueDescriptor;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.nodeTypes.NodeWithModifiers;
import com.github.javaparser.ast.nodeTypes.modifiers.NodeWithAbstractModifier;
import com.github.javaparser.ast.nodeTypes.modifiers.NodeWithFinalModifier;
import com.github.javaparser.ast.nodeTypes.modifiers.NodeWithStaticModifier;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import org.jqassistant.contrib.plugin.javasrc.api.model.AbstractDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.AccessModifierDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.AnnotatedDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.AnnotationValueDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.ClassValueDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.EnumValueDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.FieldDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.PrimitiveValueDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.TypeDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.VisibilityModifier;

public abstract class AbstractJavaSourceVisitor<D extends Descriptor> extends VoidVisitorAdapter<D> {

    protected VisitorHelper visitorHelper;

    public AbstractJavaSourceVisitor(VisitorHelper visitorHelper) {
        this.visitorHelper = visitorHelper;
    }

    protected void setVisibility(Node nodeWithModifiers, Descriptor descriptor) {
        ((AccessModifierDescriptor) descriptor).setVisibility(getAccessSpecifier(((NodeWithModifiers<?>) nodeWithModifiers).getModifiers()).getValue());
    }

    protected void setAccessModifier(Node nodeWithModifiers, Descriptor descriptor) {
        // TODO further modifiers
        if (nodeWithModifiers instanceof NodeWithAbstractModifier) {
            ((AbstractDescriptor) descriptor).setAbstract(((NodeWithAbstractModifier<?>) nodeWithModifiers).isAbstract());
        }
        if (nodeWithModifiers instanceof NodeWithFinalModifier) {
            ((AccessModifierDescriptor) descriptor).setFinal(((NodeWithFinalModifier<?>) nodeWithModifiers).isFinal());
        }
        if (nodeWithModifiers instanceof NodeWithStaticModifier) {
            ((AccessModifierDescriptor) descriptor).setStatic(((NodeWithStaticModifier<?>) nodeWithModifiers).isStatic());
        }
    }

    protected void setAnnotations(Node nodeWithAnnotations, AnnotatedDescriptor annotatedDescriptor) {
        for (AnnotationExpr annotation : ((NodeWithAnnotations<?>) nodeWithAnnotations).getAnnotations()) {
            annotation.accept(new AnnotationVisitor(visitorHelper), annotatedDescriptor);
        }
    }

    protected void setTypeParameterDependency(ClassOrInterfaceType classOrInterfaceType, TypeDescriptor typeDescriptor) {
        classOrInterfaceType.getTypeArguments().ifPresent(parameters -> {
            for (Type type : parameters) {
                visitorHelper.resolveDependency(visitorHelper.getQualifiedName(type), typeDescriptor);
            }

        });
    }

    protected ValueDescriptor<?> createValueDescriptor(String name, Expression value, TypeDescriptor typeDescriptor) {
        if (value.isLiteralExpr()) {
            PrimitiveValueDescriptor primitiveValueDescriptor = visitorHelper.getValueDescriptor(PrimitiveValueDescriptor.class);
            primitiveValueDescriptor.setName(name);
            primitiveValueDescriptor.setValue(getLiteralExpressionValue(value));
            return primitiveValueDescriptor;
        } else if (value.isClassExpr()) {
            ClassValueDescriptor classValueDescriptor = visitorHelper.getValueDescriptor(ClassValueDescriptor.class);
            classValueDescriptor.setName(name);
            classValueDescriptor.setValue(visitorHelper.resolveDependency(visitorHelper.getQualifiedName(value.asClassExpr().getType()), typeDescriptor));
            return classValueDescriptor;
        } else if (value.isArrayInitializerExpr()) {
            ArrayValueDescriptor arrayValueDescriptor = visitorHelper.getValueDescriptor(ArrayValueDescriptor.class);
            arrayValueDescriptor.setName(name);
            int i = 0;
            for (Expression arrayValue : value.asArrayInitializerExpr().getValues()) {
                arrayValueDescriptor.getValue().add(createValueDescriptor(("[" + i + "]"), arrayValue, typeDescriptor));
                i++;
            }
            return arrayValueDescriptor;
        } else if (value.isFieldAccessExpr()) {
            FieldAccessExpr fieldAccessExpr = value.asFieldAccessExpr();
            EnumValueDescriptor enumValueDescriptor = visitorHelper.getValueDescriptor(EnumValueDescriptor.class);
            enumValueDescriptor.setName(name);
            TypeDescriptor parent = visitorHelper.resolveDependency(visitorHelper.getQualifiedName(fieldAccessExpr), typeDescriptor);
            FieldDescriptor fieldDescriptor = visitorHelper.getFieldDescriptor(getFieldSignature(fieldAccessExpr), parent);
            enumValueDescriptor.setValue(fieldDescriptor);
            return enumValueDescriptor;
        } else if (value.isSingleMemberAnnotationExpr()) {
            SingleMemberAnnotationExpr singleMemberAnnotationExpr = value.asSingleMemberAnnotationExpr();
            AnnotationValueDescriptor annotationValueDescriptor = visitorHelper
                    .getAnnotationValueDescriptor(visitorHelper.getQualifiedName(singleMemberAnnotationExpr), name, null);
            // annotationValueDescriptor.setName(name);
            annotationValueDescriptor.getValue()
                    .add(createValueDescriptor(visitorHelper.SINGLE_MEMBER_ANNOTATION_NAME, singleMemberAnnotationExpr.getMemberValue(), typeDescriptor));
            return annotationValueDescriptor;
        } else if (value.isNameExpr()) {
            NameExpr nameExpr = value.asNameExpr();
            PrimitiveValueDescriptor primitiveValueDescriptor = visitorHelper.getValueDescriptor(PrimitiveValueDescriptor.class);
            primitiveValueDescriptor.setName(name);
            primitiveValueDescriptor.setValue(value.toString());
            return primitiveValueDescriptor;
        } else
            throw new RuntimeException("Type of annotation value is not supported: " + name + " " + value.getClass());
    }

    /**
     * Returns the VisibilityModifier for an EnumSet<Modifier> from java parser.
     *
     * @param modifiers
     * @return VisibilityModifier
     */
    private VisibilityModifier getAccessSpecifier(EnumSet<Modifier> modifiers) {
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
     * Returns the value of a literal expression as Object.
     *
     * @param expression
     * @return value as Object
     */
    protected Object getLiteralExpressionValue(Expression expression) {
        if (expression.isBooleanLiteralExpr()) {
            return expression.asBooleanLiteralExpr().getValue();
        } else if (expression.isDoubleLiteralExpr()) {
            return Double.parseDouble(expression.asDoubleLiteralExpr().getValue());
        } else if (expression.isLongLiteralExpr()) {
            return Long.parseLong(expression.asLongLiteralExpr().getValue());
        } else if (expression.isIntegerLiteralExpr()) {
            return Integer.parseInt(expression.asIntegerLiteralExpr().getValue());
        } else if (expression.isCharLiteralExpr()) {
            return expression.asCharLiteralExpr().getValue();
        } else if (expression.isStringLiteralExpr()) {
            return expression.asStringLiteralExpr().getValue();
        } else if (expression.isLiteralStringValueExpr()) {
            return expression.asLiteralStringValueExpr().getValue();
        } else if (expression.isNullLiteralExpr()) {
            return null;
        } else {
            throw new IllegalArgumentException("Expression value could not be resolved: " + expression.toString());
        }
    }

    protected String getFieldSignature(FieldAccessExpr fieldAccessExpr) {
        return visitorHelper.getQualifiedName(fieldAccessExpr) + " " + fieldAccessExpr.getNameAsString();
    }

}