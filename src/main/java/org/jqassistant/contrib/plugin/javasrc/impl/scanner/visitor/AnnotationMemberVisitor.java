package org.jqassistant.contrib.plugin.javasrc.impl.scanner.visitor;

import com.buschmais.jqassistant.core.store.api.model.Descriptor;
import com.buschmais.jqassistant.plugin.common.api.model.ArrayValueDescriptor;
import com.buschmais.jqassistant.plugin.common.api.model.ValueDescriptor;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.AnnotationMemberDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithModifiers;
import com.github.javaparser.ast.nodeTypes.modifiers.NodeWithAbstractModifier;
import com.github.javaparser.ast.nodeTypes.modifiers.NodeWithFinalModifier;
import com.github.javaparser.ast.nodeTypes.modifiers.NodeWithStaticModifier;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import org.jqassistant.contrib.plugin.javasrc.api.model.AbstractDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.AccessModifierDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.AnnotatedDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.AnnotationValueDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.ClassValueDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.EnumValueDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.FieldDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.MethodDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.PrimitiveValueDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.TypeDescriptor;
import org.jqassistant.contrib.plugin.javasrc.impl.scanner.TypeResolver;
import org.jqassistant.contrib.plugin.javasrc.impl.scanner.TypeResolverUtils;

/**
 * This visitor handles annotation members and creates a corresponding
 * descriptor.
 * 
 * @author Richard Müller
 *
 */
public class AnnotationMemberVisitor extends VoidVisitorAdapter<AnnotatedDescriptor> {
    private TypeResolver typeResolver;

    public AnnotationMemberVisitor(TypeResolver typeResolver) {
        this.typeResolver = typeResolver;
    }

    @Override
    public void visit(AnnotationMemberDeclaration annotationMemberDeclaration, AnnotatedDescriptor annotatedDescriptor) {
        // annotation member
        MethodDescriptor methodDescriptor = createAnnotationMember(annotationMemberDeclaration, annotatedDescriptor);
        setVisibility(annotationMemberDeclaration, methodDescriptor);
        setAccessModifier(annotationMemberDeclaration, methodDescriptor);
        setAnnotationMemberDefaultValue(annotationMemberDeclaration, methodDescriptor);
    }

    private void setAnnotationMemberDefaultValue(AnnotationMemberDeclaration annotationMemberDeclaration, MethodDescriptor methodDescriptor) {
        annotationMemberDeclaration.getDefaultValue().ifPresent(value -> {
            methodDescriptor
                    .setHasDefault(createValueDescriptor(TypeResolverUtils.ANNOTATION_MEMBER_DEFAULT_VALUE_NAME, value, methodDescriptor.getDeclaringType()));
        });

    }

    private MethodDescriptor createAnnotationMember(AnnotationMemberDeclaration annotationMemberDeclaration, AnnotatedDescriptor parent) {
        MethodDescriptor methodDescriptor = typeResolver.getMethodDescriptor(TypeResolverUtils.getAnnotationMemberSignature(annotationMemberDeclaration),
                (TypeDescriptor) parent);
        // name must be overwritten here as it is not in the signature
        methodDescriptor.setName(annotationMemberDeclaration.getNameAsString());
        return methodDescriptor;
    }

    private void setVisibility(Node nodeWithModifiers, Descriptor descriptor) {
        ((AccessModifierDescriptor) descriptor)
                .setVisibility(TypeResolverUtils.getAccessSpecifier(((NodeWithModifiers<?>) nodeWithModifiers).getModifiers()).getValue());
    }

