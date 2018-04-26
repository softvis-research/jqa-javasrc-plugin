package org.jqassistant.contrib.plugin.javasrc.impl.scanner.visitor;

import com.buschmais.jqassistant.plugin.common.api.model.ArrayValueDescriptor;
import com.buschmais.jqassistant.plugin.common.api.model.ValueDescriptor;
import com.github.javaparser.ast.body.AnnotationMemberDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import org.jqassistant.contrib.plugin.javasrc.api.model.AnnotatedDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.AnnotationValueDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.ClassValueDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.EnumValueDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.FieldDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.MethodDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.PrimitiveValueDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.TypeDescriptor;
import org.jqassistant.contrib.plugin.javasrc.impl.scanner.JavaSourceException;

/**
 * This visitor handles parsed annotations and annotation members and creates
 * corresponding descriptors.
 * 
 * @author Richard Mueller
 *
 */
public class AnnotationVisitor extends AbstractJavaSourceVisitor<AnnotatedDescriptor> {

    public AnnotationVisitor(VisitorHelper visitorHelper) {
        super(visitorHelper);
    }

    @Override
    public void visit(SingleMemberAnnotationExpr singleMemberAnnotationExpr, AnnotatedDescriptor annotatedDescriptor) {
        createAnnotation(singleMemberAnnotationExpr, annotatedDescriptor);
        setAnnotationValue(singleMemberAnnotationExpr);
    }

    @Override
    public void visit(NormalAnnotationExpr normalAnnotationExpr, AnnotatedDescriptor annotatedDescriptor) {
        createAnnotation(normalAnnotationExpr, annotatedDescriptor);
        setAnnotationValue(normalAnnotationExpr);

    }

    @Override
    public void visit(AnnotationMemberDeclaration annotationMemberDeclaration, AnnotatedDescriptor annotatedDescriptor) {
        // annotation member
        createAnnotationMember(annotationMemberDeclaration, (TypeDescriptor) annotatedDescriptor);
        // name must be overwritten here as it is not in the signature
        ((MethodDescriptor) descriptor).setName(annotationMemberDeclaration.getNameAsString());
        setVisibility(annotationMemberDeclaration);
        setAccessModifier(annotationMemberDeclaration);
        setAnnotationMemberDefaultValue(annotationMemberDeclaration);
    }

    private void createAnnotation(AnnotationExpr annotationExpr, AnnotatedDescriptor annotatedDescriptor) {
        getQualifiedName(annotationExpr).ifPresent(qualifiedAnnotationName -> {
            descriptor = visitorHelper.getAnnotationValueDescriptor(qualifiedAnnotationName, annotationExpr.getNameAsString(), annotatedDescriptor);
        });
    }

    private void createAnnotationMember(AnnotationMemberDeclaration annotationMemberDeclaration, TypeDescriptor parent) {
        getQualifiedSignature(annotationMemberDeclaration).ifPresent(qualifiedMethodSignature -> {
            descriptor = visitorHelper.getMethodDescriptor(qualifiedMethodSignature, parent);
        });
    }

    private void setAnnotationValue(AnnotationExpr annotationExpr) {
        if (annotationExpr instanceof SingleMemberAnnotationExpr) {
            ((AnnotationValueDescriptor) descriptor).getValue().add(createValueDescriptor(visitorHelper.SINGLE_MEMBER_ANNOTATION_NAME,
                    ((SingleMemberAnnotationExpr) annotationExpr).getMemberValue(), ((AnnotationValueDescriptor) descriptor).getType()));
        } else if (annotationExpr instanceof NormalAnnotationExpr) {
            for (MemberValuePair memberValuePair : ((NormalAnnotationExpr) annotationExpr).getPairs()) {
                ((AnnotationValueDescriptor) descriptor).getValue().add(createValueDescriptor(memberValuePair.getNameAsString(), memberValuePair.getValue(),
                        ((AnnotationValueDescriptor) descriptor).getType()));
            }
        }
    }

