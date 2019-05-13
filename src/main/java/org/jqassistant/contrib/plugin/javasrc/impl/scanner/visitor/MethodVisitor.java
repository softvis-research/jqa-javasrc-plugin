package org.jqassistant.contrib.plugin.javasrc.impl.scanner.visitor;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.expr.BinaryExpr.Operator;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.Type;
import org.jqassistant.contrib.plugin.javasrc.api.model.AnnotatedDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.MethodDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.ParameterDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.TypeDescriptor;

import java.util.List;

/**
 * This visitor handles parsed methods and constructors and creates
 * corresponding descriptors.
 *
 * @author Richard Mueller
 */
public class MethodVisitor extends AbstractJavaSourceVisitor<TypeDescriptor> {

    public MethodVisitor(VisitorHelper visitorHelper) {
        super(visitorHelper);
    }

    @Override
    public void visit(MethodDeclaration methodDeclaration, TypeDescriptor typeDescriptor) {
        // method
        createMethod(methodDeclaration, typeDescriptor);
        setVisibility(methodDeclaration);
        setAccessModifier(methodDeclaration);
        setParamters(methodDeclaration);
        setReturnType(methodDeclaration);
        setAnnotations(methodDeclaration, (AnnotatedDescriptor) descriptor);
        setExceptions(methodDeclaration);
        setLineCount(methodDeclaration);
        setCyclomaticComplexity(methodDeclaration);
        setInvocations(methodDeclaration);
        setVariables(methodDeclaration);
        setWrites(methodDeclaration);
        setReads(methodDeclaration);
        setAnonymousClasses(methodDeclaration);
    }

    @Override
    public void visit(ConstructorDeclaration constructorDeclaration, TypeDescriptor typeDescriptor) {
        // constructor
        createMethod(constructorDeclaration, typeDescriptor);
        setVisibility(constructorDeclaration);
        setParamters(constructorDeclaration);
        setAnnotations(constructorDeclaration, (AnnotatedDescriptor) descriptor);
        setExceptions(constructorDeclaration);
        setLineCount(constructorDeclaration);
        setCyclomaticComplexity(constructorDeclaration);
        setInvocations(constructorDeclaration);
        setVariables(constructorDeclaration);
        setWrites(constructorDeclaration);
        setReads(constructorDeclaration);
        setAnonymousClasses(constructorDeclaration);
    }

    private void createMethod(BodyDeclaration<?> bodyDeclaration, TypeDescriptor parent) {
        getQualifiedSignature(bodyDeclaration).ifPresent(qualifiedMethodSignature -> {
            descriptor = visitorHelper.getMethodDescriptor(qualifiedMethodSignature, parent);
        });
    }

    private void setParamters(CallableDeclaration<?> callableDeclaration) {
        List<Parameter> parameters = ((CallableDeclaration<?>) callableDeclaration).getParameters();
        for (int i = 0; i < parameters.size(); i++) {
            final int position = i;
            getQualifiedName(parameters.get(position).getType()).ifPresent(qualifiedParameterName -> {
                TypeDescriptor parameterTypeDescriptor = visitorHelper.resolveDependency(qualifiedParameterName,
                    ((MethodDescriptor) descriptor).getDeclaringType());
                ParameterDescriptor parameterDescriptor = visitorHelper.getParameterDescriptor(((MethodDescriptor) descriptor), position);
                parameterDescriptor.setType(parameterTypeDescriptor);
                if (parameters.get(position).getType().isClassOrInterfaceType()) {
                    setTypeParameterDependency(parameters.get(position).getType().asClassOrInterfaceType(), ((MethodDescriptor) descriptor).getDeclaringType());
                }
                setAnnotations(parameters.get(position), parameterDescriptor);
            });
        }
    }

    private void setReturnType(MethodDeclaration methodDeclaration) {
        Type returnType = methodDeclaration.getType();
        getQualifiedName(returnType).ifPresent(qualifiedReturnType -> {
            ((MethodDescriptor) descriptor)
                .setReturns(visitorHelper.resolveDependency(qualifiedReturnType, ((MethodDescriptor) descriptor).getDeclaringType()));
            if (returnType.isClassOrInterfaceType()) {
                setTypeParameterDependency(returnType.asClassOrInterfaceType(), ((MethodDescriptor) descriptor).getDeclaringType());
            }
        });
    }

    private void setExceptions(CallableDeclaration<?> callableDeclaration) {
        callableDeclaration.getThrownExceptions().forEach(exception -> {
            getQualifiedName(exception).ifPresent(qualifiedExceptionName -> {
                ((MethodDescriptor) descriptor).getDeclaredThrowables()
                    .add(visitorHelper.resolveDependency(qualifiedExceptionName, ((MethodDescriptor) descriptor).getDeclaringType()));
            });
        });
    }

