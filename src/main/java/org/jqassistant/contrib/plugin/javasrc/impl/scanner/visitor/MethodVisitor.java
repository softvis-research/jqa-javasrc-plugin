package org.jqassistant.contrib.plugin.javasrc.impl.scanner.visitor;

import java.util.List;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.AnnotationMemberDeclaration;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.BinaryExpr.Operator;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithBlockStmt;
import com.github.javaparser.ast.nodeTypes.NodeWithOptionalBlockStmt;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.BreakStmt;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.ContinueStmt;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.SwitchEntryStmt;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.type.Type;
import org.jqassistant.contrib.plugin.javasrc.api.model.AnnotatedDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.MethodDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.ParameterDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.TypeDescriptor;

/**
 * This visitor handles parsed methods, i.e. methods, constructors, and
 * annotation members and creates corresponding descriptors.
 * 
 * @author Richard Müller
 *
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
        setInvokes(methodDeclaration);
        if (methodDeclaration.isTypeDeclaration()) {
            System.out.println(methodDeclaration);
        }

        super.visit(methodDeclaration, typeDescriptor);
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
        setInvokes(constructorDeclaration);

        super.visit(constructorDeclaration, typeDescriptor);
    }

    @Override
    public void visit(AnnotationMemberDeclaration annotationMemberDeclaration, TypeDescriptor typeDescriptor) {
        // annotation member
        createMethod(annotationMemberDeclaration, typeDescriptor);
        // name must be overwritten here as it is not in the signature
        ((MethodDescriptor) descriptor).setName(annotationMemberDeclaration.getNameAsString());
        setVisibility(annotationMemberDeclaration);
        setAccessModifier(annotationMemberDeclaration);
        setAnnotationMemberDefaultValue(annotationMemberDeclaration);
    }

    private void createMethod(BodyDeclaration<?> bodyDeclaration, TypeDescriptor parent) {
        // bodyDeclaration.getParentNode().ifPresent(parentNode -> {
        // if (!(parentNode instanceof ObjectCreationExpr)) {
        descriptor = visitorHelper.getMethodDescriptor(getMethodSignature(bodyDeclaration), parent);
        // }
        // });

    }

    private void setParamters(CallableDeclaration<?> callableDeclaration) {
        List<Parameter> parameters = ((CallableDeclaration<?>) callableDeclaration).getParameters();
        for (int i = 0; i < parameters.size(); i++) {
            TypeDescriptor parameterTypeDescriptor = visitorHelper.resolveDependency(visitorHelper.getQualifiedName(parameters.get(i).getType()),
                    ((MethodDescriptor) descriptor).getDeclaringType());
            ParameterDescriptor parameterDescriptor = visitorHelper.getParameterDescriptor(((MethodDescriptor) descriptor), i);
            parameterDescriptor.setType(parameterTypeDescriptor);
            if (parameters.get(i).getType().isClassOrInterfaceType()) {
                // TODO are there other types?
                setTypeParameterDependency(parameters.get(i).getType().asClassOrInterfaceType(), ((MethodDescriptor) descriptor).getDeclaringType());
            }
            setAnnotations(parameters.get(i), parameterDescriptor);
        }
    }

    private void setReturnType(MethodDeclaration methodDeclaration) {
        Type returnType = methodDeclaration.getType();
        ((MethodDescriptor) descriptor)
                .setReturns(visitorHelper.resolveDependency(visitorHelper.getQualifiedName(returnType), ((MethodDescriptor) descriptor).getDeclaringType()));
        if (returnType.isClassOrInterfaceType()) {
            // TODO are there other types?
            setTypeParameterDependency(returnType.asClassOrInterfaceType(), ((MethodDescriptor) descriptor).getDeclaringType());
        }
    }

    private void setExceptions(CallableDeclaration<?> callableDeclaration) {
        callableDeclaration.getThrownExceptions().forEach(exception -> {
            ((MethodDescriptor) descriptor).getDeclaredThrowables()
                    .add(visitorHelper.resolveDependency(visitorHelper.getQualifiedName(exception), ((MethodDescriptor) descriptor).getDeclaringType()));
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

    private void setInvokes(NodeWithBlockStmt<?> nodeWithOptionalBlockStmt) {
        BlockStmt body = nodeWithOptionalBlockStmt.getBody();
        if (body != null) {
            body.accept(new MethodBodyVisitor(visitorHelper), ((MethodDescriptor) descriptor));
        }
    }

    private void setInvokes(NodeWithOptionalBlockStmt<?> nodeWithOptionalBlockStmt) {
        nodeWithOptionalBlockStmt.getBody().ifPresent(body -> body.accept(new MethodBodyVisitor(visitorHelper), ((MethodDescriptor) descriptor)));
    }

    private void setAnnotationMemberDefaultValue(AnnotationMemberDeclaration annotationMemberDeclaration) {
        annotationMemberDeclaration.getDefaultValue().ifPresent(value -> {
            ((MethodDescriptor) descriptor).setHasDefault(
                    createValueDescriptor(visitorHelper.ANNOTATION_MEMBER_DEFAULT_VALUE_NAME, value, ((MethodDescriptor) descriptor).getDeclaringType()));
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
     * @param node
     * @return cyclomatic complexity
     */
    private int calculateCyclomaticComplexity(Node node) {
        // TODO ForEachStmt?
        int complexity = 1;
        // if
        for (IfStmt ifStmt : node.findAll(IfStmt.class)) {
            complexity++;
        }
        // case
        for (SwitchStmt switchStmt : node.findAll(SwitchStmt.class)) {
            for (SwitchEntryStmt switchEntryStmt : switchStmt.getEntries()) {
                // filter case as default has no label
                if (switchEntryStmt.getLabel().isPresent()) {
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

    private String getMethodSignature(BodyDeclaration<?> bodyDeclaration) throws IllegalArgumentException {
        if (bodyDeclaration.isMethodDeclaration()) {
            MethodDeclaration methodDeclaration = bodyDeclaration.asMethodDeclaration();
            return visitorHelper.getQualifiedName(methodDeclaration.getType()) + " "
                    + visitorHelper.getQualifiedSignature(methodDeclaration.getNameAsString(), methodDeclaration.getParameters());
        } else if (bodyDeclaration.isConstructorDeclaration()) {
            ConstructorDeclaration constructorDeclaration = bodyDeclaration.asConstructorDeclaration();
            return visitorHelper.getQualifiedSignature(visitorHelper.CONSTRUCTOR_SIGNATURE, constructorDeclaration.getParameters());
        } else if (bodyDeclaration.isAnnotationMemberDeclaration()) {
            return visitorHelper.getQualifiedName(bodyDeclaration.asAnnotationMemberDeclaration().getType()) + " " + visitorHelper.ANNOTATION_MEMBER_SIGNATURE;
        } else {
            throw new IllegalArgumentException("Method signature could not be create for: " + bodyDeclaration.toString());
        }
    }
}
