package org.jqassistant.contrib.plugin.javasrc.impl.scanner;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.buschmais.jqassistant.core.scanner.api.ScannerContext;
import com.buschmais.jqassistant.plugin.common.api.scanner.FileResolver;
import org.jqassistant.contrib.plugin.javasrc.api.model.JavaSourceFileDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.TypeDependsOnDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.TypeDescriptor;

/**
 * The extended objects type resolver caches the parsed types and provides their
 * descriptors.
 * 
 * @author Richard Müller
 *
 */
public class JavaTypeResolver {

    private ScannerContext scannerContext;
    private Map<String, TypeDescriptor> containedTypes = new HashMap<>();
    private Map<String, TypeDescriptor> requiredTypes = new HashMap<>();
    private Map<TypeDescriptor, Map<TypeDescriptor, Integer>> dependencies = new HashMap<>();

    public JavaTypeResolver(ScannerContext scannerContext) {
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
        if (dependent != null && dependency.getFullQualifiedName().equals(dependent.getFullQualifiedName())) {
            // avoid self dependency
            return dependency;
        } else if (dependent != null) {
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

    public void addDependencies() {
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
}
