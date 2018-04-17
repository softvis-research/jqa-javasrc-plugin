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
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import org.jqassistant.contrib.plugin.javasrc.api.model.ClassTypeDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.FieldDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.MethodDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.TypeDescriptor;

/**
 * This visitor handles parsed method invocations, anonymous inner classes,
 * field reads, and field writes and creates corresponding descriptors. The call
 * of super is necessary because we need all expressions of the method body.
 * 
 * @author Richard Mueller
 *
 */
public class MethodBodyVisitor extends AbstractJavaSourceVisitor<MethodDescriptor> {
    private int anonymousInnerClassCounter = 0;

    public MethodBodyVisitor(VisitorHelper visitorHelper) {
        super(visitorHelper);
    }

    @Override
    public void visit(MethodCallExpr methodCallExpr, MethodDescriptor methodDescriptor) {
        super.visit(methodCallExpr, methodDescriptor);
        // method calls
        try {
            setInvokes(methodCallExpr, methodDescriptor);
        } catch (UnsupportedOperationException ue) {
            LOGGER.debug("Unresolved method call: " + methodCallExpr + " (" + ue.getClass() + ")");
        } catch (UnsolvedSymbolException use) {
            LOGGER.debug("Unresolved method call: " + methodCallExpr + " (" + use.getClass() + ")");
        } catch (RuntimeException re) {
            LOGGER.debug("Unresolved method call: " + methodCallExpr + " (" + re.getClass() + ")");
        }
    }

    @Override
    public void visit(ObjectCreationExpr objectCreationExpr, MethodDescriptor methodDescriptor) {
        super.visit(objectCreationExpr, methodDescriptor);
        // anonymous class
        if (objectCreationExpr.getAnonymousClassBody().isPresent()) {
            anonymousInnerClassCounter++;
            TypeDescriptor anonymousInnerClass = visitorHelper.createType(
                    methodDescriptor.getDeclaringType().getFullQualifiedName() + "$" + anonymousInnerClassCounter, visitorHelper.getJavaSourceFileDescriptor(),
                    ClassTypeDescriptor.class);
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

    @Override
    public void visit(AssignExpr assignExpr, MethodDescriptor methodDescriptor) {
        super.visit(assignExpr, methodDescriptor);
        // field writes
        setWrites(assignExpr, methodDescriptor);
    }

    @Override
    public void visit(FieldAccessExpr fieldAccessExpr, MethodDescriptor methodDescriptor) {
        super.visit(fieldAccessExpr, methodDescriptor);
        // field reads
        setReads(fieldAccessExpr, methodDescriptor);
    }

    @Override
    public void visit(NameExpr nameExpr, MethodDescriptor methodDescriptor) {
        // field reads
        // setReads(nameExpr, methodDescriptor);
        super.visit(nameExpr, methodDescriptor);
    }

    private void setInvokes(MethodCallExpr methodCallExpr, MethodDescriptor methodDescriptor) {
        ResolvedMethodDeclaration resolvedInvokedMethodDeclaration = methodCallExpr.resolveInvokedMethod();
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
            FieldDescriptor fieldDescriptor = visitorHelper.getFieldDescriptor(getQualifiedSignature(fieldAccessExpr), methodDescriptor.getDeclaringType());
            assignExpr.getBegin().ifPresent((position) -> {
                visitorHelper.addWrites(methodDescriptor, position.line, fieldDescriptor);
            });
        } else if (target.isNameExpr()) {
            // TODO extract in method
            ResolvedValueDeclaration resolvedValueDeclaration = (ResolvedValueDeclaration) visitorHelper.getFacade().solve(target.asNameExpr())
                    .getCorrespondingDeclaration();
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
            FieldDescriptor fieldDescriptor = visitorHelper.getFieldDescriptor(getQualifiedSignature(fieldAccessExpr), methodDescriptor.getDeclaringType());
            expression.getBegin().ifPresent((position) -> {
                visitorHelper.addReads(methodDescriptor, position.line, fieldDescriptor);
            });
        } else if (expression instanceof NameExpr) {
            // TODO extract in method
            ResolvedValueDeclaration resolvedValueDeclaration = (ResolvedValueDeclaration) visitorHelper.getFacade().solve((NameExpr) expression)
                    .getCorrespondingDeclaration();
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
        return getQualifiedName(resolvedMethodDeclaration.getReturnType()) + " " + resolvedMethodDeclaration.getSignature();
    }

    private String getFieldSignature(ResolvedFieldDeclaration resolvedFieldDeclaration) {
        return getQualifiedName(resolvedFieldDeclaration.getType()) + " " + resolvedFieldDeclaration.getName();
    }
}
