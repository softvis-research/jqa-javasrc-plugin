package org.unileipzig.jqassistant.plugin.parser.api.scanner.visitor;

import java.util.List;

import org.unileipzig.jqassistant.plugin.parser.api.model.ConstructorDescriptor;
import org.unileipzig.jqassistant.plugin.parser.api.model.MethodDescriptor;
import org.unileipzig.jqassistant.plugin.parser.api.model.ParameterDescriptor;
import org.unileipzig.jqassistant.plugin.parser.api.model.TypeDescriptor;
import org.unileipzig.jqassistant.plugin.parser.impl.scanner.TypeResolver;
import org.unileipzig.jqassistant.plugin.parser.impl.scanner.TypeResolverUtils;

import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.declarations.ResolvedConstructorDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedParameterDeclaration;

/**
 * This visitor handles parsed methods, i.e. methods and constructors, and
 * creates corresponding descriptors. The type resolver is used to get full
 * qualified names of parsed declarations and to solve types of return types and
 * parameters.
 * 
 * @author Richard Müller
 *
 */
public class MethodVisitor extends VoidVisitorAdapter<TypeDescriptor> {

	private TypeResolver typeResolver;
	private static final String CONSTRUCTOR_METHOD = "void <init>";

	public MethodVisitor(TypeResolver typeResolver) {
		this.typeResolver = typeResolver;
	}

	@Override
	public void visit(MethodDeclaration methodDeclaration, TypeDescriptor typeDescriptor) {
		super.visit(methodDeclaration, typeDescriptor);

		// signature, name
		ResolvedMethodDeclaration resolvedMethodDeclaration = typeResolver.solveDeclaration(methodDeclaration,
				ResolvedMethodDeclaration.class);
		TypeDescriptor returnTypeDescriptor = typeResolver
				.resolveType(TypeResolverUtils.getQualifiedName(resolvedMethodDeclaration.getReturnType()));
		MethodDescriptor methodDescriptor = typeResolver.addMethodDescriptor(typeDescriptor,
				returnTypeDescriptor.getFullQualifiedName() + " " + resolvedMethodDeclaration.getSignature());
		methodDescriptor.setName(resolvedMethodDeclaration.getName());

		// visibility and access modifiers
		methodDescriptor
				.setVisibility(TypeResolverUtils.getAccessSpecifier(methodDeclaration.getModifiers()).getValue());
		methodDescriptor.setAbstract(methodDeclaration.isAbstract());
		methodDescriptor.setFinal(methodDeclaration.isFinal());
		methodDescriptor.setStatic(methodDeclaration.isStatic());

		// parameters
		List<Parameter> parameters = methodDeclaration.getParameters();
		for (int i = 0; i < parameters.size(); i++) {
			ResolvedParameterDeclaration resolvedParameterDeclaration = typeResolver.solveDeclaration(parameters.get(i),
					ResolvedParameterDeclaration.class);
			TypeDescriptor parameterTypeDescriptor = typeResolver
					.resolveType(TypeResolverUtils.getQualifiedName(resolvedParameterDeclaration.getType()));
			ParameterDescriptor parameterDescriptor = typeResolver.addParameterDescriptor(methodDescriptor, i);
			parameterDescriptor.setType(parameterTypeDescriptor);
		}

		// return type
		methodDescriptor.setReturns(returnTypeDescriptor);
	}

	@Override
	public void visit(ConstructorDeclaration constructorDeclaration, TypeDescriptor typeDescriptor) {
		super.visit(constructorDeclaration, typeDescriptor);

		// signature, name
		ResolvedConstructorDeclaration resolvedConstructorDeclaration = typeResolver
				.solveDeclaration(constructorDeclaration, ResolvedConstructorDeclaration.class);
		final String constructorParameter = resolvedConstructorDeclaration.getSignature()
				.replaceAll(resolvedConstructorDeclaration.getName(), "");
		ConstructorDescriptor constructorDescriptor = (ConstructorDescriptor) typeResolver
				.addMethodDescriptor(typeDescriptor, CONSTRUCTOR_METHOD + constructorParameter);
		constructorDescriptor.setName(resolvedConstructorDeclaration.getName());

		// visibility
		constructorDescriptor
				.setVisibility(TypeResolverUtils.getAccessSpecifier(constructorDeclaration.getModifiers()).getValue());

		// parameters
		List<Parameter> parameters = constructorDeclaration.getParameters();
		for (int i = 0; i < parameters.size(); i++) {
			ResolvedParameterDeclaration resolvedParameterDeclaration = typeResolver.solveDeclaration(parameters.get(i),
					ResolvedParameterDeclaration.class);
			TypeDescriptor parameterTypeDescriptor = typeResolver
					.resolveType(TypeResolverUtils.getQualifiedName(resolvedParameterDeclaration.getType()));
			ParameterDescriptor parameterDescriptor = typeResolver.addParameterDescriptor(constructorDescriptor, i);
			parameterDescriptor.setType(parameterTypeDescriptor);
		}
	}

}
