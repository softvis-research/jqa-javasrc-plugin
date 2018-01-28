package org.unileipzig.jqassistant.plugin.parser.impl.scanner;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.unileipzig.jqassistant.plugin.parser.api.model.ConstructorDescriptor;
import org.unileipzig.jqassistant.plugin.parser.api.model.FieldDescriptor;
import org.unileipzig.jqassistant.plugin.parser.api.model.JavaSourceFileDescriptor;
import org.unileipzig.jqassistant.plugin.parser.api.model.MethodDescriptor;
import org.unileipzig.jqassistant.plugin.parser.api.model.ParameterDescriptor;
import org.unileipzig.jqassistant.plugin.parser.api.model.TypeDescriptor;

import com.buschmais.jqassistant.core.scanner.api.ScannerContext;
import com.buschmais.jqassistant.plugin.common.api.model.ValueDescriptor;
import com.buschmais.jqassistant.plugin.common.api.scanner.FileResolver;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.AnnotationMemberDeclaration;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedAnnotationDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedAnnotationMemberDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedClassDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedConstructorDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedEnumConstantDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedEnumDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodLikeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedParameterDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.javaparser.Navigator;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFactory;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserAnnotationMemberDeclaration;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserConstructorDeclaration;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserEnumConstantDeclaration;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserFieldDeclaration;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserMethodDeclaration;
import com.github.javaparser.symbolsolver.model.resolution.SymbolReference;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

/**
 * The type resolver has two main tasks. First, it holds an instance of the
 * java symbol solver to solve parsed java types. Second, it caches the parsed
 * types and provides concrete descriptors.
 * 
 * @author Richard Müller
 *
 */
public class TypeResolver {
	private TypeSolver javaTypeSolver;
	private ScannerContext scannerContext;
	private Map<String, TypeDescriptor> containedTypes = new HashMap<>();
	private Map<String, TypeDescriptor> requiredTypes = new HashMap<>();

	public TypeResolver(String srcDir, ScannerContext scannerContext) {
		this.javaTypeSolver = new CombinedTypeSolver(new ReflectionTypeSolver(),
				new JavaParserTypeSolver(new File(srcDir)));
		JavaParser.setStaticConfiguration(
				new ParserConfiguration().setSymbolResolver(new JavaSymbolSolver(javaTypeSolver)));
		this.containedTypes = new HashMap<>();
		this.requiredTypes = new HashMap<>();
		this.scannerContext = scannerContext;
	}

	public <T extends TypeDescriptor> T createType(String fqn, JavaSourceFileDescriptor javaSourcefileDescriptor,
			Class<T> type) {
		TypeDescriptor resolvedTypeDescriptor = javaSourcefileDescriptor.resolveType(fqn);
		T typeDescriptor;
		if (requiredTypes.containsKey(fqn)) {
			typeDescriptor = scannerContext.getStore().migrate(requiredTypes.get(fqn), type);
			requiredTypes.remove(fqn);
		} else {
			typeDescriptor = scannerContext.getStore().addDescriptorType(resolvedTypeDescriptor, type);
		}
		containedTypes.put(fqn, typeDescriptor);
		return typeDescriptor;
	}

	public TypeDescriptor resolveType(String fqn) {
		TypeDescriptor typeDescriptor;
		if (containedTypes.containsKey(fqn)) {
			return typeDescriptor = containedTypes.get(fqn);
		} else if (requiredTypes.containsKey(fqn)) {
			return typeDescriptor = requiredTypes.get(fqn);
		} else {
			String fileName = "/" + fqn.replace('.', '/') + ".java"; // Inner classes?
			FileResolver fileResolver = scannerContext.peek(FileResolver.class);
			JavaSourceFileDescriptor sourceFileDescriptor = fileResolver.require(fileName,
					JavaSourceFileDescriptor.class, scannerContext);
			typeDescriptor = sourceFileDescriptor.resolveType(fqn);
			requiredTypes.put(fqn, typeDescriptor);
		}
		return typeDescriptor;
	}

