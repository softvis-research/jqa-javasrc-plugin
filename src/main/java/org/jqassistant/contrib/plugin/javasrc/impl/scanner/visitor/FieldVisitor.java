package org.jqassistant.contrib.plugin.javasrc.impl.scanner.visitor;

import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.type.Type;
import org.jqassistant.contrib.plugin.javasrc.api.model.AnnotatedDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.FieldDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.PrimitiveValueDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.TypeDescriptor;

/**
 * This visitor handles parsed fields and enum values and creates corresponding
 * descriptors.
 * 
 * @author Richard Mueller
 *
 */
public class FieldVisitor extends AbstractJavaSourceVisitor<TypeDescriptor> {

    public FieldVisitor(VisitorHelper visitorHelper) {
        super(visitorHelper);
    }

    @Override
    public void visit(FieldDeclaration fieldDeclaration, TypeDescriptor typeDescriptor) {
        // field
        createField(fieldDeclaration, typeDescriptor);
        setVisibility(fieldDeclaration);
        setAccessModifier(fieldDeclaration);
        setFieldType(fieldDeclaration);
        setFieldValue(fieldDeclaration);
        setAnnotations(fieldDeclaration, (AnnotatedDescriptor) descriptor);
    }

    @Override
    public void visit(EnumConstantDeclaration enumConstantDeclaration, TypeDescriptor typeDescriptor) {
        // enum values
        createField(enumConstantDeclaration, typeDescriptor);
        setAnnotations(enumConstantDeclaration, (AnnotatedDescriptor) descriptor);
    }

    private void createField(BodyDeclaration<?> bodyDeclaration, TypeDescriptor parent) {
        getQualifiedSignature(bodyDeclaration).ifPresent(qualifiedFieldSignature -> {
            descriptor = visitorHelper.getFieldDescriptor(qualifiedFieldSignature, parent);
        });
    }

    private void setFieldType(FieldDeclaration fieldDeclaration) {
        Type fieldType = fieldDeclaration.getVariables().get(0).getType();
        TypeDescriptor fieldTypeDescriptor = visitorHelper.resolveDependency(getQualifiedName(fieldType.resolve()),
                ((FieldDescriptor) descriptor).getDeclaringType());
        ((FieldDescriptor) descriptor).setType(fieldTypeDescriptor);
        if (fieldType.isClassOrInterfaceType()) {
            // TODO are there other types?
            setTypeParameterDependency(fieldType.asClassOrInterfaceType(), ((FieldDescriptor) descriptor).getDeclaringType());
        }
    }

    private void setFieldValue(FieldDeclaration fieldDeclaration) {
        // field value (of first variable)
        // TODO many variables for one field, type of values
        VariableDeclarator firstVariable = fieldDeclaration.getVariables().get(0);
        firstVariable.getInitializer().ifPresent(value -> {
            PrimitiveValueDescriptor valueDescriptor = visitorHelper.getValueDescriptor(PrimitiveValueDescriptor.class);
            valueDescriptor.setValue(getLiteralExpressionValue(value));
            ((FieldDescriptor) descriptor).setValue(valueDescriptor);
        });
    }
}
