package org.jqassistant.contrib.plugin.javasrc.impl.scanner.visitor;

import com.buschmais.jqassistant.core.store.api.model.Descriptor;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.nodeTypes.NodeWithModifiers;
import com.github.javaparser.ast.nodeTypes.modifiers.NodeWithAbstractModifier;
import com.github.javaparser.ast.nodeTypes.modifiers.NodeWithFinalModifier;
import com.github.javaparser.ast.nodeTypes.modifiers.NodeWithStaticModifier;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.Resolvable;
import com.github.javaparser.resolution.declarations.ResolvedEnumConstantDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import org.jqassistant.contrib.plugin.javasrc.api.model.AbstractDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.AccessModifierDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.AnnotatedDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.FieldDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.PrimitiveValueDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.TypeDescriptor;
import org.jqassistant.contrib.plugin.javasrc.impl.scanner.TypeResolver;
import org.jqassistant.contrib.plugin.javasrc.impl.scanner.TypeResolverUtils;

/**
 * This visitor handles parsed fields and enum values and creates corresponding
 * descriptors.
 * 
 * @author Richard Müller
 *
 */
public class FieldVisitor extends VoidVisitorAdapter<TypeDescriptor> {
    private TypeResolver typeResolver;

    public FieldVisitor(TypeResolver typeResolver) {
        this.typeResolver = typeResolver;
    }

    @Override
    public void visit(FieldDeclaration fieldDeclaration, TypeDescriptor typeDescriptor) {
        // field
        FieldDescriptor fieldDescriptor = createField(fieldDeclaration, typeDescriptor);
        setVisibility(fieldDeclaration, fieldDescriptor);
        setAccessModifier(fieldDeclaration, fieldDescriptor);
        setFieldType(fieldDeclaration, fieldDescriptor);
        setFieldValue(fieldDeclaration, fieldDescriptor);
        setAnnotations(fieldDeclaration, fieldDescriptor);

        super.visit(fieldDeclaration, typeDescriptor);
    }

    @Override
    public void visit(EnumConstantDeclaration enumConstantDeclaration, TypeDescriptor typeDescriptor) {
        // enum values
        FieldDescriptor fieldDescriptor = createField(enumConstantDeclaration, typeDescriptor);
        setAnnotations(enumConstantDeclaration, fieldDescriptor);

        super.visit(enumConstantDeclaration, typeDescriptor);
    }

    private FieldDescriptor createField(Resolvable<?> resolvable, TypeDescriptor parent) {
        Object resolvedDeclaration = resolvable.resolve();
        if (resolvedDeclaration instanceof ResolvedFieldDeclaration) {
            return typeResolver.getFieldDescriptor(TypeResolverUtils.getFieldSignature((ResolvedFieldDeclaration) resolvedDeclaration), parent);
        } else if (resolvedDeclaration instanceof ResolvedEnumConstantDeclaration) {
            return typeResolver.getFieldDescriptor(TypeResolverUtils.getFieldSignature((ResolvedEnumConstantDeclaration) resolvedDeclaration), parent);
        } else {
            throw new RuntimeException("FieldDescriptor could not be created: " + resolvable + " " + resolvable.getClass());
        }
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

    private void setFieldType(FieldDeclaration fieldDeclaration, FieldDescriptor fieldDescriptor) {
        ResolvedFieldDeclaration resolvedFieldDeclaration = fieldDeclaration.resolve();
        TypeDescriptor fieldTypeDescriptor = typeResolver.resolveDependency(TypeResolverUtils.getQualifiedName(resolvedFieldDeclaration.getType()),
                fieldDescriptor.getDeclaringType());
        fieldDescriptor.setType(fieldTypeDescriptor);
    }

    private void setFieldValue(FieldDeclaration fieldDeclaration, FieldDescriptor fieldDescriptor) {
        // field value (of first variable)
        // TODO many variables for one field, type of values
        VariableDeclarator firstVariable = fieldDeclaration.getVariables().get(0);
        firstVariable.getInitializer().ifPresent(value -> {
            PrimitiveValueDescriptor valueDescriptor = typeResolver.getValueDescriptor(PrimitiveValueDescriptor.class);
            valueDescriptor.setValue(TypeResolverUtils.getLiteralExpressionValue(value));
            fieldDescriptor.setValue(valueDescriptor);
        });
    }

    private void setAnnotations(NodeWithAnnotations<?> nodeWithAnnotations, AnnotatedDescriptor annotatedDescriptor) {
        for (AnnotationExpr annotation : nodeWithAnnotations.getAnnotations()) {
            annotation.accept(new AnnotationVisitor(typeResolver), annotatedDescriptor);
        }
    }
}
