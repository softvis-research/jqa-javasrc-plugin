package org.jqassistant.contrib.plugin.javasrc.impl.scanner;

import com.buschmais.jqassistant.core.scanner.api.ScannerContext;
import com.buschmais.jqassistant.plugin.common.api.scanner.FileResolver;
import org.jqassistant.contrib.plugin.javasrc.api.model.JavaSourceFileDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.TypeDependsOnDescriptor;
import org.jqassistant.contrib.plugin.javasrc.api.model.TypeDescriptor;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * The extended objects type resolver caches the parsed types, its dependencies
 * and provides descriptors.
 *
 * @author Dirk Mahler, Richard Mueller
 */
public class JavaTypeResolver {
    private ScannerContext scannerContext;
    private Map<String, TypeDescriptor> containedTypes = new HashMap<>();
    private Map<String, TypeDescriptor> requiredTypes = new HashMap<>();
    private Map<TypeDescriptor, Map<String, Integer>> dependencies = new HashMap<>();

    public JavaTypeResolver(ScannerContext scannerContext) {
        this.containedTypes = new HashMap<>();
        this.requiredTypes = new HashMap<>();
        this.scannerContext = scannerContext;
    }

    /**
     * Resolve or create the descriptor for a Java type name.
     * <p>
     * If a the descriptor already exists it will be used and migrated to the
     * given type.
     * </p>
     *
     * @param fqn                      The fully qualified type name, e.g. "java.lang.Object".
     * @param javaSourcefileDescriptor The Java source file descriptor.
     * @param type                     The expected type of the descriptor.
     * @return The type descriptor.
     */
    public <T extends TypeDescriptor> T createType(String fqn, JavaSourceFileDescriptor javaSourcefileDescriptor, Class<T> type) {
        T typeDescriptor;
        if (requiredTypes.containsKey(fqn)) {
            typeDescriptor = scannerContext.getStore().addDescriptorType(requiredTypes.get(fqn), type);
            requiredTypes.remove(fqn);
        } else {
            TypeDescriptor resolvedTypeDescriptor = javaSourcefileDescriptor.resolveType(fqn);
            typeDescriptor = scannerContext.getStore().addDescriptorType(resolvedTypeDescriptor, type);
            typeDescriptor.setFullQualifiedName(fqn);
            typeDescriptor.setName(fqn.substring(fqn.lastIndexOf(".") + 1));
        }
        containedTypes.put(fqn, typeDescriptor);
        return typeDescriptor;
    }

    /**
     * Resolve a type by its Java type name and add it to the dependency cache.
     *
     * @param dependencyFQN The fully qualified type name, e.g. "java.lang.Object".
     * @param dependent     The dependent type.
     * @return The type descriptor.
     */
    public TypeDescriptor resolveDependency(String dependencyFQN, TypeDescriptor dependent) {
        TypeDescriptor dependency = resolveType(dependencyFQN);
        if (dependent != null && dependency.getFullQualifiedName().equals(dependent.getFullQualifiedName())) {
            // avoid self dependency
            return dependency;
        } else if (dependent != null) {
            if (dependencies.containsKey(dependent)) {
                // dependent type exists
                Map<String, Integer> tmpDependencies = dependencies.get(dependent);
                // dependency exists
                if (tmpDependencies.containsKey(dependency.getFullQualifiedName())) {
                    Integer weight = tmpDependencies.get(dependency.getFullQualifiedName());
                    weight++;
                    tmpDependencies.put(dependency.getFullQualifiedName(), weight);
                } else {
                    // dependency does not exist
                    tmpDependencies.put(dependency.getFullQualifiedName(), 1);
                }
            } else {
                // dependent type does not exist
                Map<String, Integer> tmpDependencies = new HashMap<>();
                tmpDependencies.put(dependency.getFullQualifiedName(), 1);
                dependencies.put(dependent, tmpDependencies);
            }
        }
        return dependency;
    }

    /**
     * Store all cached dependencies and clear it.
     */
    public void addDependencies() {
        for (Entry<TypeDescriptor, Map<String, Integer>> dependentEntry : dependencies.entrySet()) {
            for (Map.Entry<String, Integer> dependencyEntry : dependentEntry.getValue().entrySet()) {
                TypeDescriptor dependency = resolveType(dependencyEntry.getKey());
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