	public MethodDescriptor addMethodDescriptor(String parentFQN, String signature) {
		TypeDescriptor parentType = resolveType(parentFQN);
		MethodDescriptor methodDescriptor;
		if (signature.startsWith(TypeResolverUtils.CONSTRUCTOR_METHOD)) {
			methodDescriptor = scannerContext.getStore().create(ConstructorDescriptor.class);
		} else {
			methodDescriptor = scannerContext.getStore().create(MethodDescriptor.class);
		}
		methodDescriptor.setSignature(signature);
		parentType.getDeclaredMethods().add(methodDescriptor);

		return methodDescriptor;
	}

	public FieldDescriptor addFieldDescriptor(String parentFQN, String signature) {
		TypeDescriptor parentType = resolveType(parentFQN);
		FieldDescriptor fieldDescriptor = scannerContext.getStore().create(FieldDescriptor.class);
		fieldDescriptor.setSignature(signature);
		parentType.getDeclaredFields().add(fieldDescriptor);

		return fieldDescriptor;
	}

	public ParameterDescriptor addParameterDescriptor(MethodDescriptor methodDescriptor, int index) {
		ParameterDescriptor parameterDescriptor = scannerContext.getStore().create(ParameterDescriptor.class);
		parameterDescriptor.setIndex(index);
		methodDescriptor.getParameters().add(parameterDescriptor);
		return parameterDescriptor;
	}

	public <T extends ValueDescriptor<?>> T getValueDescriptor(Class<T> valueDescriptorType) {
		return scannerContext.getStore().create(valueDescriptorType);
	}