    private void setAnnotationMemberDefaultValue(AnnotationMemberDeclaration annotationMemberDeclaration) {
        annotationMemberDeclaration.getDefaultValue().ifPresent(value -> {
            ((MethodDescriptor) descriptor).setHasDefault(
                    createValueDescriptor(visitorHelper.ANNOTATION_MEMBER_DEFAULT_VALUE_NAME, value, ((MethodDescriptor) descriptor).getDeclaringType()));
        });
    }

    private ValueDescriptor<?> createValueDescriptor(String name, Expression value, TypeDescriptor typeDescriptor) throws JavaSourceException {
        if (value.isNormalAnnotationExpr()) {
            NormalAnnotationExpr normalAnnotationExpr = value.asNormalAnnotationExpr();
            createAnnotation(normalAnnotationExpr, null);
            setAnnotationValue(normalAnnotationExpr);
            return (ValueDescriptor<?>) descriptor;
        } else if (value.isSingleMemberAnnotationExpr()) {
            SingleMemberAnnotationExpr singleMemberAnnotationExpr = value.asSingleMemberAnnotationExpr();
            createAnnotation(singleMemberAnnotationExpr, null);
            setAnnotationValue(singleMemberAnnotationExpr);
            return (ValueDescriptor<?>) descriptor;
        } else if (value.isLiteralExpr()) {
            PrimitiveValueDescriptor primitiveValueDescriptor = visitorHelper.getValueDescriptor(PrimitiveValueDescriptor.class);
            primitiveValueDescriptor.setName(name);
            primitiveValueDescriptor.setValue(getLiteralExpressionValue(value));
            return primitiveValueDescriptor;
        } else if (value.isClassExpr()) {
            ClassValueDescriptor classValueDescriptor = visitorHelper.getValueDescriptor(ClassValueDescriptor.class);
            classValueDescriptor.setName(name);
            getQualifiedName(value.asClassExpr().getType()).ifPresent(qualifiedClassValueName -> {
                classValueDescriptor.setValue(visitorHelper.resolveDependency(qualifiedClassValueName, typeDescriptor));
            });
            return classValueDescriptor;
        } else if (value.isArrayInitializerExpr()) {
            ArrayValueDescriptor arrayValueDescriptor = visitorHelper.getValueDescriptor(ArrayValueDescriptor.class);
            arrayValueDescriptor.setName(name);
            Object[] arrayValues = value.asArrayInitializerExpr().getValues().toArray();
            for (int i = 0; i < arrayValues.length; i++) {
                Object object = arrayValues[i];
                arrayValueDescriptor.getValue().add(createValueDescriptor(("[" + i + "]"), (Expression) arrayValues[i], typeDescriptor));
            }
            return arrayValueDescriptor;
        } else if (value.isFieldAccessExpr()) {
            FieldAccessExpr fieldAccessExpr = value.asFieldAccessExpr();
            EnumValueDescriptor enumValueDescriptor = visitorHelper.getValueDescriptor(EnumValueDescriptor.class);
            enumValueDescriptor.setName(name);
            getQualifiedName(fieldAccessExpr).ifPresent(qualifiedFieldTypeName -> {
                TypeDescriptor parent = visitorHelper.resolveDependency(qualifiedFieldTypeName, typeDescriptor);
                getQualifiedSignature(fieldAccessExpr).ifPresent(qualifiedFieldSignature -> {
                    FieldDescriptor fieldDescriptor = visitorHelper.getFieldDescriptor(qualifiedFieldSignature, parent);
                    enumValueDescriptor.setValue(fieldDescriptor);
                });
            });
            return enumValueDescriptor;
        } else if (value.isNameExpr()) {
            PrimitiveValueDescriptor primitiveValueDescriptor = visitorHelper.getValueDescriptor(PrimitiveValueDescriptor.class);
            primitiveValueDescriptor.setName(name);
            primitiveValueDescriptor.setValue(value.toString());
            return primitiveValueDescriptor;
        } else
            throw new JavaSourceException("Type of annotation value is not supported: " + name + " " + value.getClass());
    }
}
