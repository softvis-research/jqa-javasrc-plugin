/**
 * 
 */
package org.jqassistant.contrib.plugin.javasrc.impl.scanner.solver;

import java.io.File;
import java.util.Iterator;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.Resolvable;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedEnumConstantDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.resolution.types.ResolvedWildcard;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
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
    private MemoryTypeSolver memoryTypeSolver;
    private CombinedTypeSolver combinedTypeSolver;

    public JavaTypeSolver(String srcDir) {
        combinedTypeSolver = new CombinedTypeSolver(new ReflectionTypeSolver(), new JavaParserTypeSolver(new File(srcDir)));
        facade = JavaParserFacade.get(combinedTypeSolver);
        JavaParser.getStaticConfiguration().setSymbolResolver(new JavaSymbolSolver(combinedTypeSolver));
    }

    public void addImportDeclarations(NodeList<ImportDeclaration> importDeclarations) {
        memoryTypeSolver = new MemoryTypeSolver();
        for (ImportDeclaration importDeclaration : importDeclarations) {
            final String importName = importDeclaration.getNameAsString();
            final String name = importName.substring(importName.lastIndexOf(".") + 1);
            memoryTypeSolver.addDeclaration(name, new ResolvedImportTypeDeclaration(name, importName));
        }
        // combinedTypeSolver.add(memoryTypeSolver);
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
            } else if (node instanceof ObjectCreationExpr) {
            } else if (node instanceof Type) {
                resolved = facade.convertToUsage(((Type) node), node);
            } else if (node instanceof Resolvable) {
                resolved = ((Resolvable<?>) node).resolve();
            } else {
                throw new IllegalArgumentException("Unexpected type of parsed node: " + node + " " + node.getClass());
            }
        } catch (UnsolvedSymbolException use) {
            System.out.println("SOMETHING UNSOLVED: " + use.getMessage() + " " + node.getClass());
            SymbolReference<ResolvedReferenceTypeDeclaration> symbolReference = SymbolReference.unsolved(ResolvedReferenceTypeDeclaration.class);
            if (node instanceof ClassOrInterfaceType) {
                ClassOrInterfaceType classOrInterfaceType = ((ClassOrInterfaceType) node).asClassOrInterfaceType();
                symbolReference = memoryTypeSolver.tryToSolveType(classOrInterfaceType.getName().getId());
                if (symbolReference.isSolved()) {
                    resolved = symbolReference.getCorrespondingDeclaration();
                    System.out.println("SOLVED: " + " " + classOrInterfaceType.getNameAsString() + " " + node.getClass());
                } else {
                    System.out.println("STILL UNSOLVED: " + node + " " + classOrInterfaceType.getScope() + " " + classOrInterfaceType.getName().getId() + " "
                            + classOrInterfaceType.getName().getIdentifier() + " " + node.getClass());
                }
            } else if (node instanceof FieldAccessExpr) {
                FieldAccessExpr fieldAccessExpr = ((FieldAccessExpr) node).asFieldAccessExpr();
                if (fieldAccessExpr.getScope().isNameExpr()) {
                    symbolReference = memoryTypeSolver.tryToSolveType(fieldAccessExpr.getScope().asNameExpr().getName().getId());
                    if (symbolReference.isSolved()) {
                        System.out.println("SOLVED: " + fieldAccessExpr.getNameAsString() + " " + node.getClass());
                        resolved = symbolReference.getCorrespondingDeclaration();
                    } else {
                        System.out.println("STILL UNSOLVED: " + " " + fieldAccessExpr.getScope() + " " + fieldAccessExpr.getNameAsString() + " "
                                + fieldAccessExpr.getClass());
                    }
                } else {
                    symbolReference = memoryTypeSolver.tryToSolveType(fieldAccessExpr.getName().getId());
                    if (symbolReference.isSolved()) {
                        System.out.println("SOLVED: " + fieldAccessExpr.getNameAsString() + " " + node.getClass());
                        resolved = symbolReference.getCorrespondingDeclaration();
                    } else {
                        System.out.println("STILL UNSOLVED: " + " " + fieldAccessExpr.getScope() + " " + fieldAccessExpr.getNameAsString() + " "
                                + fieldAccessExpr.getClass());
                    }
                }
            } else if (node instanceof MethodCallExpr) {
                MethodCallExpr methodCallExpr = ((MethodCallExpr) node).asMethodCallExpr();
                symbolReference = memoryTypeSolver.tryToSolveType(methodCallExpr.getScope().get().asNameExpr().getName().getId());
                if (symbolReference.isSolved()) {
                    System.out.println("SOLVED: " + methodCallExpr.getNameAsString() + " " + node.getClass());
                    resolved = symbolReference.getCorrespondingDeclaration();
                    System.out.println(symbolReference.getCorrespondingDeclaration().getQualifiedName());
                } else {
                    System.out.println(
                            "STILL UNSOLVED: " + " " + methodCallExpr.getScope() + " " + methodCallExpr.getNameAsString() + " " + methodCallExpr.getClass());
                }
            } else if (node instanceof AnnotationExpr) {
                AnnotationExpr annotationExpr = ((AnnotationExpr) node).asAnnotationExpr();
                symbolReference = memoryTypeSolver.tryToSolveType(annotationExpr.getName().getId());
                if (symbolReference.isSolved()) {
                    System.out.println("SOLVED: " + annotationExpr.getNameAsString() + " " + node.getClass());
                    resolved = symbolReference.getCorrespondingDeclaration();
                    System.out.println(symbolReference.getCorrespondingDeclaration().getQualifiedName());
                } else {
                    System.out.println("STILL UNSOLVED: " + " " + annotationExpr.getName().getId() + " " + annotationExpr.getNameAsString() + " "
                            + annotationExpr.getClass());
                }
            }
        }
        if (resolved != null) {
            return resolved;
        } else {
            return new ResolvedImportTypeDeclaration("UNKNOWN", "UNKNOWN");
        }
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
        } else if (resolved instanceof ResolvedValueDeclaration) {
            return getQualifiedName(((ResolvedValueDeclaration) resolved).getType());
        } else
            throw new IllegalArgumentException("Unexpected type of resolved node: " + resolved + " " + resolved.getClass());
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
        } else if (resolvedType.isWildcard()) {
            ResolvedWildcard wildcard = resolvedType.asWildcard();
            if (wildcard.isBounded()) {
                return wildcard.getBoundedType().describe();
            } else {
                return wildcard.describe();
            }
        } else {
            throw new IllegalArgumentException("Unexpected type of resolved type: " + resolvedType + " " + resolvedType.getClass());
        }
    }

    public String getQualifiedSignature(String name, NodeList<Parameter> parameters) {
        String signature = name + "(";
        for (Iterator<Parameter> iterator = parameters.iterator(); iterator.hasNext();) {
            Parameter parameter = (Parameter) iterator.next();
            signature += getQualifiedName(parameter.getType());
            if (iterator.hasNext()) {
                signature += ",";
            }
        }
        signature += ")";
        return signature;
    }

    private ResolvedValueDeclaration solveValueDeclaration(NameExpr nameExpr) {
        SymbolReference<? extends ResolvedValueDeclaration> symbolReference = facade.solve(nameExpr);
        if (symbolReference.isSolved()) {
            return symbolReference.getCorrespondingDeclaration();
        } else {
            throw new UnsolvedSymbolException("Variable declaration could not be solved: " + nameExpr.getNameAsString() + " " + nameExpr.getClass());
        }
    }

    private ResolvedMethodDeclaration solveMethodCall(MethodCallExpr methodCallExpr) throws UnsolvedSymbolException {
        System.out.println("METHOD: " + getQualifiedName(facade.getType(methodCallExpr)));
        SymbolReference<ResolvedMethodDeclaration> symbolReference = facade.solve(methodCallExpr);
        if (symbolReference.isSolved()) {
            return symbolReference.getCorrespondingDeclaration();
        } else {

            throw new UnsolvedSymbolException("Method call could not be solved: " + methodCallExpr);
        }
    }

    private ResolvedTypeDeclaration solveAnnotation(AnnotationExpr annotationExpr) throws UnsolvedSymbolException {
        // check if use of facade is possible with new version 3.6.0
        Context context = JavaParserFactory.getContext(annotationExpr, facade.getTypeSolver());
        SymbolReference<ResolvedTypeDeclaration> symbolReference = context.solveType(annotationExpr.getNameAsString(), facade.getTypeSolver());
        if (symbolReference.isSolved()) {
            return symbolReference.getCorrespondingDeclaration();
        } else {
            throw new UnsolvedSymbolException("Annotation could not be solved: " + annotationExpr);
        }
    }

    private ResolvedFieldDeclaration solveFieldAccess(FieldAccessExpr fieldAccessExpr) throws UnsolvedSymbolException {
        System.out.println("TYPE: " + getQualifiedName(facade.getType(fieldAccessExpr)));
        SymbolReference<ResolvedFieldDeclaration> symbolFieldReference = facade.solve(fieldAccessExpr);
        if (symbolFieldReference.isSolved()) {
            return symbolFieldReference.getCorrespondingDeclaration();
        } else {
            throw new UnsolvedSymbolException("Field access could not be solved: " + fieldAccessExpr);

        }
    }

    private ResolvedType solveObjectCreation(ObjectCreationExpr objectCreationExpr) throws UnsolvedSymbolException {
        // System.out.println("TYPE: " +
        // getQualifiedName(facade.getType(objectCreationExpr)));
        ResolvedType resolvedType = facade.convertToUsage(objectCreationExpr.getType(), objectCreationExpr);
        if (resolvedType != null) {
            return resolvedType;
        } else {
            throw new UnsolvedSymbolException("ObjectCreationExpr could not be solved: " + objectCreationExpr);

        }
    }

}
