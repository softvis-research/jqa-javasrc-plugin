package org.jqassistant.contrib.plugin.javasrc.impl.scanner.visitor;

import com.buschmais.jqassistant.core.store.api.model.Descriptor;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.nodeTypes.NodeWithConstructors;
import com.github.javaparser.ast.nodeTypes.NodeWithMembers;
import com.github.javaparser.ast.nodeTypes.NodeWithModifiers;
import com.github.javaparser.ast.nodeTypes.modifiers.NodeWithAbstractModifier;
import com.github.javaparser.ast.nodeTypes.modifiers.NodeWithFinalModifier;
import com.github.javaparser.ast.nodeTypes.modifiers.NodeWithStaticModifier;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.Resolvable;
import com.github.javaparser.resolution.declarations.ResolvedAnnotationDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedClassDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedEnumDeclaration;
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

    public TypeVisitor(TypeResolver typeResolver) {
        this.typeResolver = typeResolver;
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, JavaSourceFileDescriptor javaSourceFileDescriptor) {
        // class or interface
        TypeDescriptor typeDescriptor = createType(classOrInterfaceDeclaration, javaSourceFileDescriptor);
        setConstructors(classOrInterfaceDeclaration, typeDescriptor);
        setVisibility(classOrInterfaceDeclaration, typeDescriptor);
        setAccessModifier(classOrInterfaceDeclaration, typeDescriptor);
        setSuperType(classOrInterfaceDeclaration, typeDescriptor);
        setImplementedInterfaces(classOrInterfaceDeclaration, typeDescriptor);
        setInnerClassesForParent(classOrInterfaceDeclaration, typeDescriptor);
        setFields(classOrInterfaceDeclaration, typeDescriptor);
        setMethods(classOrInterfaceDeclaration, typeDescriptor);
        setAnnotations(classOrInterfaceDeclaration, (AnnotatedDescriptor) typeDescriptor);

        super.visit(classOrInterfaceDeclaration, javaSourceFileDescriptor);
    }

    @Override
    public void visit(EnumDeclaration enumDeclaration, JavaSourceFileDescriptor javaSourceFileDescriptor) {
        // enum
        TypeDescriptor typeDescriptor = createType(enumDeclaration, javaSourceFileDescriptor);
        setVisibility(enumDeclaration, typeDescriptor);
        setEnumConstants(enumDeclaration, typeDescriptor);
        setFields(enumDeclaration, typeDescriptor);
        setMethods(enumDeclaration, typeDescriptor);
        setAnnotations(enumDeclaration, (AnnotatedDescriptor) typeDescriptor);

        super.visit(enumDeclaration, javaSourceFileDescriptor);
    }

    @Override
    public void visit(AnnotationDeclaration annotationDeclaration, JavaSourceFileDescriptor javaSourceFileDescriptor) {
        // annotation
        TypeDescriptor typeDescriptor = createType(annotationDeclaration, javaSourceFileDescriptor);
        setVisibility(annotationDeclaration, typeDescriptor);
        setAccessModifier(annotationDeclaration, typeDescriptor);
        setAnnotationMembers(annotationDeclaration, typeDescriptor);

        super.visit(annotationDeclaration, javaSourceFileDescriptor);
    }

    private TypeDescriptor createType(Resolvable<?> resolvable, JavaSourceFileDescriptor javaSourceFileDescriptor) {
        Object type = resolvable.resolve();
        if (type instanceof ResolvedInterfaceDeclaration) {
            return typeResolver.createType(((ResolvedInterfaceDeclaration) type).getQualifiedName(), javaSourceFileDescriptor, InterfaceTypeDescriptor.class);
        } else if (type instanceof ResolvedClassDeclaration) {
            return typeResolver.createType(((ResolvedClassDeclaration) type).getQualifiedName(), javaSourceFileDescriptor, ClassTypeDescriptor.class);
        } else if (type instanceof ResolvedEnumDeclaration) {
            return typeResolver.createType(((ResolvedEnumDeclaration) type).getQualifiedName(), javaSourceFileDescriptor, EnumTypeDescriptor.class);
        } else if (type instanceof ResolvedAnnotationDeclaration) {
            return typeResolver.createType(((ResolvedAnnotationDeclaration) type).getQualifiedName(), javaSourceFileDescriptor, AnnotationTypeDescriptor.class);
        } else {
            throw new RuntimeException("TypeDescriptor could not be created: " + resolvable + " " + resolvable.getClass());
        }
    }

    private void setVisibility(Node nodeWithModifiers, Descriptor descriptor) {
        ((AccessModifierDescriptor) descriptor)
                .setVisibility(TypeResolverUtils.getAccessSpecifier(((NodeWithModifiers<?>) nodeWithModifiers).getModifiers()).getValue());
    }

    private void setAccessModifier(Node nodeWithModifiers, Descriptor descriptor) {
        // TODO further modifiers
        if (nodeWithModifiers instanceof NodeWithAbstractModifier) {
            ((AbstractDescriptor) descriptor).setAbstract(((NodeWithAbstractModifier<?>) nodeWithModifiers).isAbstract());
        }
        if (nodeWithModifiers instanceof NodeWithFinalModifier) {
            ((AccessModifierDescriptor) descriptor).setFinal(((NodeWithFinalModifier<?>) nodeWithModifiers).isFinal());
        }
        if (nodeWithModifiers instanceof NodeWithStaticModifier) {
            ((AccessModifierDescriptor) descriptor).setStatic(((NodeWithStaticModifier<?>) nodeWithModifiers).isStatic());
        }
    }

    private void setSuperType(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, TypeDescriptor typeDescriptor) {
        // TODO an interface might extend from multiple interfaces
        for (ClassOrInterfaceType superType : classOrInterfaceDeclaration.getExtendedTypes()) {
            ResolvedReferenceType resolvedSuperType = superType.resolve();
            ((ClassFileDescriptor) typeDescriptor).setSuperClass(typeResolver.resolveDependency(resolvedSuperType.getQualifiedName(), typeDescriptor));
            setTypeParameterDependency(resolvedSuperType, typeDescriptor);
        }
    }

    private void setImplementedInterfaces(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, TypeDescriptor typeDescriptor) {
        for (ClassOrInterfaceType superType : classOrInterfaceDeclaration.getImplementedTypes()) {
            ResolvedReferenceType resolvedSuperType = superType.resolve();
            ((ClassFileDescriptor) typeDescriptor).getInterfaces().add(typeResolver.resolveDependency(resolvedSuperType.getQualifiedName(), typeDescriptor));
            setTypeParameterDependency(resolvedSuperType, typeDescriptor);
        }
    }

    private void setInnerClassesForParent(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, TypeDescriptor typeDescriptor) {
        if (classOrInterfaceDeclaration.isInnerClass()) {
            classOrInterfaceDeclaration.getParentNode().ifPresent(parentClass -> {
                TypeDescriptor parentType = typeResolver.resolveDependency(((ClassOrInterfaceDeclaration) parentClass).resolve().getQualifiedName(),
                        typeDescriptor);
                parentType.getDeclaredInnerClasses().add(typeDescriptor);
            });
        }
    }

    private void setTypeParameterDependency(ResolvedReferenceType type, TypeDescriptor typeDescriptor) {
        for (Pair<ResolvedTypeParameterDeclaration, ResolvedType> typeParamter : type.getTypeParametersMap()) {
            typeResolver.resolveDependency(typeParamter.b.asReferenceType().getQualifiedName(), typeDescriptor);
        }
    }

    private void setFields(Node nodeWithFields, TypeDescriptor typeDescriptor) {
        for (FieldDeclaration field : ((NodeWithMembers<?>) nodeWithFields).getFields()) {
            field.accept(new FieldVisitor(typeResolver), typeDescriptor);
        }
    }

    private void setConstructors(Node node, TypeDescriptor typeDescriptor) {
        for (ConstructorDeclaration constructors : ((NodeWithConstructors<?>) node).getConstructors()) {
            constructors.accept(new MethodVisitor(typeResolver), typeDescriptor);
        }
    }

    private void setMethods(Node nodeWithMembers, TypeDescriptor typeDescriptor) {
        for (MethodDeclaration method : ((NodeWithMembers<?>) nodeWithMembers).getMethods()) {
            method.accept(new MethodVisitor(typeResolver), typeDescriptor);
        }
    }

    private void setAnnotationMembers(AnnotationDeclaration annotationDeclaration, TypeDescriptor typeDescriptor) {
        for (BodyDeclaration<?> member : annotationDeclaration.getMembers()) {
            if (member.isAnnotationMemberDeclaration()) {
                member.accept(new AnnotationMemberVisitor(typeResolver), (AnnotatedDescriptor) typeDescriptor);
            }
        }
    }

    private void setEnumConstants(EnumDeclaration enumDeclaration, TypeDescriptor typeDescriptor) {
        for (EnumConstantDeclaration entry : enumDeclaration.getEntries()) {
            entry.accept(new FieldVisitor(typeResolver), typeDescriptor);
        }
    }

    private void setAnnotations(Node nodeWithAnnotations, AnnotatedDescriptor annotatedDescriptor) {
        for (AnnotationExpr annotation : ((NodeWithAnnotations<?>) nodeWithAnnotations).getAnnotations()) {
            annotation.accept(new AnnotationVisitor(typeResolver), annotatedDescriptor);
        }
    }
}
