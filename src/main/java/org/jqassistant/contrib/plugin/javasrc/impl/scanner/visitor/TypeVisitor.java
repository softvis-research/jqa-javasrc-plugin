package org.jqassistant.contrib.plugin.javasrc.impl.scanner.visitor;

import java.util.EnumSet;

import com.github.javaparser.ast.Modifier;
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
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
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
        setType(classOrInterfaceDeclaration, javaSourceFileDescriptor);

        setConstructors((Node) classOrInterfaceDeclaration);

        if (classOrInterfaceDeclaration.isInnerClass()) {
            setInnerClasses(classOrInterfaceDeclaration);
        }

        setVisibility(classOrInterfaceDeclaration.getModifiers());

        setAccessModifier(classOrInterfaceDeclaration);

        setSuperType(classOrInterfaceDeclaration);

        setImplementedInterfaces(classOrInterfaceDeclaration);

        setFields(classOrInterfaceDeclaration);

        setMethds(classOrInterfaceDeclaration);

        setAnnotations(classOrInterfaceDeclaration);

        super.visit(classOrInterfaceDeclaration, javaSourceFileDescriptor);
    }

    @Override
    public void visit(EnumDeclaration enumDeclaration, JavaSourceFileDescriptor javaSourceFileDescriptor) {
        setType(enumDeclaration, javaSourceFileDescriptor);

        setVisibility(enumDeclaration.getModifiers());

        setEnumConstants(enumDeclaration);

        setFields(enumDeclaration);

        setMethds(enumDeclaration);

        setAnnotations(enumDeclaration);

        super.visit(enumDeclaration, javaSourceFileDescriptor);
    }

    @Override
    public void visit(AnnotationDeclaration annotationDeclaration, JavaSourceFileDescriptor javaSourceFileDescriptor) {

        setType(annotationDeclaration, javaSourceFileDescriptor);

        setVisibility(annotationDeclaration.getModifiers());

        ((AbstractDescriptor) typeDescriptor).setAbstract(annotationDeclaration.isAbstract());

        setAnnotationMembers(annotationDeclaration);

        super.visit(annotationDeclaration, javaSourceFileDescriptor);
    }

    private void setAnnotationMembers(AnnotationDeclaration annotationDeclaration) {
        for (BodyDeclaration<?> member : annotationDeclaration.getMembers()) {
            if (member.isAnnotationMemberDeclaration()) {
                member.accept(new AnnotationVisitor(typeResolver), (AnnotatedDescriptor) typeDescriptor);
            }
        }
    }

    private void setType(Node type, JavaSourceFileDescriptor javaSourceFileDescriptor) {
        // fqn, name
        if (type instanceof ClassOrInterfaceDeclaration) {
            ClassOrInterfaceDeclaration classOrInterfaceDeclaration = (ClassOrInterfaceDeclaration) type;
            if (classOrInterfaceDeclaration.isInterface()) {
                typeDescriptor = typeResolver.createType(classOrInterfaceDeclaration.resolve().getQualifiedName(), javaSourceFileDescriptor,
                        InterfaceTypeDescriptor.class);
            } else {
                typeDescriptor = typeResolver.createType(classOrInterfaceDeclaration.resolve().getQualifiedName(), javaSourceFileDescriptor,
                        ClassTypeDescriptor.class);
            }
        } else if (type instanceof EnumDeclaration) {
            EnumDeclaration enumDeclaration = (EnumDeclaration) type;
            typeDescriptor = typeResolver.createType(enumDeclaration.resolve().getQualifiedName(), javaSourceFileDescriptor, EnumTypeDescriptor.class);

        } else if (type instanceof AnnotationDeclaration) {
            AnnotationDeclaration annotationDeclaration = (AnnotationDeclaration) type;
            typeDescriptor = typeResolver.createType(annotationDeclaration.resolve().getQualifiedName(), javaSourceFileDescriptor,
                    AnnotationTypeDescriptor.class);
        }

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
        // TODO an interface might extend from multiple interfaces

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

    private void setFields(Node nodeWithFields) {
        for (FieldDeclaration field : ((NodeWithMembers<FieldDeclaration>) nodeWithFields).getFields()) {
            field.accept(new FieldVisitor(typeResolver), typeDescriptor);
        }
    }

    private void setConstructors(Node classOrInterfaceDeclaration) {
        for (ConstructorDeclaration constructors : ((NodeWithConstructors<ConstructorDeclaration>) classOrInterfaceDeclaration).getConstructors()) {
            constructors.accept(new MethodVisitor(typeResolver), typeDescriptor);
        }
    }

    private void setMethds(Node nodeWithMembers) {
        for (MethodDeclaration method : ((NodeWithMembers<MethodDeclaration>) nodeWithMembers).getMethods()) {
            method.accept(new MethodVisitor(typeResolver), typeDescriptor);
        }
    }

    private void setEnumConstants(EnumDeclaration enumDeclaration) {
        for (EnumConstantDeclaration entry : enumDeclaration.getEntries()) {
            entry.accept(new FieldVisitor(typeResolver), typeDescriptor);
        }
    }

    private void setAnnotations(Node nodeWithAnnotations) {
        for (AnnotationExpr annotation : ((NodeWithAnnotations<AnnotationExpr>) nodeWithAnnotations).getAnnotations()) {
            annotation.accept(new AnnotationVisitor(typeResolver), (AnnotatedDescriptor) typeDescriptor);
        }
    }
}
