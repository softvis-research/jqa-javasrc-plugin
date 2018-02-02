package org.jqassistant.contrib.plugin.javasrc.impl.scanner.visitor;

import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.AnnotationMemberDeclaration;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.declarations.ResolvedAnnotationDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedAnnotationMemberDeclaration;
import org.jqassistant.contrib.plugin.javasrc.api.model.JavaSourceFileDescriptor;
import org.jqassistant.contrib.plugin.javasrc.impl.scanner.TypeResolver;

/**
 * This visitor handles parsed annotations and creates corresponding
 * descriptors. The type resolver is used to get full qualified names of parsed
 * declarations and to determine the field type.
 * 
 * @author Richard Müller
 *
 */
public class AnnotationVisitor extends VoidVisitorAdapter<JavaSourceFileDescriptor> {
    private TypeResolver typeResolver;

    public AnnotationVisitor(TypeResolver typeResolver) {
        this.typeResolver = typeResolver;
    }

    @Override
    public void visit(AnnotationDeclaration annotationDeclaration, JavaSourceFileDescriptor javaSourceFileDescriptor) {
        super.visit(annotationDeclaration, javaSourceFileDescriptor);

        ResolvedAnnotationDeclaration resolvedAnnotationDeclaration = annotationDeclaration.resolve();
        System.out.println("Annotation: " + resolvedAnnotationDeclaration.getName() + " " + resolvedAnnotationDeclaration.getQualifiedName());

    }

    @Override
    public void visit(AnnotationMemberDeclaration annotationMemberDeclaration, JavaSourceFileDescriptor javaSourceFileDescriptor) {
        super.visit(annotationMemberDeclaration, javaSourceFileDescriptor);

        ResolvedAnnotationMemberDeclaration resolvedAnnotationMemberDeclaration = annotationMemberDeclaration.resolve();
        System.out.println("AnnotationMember: " + resolvedAnnotationMemberDeclaration.getName() + " " + annotationMemberDeclaration.getDefaultValue());
    }

    @Override
    public void visit(SingleMemberAnnotationExpr singleMemberAnnotationExpr, JavaSourceFileDescriptor javaSourceFileDescriptor) {
        super.visit(singleMemberAnnotationExpr, javaSourceFileDescriptor);

        System.out.println("SingleMember: " + singleMemberAnnotationExpr.getNameAsString() + " " + singleMemberAnnotationExpr.getMemberValue());
    }

    @Override
    public void visit(MarkerAnnotationExpr markerAnnotationExpr, JavaSourceFileDescriptor javaSourceFileDescriptor) {
        super.visit(markerAnnotationExpr, javaSourceFileDescriptor);
        System.out.println(markerAnnotationExpr.getNameAsString());
    }

}
