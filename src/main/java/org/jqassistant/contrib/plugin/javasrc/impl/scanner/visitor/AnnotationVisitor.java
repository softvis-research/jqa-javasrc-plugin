package org.jqassistant.contrib.plugin.javasrc.impl.scanner.visitor;

import com.buschmais.jqassistant.plugin.common.api.model.ValueDescriptor;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import org.jqassistant.contrib.plugin.javasrc.api.model.AnnotatedDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.AnnotationValueDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.TypeDescriptor;
import org.jqassistant.contrib.plugin.javasrc.impl.scanner.JavaSourceException;

/**
 * This visitor handles parsed annotations and creates corresponding
 * descriptors.
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

    private void createAnnotation(AnnotationExpr annotationExpr, AnnotatedDescriptor annotatedDescriptor) {
        getQualifiedName(annotationExpr).ifPresent(qualifiedAnnotationName -> {
            descriptor = visitorHelper.getAnnotationValueDescriptor(qualifiedAnnotationName, annotationExpr.getNameAsString(), annotatedDescriptor);
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

    @Override
    protected ValueDescriptor<?> createValueDescriptor(String name, Expression value, TypeDescriptor typeDescriptor) throws JavaSourceException {
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
        }
        return super.createValueDescriptor(name, value, typeDescriptor);
    }
}
