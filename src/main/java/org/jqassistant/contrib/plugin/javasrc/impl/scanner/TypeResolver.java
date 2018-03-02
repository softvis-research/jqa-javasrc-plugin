package org.jqassistant.contrib.plugin.javasrc.impl.scanner;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.buschmais.jqassistant.core.scanner.api.ScannerContext;
import com.buschmais.jqassistant.plugin.common.api.model.ValueDescriptor;
import com.buschmais.jqassistant.plugin.common.api.scanner.FileResolver;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.core.resolution.Context;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFactory;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.apache.commons.lang.StringUtils;
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
            typeDescriptor.setFullQualifiedName(fqn);
            typeDescriptor.setName(fqn.substring(fqn.lastIndexOf(".") + 1));
        }
        containedTypes.put(fqn, typeDescriptor);
        return typeDescriptor;
    }

    public TypeDescriptor resolveDependency(String dependencyFQN, TypeDescriptor dependent) {
        TypeDescriptor dependency = resolveType(dependencyFQN);
        if (dependent != null) {
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
        }
        return dependency;
    }

    public MethodDescriptor getMethodDescriptor(String signature, TypeDescriptor parent) {
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
            methodDescriptor.setName(TypeResolverUtils.CONSTRUCTOR_NAME);
        } else {
            methodDescriptor = scannerContext.getStore().create(MethodDescriptor.class);
            methodDescriptor.setName(StringUtils.substringBetween(signature, " ", "("));
        }
        methodDescriptor.setSignature(signature);
        parent.getDeclaredMethods().add(methodDescriptor);
        return methodDescriptor;
    }

    // TODO remove this method in next version of java symbol solver
    public FieldDescriptor getFieldDescriptor(FieldAccessExpr fieldAccessExpr, TypeDescriptor parent) {

        if (parent == null) {
            ResolvedFieldDeclaration resolvedFieldDeclaration = Issue300.solve(fieldAccessExpr, JavaParserFacade.get(javaTypeSolver))
                    .getCorrespondingDeclaration();
            TypeDescriptor fieldType = resolveType(resolvedFieldDeclaration.getType().asReferenceType().getQualifiedName());
            return getFieldDescriptor(TypeResolverUtils.getFieldSignature(resolvedFieldDeclaration), fieldType);
        } else {

            return getFieldDescriptor(
                    TypeResolverUtils.getFieldSignature(Issue300.solve(fieldAccessExpr, JavaParserFacade.get(javaTypeSolver)).getCorrespondingDeclaration()),
                    parent);
        }
    }

    public FieldDescriptor getFieldDescriptor(String signature, TypeDescriptor parent) {
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
        fieldDescriptor.setName(signature.substring(signature.indexOf(" ") + 1));
        fieldDescriptor.setSignature(signature);
        parent.getDeclaredFields().add(fieldDescriptor);
        return fieldDescriptor;
    }

    public ParameterDescriptor getParameterDescriptor(MethodDescriptor methodDescriptor, int index) {
        ParameterDescriptor parameterDescriptor = scannerContext.getStore().create(ParameterDescriptor.class);
        parameterDescriptor.setIndex(index);
        methodDescriptor.getParameters().add(parameterDescriptor);
        return parameterDescriptor;
    }

    public <T extends ValueDescriptor<?>> T getValueDescriptor(Class<T> valueDescriptorType) {
        return scannerContext.getStore().create(valueDescriptorType);
    }

    public AnnotationValueDescriptor getAnnotationValueDescriptor(AnnotationExpr annotation, AnnotatedDescriptor annotatedDescriptor) {
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

    private TypeDescriptor resolveType(String fqn) {
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
            typeDescriptor.setFullQualifiedName(fqn);
            typeDescriptor.setName(fqn.substring(fqn.lastIndexOf(".") + 1));
            requiredTypes.put(fqn, typeDescriptor);
            return typeDescriptor;
        }
    }

    private ResolvedTypeDeclaration resolveAnnotation(AnnotationExpr annotationExpr) {
        Context context = JavaParserFactory.getContext(annotationExpr, javaTypeSolver);
        return context.solveType(annotationExpr.getNameAsString(), javaTypeSolver).getCorrespondingDeclaration();
    }
}