    private void setAccessModifier(Node nodeWithModifiers, Descriptor descriptor) {
        // TODO further modifiers
        if (nodeWithModifiers instanceof NodeWithAbstractModifier) {
            ((AbstractDescriptor) descriptor).setAbstract(((NodeWithAbstractModifier<?>) nodeWithModifiers).isAbstract());
        }
        if (nodeWithModifiers instanceof NodeWithFinalModifier) {
            ((AccessModifierDescriptor) descriptor).setFinal(((NodeWithFinalModifier<?>) nodeWithModifiers).isFinal());
        }
        if (nodeWithModifiers instanceof NodeWithStaticModifier) {
            ((AccessModifierDescriptor) descriptor).setStatic(((NodeWithStaticModifier<?>) nodeWithModifiers).isStatic());
        }
    }

    private ValueDescriptor<?> createValueDescriptor(String name, Expression value, TypeDescriptor typeDescriptor) {
        if (value.isLiteralExpr()) {
            PrimitiveValueDescriptor primitiveValueDescriptor = typeResolver.getValueDescriptor(PrimitiveValueDescriptor.class);
            primitiveValueDescriptor.setName(name);
            primitiveValueDescriptor.setValue(TypeResolverUtils.getLiteralExpressionValue(value));
            return primitiveValueDescriptor;
        } else if (value.isClassExpr()) {
            ClassValueDescriptor classValueDescriptor = typeResolver.getValueDescriptor(ClassValueDescriptor.class);
            classValueDescriptor.setName(name);
            classValueDescriptor.setValue(typeResolver.resolveType(value.asClassExpr().getType().resolve().asReferenceType().getQualifiedName()));
            return classValueDescriptor;
        } else if (value.isArrayInitializerExpr()) {
            ArrayValueDescriptor arrayValueDescriptor = typeResolver.getValueDescriptor(ArrayValueDescriptor.class);
            arrayValueDescriptor.setName(name);
            int i = 0;
            for (Expression arrayValue : value.asArrayInitializerExpr().getValues()) {
                arrayValueDescriptor.getValue().add(createValueDescriptor(("[" + i + "]"), arrayValue, typeDescriptor));
                i++;
            }
            return arrayValueDescriptor;
        } else if (value.isFieldAccessExpr()) {
            EnumValueDescriptor enumValueDescriptor = typeResolver.getValueDescriptor(EnumValueDescriptor.class);
            enumValueDescriptor.setName(name);
            ResolvedFieldDeclaration resolvedFieldDeclaration = typeResolver.solve(value.asFieldAccessExpr()).getCorrespondingDeclaration();
            TypeDescriptor enumType = typeResolver.resolveDependency(resolvedFieldDeclaration.getType().asReferenceType().getQualifiedName(),
                    ((TypeDescriptor) typeDescriptor));
            FieldDescriptor fieldDescriptor = typeResolver.getFieldDescriptor(TypeResolverUtils.getFieldSignature(resolvedFieldDeclaration), enumType);
            enumValueDescriptor.setValue(fieldDescriptor);
            return enumValueDescriptor;
        } else if (value.isSingleMemberAnnotationExpr()) {
            SingleMemberAnnotationExpr singleMemberAnnotationExpr = value.asSingleMemberAnnotationExpr();
            AnnotationValueDescriptor annotationValueDescriptor = typeResolver.addAnnotationValueDescriptor(singleMemberAnnotationExpr, null);
            annotationValueDescriptor.setName(name);
            annotationValueDescriptor.getValue()
                    .add(createValueDescriptor(TypeResolverUtils.SINGLE_MEMBER_ANNOTATION_NAME, singleMemberAnnotationExpr.getMemberValue(), typeDescriptor));
            return annotationValueDescriptor;
        } else if (value.isNameExpr()) {
            NameExpr nameExpr = value.asNameExpr();
            PrimitiveValueDescriptor primitiveValueDescriptor = typeResolver.getValueDescriptor(PrimitiveValueDescriptor.class);
            primitiveValueDescriptor.setName(name);
            primitiveValueDescriptor.setValue(value.toString());
            return primitiveValueDescriptor;
        } else
            throw new RuntimeException("Type of annotation value is not supported: " + name + " " + value.getClass());
    }
}