    private void setLineCount(Node node) {
        node.getBegin().ifPresent(position -> {
            ((MethodDescriptor) descriptor).setFirstLineNumber(position.line);
        });
        node.getEnd().ifPresent(position -> {
            ((MethodDescriptor) descriptor).setLastLineNumber(position.line);
        });
        // TODO what is effective line count?
        ((MethodDescriptor) descriptor)
            .setEffectiveLineCount(((MethodDescriptor) descriptor).getLastLineNumber() - ((MethodDescriptor) descriptor).getFirstLineNumber() + 1);
    }

    private void setCyclomaticComplexity(Node node) {
        ((MethodDescriptor) descriptor).setCyclomaticComplexity(calculateCyclomaticComplexity(node));
    }

    private void setInvocations(CallableDeclaration<?> callableDeclaration) {
        callableDeclaration.findAll(MethodCallExpr.class).forEach(methodCall -> {
            methodCall.accept(new MethodBodyVisitor(visitorHelper), (MethodDescriptor) descriptor);
        });
    }

    private void setWrites(CallableDeclaration<?> callableDeclaration) {
        callableDeclaration.findAll(AssignExpr.class).forEach(methodCall -> {
            methodCall.accept(new MethodBodyVisitor(visitorHelper), (MethodDescriptor) descriptor);
        });
    }

    private void setReads(CallableDeclaration<?> callableDeclaration) {
        callableDeclaration.findAll(ReturnStmt.class).forEach(methodCall -> {
            methodCall.accept(new MethodBodyVisitor(visitorHelper), (MethodDescriptor) descriptor);
        });
    }

    private void setAnonymousClasses(CallableDeclaration<?> callableDeclaration) {
        callableDeclaration.findAll(ObjectCreationExpr.class).forEach(methodCall -> {
            methodCall.accept(new MethodBodyVisitor(visitorHelper), (MethodDescriptor) descriptor);
        });
    }

    private void setVariables(CallableDeclaration<?> callableDeclaration) {
        callableDeclaration.findAll(VariableDeclarationExpr.class).forEach(variable -> {
            variable.accept(new MethodBodyVisitor(visitorHelper), (MethodDescriptor) descriptor);
        });
    }


    /**
     * Calculates the Cyclomatic Complexity of a method body (node) according to
     * these rules: methods have a base complexity of 1, +1 for every control
     * flow statement (if, case, catch, throw, do, while, for, break, continue)
     * and conditional expression (?:), +1 for every boolean operator (&&, ||),
     * else, finally and default don’t count. (Source:
     * http://pmd.sourceforge.net/snapshot/pmd_java_metrics_index.html#cyclomatic-complexity-cyclo)
     *
     * @param node The parsed node.
     * @return cyclomatic complexity
     */
    @SuppressWarnings("unused")
    private int calculateCyclomaticComplexity(Node node) {
        // TODO ForEachStmt?
        int complexity = 1;
        // if
        for (IfStmt ifStmt : node.findAll(IfStmt.class)) {
            complexity++;
        }
        // case
        for (SwitchStmt switchStmt : node.findAll(SwitchStmt.class)) {
            for (SwitchEntry switchEntry : switchStmt.getEntries()) {
                // filter case as default has no label
                // TODO check if this is still valid (formerly: switchEntryStmt.getLabel().isPresent())
                if (switchEntry.getLabels().isNonEmpty()) {
                    complexity++;
                }
            }
        }
        // catch
        for (CatchClause catchClause : node.findAll(CatchClause.class)) {
            complexity++;
        }
        // throw
        for (ThrowStmt throwStmt : node.findAll(ThrowStmt.class)) {
            complexity++;
        }
        // do
        for (DoStmt doStmt : node.findAll(DoStmt.class)) {
            complexity++;
        }
        // while
        for (WhileStmt whileStmt : node.findAll(WhileStmt.class)) {
            complexity++;
        }
        // for
        for (ForStmt forStmt : node.findAll(ForStmt.class)) {
            complexity++;
        }
        // break
        for (BreakStmt breakStmt : node.findAll(BreakStmt.class)) {
            complexity++;
        }
        // continue
        for (ContinueStmt continueStmt : node.findAll(ContinueStmt.class)) {
            complexity++;
        }
        // ?:
        for (ConditionalExpr conditionalExpr : node.findAll(ConditionalExpr.class)) {
            complexity++;
        }

        // &&, ||
        for (BinaryExpr binaryExpr : node.findAll(BinaryExpr.class)) {
            Operator operator = binaryExpr.getOperator();
            if (operator.equals(Operator.AND)) {
                complexity++;
            } else if (operator.equals(Operator.OR)) {
                complexity++;
            }
        }
        return complexity;
    }
}
