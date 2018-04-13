package org.jqassistant.contrib.plugin.javasrc.impl.scanner.visitor;

import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import org.jqassistant.contrib.plugin.javasrc.api.model.AnnotatedDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.AnnotationValueDescriptor;

/**
 * This visitor handles parsed annotations and creates corresponding
 * descriptors. The call of super is not necessary because we already have
 * collected all annotations of the annotated descriptor.
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
        descriptor = visitorHelper.getAnnotationValueDescriptor(getQualifiedName(annotationExpr), annotationExpr.getNameAsString(), annotatedDescriptor);

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
}
