/**
 * 
 */
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
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import org.jqassistant.contrib.plugin.javasrc.api.model.ClassTypeDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.FieldDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.MethodDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.TypeDescriptor;

/**
 * This visitor handles parsed method invocations, anonymous inner classes,
 * field reads, and field writes and creates corresponding descriptors.
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
        try {
            setInvokes(methodCallExpr, methodDescriptor);
        } catch (UnsupportedOperationException ue) {
            System.out.println("Unresolved call: " + methodCallExpr + " type: " + ue.getClass());
        } catch (RuntimeException re) {
            System.out.println("Unresolved call: " + methodCallExpr + " type: " + re.getClass());
        }

        super.visit(methodCallExpr, methodDescriptor);
    }

    @Override
    public void visit(ObjectCreationExpr objectCreationExpr, MethodDescriptor methodDescriptor) {
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
        super.visit(objectCreationExpr, methodDescriptor);
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
            FieldDescriptor fieldDescriptor = visitorHelper.getFieldDescriptor(getFieldSignature(fieldAccessExpr), methodDescriptor.getDeclaringType());
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
            FieldDescriptor fieldDescriptor = visitorHelper.getFieldDescriptor(getFieldSignature(fieldAccessExpr), methodDescriptor.getDeclaringType());
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
}
