package org.jqassistant.contrib.plugin.javasrc.impl.scanner.visitor;

import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.AnnotationMemberDeclaration;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
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
        System.out.println("AnnotationDeclaration: " + resolvedAnnotationDeclaration.getName() + " " + resolvedAnnotationDeclaration.getQualifiedName());
    }

    @Override
    public void visit(AnnotationMemberDeclaration annotationMemberDeclaration, JavaSourceFileDescriptor javaSourceFileDescriptor) {
        super.visit(annotationMemberDeclaration, javaSourceFileDescriptor);

        ResolvedAnnotationMemberDeclaration resolvedAnnotationMemberDeclaration = annotationMemberDeclaration.resolve();
        System.out
                .println("AnnotationMemberDeclaration: " + resolvedAnnotationMemberDeclaration.getName() + " " + annotationMemberDeclaration.getDefaultValue());
    }

    @Override
    public void visit(SingleMemberAnnotationExpr singleMemberAnnotationExpr, JavaSourceFileDescriptor javaSourceFileDescriptor) {
        super.visit(singleMemberAnnotationExpr, javaSourceFileDescriptor);

        System.out.println("SingleMemberAnnotationExpr: " + singleMemberAnnotationExpr.getNameAsString() + " " + singleMemberAnnotationExpr.getMemberValue());
    }

    @Override
    public void visit(MarkerAnnotationExpr markerAnnotationExpr, JavaSourceFileDescriptor javaSourceFileDescriptor) {
        super.visit(markerAnnotationExpr, javaSourceFileDescriptor);

        System.out.println("MarkerAnnotationExpr: " + markerAnnotationExpr.getNameAsString());
    }

    @Override
    public void visit(NormalAnnotationExpr normalAnnotationExpr, JavaSourceFileDescriptor javaSourceFileDescriptor) {
        super.visit(normalAnnotationExpr, javaSourceFileDescriptor);
        System.out.println("NormalAnnotationExpr: " + normalAnnotationExpr.getNameAsString() + " " + normalAnnotationExpr.getPairs().toString());
    }

    @Override
    public void visit(MemberValuePair memberValuePair, JavaSourceFileDescriptor javaSourceFileDescriptor) {
        super.visit(memberValuePair, javaSourceFileDescriptor);
        // Node node = memberValuePair.getParentNode().get();
        // System.out.println(node.toString());
        // AnnotationExpr annotation = (AnnotationExpr) node;
        // System.out.println("Grandparent: " +
        // annotation.getParentNode().get().toString());
        // System.out.println("Parent: " + annotation.getNameAsString());
        // System.out.println("Member: " + memberValuePair.getNameAsString());
        // System.out.println("Value: " + memberValuePair.getValue());
    }

}
