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
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import org.jqassistant.contrib.plugin.javasrc.api.model.FieldDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.MethodDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.TypeDescriptor;
import org.jqassistant.contrib.plugin.javasrc.impl.scanner.TypeResolverUtils;

/**
 * This visitor handles parsed method invocations, field reads, and field writes
 * and creates corresponding descriptors.
 * 
 * @author Richard Müller
 *
 */
public class MethodBodyVisitor extends VoidVisitorAdapter<MethodDescriptor> {
    private VisitorHelper visitorHelper;

    public MethodBodyVisitor(VisitorHelper visitorHelper) {
        this.visitorHelper = visitorHelper;
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
        TypeDescriptor invokedMethodParent = visitorHelper.resolveDependency(resolvedInvokedMethodDeclaration.declaringType().getQualifiedName(),
                methodDescriptor.getDeclaringType());
        MethodDescriptor invokedMethodDescriptor = visitorHelper.getMethodDescriptor(TypeResolverUtils.getMethodSignature(resolvedInvokedMethodDeclaration),
                invokedMethodParent);
        methodCallExpr.getBegin().ifPresent((position) -> visitorHelper.addInvokes(methodDescriptor, position.line, invokedMethodDescriptor));
    }

    private void setWrites(AssignExpr assignExpr, MethodDescriptor methodDescriptor) {
        Expression target = assignExpr.getTarget();
        if (target.isFieldAccessExpr()) {
            // this.FIELD = VALUE;
            FieldAccessExpr fieldAccessExpr = target.asFieldAccessExpr();
            // TODO methodDescriptor.getDeclaringType()? might be better to get
            // the parent of fieldAccessExpr?
            FieldDescriptor fieldDescriptor = visitorHelper.getFieldDescriptor(
                    TypeResolverUtils.getFieldSignature(fieldAccessExpr.calculateResolvedType(), fieldAccessExpr.getNameAsString()),
                    methodDescriptor.getDeclaringType());
            assignExpr.getBegin().ifPresent((position) -> visitorHelper.addWrites(methodDescriptor, position.line, (FieldDescriptor) fieldDescriptor));
        } else if (target.isNameExpr()) {
            ResolvedValueDeclaration resolvedValueDeclaration = target.asNameExpr().resolve();
            if (resolvedValueDeclaration.isField()) {
                // FIELD = VALUE;
                FieldDescriptor fieldDescriptor = visitorHelper.getFieldDescriptor(TypeResolverUtils.getFieldSignature(resolvedValueDeclaration.asField()),
                        methodDescriptor.getDeclaringType());
                assignExpr.getBegin().ifPresent((position) -> visitorHelper.addWrites(methodDescriptor, position.line, (FieldDescriptor) fieldDescriptor));
            }
        }
    }

    private void setReads(Expression expression, MethodDescriptor methodDescriptor) {
        if (expression instanceof FieldAccessExpr) {
            // this.FIELD
            FieldAccessExpr fieldAccessExpr = expression.asFieldAccessExpr();
            // TODO methodDescriptor.getDeclaringType()? might be better to get
            // the parent of fieldAccessExpr?
            FieldDescriptor fieldDescriptor = visitorHelper.getFieldDescriptor(
                    TypeResolverUtils.getFieldSignature(fieldAccessExpr.calculateResolvedType(), fieldAccessExpr.getNameAsString()),
                    methodDescriptor.getDeclaringType());
            expression.getBegin().ifPresent((position) -> visitorHelper.addReads(methodDescriptor, position.line, (FieldDescriptor) fieldDescriptor));
        } else if (expression instanceof NameExpr) {
            ResolvedValueDeclaration resolvedValueDeclaration = ((NameExpr) expression).resolve();
            if (resolvedValueDeclaration.isField()) {
                // FIELD
                FieldDescriptor fieldDescriptor = visitorHelper.getFieldDescriptor(TypeResolverUtils.getFieldSignature(resolvedValueDeclaration.asField()),
                        methodDescriptor.getDeclaringType());
                expression.getBegin().ifPresent((position) -> visitorHelper.addReads(methodDescriptor, position.line, (FieldDescriptor) fieldDescriptor));
            }
        }

    }
}
