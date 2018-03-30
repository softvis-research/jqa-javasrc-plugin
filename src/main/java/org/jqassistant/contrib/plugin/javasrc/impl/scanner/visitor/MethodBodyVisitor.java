/**
 * 
 */
package org.jqassistant.contrib.plugin.javasrc.impl.scanner.visitor;

import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import org.jqassistant.contrib.plugin.javasrc.api.model.FieldDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.MethodDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.TypeDescriptor;

/**
 * This visitor handles parsed method invocations, field reads, and field writes
 * and creates corresponding descriptors.
 * 
 * @author Richard Müller
 *
 */
public class MethodBodyVisitor extends AbstractJavaSourceVisitor<MethodDescriptor> {

    public MethodBodyVisitor(VisitorHelper visitorHelper) {
        super(visitorHelper);
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
        ResolvedMethodDeclaration resolvedInvokedMethodDeclaration = (ResolvedMethodDeclaration) visitorHelper.solve(methodCallExpr);
        TypeDescriptor invokedMethodParent = visitorHelper.resolveDependency(resolvedInvokedMethodDeclaration.declaringType().getQualifiedName(),
                methodDescriptor.getDeclaringType());
        MethodDescriptor invokedMethodDescriptor = visitorHelper.getMethodDescriptor(getMethodSignature(resolvedInvokedMethodDeclaration), invokedMethodParent);
        methodCallExpr.getBegin().ifPresent((position) -> {
            visitorHelper.addInvokes(methodDescriptor, position.line, invokedMethodDescriptor);
        });

    }

    private void setWrites(AssignExpr assignExpr, MethodDescriptor methodDescriptor) {
        Expression target = assignExpr.getTarget();
        if (target.isFieldAccessExpr()) {
            // this.FIELD = VALUE;
            FieldAccessExpr fieldAccessExpr = target.asFieldAccessExpr();
            // TODO methodDescriptor.getDeclaringType()? might be better to get
            // the parent of fieldAccessExpr?
            FieldDescriptor fieldDescriptor = visitorHelper.getFieldDescriptor(getFieldSignature(fieldAccessExpr), methodDescriptor.getDeclaringType());
            assignExpr.getBegin().ifPresent((position) -> {
                visitorHelper.addWrites(methodDescriptor, position.line, fieldDescriptor);
            });
        } else if (target.isNameExpr()) {
            ResolvedValueDeclaration resolvedValueDeclaration = (ResolvedValueDeclaration) visitorHelper.solve(target.asNameExpr());
            if (resolvedValueDeclaration.isField()) {
                // FIELD = VALUE;
                FieldDescriptor fieldDescriptor = visitorHelper.getFieldDescriptor(getFieldSignature(resolvedValueDeclaration.asField()),
                        methodDescriptor.getDeclaringType());
                assignExpr.getBegin().ifPresent((position) -> {
                    visitorHelper.addWrites(methodDescriptor, position.line, fieldDescriptor);
                });
            }
        }
    }

    private void setReads(Expression expression, MethodDescriptor methodDescriptor) {
        if (expression instanceof FieldAccessExpr) {
            // this.FIELD
            FieldAccessExpr fieldAccessExpr = expression.asFieldAccessExpr();
            // TODO methodDescriptor.getDeclaringType()? might be better to get
            // the parent of fieldAccessExpr?
            FieldDescriptor fieldDescriptor = visitorHelper.getFieldDescriptor(getFieldSignature(fieldAccessExpr), methodDescriptor.getDeclaringType());
            expression.getBegin().ifPresent((position) -> {
                visitorHelper.addReads(methodDescriptor, position.line, fieldDescriptor);
            });
        } else if (expression instanceof NameExpr) {
            ResolvedValueDeclaration resolvedValueDeclaration = (ResolvedValueDeclaration) visitorHelper.solve((NameExpr) expression);
            if (resolvedValueDeclaration.isField()) {
                // FIELD
                FieldDescriptor fieldDescriptor = visitorHelper.getFieldDescriptor(getFieldSignature(resolvedValueDeclaration.asField()),
                        methodDescriptor.getDeclaringType());
                expression.getBegin().ifPresent((position) -> {
                    visitorHelper.addReads(methodDescriptor, position.line, fieldDescriptor);
                });
            }
        }

    }

    private String getMethodSignature(ResolvedMethodDeclaration resolvedMethodDeclaration) {
        return visitorHelper.getQualifiedName(resolvedMethodDeclaration.getReturnType()) + " " + resolvedMethodDeclaration.getSignature();
    }

    private String getFieldSignature(ResolvedFieldDeclaration resolvedFieldDeclaration) {
        return visitorHelper.getQualifiedName(resolvedFieldDeclaration.getType()) + " " + resolvedFieldDeclaration.getName();
    }
}
