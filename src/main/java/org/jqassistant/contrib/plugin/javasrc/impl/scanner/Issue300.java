package org.jqassistant.contrib.plugin.javasrc.impl.scanner;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeParameterDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.core.resolution.Context;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFactory;
import com.github.javaparser.symbolsolver.javaparsermodel.contexts.FieldAccessContext;
import com.github.javaparser.symbolsolver.model.resolution.SymbolReference;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionClassDeclaration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;


/**
 * See Github issue #300: Getting the FieldDeclaration from a FieldAccessExpr
 * https://github.com/javaparser/javasymbolsolver/issues/300
 * Expected to be natively implemented in future versions of JavaSymbolSolver -> Workaround until then
 */
public class Issue300 {
    public static SymbolReference<ResolvedFieldDeclaration> solve(FieldAccessExpr fa, JavaParserFacade jp) {
        TypeSolver typeSolver = jp.getTypeSolver();
        FieldAccessContext ctx = ((FieldAccessContext) JavaParserFactory.getContext(fa, typeSolver));
        Optional<Expression> scope = Optional.of(fa.getScope());
        Collection<ResolvedReferenceTypeDeclaration> rt = findTypeDeclarations(fa, scope, ctx, jp);
        for (ResolvedReferenceTypeDeclaration r : rt) {
            try {
                return SymbolReference.solved(r.getField(fa.getName().getId()));
            } catch (Throwable t) {
            }
        }
        return SymbolReference.unsolved(ResolvedFieldDeclaration.class);
    }

    private static Collection<ResolvedReferenceTypeDeclaration> findTypeDeclarations(
        Node node, Optional<Expression> scope, Context ctx, JavaParserFacade jp) {
        Collection<ResolvedReferenceTypeDeclaration> rt = new ArrayList<>();
        TypeSolver typeSolver = jp.getTypeSolver();
        SymbolReference<ResolvedTypeDeclaration> ref = null;
        if (scope.isPresent()) {
            if (scope.get() instanceof NameExpr) {
                NameExpr scopeAsName = (NameExpr) scope.get();
                ref = ctx.solveType(scopeAsName.getName().getId(), typeSolver);
            }
            if (ref == null || !ref.isSolved()) {
                ResolvedType typeOfScope = jp.getType(scope.get());
                if (typeOfScope.isWildcard()) {
                    if (typeOfScope.asWildcard().isExtends() || typeOfScope.asWildcard().isSuper()) {
                        rt.add(typeOfScope.asWildcard().getBoundedType().asReferenceType().getTypeDeclaration());
                    } else {
                        rt.add(new ReflectionClassDeclaration(Object.class, typeSolver).asReferenceType());
                    }
                } else if (typeOfScope.isArray()) {
                    rt.add(new ReflectionClassDeclaration(Object.class, typeSolver).asReferenceType());
                } else if (typeOfScope.isTypeVariable()) {
                    for (ResolvedTypeParameterDeclaration.Bound bound : typeOfScope.asTypeParameter().getBounds()) {
                        rt.add(bound.getType().asReferenceType().getTypeDeclaration());
                    }
                } else if (typeOfScope.isConstraint()) {
                    rt.add(typeOfScope.asConstraintType().getBound().asReferenceType().getTypeDeclaration());
                } else {
                    rt.add(typeOfScope.asReferenceType().getTypeDeclaration());
                }
            } else {
                rt.add(ref.getCorrespondingDeclaration().asReferenceType());
            }
        } else {
            rt.add(jp.getTypeOfThisIn(node).asReferenceType().getTypeDeclaration());
        }
        return rt;
    }
}
