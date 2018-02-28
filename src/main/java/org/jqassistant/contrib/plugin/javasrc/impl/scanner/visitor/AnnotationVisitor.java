package org.jqassistant.contrib.plugin.javasrc.impl.scanner.visitor;

import java.util.Optional;

import com.buschmais.jqassistant.plugin.common.api.model.ArrayValueDescriptor;
import com.buschmais.jqassistant.plugin.common.api.model.ValueDescriptor;
import com.github.javaparser.ast.body.AnnotationMemberDeclaration;
import com.github.javaparser.ast.expr.Expression;
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
        AnnotationValueDescriptor annotationValueDescriptor = typeResolver.addAnnotationValueDescriptor(singleMemberAnnotationExpr, annotatedDescriptor);
        annotationValueDescriptor.getValue()
                .add(createValueDescriptor(TypeResolverUtils.SINGLE_MEMBER_ANNOTATION_NAME, singleMemberAnnotationExpr.getMemberValue(), annotatedDescriptor));
    }

    @Override
    public void visit(NormalAnnotationExpr normalAnnotationExpr, AnnotatedDescriptor annotatedDescriptor) {
        AnnotationValueDescriptor annotationValueDescriptor = typeResolver.addAnnotationValueDescriptor(normalAnnotationExpr, annotatedDescriptor);
        for (MemberValuePair memberValuePair : normalAnnotationExpr.getPairs()) {
            annotationValueDescriptor.getValue().add(createValueDescriptor(memberValuePair.getNameAsString(), memberValuePair.getValue(), annotatedDescriptor));
        }
    }

    @Override
    public void visit(AnnotationMemberDeclaration annotationMemberDeclaration, AnnotatedDescriptor annotatedDescriptor) {
        // signature, name
        MethodDescriptor methodDescriptor = typeResolver.getMethodDescriptor(TypeResolverUtils.getAnnotationMemberSignature(annotationMemberDeclaration),
                ((TypeDescriptor) annotatedDescriptor));
        // name must be overwritten here as it is not in the signature
        methodDescriptor.setName(annotationMemberDeclaration.getNameAsString());

        // visibility and access modifiers (public, abstract)
        methodDescriptor.setVisibility(TypeResolverUtils.getAccessSpecifier(annotationMemberDeclaration.getModifiers()).getValue());
        methodDescriptor.setAbstract(annotationMemberDeclaration.isAbstract());

        // default value
        Optional<Expression> value = annotationMemberDeclaration.getDefaultValue();
        if (value.isPresent()) {
            methodDescriptor.setHasDefault(createValueDescriptor(TypeResolverUtils.ANNOTATION_MEMBER_DEFAULT_VALUE_NAME, value.get(), annotatedDescriptor));
        }
    }

    private ValueDescriptor<?> createValueDescriptor(String name, Expression value, AnnotatedDescriptor annotatedDescriptor) {
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
                arrayValueDescriptor.getValue().add(createValueDescriptor(("[" + i + "]"), arrayValue, annotatedDescriptor));
                i++;
            }
            return arrayValueDescriptor;
        } else if (value.isFieldAccessExpr()) {
            EnumValueDescriptor enumValueDescriptor = typeResolver.getValueDescriptor(EnumValueDescriptor.class);
            enumValueDescriptor.setName(name);
            ResolvedFieldDeclaration resolvedFieldDeclaration = typeResolver.solve(value.asFieldAccessExpr()).getCorrespondingDeclaration();
            TypeDescriptor enumType = typeResolver.resolveDependency(resolvedFieldDeclaration.getType().asReferenceType().getQualifiedName(),
                    ((TypeDescriptor) annotatedDescriptor));
            FieldDescriptor fieldDescriptor = typeResolver.getFieldDescriptor(TypeResolverUtils.getFieldSignature(resolvedFieldDeclaration), enumType);
            enumValueDescriptor.setValue(fieldDescriptor);
            return enumValueDescriptor;
        } else if (value.isSingleMemberAnnotationExpr()) {
            SingleMemberAnnotationExpr singleMemberAnnotationExpr = value.asSingleMemberAnnotationExpr();
            AnnotationValueDescriptor annotationValueDescriptor = typeResolver.addAnnotationValueDescriptor(singleMemberAnnotationExpr, null);
            annotationValueDescriptor.setName(name);
            annotationValueDescriptor.getValue().add(
                    createValueDescriptor(TypeResolverUtils.SINGLE_MEMBER_ANNOTATION_NAME, singleMemberAnnotationExpr.getMemberValue(), annotatedDescriptor));
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
