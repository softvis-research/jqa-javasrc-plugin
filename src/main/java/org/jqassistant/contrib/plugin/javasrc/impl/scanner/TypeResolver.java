package org.jqassistant.contrib.plugin.javasrc.impl.scanner;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import com.buschmais.jqassistant.core.scanner.api.ScannerContext;
import com.buschmais.jqassistant.plugin.common.api.model.ValueDescriptor;
import com.buschmais.jqassistant.plugin.common.api.scanner.FileResolver;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.jqassistant.contrib.plugin.javasrc.api.model.AnnotatedDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.AnnotationValueDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.ConstructorDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.FieldDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.JavaSourceFileDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.MethodDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.ParameterDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.TypeDescriptor;

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

    public TypeDescriptor resolveType(String fqn) {
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

    AnnotationValueDescriptor addAnnotation(String fqn, String typeName) {
        if (typeName != null) {
            TypeDescriptor type = resolveType(fqn);
            AnnotatedDescriptor annotatedDescriptor = (AnnotatedDescriptor) type;
            AnnotationValueDescriptor annotationDescriptor = scannerContext.getStore().create(AnnotationValueDescriptor.class);
            annotationDescriptor.setType(type);
            annotatedDescriptor.getAnnotatedBy().add(annotationDescriptor);
            return annotationDescriptor;
        }
        return null;
    }
}
