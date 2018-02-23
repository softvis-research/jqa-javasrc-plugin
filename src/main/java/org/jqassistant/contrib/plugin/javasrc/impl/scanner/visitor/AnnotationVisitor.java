package org.jqassistant.contrib.plugin.javasrc.impl.scanner.visitor;

import java.util.Optional;

import com.buschmais.jqassistant.plugin.common.api.model.ArrayValueDescriptor;
import com.buschmais.jqassistant.plugin.common.api.model.ValueDescriptor;
import com.github.javaparser.ast.body.AnnotationMemberDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
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
 * This visitor handles parsed annotations and creates corresponding
 * descriptors. The type resolver is used to get full qualified names of parsed
 * declarations and to determine the field type.
 * 
 * @author Richard Müller
 *
 */
public class AnnotationVisitor extends VoidVisitorAdapter<AnnotatedDescriptor> {
    private TypeResolver typeResolver;

    public AnnotationVisitor(TypeResolver typeResolver) {
        this.typeResolver = typeResolver;
    }

    @Override
    public void visit(SingleMemberAnnotationExpr singleMemberAnnotationExpr, AnnotatedDescriptor annotatedDescriptor) {
        super.visit(singleMemberAnnotationExpr, annotatedDescriptor);

        AnnotationValueDescriptor annotationValueDescriptor = typeResolver.addAnnotationValueDescriptor(singleMemberAnnotationExpr, annotatedDescriptor);
        annotationValueDescriptor.getValue().add(createValueDescriptor("value", singleMemberAnnotationExpr.getMemberValue()));
    }

    @Override
    public void visit(MarkerAnnotationExpr markerAnnotationExpr, AnnotatedDescriptor annotatedDescriptor) {
        super.visit(markerAnnotationExpr, annotatedDescriptor);
        System.out.println("AnnotatedDescriptor: " + annotatedDescriptor.toString());
        System.out.println("MarkerAnnotationExpr: " + markerAnnotationExpr.getNameAsString());
    }

    @Override
    public void visit(NormalAnnotationExpr normalAnnotationExpr, AnnotatedDescriptor annotatedDescriptor) {
        // TODO uncomment?
        // not calling super to avoid duplication of nested annotations
        // super.visit(normalAnnotationExpr, annotatedDescriptor);
        AnnotationValueDescriptor annotationValueDescriptor = typeResolver.addAnnotationValueDescriptor(normalAnnotationExpr, annotatedDescriptor);
        for (MemberValuePair memberValuePair : normalAnnotationExpr.getPairs()) {

            annotationValueDescriptor.getValue().add(createValueDescriptor(memberValuePair.getNameAsString(), memberValuePair.getValue()));
        }
    }

    @Override
    public void visit(AnnotationMemberDeclaration annotationMemberDeclaration, AnnotatedDescriptor annotatedDescriptor) {
        super.visit(annotationMemberDeclaration, annotatedDescriptor);

        // signature, name
        TypeDescriptor returnTypeDescriptor = typeResolver.resolveType(TypeResolverUtils.getQualifiedName(annotationMemberDeclaration.getType().resolve()));
        MethodDescriptor methodDescriptor = typeResolver.addMethodDescriptor(((TypeDescriptor) annotatedDescriptor).getFullQualifiedName(),
                returnTypeDescriptor.getFullQualifiedName() + " " + "()");
        methodDescriptor.setName(annotationMemberDeclaration.getNameAsString());

        // default value
        Optional<Expression> value = annotationMemberDeclaration.getDefaultValue();
        if (value.isPresent()) {
            methodDescriptor.setHasDefault(createValueDescriptor("null", value.get()));
        }

    }

    private ValueDescriptor<?> createValueDescriptor(String name, Expression value) {
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
                arrayValueDescriptor.getValue().add(createValueDescriptor(("[" + i + "]"), arrayValue));
                i++;
            }
            return arrayValueDescriptor;
        } else if (value.isFieldAccessExpr()) {
            EnumValueDescriptor enumValueDescriptor = typeResolver.getValueDescriptor(EnumValueDescriptor.class);
            enumValueDescriptor.setName(name);
            ResolvedFieldDeclaration resolvedFieldDeclaration = typeResolver.solve(value.asFieldAccessExpr()).getCorrespondingDeclaration();
            FieldDescriptor fieldDescriptor = typeResolver
                    .resolveField(TypeResolverUtils.getQualifiedName(resolvedFieldDeclaration.getType()) + " " + resolvedFieldDeclaration.getName());
            enumValueDescriptor.setValue(fieldDescriptor);
            return enumValueDescriptor;
        } else if (value.isSingleMemberAnnotationExpr()) {
            SingleMemberAnnotationExpr singleMemberAnnotationExpr = value.asSingleMemberAnnotationExpr();
            AnnotationValueDescriptor annotationValueDescriptor = typeResolver.addAnnotationValueDescriptor(singleMemberAnnotationExpr, null);
            annotationValueDescriptor.setName(name);
            annotationValueDescriptor.getValue().add(createValueDescriptor("value", singleMemberAnnotationExpr.getMemberValue()));
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
