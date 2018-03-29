package org.jqassistant.contrib.plugin.javasrc.impl.scanner.visitor;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.Resolvable;
import com.github.javaparser.resolution.declarations.ResolvedEnumConstantDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import org.jqassistant.contrib.plugin.javasrc.api.model.FieldDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.PrimitiveValueDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.TypeDescriptor;

/**
 * This visitor handles parsed fields and enum values and creates corresponding
 * descriptors.
 * 
 * @author Richard Müller
 *
 */
public class FieldVisitor extends AbstractJavaSourceVisitor<TypeDescriptor> {

    public FieldVisitor(VisitorHelper visitorHelper) {
        super(visitorHelper);
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
        return visitorHelper.getFieldDescriptor(getFieldSignature(resolvable), parent);
    }

    private void setFieldType(FieldDeclaration fieldDeclaration, FieldDescriptor fieldDescriptor) {
        Type fieldType = fieldDeclaration.getElementType();
        TypeDescriptor fieldTypeDescriptor = visitorHelper.resolveDependency(visitorHelper.getQualifiedName(fieldType), fieldDescriptor.getDeclaringType());
        fieldDescriptor.setType(fieldTypeDescriptor);
        if (fieldType.isClassOrInterfaceType()) {
            // TODO are there other types?
            setTypeParameterDependency(fieldType.asClassOrInterfaceType(), fieldDescriptor.getDeclaringType());
        }
    }

    private void setFieldValue(FieldDeclaration fieldDeclaration, FieldDescriptor fieldDescriptor) {
        // field value (of first variable)
        // TODO many variables for one field, type of values
        VariableDeclarator firstVariable = fieldDeclaration.getVariables().get(0);
        firstVariable.getInitializer().ifPresent(value -> {
            PrimitiveValueDescriptor valueDescriptor = visitorHelper.getValueDescriptor(PrimitiveValueDescriptor.class);
            valueDescriptor.setValue(getLiteralExpressionValue(value));
            fieldDescriptor.setValue(valueDescriptor);
        });
    }

    private String getFieldSignature(Resolvable<?> resolvable) {
        Object resolvedDeclaration = visitorHelper.solve((Node) resolvable);
        if (resolvedDeclaration instanceof ResolvedFieldDeclaration) {
            ResolvedFieldDeclaration resolvedFieldDeclaration = ((ResolvedFieldDeclaration) resolvedDeclaration).asField();
            return visitorHelper.getQualifiedName(resolvedFieldDeclaration.getType()) + " " + resolvedFieldDeclaration.getName();
        } else if (resolvedDeclaration instanceof ResolvedEnumConstantDeclaration) {
            ResolvedEnumConstantDeclaration resolvedEnumConstantDeclaration = ((ResolvedEnumConstantDeclaration) resolvedDeclaration);
            return visitorHelper.getQualifiedName(resolvedEnumConstantDeclaration.getType()) + " " + resolvedEnumConstantDeclaration.getName();
        } else {
            throw new IllegalArgumentException("Field signature could not be create for: " + resolvable.toString());
        }
    }
}
