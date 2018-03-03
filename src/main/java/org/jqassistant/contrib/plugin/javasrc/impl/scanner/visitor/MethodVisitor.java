package org.jqassistant.contrib.plugin.javasrc.impl.scanner.visitor;

import java.util.List;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.AnnotationMemberDeclaration;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.nodeTypes.NodeWithBlockStmt;
import com.github.javaparser.ast.nodeTypes.NodeWithOptionalBlockStmt;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.SwitchEntryStmt;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.resolution.Resolvable;
import com.github.javaparser.resolution.declarations.ResolvedAnnotationMemberDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedConstructorDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodLikeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedParameterDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import org.jqassistant.contrib.plugin.javasrc.api.model.MethodDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.ParameterDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.TypeDescriptor;
import org.jqassistant.contrib.plugin.javasrc.impl.scanner.TypeResolver;
import org.jqassistant.contrib.plugin.javasrc.impl.scanner.TypeResolverUtils;

/**
 * This visitor handles parsed methods, i.e. methods, constructors, and
 * annotation members and creates corresponding descriptors.
 * 
 * @author Richard Müller
 *
 */
public class MethodVisitor extends AbstractJavaSourceVisitor<TypeDescriptor> {

    public MethodVisitor(TypeResolver typeResolver) {
        super(typeResolver);
    }

    @Override
    public void visit(MethodDeclaration methodDeclaration, TypeDescriptor typeDescriptor) {
        // method
        MethodDescriptor methodDescriptor = createMethod(methodDeclaration, typeDescriptor);
        setVisibility(methodDeclaration, methodDescriptor);
        setAccessModifier(methodDeclaration, methodDescriptor);
        setParamters(methodDeclaration, methodDescriptor);
        setReturnType(methodDeclaration, methodDescriptor);
        setAnnotations(methodDeclaration, methodDescriptor);
        setExceptions(methodDeclaration, methodDescriptor);
        setLineCount(methodDeclaration, methodDescriptor);
        setCyclomaticComplexity(methodDeclaration, methodDescriptor);
        setInvokes(methodDeclaration, methodDescriptor);

        super.visit(methodDeclaration, typeDescriptor);
    }

    @Override
    public void visit(ConstructorDeclaration constructorDeclaration, TypeDescriptor typeDescriptor) {
        // constructor
        MethodDescriptor methodDescriptor = createMethod(constructorDeclaration, typeDescriptor);
        setVisibility(constructorDeclaration, methodDescriptor);
        setParamters(constructorDeclaration, methodDescriptor);
        setAnnotations(constructorDeclaration, methodDescriptor);
        setExceptions(constructorDeclaration, methodDescriptor);
        setLineCount(constructorDeclaration, methodDescriptor);
        setCyclomaticComplexity(constructorDeclaration, methodDescriptor);
        setInvokes(constructorDeclaration, methodDescriptor);

        super.visit(constructorDeclaration, typeDescriptor);
    }

    @Override
    public void visit(AnnotationMemberDeclaration annotationMemberDeclaration, TypeDescriptor typeDescriptor) {
        // annotation member
        MethodDescriptor methodDescriptor = createMethod(annotationMemberDeclaration, typeDescriptor);
        setVisibility(annotationMemberDeclaration, methodDescriptor);
        setAccessModifier(annotationMemberDeclaration, methodDescriptor);
        setAnnotationMemberDefaultValue(annotationMemberDeclaration, methodDescriptor);
    }

    private MethodDescriptor createMethod(Resolvable<?> resolvable, TypeDescriptor parent) {
        Object method = resolvable.resolve();
        if (method instanceof ResolvedMethodDeclaration) {
            return typeResolver.getMethodDescriptor(TypeResolverUtils.getMethodSignature((ResolvedMethodDeclaration) method), parent);
        } else if (method instanceof ResolvedConstructorDeclaration) {
            return typeResolver.getMethodDescriptor(TypeResolverUtils.getMethodSignature((ResolvedConstructorDeclaration) method), parent);
        } else if (method instanceof ResolvedAnnotationMemberDeclaration) {
            // TODO ResolvedAnnotationMemberDeclaration.getType throws
            // java.lang.UnsupportedOperationException
            MethodDescriptor methodDescriptor = typeResolver.getMethodDescriptor(
                    TypeResolverUtils.getAnnotationMemberSignature(((AnnotationMemberDeclaration) resolvable).getType().resolve()), parent);
            // name must be overwritten here as it is not in the signature
            methodDescriptor.setName(((AnnotationMemberDeclaration) resolvable).getNameAsString());
            return methodDescriptor;
        } else {
            throw new RuntimeException("MethodDescriptor could not be created: " + resolvable + " " + resolvable.getClass());
        }
    }

