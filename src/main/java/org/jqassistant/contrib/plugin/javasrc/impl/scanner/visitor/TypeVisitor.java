package org.jqassistant.contrib.plugin.javasrc.impl.scanner.visitor;

import java.util.function.Predicate;

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
import org.jqassistant.contrib.plugin.javasrc.api.model.TypeDescriptor;

/**
 * This visitor handles parsed types, i.e. interfaces, classes, enums, and
 * annotations, and creates corresponding descriptors.
 * 
 * @author Richard Mueller
 *
 */
public class TypeVisitor extends AbstractJavaSourceVisitor<TypeDescriptor> {

    public TypeVisitor(VisitorHelper visitorHelper) {
        super(visitorHelper);
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, TypeDescriptor typeDescriptor) {
        // class or interface
        createType(classOrInterfaceDeclaration);
        setConstructors(classOrInterfaceDeclaration);
        setVisibility(classOrInterfaceDeclaration);
        setAccessModifier(classOrInterfaceDeclaration);
        setSuperType(classOrInterfaceDeclaration);
        setImplementedInterfaces(classOrInterfaceDeclaration);
        setInnerClasses(classOrInterfaceDeclaration, typeDescriptor);
        setFields(classOrInterfaceDeclaration);
        setMethods(classOrInterfaceDeclaration);
        setAnnotations(classOrInterfaceDeclaration, (AnnotatedDescriptor) descriptor);
    }

    @Override
    public void visit(EnumDeclaration enumDeclaration, TypeDescriptor typeDescriptor) {
        // enum
        createType(enumDeclaration);
        setVisibility(enumDeclaration);
        setEnumConstants(enumDeclaration);
        setInnerClasses(enumDeclaration, typeDescriptor);
        setFields(enumDeclaration);
        setMethods(enumDeclaration);
        setAnnotations(enumDeclaration, (AnnotatedDescriptor) descriptor);

    }

    @Override
    public void visit(AnnotationDeclaration annotationDeclaration, TypeDescriptor typeDescriptor) {
        // annotation
        createType(annotationDeclaration);
        setVisibility(annotationDeclaration);
        setAccessModifier(annotationDeclaration);
        setAnnotationMembers(annotationDeclaration);
    }

    private void createType(TypeDeclaration<?> typeDeclaration) {
        if (typeDeclaration instanceof ClassOrInterfaceDeclaration) {
            if (typeDeclaration.asClassOrInterfaceDeclaration().isInterface()) {
                descriptor = visitorHelper.createType(getQualifiedName(typeDeclaration), visitorHelper.getJavaSourceFileDescriptor(),
                        InterfaceTypeDescriptor.class);
            } else {
                descriptor = visitorHelper.createType(getQualifiedName(typeDeclaration), visitorHelper.getJavaSourceFileDescriptor(),
                        ClassTypeDescriptor.class);
            }
        } else if (typeDeclaration instanceof EnumDeclaration) {
            descriptor = visitorHelper.createType(getQualifiedName(typeDeclaration), visitorHelper.getJavaSourceFileDescriptor(), EnumTypeDescriptor.class);
        } else if (typeDeclaration instanceof AnnotationDeclaration) {
            descriptor = visitorHelper.createType(getQualifiedName(typeDeclaration), visitorHelper.getJavaSourceFileDescriptor(),
                    AnnotationTypeDescriptor.class);
        }
    }

    private void setSuperType(ClassOrInterfaceDeclaration classOrInterfaceDeclaration) {
        // TODO an interface might extend multiple interfaces
        classOrInterfaceDeclaration.getExtendedTypes().forEach(superType -> {
            ((ClassFileDescriptor) descriptor).setSuperClass(visitorHelper.resolveDependency(getQualifiedName(superType), (TypeDescriptor) descriptor));
            setTypeParameterDependency(superType, (TypeDescriptor) descriptor);
        });
    }

    private void setImplementedInterfaces(ClassOrInterfaceDeclaration classOrInterfaceDeclaration) {
        classOrInterfaceDeclaration.getImplementedTypes().forEach(interfaces -> {
            ((ClassFileDescriptor) descriptor).getInterfaces().add(visitorHelper.resolveDependency(getQualifiedName(interfaces), (TypeDescriptor) descriptor));
            setTypeParameterDependency(interfaces, (TypeDescriptor) descriptor);
        });
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

    private void setInnerClasses(TypeDeclaration<?> typeDeclaration, TypeDescriptor parentType) {
        if (typeDeclaration.isNestedType() && parentType != null) {
            parentType.getDeclaredInnerClasses().add((TypeDescriptor) descriptor);
        }
        typeDeclaration.findAll(TypeDeclaration.class, isDirectChild(typeDeclaration)).forEach(innerType -> {
            innerType.accept(new TypeVisitor(visitorHelper), (TypeDescriptor) descriptor);
        });
    }

    private static Predicate<TypeDeclaration> isDirectChild(TypeDeclaration parent) {
        return child -> {
            if (child.getParentNode().isPresent()) {
                return child.getParentNode().get().equals(parent);
            } else {
                return false;
            }
        };
    }
}