	public <T> T solveDeclaration(Node node, Class<T> resultClass) {
		if (node instanceof MethodDeclaration) {
			return resultClass.cast(new JavaParserMethodDeclaration((MethodDeclaration) node, javaTypeSolver));
		}
		if (node instanceof ClassOrInterfaceDeclaration) {
			ResolvedReferenceTypeDeclaration resolved = JavaParserFactory.toTypeDeclaration(node, javaTypeSolver);
			if (resultClass.isInstance(resolved)) {
				return resultClass.cast(resolved);
			}
		}
		if (node instanceof EnumDeclaration) {
			ResolvedReferenceTypeDeclaration resolved = JavaParserFactory.toTypeDeclaration(node, javaTypeSolver);
			if (resultClass.isInstance(resolved)) {
				return resultClass.cast(resolved);
			}
		}
		if (node instanceof EnumConstantDeclaration) {
			ResolvedEnumDeclaration enumDeclaration = Navigator.findAncestor(node, EnumDeclaration.class).get()
					.resolve().asEnum();
			ResolvedEnumConstantDeclaration resolved = enumDeclaration.getEnumConstants().stream()
					.filter(c -> ((JavaParserEnumConstantDeclaration) c).getWrappedNode() == node).findFirst().get();
			if (resultClass.isInstance(resolved)) {
				return resultClass.cast(resolved);
			}
		}
		if (node instanceof ConstructorDeclaration) {
			ConstructorDeclaration constructorDeclaration = (ConstructorDeclaration) node;
			Node classOrInterfaceDeclaration = (ClassOrInterfaceDeclaration) node.getParentNode().get();
			ResolvedClassDeclaration resolvedClass = solveDeclaration(classOrInterfaceDeclaration,
					ResolvedClassDeclaration.class).asClass();
			ResolvedConstructorDeclaration resolved = resolvedClass.getConstructors().stream()
					.filter(c -> ((JavaParserConstructorDeclaration) c).getWrappedNode() == constructorDeclaration)
					.findFirst().get();
			if (resultClass.isInstance(resolved)) {
				return resultClass.cast(resolved);
			}
		}
		if (node instanceof AnnotationDeclaration) {
			ResolvedReferenceTypeDeclaration resolved = JavaParserFactory.toTypeDeclaration(node, javaTypeSolver);
			if (resultClass.isInstance(resolved)) {
				return resultClass.cast(resolved);
			}
		}
		if (node instanceof AnnotationMemberDeclaration) {
			ResolvedAnnotationDeclaration annotationDeclaration = Navigator
					.findAncestor(node, AnnotationDeclaration.class).get().resolve();
			ResolvedAnnotationMemberDeclaration resolved = annotationDeclaration.getAnnotationMembers().stream()
					.filter(c -> ((JavaParserAnnotationMemberDeclaration) c).getWrappedNode() == node).findFirst()
					.get();
			if (resultClass.isInstance(resolved)) {
				return resultClass.cast(resolved);
			}
		}
		if (node instanceof FieldDeclaration) {
			FieldDeclaration fieldDeclaration = (FieldDeclaration) node;
			if (fieldDeclaration.getVariables().size() != 1) {
				throw new RuntimeException(
						"Cannot resolve a Field Declaration including multiple variable declarators. Resolve the single variable declarators");
			}
			ResolvedFieldDeclaration resolved = new JavaParserFieldDeclaration(fieldDeclaration.getVariable(0),
					javaTypeSolver);
			if (resultClass.isInstance(resolved)) {
				return resultClass.cast(resolved);
			}
		}
		if (node instanceof VariableDeclarator) {
			ResolvedFieldDeclaration resolved = new JavaParserFieldDeclaration((VariableDeclarator) node,
					javaTypeSolver);
			if (resultClass.isInstance(resolved)) {
				return resultClass.cast(resolved);
			}
		}
		if (node instanceof MethodCallExpr) {
			SymbolReference<ResolvedMethodDeclaration> result = JavaParserFacade.get(javaTypeSolver)
					.solve((MethodCallExpr) node);
			if (result.isSolved()) {
				if (resultClass.isInstance(result.getCorrespondingDeclaration())) {
					return resultClass.cast(result.getCorrespondingDeclaration());
				}
			} else {
				throw new UnsolvedSymbolException(
						"We are unable to find the method declaration corresponding to " + node);
			}
		}
		if (node instanceof NameExpr) {
			SymbolReference<? extends ResolvedValueDeclaration> result = JavaParserFacade.get(javaTypeSolver)
					.solve((NameExpr) node);
			if (result.isSolved()) {
				if (resultClass.isInstance(result.getCorrespondingDeclaration())) {
					return resultClass.cast(result.getCorrespondingDeclaration());
				}
			} else {
				throw new UnsolvedSymbolException(
						"We are unable to find the value declaration corresponding to " + node);
			}
		}
		if (node instanceof ThisExpr) {
			SymbolReference<ResolvedTypeDeclaration> result = JavaParserFacade.get(javaTypeSolver)
					.solve((ThisExpr) node);
			if (result.isSolved()) {
				if (resultClass.isInstance(result.getCorrespondingDeclaration())) {
					return resultClass.cast(result.getCorrespondingDeclaration());
				}
			} else {
				throw new UnsolvedSymbolException(
						"We are unable to find the type declaration corresponding to " + node);
			}
		}
		if (node instanceof ExplicitConstructorInvocationStmt) {
			SymbolReference<ResolvedConstructorDeclaration> result = JavaParserFacade.get(javaTypeSolver)
					.solve((ExplicitConstructorInvocationStmt) node);
			if (result.isSolved()) {
				if (resultClass.isInstance(result.getCorrespondingDeclaration())) {
					return resultClass.cast(result.getCorrespondingDeclaration());
				}
			} else {
				throw new UnsolvedSymbolException(
						"We are unable to find the constructor declaration corresponding to " + node);
			}
		}
		if (node instanceof Parameter) {
			if (ResolvedParameterDeclaration.class.equals(resultClass)) {
				Parameter parameter = (Parameter) node;
				CallableDeclaration callableDeclaration = Navigator.findAncestor(node, CallableDeclaration.class).get();
				ResolvedMethodLikeDeclaration resolvedMethodLikeDeclaration;
				if (callableDeclaration.isConstructorDeclaration()) {
					resolvedMethodLikeDeclaration = callableDeclaration.asConstructorDeclaration().resolve();
				} else {
					resolvedMethodLikeDeclaration = callableDeclaration.asMethodDeclaration().resolve();
				}
				for (int i = 0; i < resolvedMethodLikeDeclaration.getNumberOfParams(); i++) {
					if (resolvedMethodLikeDeclaration.getParam(i).getName().equals(parameter.getNameAsString())) {
						return resultClass.cast(resolvedMethodLikeDeclaration.getParam(i));
					}
				}
			}
		}
		throw new UnsupportedOperationException("Unable to find the declaration of type " + resultClass.getSimpleName()
				+ " from " + node.getClass().getSimpleName());
	}
}
