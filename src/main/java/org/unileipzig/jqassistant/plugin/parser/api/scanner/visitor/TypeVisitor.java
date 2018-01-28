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

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.declarations.ResolvedClassDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedEnumDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedInterfaceDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;

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
	public void visit(ClassOrInterfaceDeclaration classOrInterfaceDeclaration,
			JavaSourceFileDescriptor javaSourceFileDescriptor) {
		super.visit(classOrInterfaceDeclaration, javaSourceFileDescriptor);

		if (classOrInterfaceDeclaration.isInterface()) {
			// interface
			// fqn, name
			ResolvedInterfaceDeclaration resolvedInterfaceDeclaration = classOrInterfaceDeclaration.resolve()
					.asInterface();
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

		} else {
			// class
			// fqn, name
			ResolvedClassDeclaration resolvedClassDeclaration = classOrInterfaceDeclaration.resolve().asClass();
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

		}
	}

	@Override
	public void visit(EnumDeclaration enumDeclaration, JavaSourceFileDescriptor javaSourceFileDescriptor) {
		super.visit(enumDeclaration, javaSourceFileDescriptor);

		// fqn, name
		ResolvedEnumDeclaration resolvedEnumDeclaration = enumDeclaration.resolve();
		EnumTypeDescriptor enumTypeDescriptor = typeResolver.createType(resolvedEnumDeclaration.getQualifiedName(),
				javaSourceFileDescriptor, EnumTypeDescriptor.class);
		enumTypeDescriptor.setFullQualifiedName(resolvedEnumDeclaration.getQualifiedName());
		enumTypeDescriptor.setName(resolvedEnumDeclaration.getName().toString());

		// visibility and access modifiers
		enumTypeDescriptor
				.setVisibility(TypeResolverUtils.getAccessSpecifier(enumDeclaration.getModifiers()).getValue());
		enumTypeDescriptor.setStatic(enumDeclaration.isStatic());

	}
}
