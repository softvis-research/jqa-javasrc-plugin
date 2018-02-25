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
import com.github.javaparser.symbolsolver.model.resolution.SymbolReference;
import org.jqassistant.contrib.plugin.javasrc.api.model.FieldDescriptor;
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

    @Override
    public void visit(AssignExpr assignExpr, MethodDescriptor methodDescriptor) {
        super.visit(assignExpr, methodDescriptor);
        Expression target = assignExpr.getTarget();
        if (target.isFieldAccessExpr()) {
            // this.FIELD = VALUE;
            SymbolReference<ResolvedFieldDeclaration> resolvedFieldDeclaration = typeResolver.solve(target.asFieldAccessExpr());
            FieldDescriptor fieldDescriptor = typeResolver
                    .resolveField(TypeResolverUtils.getFieldSignature(resolvedFieldDeclaration.getCorrespondingDeclaration()));
            assignExpr.getBegin().ifPresent((position) -> typeResolver.addWrites(methodDescriptor, position.line, fieldDescriptor));
        } else if (target.isNameExpr()) {
            ResolvedValueDeclaration resolvedValueDeclaration = target.asNameExpr().resolve();
            if (resolvedValueDeclaration.isField()) {
                // FIELD = VALUE;
                FieldDescriptor fieldDescriptor = typeResolver.resolveField(TypeResolverUtils.getFieldSignature(resolvedValueDeclaration.asField()));
                assignExpr.getBegin().ifPresent((position) -> typeResolver.addWrites(methodDescriptor, position.line, fieldDescriptor));
            }
        }
    }

    @Override
    public void visit(FieldAccessExpr fieldAccessExpr, MethodDescriptor methodDescriptor) {
        super.visit(fieldAccessExpr, methodDescriptor);
        // this.FIELD
        SymbolReference<ResolvedFieldDeclaration> resolvedFieldDeclaration = typeResolver.solve(fieldAccessExpr);
        FieldDescriptor fieldDescriptor = typeResolver
                .resolveField(TypeResolverUtils.getFieldSignature(resolvedFieldDeclaration.getCorrespondingDeclaration()));
        fieldAccessExpr.getBegin().ifPresent((position) -> typeResolver.addReads(methodDescriptor, position.line, fieldDescriptor));
    }

    @Override
    public void visit(NameExpr nameExpr, MethodDescriptor methodDescriptor) {
        super.visit(nameExpr, methodDescriptor);
        ResolvedValueDeclaration resolvedValueDeclaration = nameExpr.resolve();

        if (resolvedValueDeclaration.isField()) {
            // FIELD
            ResolvedFieldDeclaration resolvedFieldDeclaration = resolvedValueDeclaration.asField();
            FieldDescriptor fieldDescriptor = typeResolver.resolveField(TypeResolverUtils.getFieldSignature(resolvedFieldDeclaration));
            nameExpr.getBegin().ifPresent((position) -> typeResolver.addReads(methodDescriptor, position.line, fieldDescriptor));
        }
    }

}
