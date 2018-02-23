/**
 * 
 */
package org.jqassistant.contrib.plugin.javasrc.impl.scanner.visitor;

import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import org.jqassistant.contrib.plugin.javasrc.api.model.MethodDescriptor;
import org.jqassistant.contrib.plugin.javasrc.impl.scanner.TypeResolver;
import org.jqassistant.contrib.plugin.javasrc.impl.scanner.TypeResolverUtils;

/**
 * This visitor handles parsed method bodies and creates corresponding
 * descriptors.
 * 
 * @author Richard Müller
 *
 */
public class BodyVisitor extends VoidVisitorAdapter<MethodDescriptor> {
    private TypeResolver typeResolver;

    public BodyVisitor(TypeResolver typeResolver) {
        this.typeResolver = typeResolver;
    }

    @Override
    public void visit(MethodCallExpr methodCallExpr, MethodDescriptor methodDescriptor) {
        super.visit(methodCallExpr, methodDescriptor);
        ResolvedMethodDeclaration resolvedInvokedMethodDeclaration = methodCallExpr.resolveInvokedMethod();
        MethodDescriptor invokedMethodDescriptor = typeResolver.addMethodDescriptor(resolvedInvokedMethodDeclaration.declaringType().getQualifiedName(),
                TypeResolverUtils.getMethodSignature(resolvedInvokedMethodDeclaration));
        methodCallExpr.getBegin().ifPresent((position) -> typeResolver.addInvokes(methodDescriptor, position.line, invokedMethodDescriptor));

    }
}
