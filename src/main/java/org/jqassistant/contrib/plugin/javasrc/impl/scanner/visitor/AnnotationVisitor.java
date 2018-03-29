package org.jqassistant.contrib.plugin.javasrc.impl.scanner.visitor;

import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import org.jqassistant.contrib.plugin.javasrc.api.model.AnnotatedDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.AnnotationValueDescriptor;

/**
 * This visitor handles parsed annotations and creates corresponding
 * descriptors.
 * 
 * @author Richard Müller
 *
 */
public class AnnotationVisitor extends AbstractJavaSourceVisitor<AnnotatedDescriptor> {

    public AnnotationVisitor(VisitorHelper visitorHelper) {
        super(visitorHelper);
    }

    @Override
    public void visit(SingleMemberAnnotationExpr singleMemberAnnotationExpr, AnnotatedDescriptor annotatedDescriptor) {
        AnnotationValueDescriptor annotationValueDescriptor = createAnnotation(singleMemberAnnotationExpr, annotatedDescriptor);
        setAnnotationValue(singleMemberAnnotationExpr, annotationValueDescriptor);
    }

    @Override
    public void visit(NormalAnnotationExpr normalAnnotationExpr, AnnotatedDescriptor annotatedDescriptor) {
        AnnotationValueDescriptor annotationValueDescriptor = createAnnotation(normalAnnotationExpr, annotatedDescriptor);
        setAnnotationValue(normalAnnotationExpr, annotationValueDescriptor);
    }

    private AnnotationValueDescriptor createAnnotation(AnnotationExpr annotationExpr, AnnotatedDescriptor annotatedDescriptor) {
        return visitorHelper.getAnnotationValueDescriptor(visitorHelper.getQualifiedName(annotationExpr), annotationExpr.getNameAsString(),
                annotatedDescriptor);

    }

    private void setAnnotationValue(AnnotationExpr annotationExpr, AnnotationValueDescriptor annotationValueDescriptor) {
        if (annotationExpr instanceof SingleMemberAnnotationExpr) {
            annotationValueDescriptor.getValue().add(createValueDescriptor(visitorHelper.SINGLE_MEMBER_ANNOTATION_NAME,
                    ((SingleMemberAnnotationExpr) annotationExpr).getMemberValue(), annotationValueDescriptor.getType()));
        } else if (annotationExpr instanceof NormalAnnotationExpr) {
            for (MemberValuePair memberValuePair : ((NormalAnnotationExpr) annotationExpr).getPairs()) {
                annotationValueDescriptor.getValue()
                        .add(createValueDescriptor(memberValuePair.getNameAsString(), memberValuePair.getValue(), annotationValueDescriptor.getType()));
            }
        }
    }
}
