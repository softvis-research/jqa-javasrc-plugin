package org.jqassistant.contrib.plugin.javasrc.impl.scanner;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.buschmais.jqassistant.core.scanner.api.ScannerContext;
import com.buschmais.jqassistant.plugin.common.api.model.ValueDescriptor;
import com.buschmais.jqassistant.plugin.common.api.scanner.FileResolver;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeParameterDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.core.resolution.Context;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFactory;
import com.github.javaparser.symbolsolver.javaparsermodel.contexts.FieldAccessContext;
import com.github.javaparser.symbolsolver.model.resolution.SymbolReference;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionClassDeclaration;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.jqassistant.contrib.plugin.javasrc.api.model.AnnotatedDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.AnnotationValueDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.ConstructorDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.FieldDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.InvokesDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.JavaSourceFileDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.MethodDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.ParameterDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.ReadsDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.TypeDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.WritesDescriptor;

/**
 * The type resolver has two main tasks. First, it holds an instance of the java
 * symbol solver to solve parsed java types. Second, it caches the parsed types
 * and provides concrete descriptors.
 * 
 * @author Richard Müller
 *
 */
public class TypeResolver {
    private TypeSolver javaTypeSolver;
    private ScannerContext scannerContext;
    private Map<String, TypeDescriptor> containedTypes = new HashMap<>();
    private Map<String, FieldDescriptor> containedFields = new HashMap<>();
    private Map<String, MethodDescriptor> containedMethods = new HashMap<>();
    private Map<String, TypeDescriptor> requiredTypes = new HashMap<>();

    public TypeResolver(String srcDir, ScannerContext scannerContext) {
        this.javaTypeSolver = new CombinedTypeSolver(new ReflectionTypeSolver(), new JavaParserTypeSolver(new File(srcDir)));
        JavaParser.setStaticConfiguration(new ParserConfiguration().setSymbolResolver(new JavaSymbolSolver(javaTypeSolver)));
        this.containedTypes = new HashMap<>();
        this.containedFields = new HashMap<>();
        this.containedMethods = new HashMap<>();
        this.requiredTypes = new HashMap<>();
        this.scannerContext = scannerContext;
    }

    public <T extends TypeDescriptor> T createType(String fqn, JavaSourceFileDescriptor javaSourcefileDescriptor, Class<T> type) {
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
        // TODO remove returns?
        TypeDescriptor typeDescriptor;
        if (containedTypes.containsKey(fqn)) {
            return typeDescriptor = containedTypes.get(fqn);
        } else if (requiredTypes.containsKey(fqn)) {
            return typeDescriptor = requiredTypes.get(fqn);
        } else {
            String fileName = "/" + fqn.replace('.', '/') + ".java"; // Inner
                                                                     // classes?
            FileResolver fileResolver = scannerContext.peek(FileResolver.class);
            JavaSourceFileDescriptor sourceFileDescriptor = fileResolver.require(fileName, JavaSourceFileDescriptor.class, scannerContext);
            typeDescriptor = sourceFileDescriptor.resolveType(fqn);
            requiredTypes.put(fqn, typeDescriptor);
        }
        return typeDescriptor;
    }

    public FieldDescriptor resolveField(String signature) {
        FieldDescriptor fieldDescriptor = null;
        if (containedFields.containsKey(signature)) {
            return fieldDescriptor = containedFields.get(signature);
        } else {
            String fqn = signature.substring(0, signature.indexOf(" "));
            TypeDescriptor typeDescriptor = resolveType(fqn);
            fieldDescriptor = addFieldDescriptor(fqn, signature);
            return fieldDescriptor;
        }
    }

    public MethodDescriptor resolveMethod(String signature) {
        MethodDescriptor methodDescriptor = null;
        if (containedMethods.containsKey(signature)) {
            methodDescriptor = containedMethods.get(signature);
        } else {
            throw new RuntimeException("MethodDescriptor not found: " + signature);
        }
        return methodDescriptor;
    }

    public MethodDescriptor addMethodDescriptor(String parentFQN, String signature) {
        if (containedMethods.containsKey(signature)) {
            return containedMethods.get(signature);
        } else {
            TypeDescriptor parentType = resolveType(parentFQN);
            MethodDescriptor methodDescriptor;
            if (signature.startsWith(TypeResolverUtils.VOID + " " + TypeResolverUtils.CONSTRUCTOR_NAME)) {
                methodDescriptor = scannerContext.getStore().create(ConstructorDescriptor.class);
            } else {
                methodDescriptor = scannerContext.getStore().create(MethodDescriptor.class);
            }
            methodDescriptor.setSignature(signature);
            parentType.getDeclaredMethods().add(methodDescriptor);

            containedMethods.put(signature, methodDescriptor);

            return methodDescriptor;
        }
    }

