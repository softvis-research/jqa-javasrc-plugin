package org.unileipzig.jqassistant.plugin.parser.api.scanner.visitor;

import java.util.List;
import java.util.Set;

import org.unileipzig.jqassistant.plugin.parser.api.model.ClassTypeDescriptor;
import org.unileipzig.jqassistant.plugin.parser.api.model.EnumTypeDescriptor;
import org.unileipzig.jqassistant.plugin.parser.api.model.InterfaceTypeDescriptor;
import org.unileipzig.jqassistant.plugin.parser.api.model.JavaSourceFileDescriptor;
import org.unileipzig.jqassistant.plugin.parser.api.model.TypeDescriptor;
import org.unileipzig.jqassistant.plugin.parser.impl.scanner.TypeResolver;
import org.unileipzig.jqassistant.plugin.parser.impl.scanner.TypeResolverUtils;

import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.declarations.ResolvedClassDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedEnumDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedInterfaceDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;

/**
 * This visitor handles parsed types, i.e. interfaces, classes, enums, and
 * (annotations), and creates corresponding descriptors. It explicitly calls the
 * visitors for its fields and methods. The type resolver is used to get full
 * qualified names of parsed declarations.
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
	public void visit(ClassOrInterfaceDeclaration classOrInterfaceDeclaration,
			JavaSourceFileDescriptor javaSourceFileDescriptor) {
		super.visit(classOrInterfaceDeclaration, javaSourceFileDescriptor);
		TypeDescriptor classOrInterfaceTypeDescriptor;
		
		if (classOrInterfaceDeclaration.isInterface()) {
			// interface
			// fqn, name
			ResolvedInterfaceDeclaration resolvedInterfaceDeclaration = typeResolver
					.solveDeclaration(classOrInterfaceDeclaration, ResolvedInterfaceDeclaration.class);
			InterfaceTypeDescriptor interfaceTypeDescriptor = typeResolver.createType(
					resolvedInterfaceDeclaration.getQualifiedName(), javaSourceFileDescriptor,
					InterfaceTypeDescriptor.class);
			interfaceTypeDescriptor.setFullQualifiedName(resolvedInterfaceDeclaration.getQualifiedName());
			interfaceTypeDescriptor.setName(classOrInterfaceDeclaration.getNameAsString());
			
			// visibility and access modifiers
			interfaceTypeDescriptor.setVisibility(
					TypeResolverUtils.getAccessSpecifier(classOrInterfaceDeclaration.getModifiers()).getValue());
			interfaceTypeDescriptor.setAbstract(classOrInterfaceDeclaration.isAbstract());
			interfaceTypeDescriptor.setFinal(classOrInterfaceDeclaration.isFinal());
			interfaceTypeDescriptor.setStatic(classOrInterfaceDeclaration.isStatic());

			
			// extends, implements
			List<ResolvedReferenceType> resolvedSuperTypes = resolvedInterfaceDeclaration.getAncestors();
			for (ResolvedReferenceType resolvedSuperType : resolvedSuperTypes) {
				interfaceTypeDescriptor
						.setSuperClass(typeResolver.resolveType(TypeResolverUtils.getQualifiedName(resolvedSuperType)));
			}

			// inner class
			Set<ResolvedReferenceTypeDeclaration> resolvedInnerClasses = resolvedInterfaceDeclaration.internalTypes();
			for (ResolvedReferenceTypeDeclaration resolvedInnerClass : resolvedInnerClasses) {
				interfaceTypeDescriptor.getDeclaredInnerClasses()
						.add(typeResolver.resolveType(resolvedInnerClass.getQualifiedName()));
			
			}
			
			classOrInterfaceTypeDescriptor = interfaceTypeDescriptor;
		} else {
			// class
			// fqn, name
			ResolvedClassDeclaration resolvedClassDeclaration = typeResolver
					.solveDeclaration(classOrInterfaceDeclaration, ResolvedClassDeclaration.class);
			ClassTypeDescriptor classTypeDescriptor = typeResolver.createType(
					resolvedClassDeclaration.getQualifiedName(), javaSourceFileDescriptor, ClassTypeDescriptor.class);
			classTypeDescriptor.setFullQualifiedName(resolvedClassDeclaration.getQualifiedName());
			classTypeDescriptor.setName(classOrInterfaceDeclaration.getNameAsString());
			
			// visibility and access modifiers
			classTypeDescriptor.setVisibility(
					TypeResolverUtils.getAccessSpecifier(classOrInterfaceDeclaration.getModifiers()).getValue());
			classTypeDescriptor.setAbstract(classOrInterfaceDeclaration.isAbstract());
			classTypeDescriptor.setFinal(classOrInterfaceDeclaration.isFinal());
			classTypeDescriptor.setStatic(classOrInterfaceDeclaration.isStatic());

			// extends
			ResolvedReferenceType resolvedSuperType = resolvedClassDeclaration.getSuperClass();
			TypeDescriptor superClassTypeDescriptor = typeResolver
					.resolveType(TypeResolverUtils.getQualifiedName(resolvedSuperType));
			classTypeDescriptor.setSuperClass(superClassTypeDescriptor);

			// implements
			List<ResolvedReferenceType> resolvedInterfaces = resolvedClassDeclaration.getInterfaces();
			for (ResolvedReferenceType resolvedInterface : resolvedInterfaces) {
				classTypeDescriptor.getInterfaces()
						.add(typeResolver.resolveType(TypeResolverUtils.getQualifiedName(resolvedInterface)));
			}

			// inner class
			Set<ResolvedReferenceTypeDeclaration> resolvedInnerClasses = resolvedClassDeclaration.internalTypes();
			for (ResolvedReferenceTypeDeclaration resolvedInnerClass : resolvedInnerClasses) {
				classTypeDescriptor.getDeclaredInnerClasses()
						.add(typeResolver.resolveType(resolvedInnerClass.getQualifiedName()));
			}

			// constructors
			for (ConstructorDeclaration constructor : classOrInterfaceDeclaration.getConstructors()) {
				constructor.accept(new MethodVisitor(typeResolver), classTypeDescriptor);
			}
			
			classOrInterfaceTypeDescriptor = classTypeDescriptor;

		}
		// methods
		for (MethodDeclaration method : classOrInterfaceDeclaration.getMethods()) {
			method.accept(new MethodVisitor(typeResolver), classOrInterfaceTypeDescriptor);
		}

		// fields
		for (FieldDeclaration field : classOrInterfaceDeclaration.getFields()) {
			field.accept(new FieldVisitor(typeResolver), classOrInterfaceTypeDescriptor);
		}
	}

	@Override
	public void visit(EnumDeclaration enumDeclaration, JavaSourceFileDescriptor javaSourceFileDescriptor) {
		super.visit(enumDeclaration, javaSourceFileDescriptor);

		// fqn, name
		ResolvedEnumDeclaration resolvedEnumDeclaration = typeResolver.solveDeclaration(enumDeclaration,
				ResolvedEnumDeclaration.class);
		String fqn = resolvedEnumDeclaration.getQualifiedName();
		EnumTypeDescriptor enumTypeDescriptor = typeResolver.createType(fqn, javaSourceFileDescriptor,
				EnumTypeDescriptor.class);
		enumTypeDescriptor.setFullQualifiedName(fqn);
		enumTypeDescriptor.setName(resolvedEnumDeclaration.getName().toString());

		// visibility and access modifiers
		enumTypeDescriptor
				.setVisibility(TypeResolverUtils.getAccessSpecifier(enumDeclaration.getModifiers()).getValue());
		enumTypeDescriptor.setStatic(enumDeclaration.isStatic());

		// enum constants
		for (EnumConstantDeclaration field : enumDeclaration.getEntries()) {
			field.accept(new FieldVisitor(typeResolver), enumTypeDescriptor);
		}

		// fields
		for (FieldDeclaration field : enumDeclaration.getFields()) {
			field.accept(new FieldVisitor(typeResolver), enumTypeDescriptor);
		}

		// constructors
		// for (ConstructorDeclaration constructor : enumDeclaration.getConstructors())
		// {
		// constructor.accept(new MethodVisitor(typeResolver), enumTypeDescriptor);
		// }
	}
}
