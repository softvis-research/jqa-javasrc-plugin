package org.jqassistant.contrib.plugin.javasrc.impl.scanner.visitor;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithConstructors;
import com.github.javaparser.ast.nodeTypes.NodeWithMembers;
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
        createType(classOrInterfaceDeclaration, javaSourceFileDescriptor);
        setConstructors(classOrInterfaceDeclaration);
        setVisibility(classOrInterfaceDeclaration);
        setAccessModifier(classOrInterfaceDeclaration);
        setSuperType(classOrInterfaceDeclaration);
        setImplementedInterfaces(classOrInterfaceDeclaration);
        setInnerClassesForParent(classOrInterfaceDeclaration);
        setFields(classOrInterfaceDeclaration);
        setMethods(classOrInterfaceDeclaration);
        setAnnotations(classOrInterfaceDeclaration, (AnnotatedDescriptor) descriptor);
    }

    @Override
    public void visit(EnumDeclaration enumDeclaration, JavaSourceFileDescriptor javaSourceFileDescriptor) {
        // enum
        createType(enumDeclaration, javaSourceFileDescriptor);
        setVisibility(enumDeclaration);
        setEnumConstants(enumDeclaration);
        setFields(enumDeclaration);
        setMethods(enumDeclaration);
        setAnnotations(enumDeclaration, (AnnotatedDescriptor) descriptor);

        super.visit(enumDeclaration, javaSourceFileDescriptor);
    }

    @Override
    public void visit(AnnotationDeclaration annotationDeclaration, JavaSourceFileDescriptor javaSourceFileDescriptor) {
        // annotation
        createType(annotationDeclaration, javaSourceFileDescriptor);
        setVisibility(annotationDeclaration);
        setAccessModifier(annotationDeclaration);
        setAnnotationMembers(annotationDeclaration);

        super.visit(annotationDeclaration, javaSourceFileDescriptor);
    }

    private void createType(TypeDeclaration<?> typeDeclaration, JavaSourceFileDescriptor javaSourceFileDescriptor) {
        if (typeDeclaration instanceof ClassOrInterfaceDeclaration) {
            if (typeDeclaration.asClassOrInterfaceDeclaration().isInterface()) {
                descriptor = visitorHelper.createType(visitorHelper.getQualifiedName(typeDeclaration), javaSourceFileDescriptor, InterfaceTypeDescriptor.class);
            } else {
                descriptor = visitorHelper.createType(visitorHelper.getQualifiedName(typeDeclaration), javaSourceFileDescriptor, ClassTypeDescriptor.class);
            }
        } else if (typeDeclaration instanceof EnumDeclaration) {
            descriptor = visitorHelper.createType(visitorHelper.getQualifiedName(typeDeclaration), javaSourceFileDescriptor, EnumTypeDescriptor.class);
        } else if (typeDeclaration instanceof AnnotationDeclaration) {
            descriptor = visitorHelper.createType(visitorHelper.getQualifiedName(typeDeclaration), javaSourceFileDescriptor, AnnotationTypeDescriptor.class);
        }
    }

    private void setSuperType(ClassOrInterfaceDeclaration classOrInterfaceDeclaration) {
        // TODO an interface might extend multiple interfaces
        classOrInterfaceDeclaration.getExtendedTypes().forEach(superType -> {
            ((ClassFileDescriptor) descriptor)
                    .setSuperClass(visitorHelper.resolveDependency(visitorHelper.getQualifiedName(superType), (TypeDescriptor) descriptor));
            setTypeParameterDependency(superType, (TypeDescriptor) descriptor);
        });
    }

    private void setImplementedInterfaces(ClassOrInterfaceDeclaration classOrInterfaceDeclaration) {
        classOrInterfaceDeclaration.getImplementedTypes().forEach(superType -> {
            ((ClassFileDescriptor) descriptor).getInterfaces()
                    .add(visitorHelper.resolveDependency(visitorHelper.getQualifiedName(superType), (TypeDescriptor) descriptor));
            setTypeParameterDependency(superType, (TypeDescriptor) descriptor);
        });
    }

    private void setInnerClassesForParent(ClassOrInterfaceDeclaration classOrInterfaceDeclaration) {
        if (classOrInterfaceDeclaration.isInnerClass()) {
            classOrInterfaceDeclaration.getParentNode().ifPresent(parentClass -> {
                TypeDescriptor parentType = visitorHelper.resolveDependency(visitorHelper.getQualifiedName(parentClass), null);
                parentType.getDeclaredInnerClasses().add((TypeDescriptor) descriptor);
            });
        }
    }

    private void setFields(Node nodeWithFields) {
        ((NodeWithMembers<?>) nodeWithFields).getFields().forEach(field -> {
            field.accept(new FieldVisitor(visitorHelper), (TypeDescriptor) descriptor);
        });
    }

    private void setConstructors(Node node) {
        ((NodeWithConstructors<?>) node).getConstructors().forEach(constructor -> {
            constructor.accept(new MethodVisitor(visitorHelper), (TypeDescriptor) descriptor);
        });
    }

    private void setMethods(Node nodeWithMembers) {
        ((NodeWithMembers<?>) nodeWithMembers).getMethods().forEach(method -> {
            method.getParentNode().ifPresent(parentNode -> {
                // filter methods of anonymous inner classes
                if (!(parentNode instanceof ObjectCreationExpr)) {
                    method.accept(new MethodVisitor(visitorHelper), (TypeDescriptor) descriptor);
                }
            });
        });
    }

    private void setAnnotationMembers(AnnotationDeclaration annotationDeclaration) {
        annotationDeclaration.getMembers().forEach(member -> {
            if (member.isAnnotationMemberDeclaration()) {
                member.accept(new MethodVisitor(visitorHelper), (TypeDescriptor) descriptor);
            }
        });
    }

    private void setEnumConstants(EnumDeclaration enumDeclaration) {
        enumDeclaration.getEntries().forEach(entry -> {
            entry.accept(new FieldVisitor(visitorHelper), (TypeDescriptor) descriptor);
        });
    }
}
