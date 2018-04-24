package org.jqassistant.contrib.plugin.javasrc.impl.scanner.visitor;

import com.github.javaparser.Position;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import org.jqassistant.contrib.plugin.javasrc.api.model.FieldDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.MethodDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.TypeDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.VariableDescriptor;

/**
 * This visitor handles parsed method invocations, anonymous inner classes,
 * variables, field reads (parameter, return, and assign expression), and field
 * writes (assign expression) and creates corresponding descriptors.
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
        // field reads
        methodCallExpr.getArguments().forEach(argument -> {
            if (argument.isFieldAccessExpr()) {
                // method(this.field)
                argument.getBegin().ifPresent(position -> {
                    setReads(argument.asFieldAccessExpr(), methodDescriptor, position);
                });
            } else if (argument.isNameExpr()) {
                // method(field)
                argument.getBegin().ifPresent(position -> {
                    setReads(argument.asNameExpr(), methodDescriptor, position);
                });
            }
        });
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
        Expression target = assignExpr.getTarget();
        if (target.isFieldAccessExpr()) {
            // this.field = ...;
            assignExpr.getBegin().ifPresent(position -> {
                setWrites(target.asFieldAccessExpr(), methodDescriptor, position);
            });
        } else if (target.isNameExpr()) {
            // field = ...;
            assignExpr.getBegin().ifPresent(position -> {
                setWrites(target.asNameExpr(), methodDescriptor, position);
            });
        }

        // field reads
        Expression value = assignExpr.getValue();
        if (value.isFieldAccessExpr()) {
            // ... = this.field;
            assignExpr.getBegin().ifPresent(position -> {
                setReads(value.asFieldAccessExpr(), methodDescriptor, position);
            });
        } else if (value.isNameExpr()) {
            // ... = field;
            assignExpr.getBegin().ifPresent(position -> {
                setReads(value.asNameExpr(), methodDescriptor, position);
            });
        }
    }

    @Override
    public void visit(ReturnStmt returnStmt, MethodDescriptor methodDescriptor) {
        // field reads
        returnStmt.getExpression().ifPresent(returnExpression -> {
            returnStmt.getBegin().ifPresent(position -> {
                if (returnExpression.isFieldAccessExpr()) {
                    // return this.field;
                    setReads(returnExpression.asFieldAccessExpr(), methodDescriptor, position);
                } else if (returnExpression.isNameExpr()) {
                    // return field;
                    setReads(returnExpression.asNameExpr(), methodDescriptor, position);
                }
            });
        });
    }

    private void setInvokes(MethodCallExpr methodCallExpr, MethodDescriptor methodDescriptor) {
        getQualifiedName(methodCallExpr).ifPresent(qualifiedInvokedMethodParentName -> {
            TypeDescriptor invokedMethodParent = visitorHelper.resolveDependency(qualifiedInvokedMethodParentName, methodDescriptor.getDeclaringType());
            getQualifiedSignature(methodCallExpr).ifPresent(qualifiedMethodSignature -> {
                MethodDescriptor invokedMethodDescriptor = visitorHelper.getMethodDescriptor(qualifiedMethodSignature, invokedMethodParent);
                methodCallExpr.getBegin().ifPresent((position) -> {
                    visitorHelper.addInvokes(methodDescriptor, position.line, invokedMethodDescriptor);
                });
            });
        });
    }

    private void setVariables(VariableDeclarationExpr variableDeclarationExpr, MethodDescriptor methodDescriptor) {
        variableDeclarationExpr.getVariables().forEach(variable -> {
            getQualifiedName(variable).ifPresent(qualifiedVariableTypeName -> {
                VariableDescriptor variableDescriptor = visitorHelper.getVariableDescriptor(variable.getNameAsString(),
                        qualifiedVariableTypeName + " " + variable.getNameAsString());
                variableDescriptor.setType(visitorHelper.resolveDependency(qualifiedVariableTypeName, methodDescriptor.getDeclaringType()));
                methodDescriptor.getVariables().add(variableDescriptor);
                // type parameters
                if (variable.getType().isClassOrInterfaceType()) {
                    setTypeParameterDependency(variable.getType().asClassOrInterfaceType(), methodDescriptor.getDeclaringType());
                }
            });
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

    private void setWrites(Expression expression, MethodDescriptor methodDescriptor, Position position) {
        getQualifiedSignature(expression).ifPresent(qualifiedFieldSignature -> {
            // TODO methodDescriptor.getDeclaringType()? might be better to get
            // the parent of fieldAccessExpr?
            FieldDescriptor fieldDescriptor = visitorHelper.getFieldDescriptor(qualifiedFieldSignature, methodDescriptor.getDeclaringType());
            visitorHelper.addWrites(methodDescriptor, position.line, fieldDescriptor);
        });
    }

    private void setReads(Expression expression, MethodDescriptor methodDescriptor, Position position) {
        getQualifiedSignature(expression).ifPresent(qualifiedFieldSignature -> {
            // TODO methodDescriptor.getDeclaringType()? might be better to get
            // the parent of fieldAccessExpr?
            FieldDescriptor fieldDescriptor = visitorHelper.getFieldDescriptor(qualifiedFieldSignature, methodDescriptor.getDeclaringType());
            visitorHelper.addReads(methodDescriptor, position.line, fieldDescriptor);
        });
    }
}
