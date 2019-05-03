package org.jqassistant.contrib.plugin.javasrc.impl.scanner.visitor;

import com.buschmais.jqassistant.core.store.api.model.Descriptor;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.nodeTypes.NodeWithModifiers;
import com.github.javaparser.ast.nodeTypes.modifiers.NodeWithAbstractModifier;
import com.github.javaparser.ast.nodeTypes.modifiers.NodeWithFinalModifier;
import com.github.javaparser.ast.nodeTypes.modifiers.NodeWithStaticModifier;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.*;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.resolution.types.ResolvedWildcard;
import com.github.javaparser.symbolsolver.core.resolution.Context;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFactory;
import com.github.javaparser.symbolsolver.model.resolution.SymbolReference;
import org.jqassistant.contrib.plugin.javasrc.api.model.*;
import org.jqassistant.contrib.plugin.javasrc.impl.scanner.JavaSourceException;

import java.util.Optional;

/**
 * This abstract visitor contains all common fields and methods of the other
 * visitors.
 *
 * @param <D> The descriptor.
 * @author Richard Mueller
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
                getQualifiedName(type).ifPresent(qualifiedParameterTypeName -> {
                    visitorHelper.resolveDependency(qualifiedParameterTypeName, typeDescriptor);
                });
            }
        });
    }

    protected VisibilityModifier getAccessSpecifier(NodeList<Modifier> modifiers) {
        if (modifiers.contains(Modifier.publicModifier())) {
            return VisibilityModifier.PUBLIC;
        } else if (modifiers.contains(Modifier.protectedModifier())) {
            return VisibilityModifier.PROTECTED;
        } else if (modifiers.contains(Modifier.privateModifier())) {
            return VisibilityModifier.PRIVATE;
        } else {
            return VisibilityModifier.DEFAULT;
        }
    }

    protected Object getLiteralExpressionValue(Expression expression) throws JavaSourceException {
        if (expression.isBooleanLiteralExpr()) {
            return expression.asBooleanLiteralExpr().getValue();
        } else if (expression.isDoubleLiteralExpr()) {
            // remove d to avoid NumberFormatException
            return Double.parseDouble(expression.asDoubleLiteralExpr().getValue().replace("d", ""));
        } else if (expression.isLongLiteralExpr()) {
            // remove L or l to avoid NumberFormatException
            String longLiteralExpr = expression.asLongLiteralExpr().getValue();
            if (longLiteralExpr.contains("L")) {
                return Long.parseLong(longLiteralExpr.replace("L", ""));
            } else if (longLiteralExpr.contains("l")) {
                return Long.parseLong(longLiteralExpr.replace("l", ""));
            } else {
                return Long.parseLong(expression.asLongLiteralExpr().getValue());
            }
        } else if (expression.isIntegerLiteralExpr()) {
            String integerLiteralExpr = expression.asIntegerLiteralExpr().getValue();
            if (integerLiteralExpr.startsWith("0x") || integerLiteralExpr.startsWith("0X")) {
                return Integer.decode(expression.asIntegerLiteralExpr().getValue());
            }
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
        } else if (expression.isArrayInitializerExpr()) {
            return expression.asArrayInitializerExpr().getValues().toString();
        }
        throw new JavaSourceException("Unexpected expression value: " + expression + " " + expression.getClass());
    }

    protected Optional<String> getQualifiedName(Node node) throws JavaSourceException {
        try {
            if (node instanceof TypeDeclaration<?>) {
                // types such as class, enum, or annotation declaration
                return Optional.of(visitorHelper.getFacade().getTypeDeclaration(node).getQualifiedName());
            } else if (node instanceof Type) {
                // interfaces, super class, parameter types, exceptions
                return Optional.of(getQualifiedName(visitorHelper.getFacade().convertToUsage(((Type) node), node)));
            } else if (node instanceof FieldAccessExpr) {
                // field e.g. in annotations
                FieldAccessExpr fieldAccessExpr = (FieldAccessExpr) node;
                SymbolReference<ResolvedValueDeclaration> symbolReference = visitorHelper.getFacade().solve(fieldAccessExpr);
                if (symbolReference.isSolved()) {
                    return Optional.of(getQualifiedName(symbolReference.getCorrespondingDeclaration().getType()));
                } else {
                    throw new UnsolvedSymbolException("Unsolved qualified name of field type.");
                }
            } else if (node instanceof AnnotationExpr) {
                // annotations
                AnnotationExpr annotationExpr = (AnnotationExpr) node;
                Context context = JavaParserFactory.getContext(annotationExpr, visitorHelper.getTypeSolver());
                SymbolReference<ResolvedTypeDeclaration> symbolReference = context.solveType(annotationExpr.getNameAsString());
                if (symbolReference.isSolved()) {
                    return Optional.of(symbolReference.getCorrespondingDeclaration().getQualifiedName());
                } else {
                    throw new UnsolvedSymbolException("Unsolved qualified name of annotation.");
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
                    return Optional.of(solvedInvokedMethod.declaringType().getQualifiedName());
                } else {
                    throw new UnsolvedSymbolException("Unsolved qualified name of method call.");
                }
            } else if (node instanceof VariableDeclarator) {
                // method variable
                ResolvedType solvedVariable = visitorHelper.getFacade().convertToUsageVariableType((VariableDeclarator) node);
                return Optional.of(getQualifiedName(solvedVariable));
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

    protected Optional<String> getQualifiedSignature(Node node) throws JavaSourceException {
        try {
            if (node instanceof MethodDeclaration) {
                // method signature
                ResolvedMethodDeclaration solvedMethod = ((MethodDeclaration) node).resolve();
                return Optional.of(getQualifiedName(solvedMethod.getReturnType()) + " " + solvedMethod.getSignature());
            } else if (node instanceof ConstructorDeclaration) {
                // constructor signature
                ResolvedConstructorDeclaration solvedConstructor = ((ConstructorDeclaration) node).resolve();
                return Optional.of(visitorHelper.CONSTRUCTOR_SIGNATURE + solvedConstructor.getSignature().replaceAll(solvedConstructor.getName(), ""));
            } else if (node instanceof AnnotationMemberDeclaration) {
                // annotation member signature
                return Optional
                    .of(getQualifiedName(((AnnotationMemberDeclaration) node).getType().resolve()) + " " + visitorHelper.ANNOTATION_MEMBER_SIGNATURE);
            } else if (node instanceof FieldDeclaration) {
                // field signature
                FieldDeclaration fieldDeclaration = ((FieldDeclaration) node);
                return Optional.of(getQualifiedName(fieldDeclaration.getVariable(0).getType().resolve()) + " " + fieldDeclaration.getVariable(0).getName());
            } else if (node instanceof EnumConstantDeclaration) {
                // enum signature
                EnumConstantDeclaration enumConstantDeclaration = ((EnumConstantDeclaration) node);
                ResolvedEnumConstantDeclaration solvedEnum = enumConstantDeclaration.resolve();
                return Optional.of(getQualifiedName(solvedEnum.getType()) + " " + enumConstantDeclaration.getName());
            } else if (node instanceof FieldAccessExpr) {
                // field signature
                FieldAccessExpr fieldAccessExpr = (FieldAccessExpr) node;
                SymbolReference<ResolvedValueDeclaration> symbolReference = visitorHelper.getFacade().solve(fieldAccessExpr);
                if (symbolReference.isSolved()) {
                    return Optional.of(getQualifiedName(symbolReference.getCorrespondingDeclaration().getType()) + " " + fieldAccessExpr.getNameAsString());
                } else {
                    throw new UnsolvedSymbolException("Unsolved qualified signature of field type.");
                }
            } else if (node instanceof MethodCallExpr) {
                // method calls
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
                    return Optional.of(getQualifiedName(solvedInvokedMethod.getReturnType()) + " " + solvedInvokedMethod.getSignature());
                } else {
                    throw new UnsolvedSymbolException("Unsolved qualified signature of method call.");
                }
            } else if (node instanceof NameExpr) {
                // field write, field read
                ResolvedValueDeclaration solvedValueDeclaration = (ResolvedValueDeclaration) visitorHelper.getFacade().solve((NameExpr) node)
                    .getCorrespondingDeclaration();
                if (solvedValueDeclaration.isField()) {
                    return Optional.of(getQualifiedName(solvedValueDeclaration.getType()) + " " + solvedValueDeclaration.getName());
                } else {
                    return Optional.empty();
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

    private String getQualifiedName(ResolvedType resolvedType) throws JavaSourceException {
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
}
