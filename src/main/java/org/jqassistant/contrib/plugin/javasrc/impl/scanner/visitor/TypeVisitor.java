package org.jqassistant.contrib.plugin.javasrc.impl.scanner.visitor;

import java.util.List;
import java.util.Set;

import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.declarations.ResolvedAnnotationDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedClassDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedEnumDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedInterfaceDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import org.jqassistant.contrib.plugin.javasrc.api.model.AnnotationTypeDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.ClassTypeDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.EnumTypeDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.InterfaceTypeDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.JavaSourceFileDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.TypeDescriptor;
import org.jqassistant.contrib.plugin.javasrc.impl.scanner.TypeResolver;
import org.jqassistant.contrib.plugin.javasrc.impl.scanner.TypeResolverUtils;

/**
 * This visitor handles parsed types, i.e. interfaces, classes, enums, and
 * (annotations), and creates corresponding descriptors.
 * 
 * @author Richard Müller
 *
 */
public class TypeVisitor extends VoidVisitorAdapter<JavaSourceFileDescriptor> {
    private TypeResolver typeResolver;

    public TypeVisitor(TypeResolver typeResolver) {
        this.typeResolver = typeResolver;
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, JavaSourceFileDescriptor javaSourceFileDescriptor) {
        super.visit(classOrInterfaceDeclaration, javaSourceFileDescriptor);

        if (classOrInterfaceDeclaration.isInterface()) {
            // interface
            // fqn, name
            ResolvedInterfaceDeclaration resolvedInterfaceDeclaration = classOrInterfaceDeclaration.resolve().asInterface();
            InterfaceTypeDescriptor interfaceTypeDescriptor = typeResolver.createType(resolvedInterfaceDeclaration.getQualifiedName(), javaSourceFileDescriptor,
                    InterfaceTypeDescriptor.class);
            interfaceTypeDescriptor.setFullQualifiedName(resolvedInterfaceDeclaration.getQualifiedName());
            interfaceTypeDescriptor.setName(classOrInterfaceDeclaration.getNameAsString());

            // visibility and access modifiers
            interfaceTypeDescriptor.setVisibility(TypeResolverUtils.getAccessSpecifier(classOrInterfaceDeclaration.getModifiers()).getValue());
            interfaceTypeDescriptor.setAbstract(classOrInterfaceDeclaration.isAbstract());
            interfaceTypeDescriptor.setFinal(classOrInterfaceDeclaration.isFinal());
            interfaceTypeDescriptor.setStatic(classOrInterfaceDeclaration.isStatic());

            // extends, implements
            List<ResolvedReferenceType> resolvedSuperTypes = resolvedInterfaceDeclaration.getAncestors();
            for (ResolvedReferenceType resolvedSuperType : resolvedSuperTypes) {
                interfaceTypeDescriptor
                        .setSuperClass(typeResolver.resolveDependency(TypeResolverUtils.getQualifiedName(resolvedSuperType), interfaceTypeDescriptor));
            }

            // inner class
            Set<ResolvedReferenceTypeDeclaration> resolvedInnerClasses = resolvedInterfaceDeclaration.internalTypes();
            for (ResolvedReferenceTypeDeclaration resolvedInnerClass : resolvedInnerClasses) {
                interfaceTypeDescriptor.getDeclaredInnerClasses()
                        .add(typeResolver.resolveDependency(resolvedInnerClass.getQualifiedName(), interfaceTypeDescriptor));
            }

            // fields
            for (FieldDeclaration field : classOrInterfaceDeclaration.getFields()) {
                field.accept(new FieldVisitor(typeResolver), interfaceTypeDescriptor);
            }

            // methods
            for (MethodDeclaration method : classOrInterfaceDeclaration.getMethods()) {
                method.accept(new MethodVisitor(typeResolver), interfaceTypeDescriptor);
            }

            // annotations
            for (AnnotationExpr annotation : classOrInterfaceDeclaration.getAnnotations()) {
                annotation.accept(new AnnotationVisitor(typeResolver), interfaceTypeDescriptor);
            }

        } else {
            // class
            // fqn, name
            ResolvedClassDeclaration resolvedClassDeclaration = classOrInterfaceDeclaration.resolve().asClass();
            ClassTypeDescriptor classTypeDescriptor = typeResolver.createType(resolvedClassDeclaration.getQualifiedName(), javaSourceFileDescriptor,
                    ClassTypeDescriptor.class);
            classTypeDescriptor.setFullQualifiedName(resolvedClassDeclaration.getQualifiedName());
            classTypeDescriptor.setName(classOrInterfaceDeclaration.getNameAsString());

            // visibility and access modifiers
            classTypeDescriptor.setVisibility(TypeResolverUtils.getAccessSpecifier(classOrInterfaceDeclaration.getModifiers()).getValue());
            classTypeDescriptor.setAbstract(classOrInterfaceDeclaration.isAbstract());
            classTypeDescriptor.setFinal(classOrInterfaceDeclaration.isFinal());
            classTypeDescriptor.setStatic(classOrInterfaceDeclaration.isStatic());

            // extends
            ResolvedReferenceType resolvedSuperType = resolvedClassDeclaration.getSuperClass();
            TypeDescriptor superClassTypeDescriptor = typeResolver.resolveDependency(TypeResolverUtils.getQualifiedName(resolvedSuperType),
                    classTypeDescriptor);
            classTypeDescriptor.setSuperClass(superClassTypeDescriptor);

            // implements
            List<ResolvedReferenceType> resolvedInterfaces = resolvedClassDeclaration.getInterfaces();
            for (ResolvedReferenceType resolvedInterface : resolvedInterfaces) {
                classTypeDescriptor.getInterfaces()
                        .add(typeResolver.resolveDependency(TypeResolverUtils.getQualifiedName(resolvedInterface), classTypeDescriptor));
            }

            // inner class
            Set<ResolvedReferenceTypeDeclaration> resolvedInnerClasses = resolvedClassDeclaration.internalTypes();
            for (ResolvedReferenceTypeDeclaration resolvedInnerClass : resolvedInnerClasses) {
                classTypeDescriptor.getDeclaredInnerClasses().add(typeResolver.resolveDependency(resolvedInnerClass.getQualifiedName(), classTypeDescriptor));
            }

            // fields
            for (FieldDeclaration field : classOrInterfaceDeclaration.getFields()) {
                field.accept(new FieldVisitor(typeResolver), classTypeDescriptor);
            }

            // methods
            for (MethodDeclaration method : classOrInterfaceDeclaration.getMethods()) {
                method.accept(new MethodVisitor(typeResolver), classTypeDescriptor);
            }

            // constructors
            for (ConstructorDeclaration constructor : classOrInterfaceDeclaration.getConstructors()) {
                constructor.accept(new MethodVisitor(typeResolver), classTypeDescriptor);
            }

            // annotations
            for (AnnotationExpr annotation : classOrInterfaceDeclaration.getAnnotations()) {
                annotation.accept(new AnnotationVisitor(typeResolver), classTypeDescriptor);
            }
        }
    }

