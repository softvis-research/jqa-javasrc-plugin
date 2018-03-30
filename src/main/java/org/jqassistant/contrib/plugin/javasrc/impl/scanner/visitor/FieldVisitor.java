package org.jqassistant.contrib.plugin.javasrc.impl.scanner.visitor;

import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.type.Type;
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

    private FieldDescriptor createField(BodyDeclaration<?> bodyDeclaration, TypeDescriptor parent) {
        return visitorHelper.getFieldDescriptor(getFieldSignature(bodyDeclaration), parent);
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

    private String getFieldSignature(BodyDeclaration<?> bodyDEclaration) {
        if (bodyDEclaration instanceof FieldDeclaration) {
            FieldDeclaration fieldDeclaration = bodyDEclaration.asFieldDeclaration();
            return visitorHelper.getQualifiedName(fieldDeclaration.getElementType()) + " " + fieldDeclaration.getVariable(0).getName();
        } else if (bodyDEclaration instanceof EnumConstantDeclaration) {
            EnumConstantDeclaration enumConstantDeclaration = bodyDEclaration.asEnumConstantDeclaration();
            return visitorHelper.getQualifiedName(enumConstantDeclaration) + " " + enumConstantDeclaration.getName();
        } else {
            throw new IllegalArgumentException("Field signature could not be create for: " + bodyDEclaration.toString());
        }
    }
}
