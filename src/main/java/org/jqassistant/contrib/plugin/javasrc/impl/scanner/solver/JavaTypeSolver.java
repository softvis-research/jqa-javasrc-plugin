/**
 * 
 */
package org.jqassistant.contrib.plugin.javasrc.impl.scanner.solver;

import java.io.File;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.Resolvable;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedConstructorDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedEnumConstantDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.core.resolution.Context;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFactory;
import com.github.javaparser.symbolsolver.model.resolution.SymbolReference;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.MemoryTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

/**
 * @author Richard Müller
 *
 */
public class JavaTypeSolver {
    private JavaParserFacade facade;
    private CombinedTypeSolver combinedTypeSolver;

    public JavaTypeSolver(String srcDir) {
        combinedTypeSolver = new CombinedTypeSolver(new ReflectionTypeSolver(), new JavaParserTypeSolver(new File(srcDir)));
        facade = JavaParserFacade.get(combinedTypeSolver);
        JavaParser.getStaticConfiguration().setSymbolResolver(new com.github.javaparser.symbolsolver.JavaSymbolSolver(combinedTypeSolver));
    }

    public void addImportDeclarations(NodeList<ImportDeclaration> importDeclarations) {
        MemoryTypeSolver memoryTypeSolver = new MemoryTypeSolver();
        for (ImportDeclaration importDeclaration : importDeclarations) {
            final String name = importDeclaration.getNameAsString();
            memoryTypeSolver.addDeclaration(name, new ResolvedImportTypeDeclaration(name));
        }
        this.combinedTypeSolver.add(memoryTypeSolver);
    }

    public Object solve(Node node) {
        Object resolved = null;
        try {
            if (node instanceof MethodCallExpr) {
                resolved = solveMethodCall(((MethodCallExpr) node));
            } else if (node instanceof NameExpr) {
                resolved = solveValueDeclaration(((NameExpr) node));
            } else if (node instanceof FieldAccessExpr) {
                resolved = solveFieldAccess((FieldAccessExpr) node);
            } else if (node instanceof AnnotationExpr) {
                resolved = solveAnnotation(((AnnotationExpr) node));
            } else if (node instanceof Type) {
                resolved = facade.convertToUsage(((Type) node));
            } else if (node instanceof Resolvable) {
                resolved = ((Resolvable<?>) node).resolve();
            } else {
                throw new IllegalArgumentException("Unexpected type of parsed node: " + node + " " + node.getClass());
            }

        } catch (UnsolvedSymbolException use) {
            System.out.println("SOMETHING UNSOLVED in solve: " + use.getMessage());
            // TODO handle unsolved types
        }

        return resolved;
    }

    public String getQualifiedName(Node node) {
        Object resolved = solve(node);
        if (resolved instanceof ResolvedTypeDeclaration) {
            return ((ResolvedTypeDeclaration) resolved).asType().getQualifiedName();
        } else if (resolved instanceof ResolvedType) {
            return getQualifiedName((ResolvedType) resolved);
        } else if (resolved instanceof ResolvedEnumConstantDeclaration) {
            return getQualifiedName(((ResolvedEnumConstantDeclaration) resolved).getType());
        } else if (resolved instanceof ResolvedFieldDeclaration) {
            return getQualifiedName(((ResolvedFieldDeclaration) resolved).getType());
        } else {
            throw new IllegalArgumentException("Unexpected type of resolved node: " + resolved + " " + resolved.getClass());
        }
    }

    public String getQualifiedName(ResolvedType resolvedType) {
        if (resolvedType.isReferenceType()) {
            return resolvedType.asReferenceType().getQualifiedName();
        } else if (resolvedType.isPrimitive()) {
            return resolvedType.asPrimitive().describe();
        } else if (resolvedType.isVoid()) {
            return resolvedType.describe();
        } else if (resolvedType.isArray()) {
            return resolvedType.asArrayType().describe();
        } else if (resolvedType.isTypeVariable()) {
            return resolvedType.asTypeVariable().qualifiedName();
        } else {
            throw new IllegalArgumentException("Unexpected type of resolved type: " + resolvedType + " " + resolvedType.getClass());
        }
    }

    public String getQualifiedSignature(BodyDeclaration<?> bodyDeclaration) {
        Object resolved = solve(bodyDeclaration);

        if (resolved instanceof ResolvedMethodDeclaration) {
            return ((ResolvedMethodDeclaration) resolved).getSignature();
        } else if (resolved instanceof ResolvedConstructorDeclaration) {
            ResolvedConstructorDeclaration resolvedConstructorDeclaration = ((ResolvedConstructorDeclaration) resolved);
            return resolvedConstructorDeclaration.getSignature().replaceAll(resolvedConstructorDeclaration.getName(), "");
        } else {
            throw new IllegalArgumentException("Unexpected type of resolved body declaration: " + resolved + " " + resolved.getClass());
        }
    }

    private ResolvedValueDeclaration solveValueDeclaration(NameExpr nameExpr) {
        SymbolReference<? extends ResolvedValueDeclaration> symbolReference = facade.solve(nameExpr);
        if (symbolReference.isSolved()) {
            return symbolReference.getCorrespondingDeclaration();
        } else {
            throw new UnsolvedSymbolException("Variable declaration could not be solved: " + nameExpr);
        }
    }

    private ResolvedMethodDeclaration solveMethodCall(MethodCallExpr methodCallExpr) throws UnsolvedSymbolException {
        SymbolReference<ResolvedMethodDeclaration> symbolReference = facade.solve(methodCallExpr);
        if (symbolReference.isSolved()) {
            return symbolReference.getCorrespondingDeclaration();
        } else {
            throw new UnsolvedSymbolException("Method call could not be solved: " + methodCallExpr);
        }
    }

    private ResolvedTypeDeclaration solveAnnotation(AnnotationExpr annotationExpr) throws UnsolvedSymbolException {
        Context context = JavaParserFactory.getContext(annotationExpr, facade.getTypeSolver());
        ResolvedTypeDeclaration resolvedTypeDeclaration = context.solveType(annotationExpr.getNameAsString(), facade.getTypeSolver())
                .getCorrespondingDeclaration();
        if (resolvedTypeDeclaration != null) {
            return resolvedTypeDeclaration;
        } else {
            throw new UnsolvedSymbolException("Annotation could not be solved: " + annotationExpr);
        }
    }

    private ResolvedFieldDeclaration solveFieldAccess(FieldAccessExpr fieldAccessExpr) throws UnsolvedSymbolException {
        SymbolReference<ResolvedFieldDeclaration> symbolReference = facade.solve(fieldAccessExpr);
        if (symbolReference.isSolved()) {
            return symbolReference.getCorrespondingDeclaration();
        } else {
            throw new UnsolvedSymbolException("Field access could not be solved: " + fieldAccessExpr);
        }
    }
}
