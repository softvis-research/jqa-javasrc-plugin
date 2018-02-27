package org.jqassistant.contrib.plugin.javasrc.impl.scanner.visitor;

import java.util.Optional;

import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.declarations.ResolvedEnumConstantDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import org.jqassistant.contrib.plugin.javasrc.api.model.FieldDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.PrimitiveValueDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.TypeDescriptor;
import org.jqassistant.contrib.plugin.javasrc.impl.scanner.TypeResolver;
import org.jqassistant.contrib.plugin.javasrc.impl.scanner.TypeResolverUtils;

/**
 * This visitor handles parsed fields and creates corresponding descriptors. The
 * type resolver is used to get full qualified names of parsed declarations and
 * to determine the field type.
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
        // signature, name
        ResolvedFieldDeclaration resolvedFieldDeclaration = fieldDeclaration.resolve();
        TypeDescriptor fieldTypeDescriptor = typeResolver.resolveDependency(TypeResolverUtils.getQualifiedName(resolvedFieldDeclaration.getType()),
                typeDescriptor);
        FieldDescriptor fieldDescriptor = typeResolver.getFieldDescriptor(TypeResolverUtils.getFieldSignature(resolvedFieldDeclaration), typeDescriptor);

        // visibility and access modifiers
        fieldDescriptor.setVisibility(TypeResolverUtils.getAccessSpecifier(fieldDeclaration.getModifiers()).getValue());
        fieldDescriptor.setFinal(fieldDeclaration.isFinal());
        fieldDescriptor.setStatic(fieldDeclaration.isStatic());

        // type
        fieldDescriptor.setType(fieldTypeDescriptor);

        // field value (of first variable)
        // TODO many variables for one field, type of values
        VariableDeclarator variable = fieldDeclaration.getVariables().get(0);
        Optional<Expression> value = variable.getInitializer();
        if (value.isPresent()) {
            PrimitiveValueDescriptor valueDescriptor = typeResolver.getValueDescriptor(PrimitiveValueDescriptor.class);
            valueDescriptor.setValue(TypeResolverUtils.getLiteralExpressionValue(value.get()));
            fieldDescriptor.setValue(valueDescriptor);
        }

        // annotations
        for (AnnotationExpr annotation : fieldDeclaration.getAnnotations()) {
            annotation.accept(new AnnotationVisitor(typeResolver), fieldDescriptor);
        }

        super.visit(fieldDeclaration, typeDescriptor);
    }

    @Override
    public void visit(EnumConstantDeclaration enumConstantDeclaration, TypeDescriptor typeDescriptor) {
        // fqn, name
        ResolvedEnumConstantDeclaration resolvedEnumConstantDeclaration = enumConstantDeclaration.resolve();
        FieldDescriptor fieldDescriptor = typeResolver.getFieldDescriptor(TypeResolverUtils.getFieldSignature(resolvedEnumConstantDeclaration), typeDescriptor);

        // TODO remove?
        // annotations
        for (AnnotationExpr annotation : enumConstantDeclaration.getAnnotations()) {
            annotation.accept(new AnnotationVisitor(typeResolver), fieldDescriptor);
        }

        super.visit(enumConstantDeclaration, typeDescriptor);
    }
}
