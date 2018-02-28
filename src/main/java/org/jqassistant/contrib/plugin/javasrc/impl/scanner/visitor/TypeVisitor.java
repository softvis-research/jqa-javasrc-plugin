package org.jqassistant.contrib.plugin.javasrc.impl.scanner.visitor;

import java.util.EnumSet;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.declarations.ResolvedClassDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedInterfaceDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeParameterDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.utils.Pair;
import org.jqassistant.contrib.plugin.javasrc.api.model.AbstractDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.AccessModifierDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.AnnotatedDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.AnnotationTypeDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.ClassFileDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.ClassTypeDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.EnumTypeDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.InterfaceTypeDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.JavaSourceFileDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.TypeDescriptor;
import org.jqassistant.contrib.plugin.javasrc.impl.scanner.TypeResolver;
import org.jqassistant.contrib.plugin.javasrc.impl.scanner.TypeResolverUtils;

/**
 * This visitor handles parsed types, i.e. interfaces, classes, enums, and
 * annotations, and creates corresponding descriptors.
 * 
 * @author Richard Müller
 *
 */
public class TypeVisitor extends VoidVisitorAdapter<JavaSourceFileDescriptor> {
    private TypeResolver typeResolver;
    private TypeDescriptor typeDescriptor;

    public TypeVisitor(TypeResolver typeResolver) {
        this.typeResolver = typeResolver;
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, JavaSourceFileDescriptor javaSourceFileDescriptor) {

        if (classOrInterfaceDeclaration.isInterface()) {
            // interface
            // fqn, name
            ResolvedInterfaceDeclaration resolvedInterfaceDeclaration = classOrInterfaceDeclaration.resolve().asInterface();
            typeDescriptor = typeResolver.createType(resolvedInterfaceDeclaration.getQualifiedName(), javaSourceFileDescriptor, InterfaceTypeDescriptor.class);
        } else {
            // class
            // fqn, name
            ResolvedClassDeclaration resolvedClassDeclaration = classOrInterfaceDeclaration.resolve().asClass();
            typeDescriptor = typeResolver.createType(resolvedClassDeclaration.getQualifiedName(), javaSourceFileDescriptor, ClassTypeDescriptor.class);

            // constructors
            for (ConstructorDeclaration constructors : classOrInterfaceDeclaration.getConstructors()) {
                constructors.accept(new MethodVisitor(typeResolver), typeDescriptor);
            }
        }

        // inner classes
        if (classOrInterfaceDeclaration.isInnerClass()) {
            setInnerClasses(classOrInterfaceDeclaration);
        }

        // visibility and access modifiers
        setVisibility(classOrInterfaceDeclaration.getModifiers());
        setAccessModifier(classOrInterfaceDeclaration);

        // extends
        // TODO an interface might extend from multiple interfaces
        setSuperType(classOrInterfaceDeclaration);

        // implements
        setImplementedInterfaces(classOrInterfaceDeclaration);

        // fields
        for (FieldDeclaration field : classOrInterfaceDeclaration.getFields()) {
            field.accept(new FieldVisitor(typeResolver), typeDescriptor);
        }

        // methods
        for (MethodDeclaration method : classOrInterfaceDeclaration.getMethods()) {
            method.accept(new MethodVisitor(typeResolver), typeDescriptor);
        }

        // annotations
        for (AnnotationExpr annotation : classOrInterfaceDeclaration.getAnnotations()) {
            annotation.accept(new AnnotationVisitor(typeResolver), (AnnotatedDescriptor) typeDescriptor);
        }

        super.visit(classOrInterfaceDeclaration, javaSourceFileDescriptor);
    }

