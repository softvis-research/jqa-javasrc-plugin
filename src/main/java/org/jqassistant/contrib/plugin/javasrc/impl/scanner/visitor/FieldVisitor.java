package org.jqassistant.contrib.plugin.javasrc.impl.scanner.visitor;

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
import com.github.javaparser.resolution.declarations.ResolvedDeclaration;
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
    private FieldDescriptor fieldDescriptor;

    public FieldVisitor(TypeResolver typeResolver) {
        this.typeResolver = typeResolver;
    }

    @Override
    public void visit(FieldDeclaration fieldDeclaration, TypeDescriptor typeDescriptor) {
        // field
        ResolvedFieldDeclaration resolvedFieldDeclaration = fieldDeclaration.resolve();
        setField(resolvedFieldDeclaration, typeDescriptor);
        setVisibility(fieldDeclaration);
        setAccessModifier(fieldDeclaration);
        setFieldType(resolvedFieldDeclaration, typeDescriptor);
        setFieldValue(fieldDeclaration);
        setAnnotations(fieldDeclaration, fieldDescriptor);

        super.visit(fieldDeclaration, typeDescriptor);
    }

    @Override
    public void visit(EnumConstantDeclaration enumConstantDeclaration, TypeDescriptor typeDescriptor) {
        // enum values
        ResolvedEnumConstantDeclaration resolvedEnumConstantDeclaration = enumConstantDeclaration.resolve();
        setField(resolvedEnumConstantDeclaration, typeDescriptor);
        setAnnotations(enumConstantDeclaration, fieldDescriptor);

        super.visit(enumConstantDeclaration, typeDescriptor);
    }

    private void setField(ResolvedDeclaration resolvedDeclaration, TypeDescriptor parent) {
        if (resolvedDeclaration instanceof ResolvedFieldDeclaration) {
            fieldDescriptor = typeResolver.getFieldDescriptor(TypeResolverUtils.getFieldSignature((ResolvedFieldDeclaration) resolvedDeclaration), parent);
        } else if (resolvedDeclaration instanceof ResolvedEnumConstantDeclaration) {
            fieldDescriptor = typeResolver.getFieldDescriptor(TypeResolverUtils.getFieldSignature((ResolvedEnumConstantDeclaration) resolvedDeclaration),
                    parent);
        }
    }

    private void setVisibility(Node nodeWithModifiers) {
        ((AccessModifierDescriptor) fieldDescriptor)
                .setVisibility(TypeResolverUtils.getAccessSpecifier(((NodeWithModifiers<?>) nodeWithModifiers).getModifiers()).getValue());
    }

    private void setAccessModifier(Node nodeWithModifiers) {
        // TODO further modifiers
        if (nodeWithModifiers instanceof NodeWithAbstractModifier) {
            ((AbstractDescriptor) fieldDescriptor).setAbstract(((NodeWithAbstractModifier<?>) nodeWithModifiers).isAbstract());
        }
        if (nodeWithModifiers instanceof NodeWithFinalModifier) {
            ((AccessModifierDescriptor) fieldDescriptor).setFinal(((NodeWithFinalModifier<?>) nodeWithModifiers).isFinal());
        }
        if (nodeWithModifiers instanceof NodeWithStaticModifier) {
            ((AccessModifierDescriptor) fieldDescriptor).setStatic(((NodeWithStaticModifier<?>) nodeWithModifiers).isStatic());
        }
    }

    private void setFieldType(ResolvedFieldDeclaration resolvedFieldDeclaration, TypeDescriptor parent) {
        TypeDescriptor fieldTypeDescriptor = typeResolver.resolveDependency(TypeResolverUtils.getQualifiedName(resolvedFieldDeclaration.getType()), parent);
        fieldDescriptor.setType(fieldTypeDescriptor);
    }

    private void setFieldValue(FieldDeclaration fieldDeclaration) {
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
