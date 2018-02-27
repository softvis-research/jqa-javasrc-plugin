package org.jqassistant.contrib.plugin.javasrc.impl.scanner;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
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
import org.jqassistant.contrib.plugin.javasrc.api.model.TypeDependsOnDescriptor;
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
    private Map<String, TypeDescriptor> requiredTypes = new HashMap<>();
    private Map<TypeDescriptor, Map<TypeDescriptor, Integer>> dependencies = new HashMap<>();

    public TypeResolver(String srcDir, ScannerContext scannerContext) {
        this.javaTypeSolver = new CombinedTypeSolver(new ReflectionTypeSolver(), new JavaParserTypeSolver(new File(srcDir)));
        JavaParser.setStaticConfiguration(new ParserConfiguration().setSymbolResolver(new JavaSymbolSolver(javaTypeSolver)));
        this.containedTypes = new HashMap<>();
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

    public TypeDescriptor resolveDependency(String dependencyFQN, TypeDescriptor dependent) {
        TypeDescriptor dependency = resolveType(dependencyFQN);
        if (dependencies.containsKey(dependent)) {
            // dependent type exists
            Map<TypeDescriptor, Integer> tmpDependencies = dependencies.get(dependent);
            // dependency exists
            if (tmpDependencies.containsKey(dependency)) {
                Integer weight = tmpDependencies.get(dependency);
                weight++;
                tmpDependencies.put(dependency, weight);
            } else {
                // dependency does not exist
                tmpDependencies.put(dependency, 1);
            }
        } else {
            // dependent type does not exist
            Map<TypeDescriptor, Integer> tmpDependencies = new HashMap<>();
            tmpDependencies.put(dependency, 1);
            dependencies.put(dependent, tmpDependencies);

        }
        return dependency;
    }

    public TypeDescriptor resolveType(String fqn) {
        if (containedTypes.containsKey(fqn)) {
            return containedTypes.get(fqn);
        } else if (requiredTypes.containsKey(fqn)) {
            return requiredTypes.get(fqn);
        } else {
            // TODO handle inner classes
            String fileName = "/" + fqn.replace('.', '/') + ".java";
            FileResolver fileResolver = scannerContext.peek(FileResolver.class);
            JavaSourceFileDescriptor sourceFileDescriptor = fileResolver.require(fileName, JavaSourceFileDescriptor.class, scannerContext);
            TypeDescriptor typeDescriptor = sourceFileDescriptor.resolveType(fqn);
            requiredTypes.put(fqn, typeDescriptor);
            return typeDescriptor;
        }
    }

    public MethodDescriptor addMethodDescriptor(String signature, TypeDescriptor parent) {
        MethodDescriptor methodDescriptor = null;
        for (Iterator iterator = parent.getDeclaredFields().iterator(); iterator.hasNext();) {
            Object member = iterator.next();
            if (member instanceof MethodDescriptor) {
                MethodDescriptor existingMethodDescriptor = (MethodDescriptor) member;
                if (existingMethodDescriptor.getSignature().equals(signature)) {
                    methodDescriptor = existingMethodDescriptor;
                }
            }
        }
        if (methodDescriptor != null) {
            return methodDescriptor;
        }
        if (signature.startsWith(TypeResolverUtils.CONSTRUCTOR_SIGNATURE)) {
            methodDescriptor = scannerContext.getStore().create(ConstructorDescriptor.class);
        } else {
            methodDescriptor = scannerContext.getStore().create(MethodDescriptor.class);
        }
        methodDescriptor.setSignature(signature);
        parent.getDeclaredMethods().add(methodDescriptor);
        return methodDescriptor;
    }

    public FieldDescriptor addFieldDescriptor(String signature, TypeDescriptor parent) {
        FieldDescriptor fieldDescriptor = null;
        for (Iterator iterator = parent.getDeclaredFields().iterator(); iterator.hasNext();) {
            Object member = iterator.next();
            if (member instanceof FieldDescriptor) {
                FieldDescriptor existingFieldDescriptor = (FieldDescriptor) member;
                if (existingFieldDescriptor.getSignature().equals(signature)) {
                    fieldDescriptor = existingFieldDescriptor;
                }
            }
        }
        if (fieldDescriptor != null) {
            return fieldDescriptor;
        }
        fieldDescriptor = scannerContext.getStore().create(FieldDescriptor.class);
        fieldDescriptor.setSignature(signature);
        parent.getDeclaredFields().add(fieldDescriptor);
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

    void storeDependencies() {
        for (Entry<TypeDescriptor, Map<TypeDescriptor, Integer>> dependentEntry : dependencies.entrySet()) {
            for (Map.Entry<TypeDescriptor, Integer> dependencyEntry : dependentEntry.getValue().entrySet()) {
                TypeDescriptor dependency = dependencyEntry.getKey();
                final Integer weight = dependencyEntry.getValue();
                TypeDescriptor dependent = dependentEntry.getKey();
                TypeDependsOnDescriptor dependsOnDescriptor = scannerContext.getStore().create(dependent, TypeDependsOnDescriptor.class, dependency);
                dependsOnDescriptor.setWeight(weight);
            }
        }

        dependencies.clear();

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