    @Override
    public void visit(EnumDeclaration enumDeclaration, JavaSourceFileDescriptor javaSourceFileDescriptor) {
        // fqn, name
        ResolvedEnumDeclaration resolvedEnumDeclaration = enumDeclaration.resolve();
        EnumTypeDescriptor enumTypeDescriptor = typeResolver.createType(resolvedEnumDeclaration.getQualifiedName(), javaSourceFileDescriptor,
                EnumTypeDescriptor.class);
        enumTypeDescriptor.setFullQualifiedName(resolvedEnumDeclaration.getQualifiedName());
        enumTypeDescriptor.setName(resolvedEnumDeclaration.getName().toString());

        // visibility and access modifiers
        enumTypeDescriptor.setVisibility(TypeResolverUtils.getAccessSpecifier(enumDeclaration.getModifiers()).getValue());
        enumTypeDescriptor.setStatic(enumDeclaration.isStatic());

        // fields
        // TODO remove?
        for (FieldDeclaration field : enumDeclaration.getFields()) {
            field.accept(new FieldVisitor(typeResolver), enumTypeDescriptor);
        }

        // enum constants
        for (EnumConstantDeclaration entry : enumDeclaration.getEntries()) {
            entry.accept(new FieldVisitor(typeResolver), enumTypeDescriptor);
        }

        // methods
        for (MethodDeclaration method : enumDeclaration.getMethods()) {
            method.accept(new MethodVisitor(typeResolver), enumTypeDescriptor);
        }

        // annotations
        for (AnnotationExpr annotation : enumDeclaration.getAnnotations()) {
            annotation.accept(new AnnotationVisitor(typeResolver), enumTypeDescriptor);
        }

        super.visit(enumDeclaration, javaSourceFileDescriptor);
    }

    @Override
    public void visit(AnnotationDeclaration annotationDeclaration, JavaSourceFileDescriptor javaSourceFileDescriptor) {

        // fqn, name
        ResolvedAnnotationDeclaration resolvedAnnotationDeclaration = annotationDeclaration.resolve();

        AnnotationTypeDescriptor annotationTypeDescriptor = typeResolver.createType(resolvedAnnotationDeclaration.getQualifiedName(), javaSourceFileDescriptor,
                AnnotationTypeDescriptor.class);
        annotationTypeDescriptor.setFullQualifiedName(resolvedAnnotationDeclaration.getQualifiedName());
        annotationTypeDescriptor.setName(resolvedAnnotationDeclaration.getName().toString());

        // visibility and access modifiers (public, abstract)
        annotationTypeDescriptor.setVisibility(TypeResolverUtils.getAccessSpecifier(annotationDeclaration.getModifiers()).getValue());
        annotationTypeDescriptor.setAbstract(annotationDeclaration.isAbstract());

        // annotation members
        for (BodyDeclaration<?> member : annotationDeclaration.getMembers()) {
            if (member.isAnnotationMemberDeclaration()) {
                member.accept(new AnnotationVisitor(typeResolver), annotationTypeDescriptor);
            }
        }

        super.visit(annotationDeclaration, javaSourceFileDescriptor);
    }
}