    private void setParamters(CallableDeclaration<?> callableDeclaration, MethodDescriptor methodDescriptor) {
        List<Parameter> parameters = ((CallableDeclaration<?>) callableDeclaration).getParameters();
        for (int i = 0; i < parameters.size(); i++) {
            ResolvedParameterDeclaration resolvedParameterDeclaration = parameters.get(i).resolve();
            TypeDescriptor parameterTypeDescriptor = typeResolver.resolveDependency(TypeResolverUtils.getQualifiedName(resolvedParameterDeclaration.getType()),
                    methodDescriptor.getDeclaringType());
            ParameterDescriptor parameterDescriptor = typeResolver.getParameterDescriptor(methodDescriptor, i);
            parameterDescriptor.setType(parameterTypeDescriptor);
            if (resolvedParameterDeclaration.getType().isReferenceType()) {
                // TODO are there other types?
                setTypeParameterDependency(resolvedParameterDeclaration.getType().asReferenceType(), methodDescriptor.getDeclaringType());
            }
            setAnnotations(parameters.get(i), parameterDescriptor);
        }
    }

    private void setReturnType(MethodDeclaration methodDeclaration, MethodDescriptor methodDescriptor) {
        ResolvedType resolvedReturnType = methodDeclaration.resolve().getReturnType();
        methodDescriptor
                .setReturns(typeResolver.resolveDependency(TypeResolverUtils.getQualifiedName(resolvedReturnType), methodDescriptor.getDeclaringType()));
        if (resolvedReturnType.isReferenceType()) {
            // TODO are there other types?
            setTypeParameterDependency(resolvedReturnType.asReferenceType(), methodDescriptor.getDeclaringType());
        }
    }

    private void setExceptions(Resolvable<?> resolvable, MethodDescriptor methodDescriptor) {
        ResolvedMethodLikeDeclaration resolvedMethodLikeDeclaration = (ResolvedMethodLikeDeclaration) resolvable.resolve();
        for (ResolvedType exception : resolvedMethodLikeDeclaration.getSpecifiedExceptions()) {
            methodDescriptor.getDeclaredThrowables()
                    .add(typeResolver.resolveDependency(exception.asReferenceType().getQualifiedName(), methodDescriptor.getDeclaringType()));
        }
    }

    private void setLineCount(Node node, MethodDescriptor methodDescriptor) {
        node.getBegin().ifPresent(position -> {
            methodDescriptor.setFirstLineNumber(position.line);
        });
        node.getEnd().ifPresent(position -> {
            methodDescriptor.setLastLineNumber(position.line);
        });
        // TODO what is effective line count?
        methodDescriptor.setEffectiveLineCount(methodDescriptor.getLastLineNumber() - methodDescriptor.getFirstLineNumber() + 1);
    }

    private void setCyclomaticComplexity(Node node, MethodDescriptor methodDescriptor) {
        methodDescriptor.setCyclomaticComplexity(calculateCyclomaticComplexity(node));
    }

    private void setInvokes(NodeWithBlockStmt<?> nodeWithOptionalBlockStmt, MethodDescriptor methodDescriptor) {
        BlockStmt body = nodeWithOptionalBlockStmt.getBody();
        if (body != null) {
            body.accept(new MethodBodyVisitor(typeResolver), methodDescriptor);
        }
    }

    private void setInvokes(NodeWithOptionalBlockStmt<?> nodeWithOptionalBlockStmt, MethodDescriptor methodDescriptor) {
        nodeWithOptionalBlockStmt.getBody().ifPresent(body -> body.accept(new MethodBodyVisitor(typeResolver), methodDescriptor));
    }

    private int calculateCyclomaticComplexity(Node node) {
        int complexity = 0;
        for (IfStmt ifStmt : node.findAll(IfStmt.class)) {
            // increase complexity for "if"
            complexity++;
            if (ifStmt.getElseStmt().isPresent()) {
                // this "if" has an "else"
                Statement elseStmt = ifStmt.getElseStmt().get();
                if (elseStmt instanceof IfStmt) {
                    // it's an "else-if" that is already counted above
                } else {
                    // it's an "else-something"
                    complexity++;
                }
            }
        }
        for (SwitchStmt switchStmt : node.findAll(SwitchStmt.class)) {
            for (SwitchEntryStmt switchEntryStmt : switchStmt.getEntries()) {
                // increase complexity for each "case" and "default"
                complexity++;
            }
        }
        return (complexity == 0) ? 1 : complexity;
    }

    private void setAnnotationMemberDefaultValue(AnnotationMemberDeclaration annotationMemberDeclaration, MethodDescriptor methodDescriptor) {
        annotationMemberDeclaration.getDefaultValue().ifPresent(value -> {
            methodDescriptor
                    .setHasDefault(createValueDescriptor(TypeResolverUtils.ANNOTATION_MEMBER_DEFAULT_VALUE_NAME, value, methodDescriptor.getDeclaringType()));
        });

    }
}
