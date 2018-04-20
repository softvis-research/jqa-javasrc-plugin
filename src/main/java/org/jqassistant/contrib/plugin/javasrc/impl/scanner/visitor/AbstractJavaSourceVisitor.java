package org.jqassistant.contrib.plugin.javasrc.impl.scanner.visitor;

import java.util.EnumSet;

import com.buschmais.jqassistant.core.store.api.model.Descriptor;
import com.buschmais.jqassistant.plugin.common.api.model.ArrayValueDescriptor;
import com.buschmais.jqassistant.plugin.common.api.model.ValueDescriptor;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.AnnotationMemberDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.MethodCallExpr;
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
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedConstructorDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedEnumConstantDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
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
import org.jqassistant.contrib.plugin.javasrc.impl.scanner.JavaSourceException;

/**
 * This abstract visitor contains all common fields and methods of the other
 * visitors.
 * 
 * @author Richard Mueller
 *
 * @param <D>
 *            The descriptor.
 */
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
                visitorHelper.resolveDependency(getQualifiedName(type), typeDescriptor);
            }

        });
    }

    protected ValueDescriptor<?> createValueDescriptor(String name, Expression value, TypeDescriptor typeDescriptor) throws JavaSourceException {
        if (value.isLiteralExpr()) {
            PrimitiveValueDescriptor primitiveValueDescriptor = visitorHelper.getValueDescriptor(PrimitiveValueDescriptor.class);
            primitiveValueDescriptor.setName(name);
            primitiveValueDescriptor.setValue(getLiteralExpressionValue(value));
            return primitiveValueDescriptor;
        } else if (value.isClassExpr()) {
            ClassValueDescriptor classValueDescriptor = visitorHelper.getValueDescriptor(ClassValueDescriptor.class);
            classValueDescriptor.setName(name);
            classValueDescriptor.setValue(visitorHelper.resolveDependency(getQualifiedName(value.asClassExpr().getType()), typeDescriptor));
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
            FieldDescriptor fieldDescriptor = visitorHelper.getFieldDescriptor(getQualifiedSignature(fieldAccessExpr), parent);
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
            throw new JavaSourceException("Type of annotation value is not supported: " + name + " " + value.getClass());
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
    protected Object getLiteralExpressionValue(Expression expression) throws JavaSourceException {
        if (expression.isBooleanLiteralExpr()) {
            return expression.asBooleanLiteralExpr().getValue();
        } else if (expression.isDoubleLiteralExpr()) {
            // remove d to avoid NumberFormatException
            return Double.parseDouble(expression.asDoubleLiteralExpr().getValue().replace("d", ""));
        } else if (expression.isLongLiteralExpr()) {
            // remove L to avoid NumberFormatException
            return Long.parseLong(expression.asLongLiteralExpr().getValue().replace("L", ""));
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
        } else if (expression.isObjectCreationExpr()) {
            return expression.toString();
        } else if (expression.isMethodCallExpr()) {
            return expression.asMethodCallExpr().toString();
        } else if (expression.isFieldAccessExpr()) {
            return expression.asFieldAccessExpr().toString();
        }
        throw new JavaSourceException("Unexpected expression value: " + expression + " " + expression.getClass());
    }

    protected String getQualifiedName(ResolvedType resolvedType) throws JavaSourceException {
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
        }
        throw new JavaSourceException("Unexpected type of resolved type for qualified name: " + resolvedType.getClass());
    }

    protected String getQualifiedName(Node node) throws JavaSourceException {
        try {
            if (node instanceof TypeDeclaration<?>) {
                // types such as class, enum, or annotation declaration
                return visitorHelper.getFacade().getTypeDeclaration(node).getQualifiedName();
            } else if (node instanceof Type) {
                // interfaces, super class, parameter types, exceptions
                return getQualifiedName(visitorHelper.getFacade().convertToUsage(((Type) node), node));
            } else if (node instanceof FieldAccessExpr) {
                // field e.g. in annotations
                FieldAccessExpr fieldAccessExpr = (FieldAccessExpr) node;
                SymbolReference<ResolvedFieldDeclaration> symbolReference = visitorHelper.getFacade().solve(fieldAccessExpr);
                if (symbolReference.isSolved()) {
                    return getQualifiedName(symbolReference.getCorrespondingDeclaration().getType());
                } else {
                    // TODO show a warning
                    return fieldAccessExpr.getNameAsString();
                }
            } else if (node instanceof AnnotationExpr) {
                // TODO check type of annotation before!
                // annotations
                AnnotationExpr annotationExpr = (AnnotationExpr) node;
                // try {
                Context context = JavaParserFactory.getContext(annotationExpr, visitorHelper.getTypeSolver());
                SymbolReference<ResolvedTypeDeclaration> symbolReference = context.solveType(annotationExpr.getNameAsString(), visitorHelper.getTypeSolver());
                if (symbolReference.isSolved()) {
                    return symbolReference.getCorrespondingDeclaration().getQualifiedName();
                } else {
                    // TODO show a warning
                    return annotationExpr.getNameAsString();
                }
            } else if (node instanceof MethodCallExpr) {
                // method call
                MethodCallExpr methodCallExpr = (MethodCallExpr) node;
                SymbolReference<ResolvedMethodDeclaration> symbolReference = SymbolReference.unsolved(ResolvedMethodDeclaration.class);
                try {
                    symbolReference = visitorHelper.getFacade().solve(methodCallExpr);
                } catch (RuntimeException re) {
                    ResolvedMethodDeclaration resolvedInvokedMethodDeclaration = visitorHelper.getFacade().solveMethodAsUsage(methodCallExpr).getDeclaration();
                    if (resolvedInvokedMethodDeclaration != null) {
                        symbolReference = SymbolReference.solved(resolvedInvokedMethodDeclaration);
                    }
                }
                if (symbolReference.isSolved()) {
                    ResolvedMethodDeclaration solvedInvokedMethod = symbolReference.getCorrespondingDeclaration();
                    return solvedInvokedMethod.declaringType().getQualifiedName();
                } else {
                    // TODO show a warning
                    return methodCallExpr.getNameAsString();
                }
            } else if (node instanceof VariableDeclarator) {
                // method variable
                ResolvedType solvedVariable = visitorHelper.getFacade().convertToUsageVariableType((VariableDeclarator) node);
                return getQualifiedName(solvedVariable);
            }
            throw new JavaSourceException("Unexpected type of node for qualified name: " + node + " " + node.getClass());
        } catch (UnsolvedSymbolException use) {
            throw new JavaSourceException(
                    use.getClass().getSimpleName() + " " + use.getMessage() + "  Unsolved qualified name of node: " + node + " " + node.getClass());
        } catch (UnsupportedOperationException uoe) {
            throw new JavaSourceException(
                    uoe.getClass().getSimpleName() + " " + uoe.getMessage() + "  Unsolved qualified name of node: " + node + " " + node.getClass());
        } catch (RuntimeException re) {
            throw new JavaSourceException(
                    re.getClass().getSimpleName() + " " + re.getMessage() + "  Unsolved qualified name of node: " + node + " " + node.getClass());
        }
    }

    protected String getQualifiedSignature(Node node) throws JavaSourceException {
        try {
            if (node instanceof MethodDeclaration) {
                // method signature
                ResolvedMethodDeclaration solvedMethod = ((MethodDeclaration) node).resolve();
                return getQualifiedName(solvedMethod.getReturnType()) + " " + solvedMethod.getSignature();
            } else if (node instanceof ConstructorDeclaration) {
                // constructor signature
                ResolvedConstructorDeclaration solvedConstructor = ((ConstructorDeclaration) node).resolve();
                return visitorHelper.CONSTRUCTOR_SIGNATURE + solvedConstructor.getSignature().replaceAll(solvedConstructor.getName(), "");
            } else if (node instanceof AnnotationMemberDeclaration) {
                // annotation member signature
                return getQualifiedName(((AnnotationMemberDeclaration) node).getType().resolve()) + " " + visitorHelper.ANNOTATION_MEMBER_SIGNATURE;
            } else if (node instanceof FieldDeclaration) {
                // field signature
                FieldDeclaration fieldDeclaration = ((FieldDeclaration) node);
                return getQualifiedName(fieldDeclaration.getVariable(0).getType().resolve()) + " " + fieldDeclaration.getVariable(0).getName();
            } else if (node instanceof EnumConstantDeclaration) {
                // enum signature
                EnumConstantDeclaration enumConstantDeclaration = ((EnumConstantDeclaration) node);
                ResolvedEnumConstantDeclaration solvedEnum = enumConstantDeclaration.resolve();
                return getQualifiedName(solvedEnum.getType()) + " " + enumConstantDeclaration.getName();
            } else if (node instanceof FieldAccessExpr) {
                // field signature
                FieldAccessExpr fieldAccessExpr = (FieldAccessExpr) node;
                SymbolReference<ResolvedFieldDeclaration> symbolReference = visitorHelper.getFacade().solve(fieldAccessExpr);
                if (symbolReference.isSolved()) {
                    return getQualifiedName(symbolReference.getCorrespondingDeclaration().getType()) + " " + fieldAccessExpr.getNameAsString();
                } else {
                    // TODO show a warning
                    return fieldAccessExpr.getNameAsString();
                }
            } else if (node instanceof MethodCallExpr) {
                MethodCallExpr methodCallExpr = (MethodCallExpr) node;
                SymbolReference<ResolvedMethodDeclaration> symbolReference = SymbolReference.unsolved(ResolvedMethodDeclaration.class);
                try {
                    symbolReference = visitorHelper.getFacade().solve(methodCallExpr);
                } catch (RuntimeException re) {
                    ResolvedMethodDeclaration resolvedInvokedMethodDeclaration = visitorHelper.getFacade().solveMethodAsUsage(methodCallExpr).getDeclaration();
                    if (resolvedInvokedMethodDeclaration != null) {
                        symbolReference = SymbolReference.solved(resolvedInvokedMethodDeclaration);
                    }
                }
                if (symbolReference.isSolved()) {
                    ResolvedMethodDeclaration solvedInvokedMethod = symbolReference.getCorrespondingDeclaration();
                    return getQualifiedName(solvedInvokedMethod.getReturnType()) + " " + solvedInvokedMethod.getSignature();
                } else {
                    // TODO show a warning
                    return methodCallExpr.getNameAsString();
                }
            }
            throw new JavaSourceException("Unexpected type of node for qualified signature: " + node + " " + node.getClass());
        } catch (UnsolvedSymbolException use) {
            throw new JavaSourceException(
                    use.getClass().getSimpleName() + " " + use.getMessage() + "  Unsolved qualified signature of node: " + node + " " + node.getClass());
        } catch (UnsupportedOperationException uoe) {
            throw new JavaSourceException(
                    uoe.getClass().getSimpleName() + " " + uoe.getMessage() + "  Unsolved qualified signature of node: " + node + " " + node.getClass());
        } catch (RuntimeException re) {
            throw new JavaSourceException(
                    re.getClass().getSimpleName() + " " + re.getMessage() + "  Unsolved qualified signature of node: " + node + " " + node.getClass());
        }
    }
}