package org.jqassistant.contrib.plugin.javasrc.impl.scanner.visitor;

import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.resolution.Resolvable;
import com.github.javaparser.resolution.declarations.ResolvedEnumConstantDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
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
public class FieldVisitor extends AbstractJavaSourceVisitor<TypeDescriptor> {

    public FieldVisitor(TypeResolver typeResolver) {
        super(typeResolver);
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
}
