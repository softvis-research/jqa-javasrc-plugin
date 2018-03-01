package org.jqassistant.contrib.plugin.javasrc.impl.scanner.visitor;

import java.util.List;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.nodeTypes.NodeWithBlockStmt;
import com.github.javaparser.ast.nodeTypes.NodeWithModifiers;
import com.github.javaparser.ast.nodeTypes.NodeWithOptionalBlockStmt;
import com.github.javaparser.ast.nodeTypes.modifiers.NodeWithAbstractModifier;
import com.github.javaparser.ast.nodeTypes.modifiers.NodeWithFinalModifier;
import com.github.javaparser.ast.nodeTypes.modifiers.NodeWithStaticModifier;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.SwitchEntryStmt;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.declarations.ResolvedConstructorDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodLikeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedParameterDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import org.jqassistant.contrib.plugin.javasrc.api.model.AbstractDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.AccessModifierDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.AnnotatedDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.MethodDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.ParameterDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.TypeDescriptor;
import org.jqassistant.contrib.plugin.javasrc.impl.scanner.TypeResolver;
import org.jqassistant.contrib.plugin.javasrc.impl.scanner.TypeResolverUtils;

/**
 * This visitor handles parsed methods, i.e. methods and constructors, and
 * creates corresponding descriptors.
 * 
 * @author Richard Müller
 *
 */
public class MethodVisitor extends VoidVisitorAdapter<TypeDescriptor> {
    private TypeResolver typeResolver;
    private MethodDescriptor methodDescriptor;

    public MethodVisitor(TypeResolver typeResolver) {
        this.typeResolver = typeResolver;
    }

    @Override
    public void visit(MethodDeclaration methodDeclaration, TypeDescriptor typeDescriptor) {
        // method
        ResolvedMethodDeclaration resolvedMethodDeclaration = methodDeclaration.resolve();
        setMethod(resolvedMethodDeclaration, typeDescriptor);
        setVisibility(methodDeclaration);
        setAccessModifier(methodDeclaration);
        setParamters(methodDeclaration, typeDescriptor);
        setReturnType(methodDeclaration, typeDescriptor);
        setAnnotations(methodDeclaration, methodDescriptor);
        setExceptions(resolvedMethodDeclaration, typeDescriptor);
        setLineCount(methodDeclaration);
        setCyclomaticComplexity(methodDeclaration);
        setInvokes(methodDeclaration);

        super.visit(methodDeclaration, typeDescriptor);
    }

    @Override
    public void visit(ConstructorDeclaration constructorDeclaration, TypeDescriptor typeDescriptor) {
        // constructor
        ResolvedConstructorDeclaration resolvedConstructorDeclaration = constructorDeclaration.resolve();
        setMethod(resolvedConstructorDeclaration, typeDescriptor);
        setVisibility(constructorDeclaration);
        setParamters(constructorDeclaration, typeDescriptor);
        setAnnotations(constructorDeclaration, methodDescriptor);
        setExceptions(resolvedConstructorDeclaration, typeDescriptor);
        setLineCount(constructorDeclaration);
        setCyclomaticComplexity(constructorDeclaration);
        setInvokes(constructorDeclaration);

        super.visit(constructorDeclaration, typeDescriptor);
    }

    private void setMethod(ResolvedMethodLikeDeclaration resolvedMethodLikeDeclaration, TypeDescriptor parent) {
        if (resolvedMethodLikeDeclaration instanceof ResolvedMethodDeclaration) {
            methodDescriptor = typeResolver.getMethodDescriptor(TypeResolverUtils.getMethodSignature((ResolvedMethodDeclaration) resolvedMethodLikeDeclaration),
                    parent);
        } else if (resolvedMethodLikeDeclaration instanceof ResolvedConstructorDeclaration) {
            methodDescriptor = typeResolver
                    .getMethodDescriptor(TypeResolverUtils.getMethodSignature((ResolvedConstructorDeclaration) resolvedMethodLikeDeclaration), parent);
        }

    }

    private void setVisibility(Node nodeWithModifiers) {
        ((AccessModifierDescriptor) methodDescriptor)
                .setVisibility(TypeResolverUtils.getAccessSpecifier(((NodeWithModifiers<?>) nodeWithModifiers).getModifiers()).getValue());
    }

