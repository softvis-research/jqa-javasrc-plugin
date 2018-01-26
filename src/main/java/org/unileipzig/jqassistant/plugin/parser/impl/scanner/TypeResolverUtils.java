package org.unileipzig.jqassistant.plugin.parser.impl.scanner;

import java.util.EnumSet;

import org.unileipzig.jqassistant.plugin.parser.api.model.VisibilityModifier;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.resolution.types.ResolvedType;

/**
 * This utility class provides often used methods related to parsing and
 * solving.
 * 
 * @author Richard Müller
 *
 */
public class TypeResolverUtils {

	public static final String CONSTRUCTOR_METHOD = "void <init>";

	/**
	 * Returns the VisibilityModifier for an EnumSet<Modifier> from java parser.
	 * 
	 * @param modifiers
	 * @return VisibilityModifier
	 */
	public static VisibilityModifier getAccessSpecifier(EnumSet<Modifier> modifiers) {
		if (modifiers.contains(Modifier.PUBLIC)) {
			return VisibilityModifier.PUBLIC;
		} else if (modifiers.contains(Modifier.PROTECTED)) {
			return VisibilityModifier.PROTECTED;
		} else if (modifiers.contains(Modifier.PRIVATE)) {
			return VisibilityModifier.PRIVATE;
		} else {
			return VisibilityModifier.DEFAULT;
		}
	}

	/**
	 * Returns the full qualified name of a resolved type.
	 * 
	 * @param resolvedType
	 * @return full qualified name
	 */
	public static String getQualifiedName(ResolvedType resolvedType) {
		String fqn = "";
		if (resolvedType.isVoid()) {
			fqn = resolvedType.describe();
		} else if (resolvedType.isPrimitive()) {
			fqn = resolvedType.asPrimitive().describe();
		} else if (resolvedType.isReferenceType()) {
			fqn = resolvedType.asReferenceType().getTypeDeclaration().getQualifiedName();
		}else if (resolvedType.isArray()) {
			fqn = resolvedType.asArrayType().describe();
		}else if (resolvedType.isTypeVariable()) {
			fqn = resolvedType.asTypeVariable().qualifiedName();
		} else {
			throw new RuntimeException("Type could not be resolved: " + resolvedType.toString());
		}
		return fqn;
	}
}