    public FieldDescriptor addFieldDescriptor(String parentFQN, String signature) {
        if (containedFields.containsKey(signature)) {
            return containedFields.get(signature);
        } else {
            TypeDescriptor parentType = resolveType(parentFQN);
            FieldDescriptor fieldDescriptor = scannerContext.getStore().create(FieldDescriptor.class);
            fieldDescriptor.setSignature(signature);
            parentType.getDeclaredFields().add(fieldDescriptor);

            containedFields.put(signature, fieldDescriptor);

            return fieldDescriptor;
        }
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

    public AnnotationValueDescriptor addAnnotationValueDescriptor(AnnotationExpr annotation, AnnotatedDescriptor annotatedDescriptor) {
        if (annotatedDescriptor != null) {
            AnnotationValueDescriptor annotationValueDescriptor = scannerContext.getStore().create(AnnotationValueDescriptor.class);
            annotationValueDescriptor.setType(resolveType(resolveAnnotation(annotation).getQualifiedName()));
            annotationValueDescriptor.setName(annotation.getNameAsString());
            annotatedDescriptor.getAnnotatedBy().add(annotationValueDescriptor);
            return annotationValueDescriptor;
        } else {
            AnnotationValueDescriptor annotationValueDescriptor = scannerContext.getStore().create(AnnotationValueDescriptor.class);
            annotationValueDescriptor.setType(resolveType(resolveAnnotation(annotation).getQualifiedName()));
            annotationValueDescriptor.setName(annotation.getNameAsString());
            return annotationValueDescriptor;
        }
    }

    public void addInvokes(MethodDescriptor methodDescriptor, final Integer lineNumber, MethodDescriptor invokedMethodDescriptor) {
        InvokesDescriptor invokesDescriptor = scannerContext.getStore().create(methodDescriptor, InvokesDescriptor.class, invokedMethodDescriptor);
        invokesDescriptor.setLineNumber(lineNumber);
    }

    public void addReads(MethodDescriptor methodDescriptor, final Integer lineNumber, FieldDescriptor fieldDescriptor) {
        ReadsDescriptor readsDescriptor = scannerContext.getStore().create(methodDescriptor, ReadsDescriptor.class, fieldDescriptor);
        readsDescriptor.setLineNumber(lineNumber);
    }

    public void addWrites(MethodDescriptor methodDescriptor, final Integer lineNumber, FieldDescriptor fieldDescriptor) {
        WritesDescriptor writesDescriptor = scannerContext.getStore().create(methodDescriptor, WritesDescriptor.class, fieldDescriptor);
        writesDescriptor.setLineNumber(lineNumber);
    }

    private ResolvedTypeDeclaration resolveAnnotation(AnnotationExpr annotationExpr) {
        Context context = JavaParserFactory.getContext(annotationExpr, javaTypeSolver);
        return context.solveType(annotationExpr.getNameAsString(), javaTypeSolver).getCorrespondingDeclaration();
    }

    public SymbolReference<ResolvedFieldDeclaration> solve(FieldAccessExpr fa) {
        FieldAccessContext ctx = ((FieldAccessContext) JavaParserFactory.getContext(fa, javaTypeSolver));
        Optional<Expression> scope = Optional.of(fa.getScope());
        Collection<ResolvedReferenceTypeDeclaration> rt = findTypeDeclarations(fa, scope, ctx);
        for (ResolvedReferenceTypeDeclaration r : rt) {
            try {
                return SymbolReference.solved(r.getField(fa.getName().getId()));
            } catch (Throwable t) {
            }
        }
        return SymbolReference.unsolved(ResolvedFieldDeclaration.class);
    }

    private Collection<ResolvedReferenceTypeDeclaration> findTypeDeclarations(Node node, Optional<Expression> scope, Context ctx) {
        JavaParserFacade javaParserFacade = JavaParserFacade.get(javaTypeSolver);
        Collection<ResolvedReferenceTypeDeclaration> rt = new ArrayList<>();
        SymbolReference<ResolvedTypeDeclaration> ref = null;
        if (scope.isPresent()) {
            if (scope.get() instanceof NameExpr) {
                NameExpr scopeAsName = (NameExpr) scope.get();
                ref = ctx.solveType(scopeAsName.getName().getId(), javaTypeSolver);
            }
            if (ref == null || !ref.isSolved()) {
                ResolvedType typeOfScope = javaParserFacade.getType(scope.get());
                if (typeOfScope.isWildcard()) {
                    if (typeOfScope.asWildcard().isExtends() || typeOfScope.asWildcard().isSuper()) {
                        rt.add(typeOfScope.asWildcard().getBoundedType().asReferenceType().getTypeDeclaration());
                    } else {
                        rt.add(new ReflectionClassDeclaration(Object.class, javaTypeSolver).asReferenceType());
                    }
                } else if (typeOfScope.isArray()) {
                    rt.add(new ReflectionClassDeclaration(Object.class, javaTypeSolver).asReferenceType());
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
            rt.add(javaParserFacade.getTypeOfThisIn(node).asReferenceType().getTypeDeclaration());
        }
        return rt;
    }

}
