package org.jqassistant.contrib.plugin.javasrc.impl.scanner.visitor;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithConstructors;
import com.github.javaparser.ast.nodeTypes.NodeWithMembers;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.resolution.Resolvable;
import com.github.javaparser.resolution.declarations.ResolvedAnnotationDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedClassDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedEnumDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedInterfaceDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import org.jqassistant.contrib.plugin.javasrc.api.model.AnnotatedDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.AnnotationTypeDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.ClassFileDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.ClassTypeDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.EnumTypeDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.InterfaceTypeDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.JavaSourceFileDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.TypeDescriptor;
import org.jqassistant.contrib.plugin.javasrc.impl.scanner.TypeResolver;

/**
 * This visitor handles parsed types, i.e. interfaces, classes, enums, and
 * annotations, and creates corresponding descriptors.
 * 
 * @author Richard Müller
 *
 */
public class TypeVisitor extends AbstractJavaSourceVisitor<JavaSourceFileDescriptor> {
    public TypeVisitor(TypeResolver typeResolver) {
        super(typeResolver);
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, JavaSourceFileDescriptor javaSourceFileDescriptor) {
        // TODO call super first because of inner classes?
        super.visit(classOrInterfaceDeclaration, javaSourceFileDescriptor);

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
                TypeDescriptor parentType = typeResolver.resolveDependency(((ClassOrInterfaceDeclaration) parentClass).resolve().getQualifiedName(), null);
                parentType.getDeclaredInnerClasses().add(typeDescriptor);
            });
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
                member.accept(new MethodVisitor(typeResolver), typeDescriptor);
            }
        }
    }

    private void setEnumConstants(EnumDeclaration enumDeclaration, TypeDescriptor typeDescriptor) {
        for (EnumConstantDeclaration entry : enumDeclaration.getEntries()) {
            entry.accept(new FieldVisitor(typeResolver), typeDescriptor);
        }
    }
}
