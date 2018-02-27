package org.jqassistant.contrib.plugin.javasrc.impl.scanner.visitor;

import java.util.List;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.SwitchEntryStmt;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.declarations.ResolvedConstructorDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedParameterDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import org.jqassistant.contrib.plugin.javasrc.api.model.ConstructorDescriptor;
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

    public MethodVisitor(TypeResolver typeResolver) {
        this.typeResolver = typeResolver;
    }

    @Override
    public void visit(MethodDeclaration methodDeclaration, TypeDescriptor typeDescriptor) {
        // super.visit(methodDeclaration, typeDescriptor);

        // signature, name
        ResolvedMethodDeclaration resolvedMethodDeclaration = methodDeclaration.resolve();
        TypeDescriptor returnTypeDescriptor = typeResolver.resolveDependency(TypeResolverUtils.getQualifiedName(resolvedMethodDeclaration.getReturnType()),
                typeDescriptor);
        MethodDescriptor methodDescriptor = typeResolver.addMethodDescriptor(TypeResolverUtils.getMethodSignature(resolvedMethodDeclaration), typeDescriptor);
        methodDescriptor.setName(resolvedMethodDeclaration.getName());

        // visibility and access modifiers
        methodDescriptor.setVisibility(TypeResolverUtils.getAccessSpecifier(methodDeclaration.getModifiers()).getValue());
        methodDescriptor.setAbstract(methodDeclaration.isAbstract());
        methodDescriptor.setFinal(methodDeclaration.isFinal());
        methodDescriptor.setStatic(methodDeclaration.isStatic());

        // parameters
        List<Parameter> parameters = methodDeclaration.getParameters();
        for (int i = 0; i < parameters.size(); i++) {
            ResolvedParameterDeclaration resolvedParameterDeclaration = parameters.get(i).resolve();
            TypeDescriptor parameterTypeDescriptor = typeResolver.resolveDependency(TypeResolverUtils.getQualifiedName(resolvedParameterDeclaration.getType()),
                    typeDescriptor);
            ParameterDescriptor parameterDescriptor = typeResolver.addParameterDescriptor(methodDescriptor, i);
            parameterDescriptor.setType(parameterTypeDescriptor);

            // annotations
            for (AnnotationExpr annotation : parameters.get(i).getAnnotations()) {
                annotation.accept(new AnnotationVisitor(typeResolver), parameterDescriptor);
            }
        }

        // return type
        methodDescriptor.setReturns(returnTypeDescriptor);

        // annotations
        for (AnnotationExpr annotation : methodDeclaration.getAnnotations()) {
            annotation.accept(new AnnotationVisitor(typeResolver), methodDescriptor);
        }

        // invokes
        methodDeclaration.getBody().ifPresent((body) -> setInvokes(body, methodDescriptor));

        // exceptions
        for (ResolvedType exception : resolvedMethodDeclaration.getSpecifiedExceptions()) {
            methodDescriptor.getDeclaredThrowables().add(typeResolver.resolveDependency(exception.asReferenceType().getQualifiedName(), typeDescriptor));
        }

        // loc
        methodDeclaration.getBegin().ifPresent(position -> {
            methodDescriptor.setFirstLineNumber(position.line);
        });
        methodDeclaration.getEnd().ifPresent(position -> {
            methodDescriptor.setLastLineNumber(position.line);
        });
        // TODO what is effective line count?
        methodDescriptor.setEffectiveLineCount(methodDescriptor.getLastLineNumber() - methodDescriptor.getFirstLineNumber() + 1);

        // complexity
        methodDescriptor.setCyclomaticComplexity(calculateCyclomaticComplexity(methodDeclaration));
    }

    @Override
    public void visit(ConstructorDeclaration constructorDeclaration, TypeDescriptor typeDescriptor) {
        // super.visit(constructorDeclaration, typeDescriptor);

        // enum constructors are currently not supported:
        // https://github.com/javaparser/javaparser/pull/1315
        Node parent = constructorDeclaration.getParentNode().get();
        if (!(parent instanceof EnumDeclaration)) {
            // signature, name
            ResolvedConstructorDeclaration resolvedConstructorDeclaration = constructorDeclaration.resolve();
            ConstructorDescriptor constructorDescriptor = (ConstructorDescriptor) typeResolver
                    .addMethodDescriptor(TypeResolverUtils.getMethodSignature(resolvedConstructorDeclaration), typeDescriptor);
            constructorDescriptor.setName(TypeResolverUtils.CONSTRUCTOR_NAME);

            // visibility
            constructorDescriptor.setVisibility(TypeResolverUtils.getAccessSpecifier(constructorDeclaration.getModifiers()).getValue());

            // parameters
            List<Parameter> parameters = constructorDeclaration.getParameters();
            for (int i = 0; i < parameters.size(); i++) {
                ResolvedParameterDeclaration resolvedParameterDeclaration = parameters.get(i).resolve();
                TypeDescriptor parameterTypeDescriptor = typeResolver
                        .resolveDependency(TypeResolverUtils.getQualifiedName(resolvedParameterDeclaration.getType()), typeDescriptor);
                ParameterDescriptor parameterDescriptor = typeResolver.addParameterDescriptor(constructorDescriptor, i);
                parameterDescriptor.setType(parameterTypeDescriptor);

                // annotations
                for (AnnotationExpr annotation : parameters.get(i).getAnnotations()) {
                    annotation.accept(new AnnotationVisitor(typeResolver), parameterDescriptor);
                }
            }

            // annotations
            for (AnnotationExpr annotation : constructorDeclaration.getAnnotations()) {
                annotation.accept(new AnnotationVisitor(typeResolver), constructorDescriptor);
            }

            // exceptions
            for (ResolvedType exception : resolvedConstructorDeclaration.getSpecifiedExceptions()) {
                constructorDescriptor.getDeclaredThrowables()
                        .add(typeResolver.resolveDependency(exception.asReferenceType().getQualifiedName(), typeDescriptor));
            }

            // loc
            constructorDeclaration.getBegin().ifPresent(position -> {
                constructorDescriptor.setFirstLineNumber(position.line);
            });
            constructorDeclaration.getEnd().ifPresent(position -> {
                constructorDescriptor.setLastLineNumber(position.line);
            });
            // TODO what is effective line count?
            constructorDescriptor.setEffectiveLineCount(constructorDescriptor.getLastLineNumber() - constructorDescriptor.getFirstLineNumber() + 1);

            // complexity
            constructorDescriptor.setCyclomaticComplexity(calculateCyclomaticComplexity(constructorDeclaration));
        }
    }

    private void setInvokes(BlockStmt body, MethodDescriptor methodDescriptor) {
        body.accept(new BodyVisitor(typeResolver), methodDescriptor);
    }

    private int calculateCyclomaticComplexity(Node methodOrConstructorDeclaration) {
        int complexity = 0;
        for (IfStmt ifStmt : methodOrConstructorDeclaration.findAll(IfStmt.class)) {
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
        for (SwitchStmt switchStmt : methodOrConstructorDeclaration.findAll(SwitchStmt.class)) {
            for (SwitchEntryStmt switchEntryStmt : switchStmt.getEntries()) {
                // increase complexity for each "case" and "default"
                complexity++;
            }
        }
        return (complexity == 0) ? 1 : complexity;
    }
}