    private void setAccessModifier(Node nodeWithModifiers) {
        // TODO further modifiers
        if (nodeWithModifiers instanceof NodeWithAbstractModifier) {
            ((AbstractDescriptor) methodDescriptor).setAbstract(((NodeWithAbstractModifier<?>) nodeWithModifiers).isAbstract());
        }
        if (nodeWithModifiers instanceof NodeWithFinalModifier) {
            ((AccessModifierDescriptor) methodDescriptor).setFinal(((NodeWithFinalModifier<?>) nodeWithModifiers).isFinal());
        }
        if (nodeWithModifiers instanceof NodeWithStaticModifier) {
            ((AccessModifierDescriptor) methodDescriptor).setStatic(((NodeWithStaticModifier<?>) nodeWithModifiers).isStatic());
        }
    }

    private void setParamters(CallableDeclaration<?> callableDeclaration, TypeDescriptor typeDescriptor) {
        List<Parameter> parameters = ((CallableDeclaration<?>) callableDeclaration).getParameters();
        for (int i = 0; i < parameters.size(); i++) {
            ResolvedParameterDeclaration resolvedParameterDeclaration = parameters.get(i).resolve();
            TypeDescriptor parameterTypeDescriptor = typeResolver.resolveDependency(TypeResolverUtils.getQualifiedName(resolvedParameterDeclaration.getType()),
                    typeDescriptor);
            ParameterDescriptor parameterDescriptor = typeResolver.addParameterDescriptor(methodDescriptor, i);
            parameterDescriptor.setType(parameterTypeDescriptor);

            setAnnotations(parameters.get(i), parameterDescriptor);
        }
    }

    private void setReturnType(MethodDeclaration methodDeclaration, TypeDescriptor parent) {
        methodDescriptor.setReturns(typeResolver.resolveDependency(TypeResolverUtils.getQualifiedName(methodDeclaration.resolve().getReturnType()), parent));
    }

    private void setExceptions(ResolvedMethodLikeDeclaration resolvedMethodLikeDeclaration, TypeDescriptor parent) {
        for (ResolvedType exception : resolvedMethodLikeDeclaration.getSpecifiedExceptions()) {
            methodDescriptor.getDeclaredThrowables().add(typeResolver.resolveDependency(exception.asReferenceType().getQualifiedName(), parent));
        }
    }

    private void setLineCount(Node node) {
        node.getBegin().ifPresent(position -> {
            methodDescriptor.setFirstLineNumber(position.line);
        });
        node.getEnd().ifPresent(position -> {
            methodDescriptor.setLastLineNumber(position.line);
        });
        // TODO what is effective line count?
        methodDescriptor.setEffectiveLineCount(methodDescriptor.getLastLineNumber() - methodDescriptor.getFirstLineNumber() + 1);
    }

    private void setCyclomaticComplexity(Node node) {
        methodDescriptor.setCyclomaticComplexity(calculateCyclomaticComplexity(node));
    }

    private void setInvokes(NodeWithBlockStmt<?> nodeWithOptionalBlockStmt) {
        BlockStmt body = nodeWithOptionalBlockStmt.getBody();
        if (body != null) {
            body.accept(new BodyVisitor(typeResolver), methodDescriptor);
        }
    }

    private void setInvokes(NodeWithOptionalBlockStmt<?> nodeWithOptionalBlockStmt) {
        nodeWithOptionalBlockStmt.getBody().ifPresent(body -> body.accept(new BodyVisitor(typeResolver), methodDescriptor));
    }

    private void setAnnotations(NodeWithAnnotations<?> nodeWithAnnotations, AnnotatedDescriptor annotatedDescriptor) {
        for (AnnotationExpr annotation : nodeWithAnnotations.getAnnotations()) {
            annotation.accept(new AnnotationVisitor(typeResolver), annotatedDescriptor);
        }
    }

    private int calculateCyclomaticComplexity(Node node) {
        int complexity = 0;
        for (IfStmt ifStmt : node.findAll(IfStmt.class)) {
            // increase complexity for "if"
            complexity++;
            if (ifStmt.getElseStmt().isPresent()) {
                // this "if" has an "else"
                Statement elseStmt = ifStmt.getElseStmt().get();
                if (elseStmt instanceof IfStmt) {
                    // it's an "else-if" that is already counted above
                } else {
                    // it's an "else-something"
                    complexity++;
                }
            }
        }
        for (SwitchStmt switchStmt : node.findAll(SwitchStmt.class)) {
            for (SwitchEntryStmt switchEntryStmt : switchStmt.getEntries()) {
                // increase complexity for each "case" and "default"
                complexity++;
            }
        }
        return (complexity == 0) ? 1 : complexity;
    }
}
