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
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithConstructors;
import com.github.javaparser.ast.nodeTypes.NodeWithMembers;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import org.jqassistant.contrib.plugin.javasrc.api.model.AnnotatedDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.AnnotationTypeDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.ClassFileDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.ClassTypeDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.EnumTypeDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.InterfaceTypeDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.JavaSourceFileDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.TypeDescriptor;

/**
 * This visitor handles parsed types, i.e. interfaces, classes, enums, and
 * annotations, and creates corresponding descriptors.
 * 
 * @author Richard Müller
 *
 */
public class TypeVisitor extends AbstractJavaSourceVisitor<JavaSourceFileDescriptor> {

    public TypeVisitor(VisitorHelper visitorHelper) {
        super(visitorHelper);
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

    private TypeDescriptor createType(TypeDeclaration typeDeclaration, JavaSourceFileDescriptor javaSourceFileDescriptor) {

        if (typeDeclaration instanceof ClassOrInterfaceDeclaration) {
            if (typeDeclaration.asClassOrInterfaceDeclaration().isInterface()) {
                return visitorHelper.createType(typeDeclaration, javaSourceFileDescriptor, InterfaceTypeDescriptor.class);
            } else {
                return visitorHelper.createType(typeDeclaration, javaSourceFileDescriptor, ClassTypeDescriptor.class);
            }
        } else if (typeDeclaration instanceof EnumDeclaration) {
            return visitorHelper.createType(typeDeclaration, javaSourceFileDescriptor, EnumTypeDescriptor.class);
        } else if (typeDeclaration instanceof AnnotationDeclaration) {
            return visitorHelper.createType(typeDeclaration, javaSourceFileDescriptor, AnnotationTypeDescriptor.class);
        } else {
            // TODO remove throw exeption, make typeDescriptor global
            throw new RuntimeException("TypeDescriptor could not be created: " + typeDeclaration + " " + typeDeclaration.getClass());
        }
    }

    private void setSuperType(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, TypeDescriptor typeDescriptor) {
        // TODO an interface might extend from multiple interfaces
        for (ClassOrInterfaceType superType : classOrInterfaceDeclaration.getExtendedTypes()) {
            ((ClassFileDescriptor) typeDescriptor).setSuperClass(visitorHelper.resolveDependency(visitorHelper.getQualifiedName(superType), typeDescriptor));
            setTypeParameterDependency(superType, typeDescriptor);
        }
    }

    private void setImplementedInterfaces(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, TypeDescriptor typeDescriptor) {
        for (ClassOrInterfaceType superType : classOrInterfaceDeclaration.getImplementedTypes()) {
            ((ClassFileDescriptor) typeDescriptor).getInterfaces()
                    .add(visitorHelper.resolveDependency(visitorHelper.getQualifiedName(superType), typeDescriptor));
            setTypeParameterDependency(superType, typeDescriptor);
        }
    }

    private void setInnerClassesForParent(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, TypeDescriptor typeDescriptor) {
        if (classOrInterfaceDeclaration.isInnerClass()) {
            classOrInterfaceDeclaration.getParentNode().ifPresent(parentClass -> {
                TypeDescriptor parentType = visitorHelper.resolveDependency(visitorHelper.getQualifiedName(parentClass), null);
                parentType.getDeclaredInnerClasses().add(typeDescriptor);
            });
        }
    }

    private void setFields(Node nodeWithFields, TypeDescriptor typeDescriptor) {
        for (FieldDeclaration field : ((NodeWithMembers<?>) nodeWithFields).getFields()) {
            field.accept(new FieldVisitor(visitorHelper), typeDescriptor);
        }
    }

    private void setConstructors(Node node, TypeDescriptor typeDescriptor) {
        for (ConstructorDeclaration constructors : ((NodeWithConstructors<?>) node).getConstructors()) {
            constructors.accept(new MethodVisitor(visitorHelper), typeDescriptor);
        }
    }

    private void setMethods(Node nodeWithMembers, TypeDescriptor typeDescriptor) {
        for (MethodDeclaration method : ((NodeWithMembers<?>) nodeWithMembers).getMethods()) {
            method.accept(new MethodVisitor(visitorHelper), typeDescriptor);
        }
    }

    private void setAnnotationMembers(AnnotationDeclaration annotationDeclaration, TypeDescriptor typeDescriptor) {
        for (BodyDeclaration<?> member : annotationDeclaration.getMembers()) {
            if (member.isAnnotationMemberDeclaration()) {
                member.accept(new MethodVisitor(visitorHelper), typeDescriptor);
            }
        }
    }

    private void setEnumConstants(EnumDeclaration enumDeclaration, TypeDescriptor typeDescriptor) {
        for (EnumConstantDeclaration entry : enumDeclaration.getEntries()) {
            entry.accept(new FieldVisitor(visitorHelper), typeDescriptor);
        }
    }
}