    @Override
    public void visit(EnumDeclaration enumDeclaration, JavaSourceFileDescriptor javaSourceFileDescriptor) {
        // fqn, name
        typeDescriptor = typeResolver.createType(enumDeclaration.resolve().getQualifiedName(), javaSourceFileDescriptor, EnumTypeDescriptor.class);

        // visibility and access modifiers (public)
        setVisibility(enumDeclaration.getModifiers());

        // fields
        for (FieldDeclaration field : enumDeclaration.getFields()) {
            field.accept(new FieldVisitor(typeResolver), typeDescriptor);
        }

        // enum constants
        for (EnumConstantDeclaration entry : enumDeclaration.getEntries()) {
            entry.accept(new FieldVisitor(typeResolver), typeDescriptor);
        }

        // methods
        for (MethodDeclaration method : enumDeclaration.getMethods()) {
            method.accept(new MethodVisitor(typeResolver), typeDescriptor);
        }

        // annotations
        for (AnnotationExpr annotation : enumDeclaration.getAnnotations()) {
            annotation.accept(new AnnotationVisitor(typeResolver), (AnnotatedDescriptor) typeDescriptor);
        }

        super.visit(enumDeclaration, javaSourceFileDescriptor);
    }

    @Override
    public void visit(AnnotationDeclaration annotationDeclaration, JavaSourceFileDescriptor javaSourceFileDescriptor) {

        // fqn, name
        typeDescriptor = typeResolver.createType(annotationDeclaration.resolve().getQualifiedName(), javaSourceFileDescriptor, AnnotationTypeDescriptor.class);

        // visibility and access modifiers (public, abstract)
        setVisibility(annotationDeclaration.getModifiers());
        ((AbstractDescriptor) typeDescriptor).setAbstract(annotationDeclaration.isAbstract());

        // annotation members
        for (BodyDeclaration<?> member : annotationDeclaration.getMembers()) {
            if (member.isAnnotationMemberDeclaration()) {
                member.accept(new AnnotationVisitor(typeResolver), (AnnotatedDescriptor) typeDescriptor);
            }
        }

        super.visit(annotationDeclaration, javaSourceFileDescriptor);
    }

    private void setInnerClasses(ClassOrInterfaceDeclaration classOrInterfaceDeclaration) {
        classOrInterfaceDeclaration.getParentNode().ifPresent(parentClass -> {
            TypeDescriptor parentType = typeResolver.resolveDependency(((ClassOrInterfaceDeclaration) parentClass).resolve().getQualifiedName(),
                    typeDescriptor);
            parentType.getDeclaredInnerClasses().add(typeDescriptor);
        });
    }

    private void setAccessModifier(ClassOrInterfaceDeclaration classOrInterfaceDeclaration) {
        ((AbstractDescriptor) typeDescriptor).setAbstract(classOrInterfaceDeclaration.isAbstract());
        ((AccessModifierDescriptor) typeDescriptor).setFinal(classOrInterfaceDeclaration.isFinal());
        ((AccessModifierDescriptor) typeDescriptor).setStatic(classOrInterfaceDeclaration.isStatic());
    }

    private void setVisibility(EnumSet<Modifier> modifiers) {
        ((AccessModifierDescriptor) typeDescriptor).setVisibility(TypeResolverUtils.getAccessSpecifier(modifiers).getValue());
    }

    private void setSuperType(ClassOrInterfaceDeclaration classOrInterfaceDeclaration) {
        for (ClassOrInterfaceType superType : classOrInterfaceDeclaration.getExtendedTypes()) {
            ResolvedReferenceType resolvedSuperType = superType.resolve();
            ((ClassFileDescriptor) typeDescriptor).setSuperClass(typeResolver.resolveDependency(resolvedSuperType.getQualifiedName(), typeDescriptor));
            setTypeParameterDependency(resolvedSuperType);
        }
    }

    private void setImplementedInterfaces(ClassOrInterfaceDeclaration classOrInterfaceDeclaration) {
        for (ClassOrInterfaceType superType : classOrInterfaceDeclaration.getImplementedTypes()) {
            ResolvedReferenceType resolvedSuperType = superType.resolve();
            ((ClassFileDescriptor) typeDescriptor).getInterfaces().add(typeResolver.resolveDependency(resolvedSuperType.getQualifiedName(), typeDescriptor));
            setTypeParameterDependency(resolvedSuperType);
        }
    }

    private void setTypeParameterDependency(ResolvedReferenceType type) {
        for (Pair<ResolvedTypeParameterDeclaration, ResolvedType> typeParamter : type.getTypeParametersMap()) {
            typeResolver.resolveDependency(typeParamter.b.asReferenceType().getQualifiedName(), typeDescriptor);
        }
    }
}
