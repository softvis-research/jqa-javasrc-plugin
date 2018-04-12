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
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.nodeTypes.NodeWithModifiers;
import com.github.javaparser.ast.nodeTypes.modifiers.NodeWithAbstractModifier;
import com.github.javaparser.ast.nodeTypes.modifiers.NodeWithFinalModifier;
import com.github.javaparser.ast.nodeTypes.modifiers.NodeWithStaticModifier;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.resolution.types.ResolvedWildcard;
import com.github.javaparser.symbolsolver.core.resolution.Context;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFactory;
import com.github.javaparser.symbolsolver.model.resolution.SymbolReference;
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
    protected Descriptor descriptor;

    public AbstractJavaSourceVisitor(VisitorHelper visitorHelper) {
        this.visitorHelper = visitorHelper;
    }

    protected void setVisibility(Node nodeWithModifiers) {
        ((AccessModifierDescriptor) descriptor).setVisibility(getAccessSpecifier(((NodeWithModifiers<?>) nodeWithModifiers).getModifiers()).getValue());
    }

    protected void setAccessModifier(Node nodeWithModifiers) {
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
                ResolvedType solvedType = visitorHelper.getFacade().convertToUsage(type, classOrInterfaceType);
                visitorHelper.resolveDependency(getQualifiedName(solvedType), typeDescriptor);
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
            classValueDescriptor.setValue(visitorHelper.resolveDependency(getQualifiedName(value.asClassExpr().getType().resolve()), typeDescriptor));
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
            TypeDescriptor parent = visitorHelper.resolveDependency(getQualifiedName(fieldAccessExpr), typeDescriptor);
            FieldDescriptor fieldDescriptor = visitorHelper.getFieldDescriptor(getFieldSignature(fieldAccessExpr), parent);
            enumValueDescriptor.setValue(fieldDescriptor);
            return enumValueDescriptor;
        } else if (value.isSingleMemberAnnotationExpr()) {
            SingleMemberAnnotationExpr singleMemberAnnotationExpr = value.asSingleMemberAnnotationExpr();
            AnnotationValueDescriptor annotationValueDescriptor = visitorHelper.getAnnotationValueDescriptor(getQualifiedName(singleMemberAnnotationExpr), name,
                    null);
            // TODO annotationValueDescriptor.setName(name);?
            annotationValueDescriptor.getValue()
                    .add(createValueDescriptor(visitorHelper.SINGLE_MEMBER_ANNOTATION_NAME, singleMemberAnnotationExpr.getMemberValue(), typeDescriptor));
            return annotationValueDescriptor;
        } else if (value.isNameExpr()) {
            NameExpr nameExpr = value.asNameExpr();
            PrimitiveValueDescriptor primitiveValueDescriptor = visitorHelper.getValueDescriptor(PrimitiveValueDescriptor.class);
            primitiveValueDescriptor.setName(name);
            primitiveValueDescriptor.setValue(value.toString());
            return primitiveValueDescriptor;
        } else if (value.isNormalAnnotationExpr()) {
            NormalAnnotationExpr normalAnnotationExpr = value.asNormalAnnotationExpr();
            AnnotationValueDescriptor annotationValueDescriptor = visitorHelper.getAnnotationValueDescriptor(getQualifiedName(normalAnnotationExpr), name,
                    null);
            for (MemberValuePair memberValuePair : normalAnnotationExpr.getPairs()) {
                annotationValueDescriptor.getValue()
                        .add(createValueDescriptor(memberValuePair.getNameAsString(), memberValuePair.getValue(), annotationValueDescriptor.getType()));
            }
            // TODO annotationValueDescriptor.setName(name);?
            return annotationValueDescriptor;
        } else
            throw new RuntimeException("Type of annotation value is not supported: " + name + " " + value.getClass());
    }

    /**
     * Returns the VisibilityModifier for an EnumSet<Modifier> from java parser.
     *
     * @param modifiers
     * @return VisibilityModifier
     */
    protected VisibilityModifier getAccessSpecifier(EnumSet<Modifier> modifiers) {
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
        return getQualifiedName(fieldAccessExpr) + " " + fieldAccessExpr.getNameAsString();
    }

    protected String getQualifiedName(AnnotationExpr annotationExpr) {
        Context context = JavaParserFactory.getContext(annotationExpr, visitorHelper.getTypeSolver());
        SymbolReference<ResolvedTypeDeclaration> symbolReference = context.solveType(annotationExpr.getNameAsString(), visitorHelper.getTypeSolver());
        if (symbolReference.isSolved()) {
            return symbolReference.getCorrespondingDeclaration().getQualifiedName();
        } else {
            // TODO show a warning
            return annotationExpr.getNameAsString();
        }
    }

    protected String getQualifiedName(FieldAccessExpr fieldAccessExpr) {
        SymbolReference<ResolvedFieldDeclaration> symbolReference = visitorHelper.getFacade().solve(fieldAccessExpr);
        if (symbolReference.isSolved()) {
            return getQualifiedName(symbolReference.getCorrespondingDeclaration().getType());
        } else {
            // TODO show a warning
            return fieldAccessExpr.getNameAsString();
        }
    }

    protected String getQualifiedName(ResolvedType resolvedType) {
        if (resolvedType.isReferenceType()) {
            return resolvedType.asReferenceType().getQualifiedName();
        } else if (resolvedType.isPrimitive()) {
            return resolvedType.asPrimitive().describe();
        } else if (resolvedType.isVoid()) {
            return resolvedType.describe();
        } else if (resolvedType.isArray()) {
            return resolvedType.asArrayType().describe();
        } else if (resolvedType.isTypeVariable()) {
            return resolvedType.asTypeVariable().qualifiedName();
        } else if (resolvedType.isWildcard()) {
            ResolvedWildcard wildcard = resolvedType.asWildcard();
            if (wildcard.isBounded()) {
                return wildcard.getBoundedType().describe();
            } else {
                return wildcard.describe();
            }
        } else {
            throw new IllegalArgumentException("Unexpected type of resolved type: " + resolvedType + " " + resolvedType.getClass());
        }
    }

}