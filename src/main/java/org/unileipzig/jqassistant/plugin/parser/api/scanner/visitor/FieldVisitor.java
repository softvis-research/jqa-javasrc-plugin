package org.unileipzig.jqassistant.plugin.parser.api.scanner.visitor;

import java.util.List;
import java.util.Optional;

import org.unileipzig.jqassistant.plugin.parser.api.model.FieldDescriptor;
import org.unileipzig.jqassistant.plugin.parser.api.model.PrimitiveValueDescriptor;
import org.unileipzig.jqassistant.plugin.parser.api.model.TypeDescriptor;
import org.unileipzig.jqassistant.plugin.parser.impl.scanner.TypeResolver;
import org.unileipzig.jqassistant.plugin.parser.impl.scanner.Utils;

import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.declarations.ResolvedEnumConstantDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;

/**
 * @author Richard Müller
 *
 */
public class FieldVisitor extends VoidVisitorAdapter<TypeDescriptor> {
	private TypeResolver typeResolver;

	public FieldVisitor(TypeResolver typeResolver) {
		this.typeResolver = typeResolver;
	}

	@Override
	public void visit(FieldDeclaration fieldDeclaration, TypeDescriptor typeDescriptor) {
		// signature, name
		ResolvedFieldDeclaration resolvedFieldDeclaration = typeResolver.solveDeclaration(fieldDeclaration, ResolvedFieldDeclaration.class);
		TypeDescriptor fieldTypeDescriptor = typeResolver.getTypeDescriptor(resolvedFieldDeclaration.getType()); 
		FieldDescriptor fieldDescriptor = typeResolver.getFieldDescriptor(typeDescriptor.getFullQualifiedName(), fieldTypeDescriptor.getFullQualifiedName() + " " + resolvedFieldDeclaration.getName());
		fieldDescriptor.setName(resolvedFieldDeclaration.getName());
		
		// visibility and access modifiers
		fieldDescriptor.setVisibility(Utils.getAccessSpecifier(fieldDeclaration.getModifiers()).getValue());
		fieldDescriptor.setFinal(fieldDeclaration.isFinal());
		fieldDescriptor.setStatic(fieldDeclaration.isStatic());

		// type
		fieldDescriptor.setType(fieldTypeDescriptor);
		
		// field value (of first variable)
		// TODO many variables for one field, type of values
		VariableDeclarator variable = fieldDeclaration.getVariables().get(0);
		Optional<Expression> value = variable.getInitializer();
		if (value.isPresent()) {
			PrimitiveValueDescriptor valueDescriptor = typeResolver.getValueDescriptor(PrimitiveValueDescriptor.class);
			if (value.get().isLiteralStringValueExpr()) {
				valueDescriptor.setValue(value.get().toString().replace("\"", ""));
			} else {
				valueDescriptor.setValue(value.get());
			}

			fieldDescriptor.setValue(valueDescriptor);
		}
       
		
		super.visit(fieldDeclaration, typeDescriptor);
	}
	
	@Override
	public void visit(EnumConstantDeclaration enumConstantDeclaration, TypeDescriptor typeDescriptor) {
		// fqn, name
		ResolvedEnumConstantDeclaration resolvedEnumConstantDeclaration = typeResolver.solveDeclaration(enumConstantDeclaration, ResolvedEnumConstantDeclaration.class);
		TypeDescriptor fieldTypeDescriptor = typeResolver.getTypeDescriptor(resolvedEnumConstantDeclaration.getType()); 
		FieldDescriptor fieldDescriptor = typeResolver.getFieldDescriptor(typeDescriptor.getFullQualifiedName(), fieldTypeDescriptor.getFullQualifiedName() + " " + resolvedEnumConstantDeclaration.getName());
		fieldDescriptor.setName(resolvedEnumConstantDeclaration.getName());

		super.visit(enumConstantDeclaration, typeDescriptor);
	}
}
