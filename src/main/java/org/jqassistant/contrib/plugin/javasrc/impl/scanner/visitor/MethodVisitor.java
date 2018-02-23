package org.jqassistant.contrib.plugin.javasrc.impl.scanner.visitor;

import java.util.List;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.declarations.ResolvedConstructorDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedParameterDeclaration;
import org.jqassistant.contrib.plugin.javasrc.api.model.ConstructorDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.JavaSourceFileDescriptor;
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
public class MethodVisitor extends VoidVisitorAdapter<JavaSourceFileDescriptor> {

    private TypeResolver typeResolver;

    public MethodVisitor(TypeResolver typeResolver) {
        this.typeResolver = typeResolver;
    }

    @Override
    public void visit(MethodDeclaration methodDeclaration, JavaSourceFileDescriptor javaSourceFileDescriptor) {
        super.visit(methodDeclaration, javaSourceFileDescriptor);

        // signature, name
        ResolvedMethodDeclaration resolvedMethodDeclaration = methodDeclaration.resolve();
        TypeDescriptor returnTypeDescriptor = typeResolver.resolveType(TypeResolverUtils.getQualifiedName(resolvedMethodDeclaration.getReturnType()));
        MethodDescriptor methodDescriptor = typeResolver.addMethodDescriptor(resolvedMethodDeclaration.declaringType().getQualifiedName(),
                TypeResolverUtils.getMethodSignature(resolvedMethodDeclaration));
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
            TypeDescriptor parameterTypeDescriptor = typeResolver.resolveType(TypeResolverUtils.getQualifiedName(resolvedParameterDeclaration.getType()));
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
    }

    @Override
    public void visit(ConstructorDeclaration constructorDeclaration, JavaSourceFileDescriptor javaSourceFileDescriptor) {
        super.visit(constructorDeclaration, javaSourceFileDescriptor);

        // enum constructors are currently not supported:
        // https://github.com/javaparser/javaparser/pull/1315
        Node parent = constructorDeclaration.getParentNode().get();
        if (!(parent instanceof EnumDeclaration)) {
            // signature, name
            ResolvedConstructorDeclaration resolvedConstructorDeclaration = constructorDeclaration.resolve();
            ConstructorDescriptor constructorDescriptor = (ConstructorDescriptor) typeResolver.addMethodDescriptor(
                    resolvedConstructorDeclaration.declaringType().getQualifiedName(),
                    TypeResolverUtils.getConstructorSignature(resolvedConstructorDeclaration));
            constructorDescriptor.setName(resolvedConstructorDeclaration.getName());

            // visibility
            constructorDescriptor.setVisibility(TypeResolverUtils.getAccessSpecifier(constructorDeclaration.getModifiers()).getValue());

            // parameters
            List<Parameter> parameters = constructorDeclaration.getParameters();
            for (int i = 0; i < parameters.size(); i++) {
                ResolvedParameterDeclaration resolvedParameterDeclaration = parameters.get(i).resolve();
                TypeDescriptor parameterTypeDescriptor = typeResolver.resolveType(TypeResolverUtils.getQualifiedName(resolvedParameterDeclaration.getType()));
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
        }
    }

    private void setInvokes(BlockStmt body, MethodDescriptor methodDescriptor) {
        body.accept(new BodyVisitor(typeResolver), methodDescriptor);
    }
}
