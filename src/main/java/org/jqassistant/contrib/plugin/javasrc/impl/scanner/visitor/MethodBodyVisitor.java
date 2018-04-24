package org.jqassistant.contrib.plugin.javasrc.impl.scanner.visitor;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import org.jqassistant.contrib.plugin.javasrc.api.model.FieldDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.MethodDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.TypeDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.VariableDescriptor;

/**
 * This visitor handles parsed method invocations, anonymous inner classes,
 * variables, field reads, and field writes and creates corresponding
 * descriptors.
 * 
 * @author Richard Mueller
 *
 */
public class MethodBodyVisitor extends AbstractJavaSourceVisitor<MethodDescriptor> {

    public MethodBodyVisitor(VisitorHelper visitorHelper) {
        super(visitorHelper);
    }

    @Override
    public void visit(MethodCallExpr methodCallExpr, MethodDescriptor methodDescriptor) throws UnsolvedSymbolException {
        // method calls
        setInvokes(methodCallExpr, methodDescriptor);
    }

    @Override
    public void visit(ObjectCreationExpr objectCreationExpr, MethodDescriptor methodDescriptor) {
        // anonymous class
        setAnonymousInnerClasses(objectCreationExpr, methodDescriptor);
    }

    @Override
    public void visit(VariableDeclarationExpr variableDeclarationExpr, MethodDescriptor methodDescriptor) {
        // method variables
        setVariables(variableDeclarationExpr, methodDescriptor);
    }

    @Override
    public void visit(AssignExpr assignExpr, MethodDescriptor methodDescriptor) {
        // field writes
        setWrites(assignExpr, methodDescriptor);
    }

    @Override
    public void visit(FieldAccessExpr fieldAccessExpr, MethodDescriptor methodDescriptor) {
        // field reads
        // setReads(fieldAccessExpr, methodDescriptor);
    }

    @Override
    public void visit(NameExpr nameExpr, MethodDescriptor methodDescriptor) {
        // field reads
        // setReads(nameExpr, methodDescriptor);
    }

    private void setInvokes(MethodCallExpr methodCallExpr, MethodDescriptor methodDescriptor) {
        TypeDescriptor invokedMethodParent = visitorHelper.resolveDependency(getQualifiedName(methodCallExpr), methodDescriptor.getDeclaringType());
        getQualifiedSignature(methodCallExpr).ifPresent(qualifiedMethodSignature -> {
            MethodDescriptor invokedMethodDescriptor = visitorHelper.getMethodDescriptor(qualifiedMethodSignature, invokedMethodParent);
            methodCallExpr.getBegin().ifPresent((position) -> {
                visitorHelper.addInvokes(methodDescriptor, position.line, invokedMethodDescriptor);
            });
        });
    }

    private void setVariables(VariableDeclarationExpr variableDeclarationExpr, MethodDescriptor methodDescriptor) {
        variableDeclarationExpr.getVariables().forEach(variable -> {
            VariableDescriptor variableDescriptor = visitorHelper.getVariableDescriptor(variable.getNameAsString(),
                    getQualifiedName(variable) + " " + variable.getNameAsString());
            variableDescriptor.setType(visitorHelper.resolveDependency(getQualifiedName(variable), methodDescriptor.getDeclaringType()));
            methodDescriptor.getVariables().add(variableDescriptor);
            // type parameters
            if (variable.getType().isClassOrInterfaceType()) {
                setTypeParameterDependency(variable.getType().asClassOrInterfaceType(), methodDescriptor.getDeclaringType());
            }
        });
    }

    private void setAnonymousInnerClasses(ObjectCreationExpr objectCreationExpr, MethodDescriptor methodDescriptor) {
        if (objectCreationExpr.getAnonymousClassBody().isPresent()) {
            TypeDescriptor anonymousInnerClass = visitorHelper.createAnonymousType(methodDescriptor);
            methodDescriptor.getDeclaredInnerClasses().add(anonymousInnerClass);
            methodDescriptor.getDeclaringType().getDeclaredInnerClasses().add(anonymousInnerClass);
            NodeList<BodyDeclaration<?>> bodyDeclarations = objectCreationExpr.getAnonymousClassBody().get();
            for (BodyDeclaration<?> bodyDeclaration : bodyDeclarations) {
                if (bodyDeclaration instanceof MethodDeclaration) {
                    bodyDeclaration.accept(new MethodVisitor(visitorHelper), anonymousInnerClass);
                }
                if (bodyDeclaration instanceof FieldDeclaration) {
                    bodyDeclaration.accept(new FieldVisitor(visitorHelper), anonymousInnerClass);
                }
            }
        }
    }

    private void setWrites(AssignExpr assignExpr, MethodDescriptor methodDescriptor) {
        Expression target = assignExpr.getTarget();
        if (target.isFieldAccessExpr()) {
            // this.FIELD = VALUE;
            FieldAccessExpr fieldAccessExpr = target.asFieldAccessExpr();
            // TODO methodDescriptor.getDeclaringType()? might be better to get
            // the parent of fieldAccessExpr?
            getQualifiedSignature(fieldAccessExpr).ifPresent(qualifiedFieldSignature -> {
                FieldDescriptor fieldDescriptor = visitorHelper.getFieldDescriptor(qualifiedFieldSignature, methodDescriptor.getDeclaringType());
                assignExpr.getBegin().ifPresent((position) -> {
                    visitorHelper.addWrites(methodDescriptor, position.line, fieldDescriptor);
                });
            });

        } else if (target.isNameExpr()) {
            // FIELD = VALUE;
            // TODO methodDescriptor.getDeclaringType()? might be better to get
            // the parent of fieldAccessExpr?
            getQualifiedSignature(target.asNameExpr()).ifPresent(qualifiedFieldSignature -> {
                FieldDescriptor fieldDescriptor = visitorHelper.getFieldDescriptor(qualifiedFieldSignature, methodDescriptor.getDeclaringType());
                assignExpr.getBegin().ifPresent((position) -> {
                    visitorHelper.addWrites(methodDescriptor, position.line, fieldDescriptor);
                });
            });
        }
    }

    private void setReads(Expression expression, MethodDescriptor methodDescriptor) {
        if (expression instanceof FieldAccessExpr) {
            // this.FIELD
            FieldAccessExpr fieldAccessExpr = expression.asFieldAccessExpr();
            // TODO methodDescriptor.getDeclaringType()? might be better to get
            // the parent of fieldAccessExpr?
            getQualifiedSignature(fieldAccessExpr).ifPresent(qualifiedFieldSignature -> {
                FieldDescriptor fieldDescriptor = visitorHelper.getFieldDescriptor(qualifiedFieldSignature, methodDescriptor.getDeclaringType());
                expression.getBegin().ifPresent((position) -> {
                    visitorHelper.addReads(methodDescriptor, position.line, fieldDescriptor);
                });
            });

        } else if (expression instanceof NameExpr) {
            // FIELD
            // TODO methodDescriptor.getDeclaringType()? might be better to get
            // the parent of fieldAccessExpr?
            getQualifiedSignature(expression.asNameExpr()).ifPresent(qualifiedFieldSignature -> {
                FieldDescriptor fieldDescriptor = visitorHelper.getFieldDescriptor(qualifiedFieldSignature, methodDescriptor.getDeclaringType());
                expression.getBegin().ifPresent((position) -> {
                    visitorHelper.addReads(methodDescriptor, position.line, fieldDescriptor);
                });
            });
        }
    }
}
