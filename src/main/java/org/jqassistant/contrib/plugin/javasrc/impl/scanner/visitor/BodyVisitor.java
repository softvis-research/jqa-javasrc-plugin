/**
 * 
 */
package org.jqassistant.contrib.plugin.javasrc.impl.scanner.visitor;

import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import org.jqassistant.contrib.plugin.javasrc.api.model.FieldDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.MethodDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.TypeDescriptor;
import org.jqassistant.contrib.plugin.javasrc.impl.scanner.TypeResolver;
import org.jqassistant.contrib.plugin.javasrc.impl.scanner.TypeResolverUtils;

/**
 * This visitor handles parsed method invocations, field reads, and field writes
 * and creates corresponding descriptors.
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
        setInvokes(methodCallExpr, methodDescriptor);

        super.visit(methodCallExpr, methodDescriptor);
    }

    @Override
    public void visit(AssignExpr assignExpr, MethodDescriptor methodDescriptor) {
        setWrites(assignExpr, methodDescriptor);

        super.visit(assignExpr, methodDescriptor);
    }

    @Override
    public void visit(FieldAccessExpr fieldAccessExpr, MethodDescriptor methodDescriptor) {
        setReads(fieldAccessExpr, methodDescriptor);

        super.visit(fieldAccessExpr, methodDescriptor);
    }

    @Override
    public void visit(NameExpr nameExpr, MethodDescriptor methodDescriptor) {
        setReads(nameExpr, methodDescriptor);

        super.visit(nameExpr, methodDescriptor);
    }

    private void setInvokes(MethodCallExpr methodCallExpr, MethodDescriptor methodDescriptor) {
        ResolvedMethodDeclaration resolvedInvokedMethodDeclaration = methodCallExpr.resolveInvokedMethod();
        TypeDescriptor invokedMethodParent = typeResolver.resolveType(resolvedInvokedMethodDeclaration.declaringType().getQualifiedName());
        MethodDescriptor invokedMethodDescriptor = typeResolver.getMethodDescriptor(TypeResolverUtils.getMethodSignature(resolvedInvokedMethodDeclaration),
                invokedMethodParent);
        methodCallExpr.getBegin().ifPresent((position) -> typeResolver.addInvokes(methodDescriptor, position.line, invokedMethodDescriptor));
    }

    private void setWrites(AssignExpr assignExpr, MethodDescriptor methodDescriptor) {
        Expression target = assignExpr.getTarget();
        if (target.isFieldAccessExpr()) {
            // this.FIELD = VALUE;
            ResolvedFieldDeclaration resolvedFieldDeclaration = typeResolver.solve(target.asFieldAccessExpr()).getCorrespondingDeclaration();
            FieldDescriptor fieldDescriptor = typeResolver.getFieldDescriptor(TypeResolverUtils.getFieldSignature(resolvedFieldDeclaration),
                    methodDescriptor.getDeclaringType());
            assignExpr.getBegin().ifPresent((position) -> typeResolver.addWrites(methodDescriptor, position.line, (FieldDescriptor) fieldDescriptor));
        } else if (target.isNameExpr()) {
            ResolvedValueDeclaration resolvedValueDeclaration = target.asNameExpr().resolve();
            if (resolvedValueDeclaration.isField()) {
                // FIELD = VALUE;
                FieldDescriptor fieldDescriptor = typeResolver.getFieldDescriptor(TypeResolverUtils.getFieldSignature(resolvedValueDeclaration.asField()),
                        methodDescriptor.getDeclaringType());
                assignExpr.getBegin().ifPresent((position) -> typeResolver.addWrites(methodDescriptor, position.line, (FieldDescriptor) fieldDescriptor));
            }
        }
    }

    private void setReads(Expression expression, MethodDescriptor methodDescriptor) {
        if (expression instanceof FieldAccessExpr) {
            // this.FIELD
            ResolvedFieldDeclaration resolvedFieldDeclaration = typeResolver.solve((FieldAccessExpr) expression).getCorrespondingDeclaration();
            FieldDescriptor fieldDescriptor = typeResolver.getFieldDescriptor(TypeResolverUtils.getFieldSignature(resolvedFieldDeclaration),
                    methodDescriptor.getDeclaringType());
            expression.getBegin().ifPresent((position) -> typeResolver.addReads(methodDescriptor, position.line, (FieldDescriptor) fieldDescriptor));
        } else if (expression instanceof NameExpr) {
            ResolvedValueDeclaration resolvedValueDeclaration = ((NameExpr) expression).resolve();
            if (resolvedValueDeclaration.isField()) {
                // FIELD
                FieldDescriptor fieldDescriptor = typeResolver.getFieldDescriptor(TypeResolverUtils.getFieldSignature(resolvedValueDeclaration.asField()),
                        methodDescriptor.getDeclaringType());
                expression.getBegin().ifPresent((position) -> typeResolver.addReads(methodDescriptor, position.line, (FieldDescriptor) fieldDescriptor));
            }
        }

    }
}
