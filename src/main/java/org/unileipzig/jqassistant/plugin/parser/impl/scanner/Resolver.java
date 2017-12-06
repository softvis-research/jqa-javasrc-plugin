package org.unileipzig.jqassistant.plugin.parser.impl.scanner;

import com.buschmais.jqassistant.core.store.api.Store;
import com.buschmais.jqassistant.core.store.api.model.Descriptor;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import java.io.File;
import java.util.*;

/**
 * This class has two jobs:
 * - hold an instance of a well-prepared JavaSymbolSolver's CombinedTypeSolver
 * - manage translating instances of JavaParser's AST Nodes to XO Descriptor Classes
 * - (org.unileipzig.jqassistant.plugin.parser.api.model)
 */
public class Resolver {
    public Store store;
    public TypeSolver typeSolver;
    public Map<String, Object> descriptorCache;

    public Set<File> recursiveSubDirs(File parent, Set<File> resultSet) {
        File[] files = parent.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    if (!resultSet.contains(file)) { // avoid endless recursion because of filesystem links and such
                        resultSet.add(file);
                        recursiveSubDirs(file, resultSet);
                    }
                }
            }
        }
        return resultSet;
    }

    public Resolver(String srcDir, Store store) {
        // create all needed typeSolvers to be passed to CombinedTypeSolver
        List<TypeSolver> typeSolvers = new LinkedList<>(); // solvers to pass on CombinedTypeSolver
        typeSolvers.add(new ReflectionTypeSolver()); // resolves builtin types, e.g. java.lang.Object);

        // add a JavaParserTypeSolver for every (sub)directory of the given srcDir
        for (File dir : this.recursiveSubDirs(new File(srcDir), new HashSet<>())) {
            typeSolvers.add(new JavaParserTypeSolver(dir));
        }

        // Java seems to have only the following ugly way to pass arrays/lists to varargs, so here we go...
        this.typeSolver = new CombinedTypeSolver(typeSolvers.toArray(new TypeSolver[typeSolvers.size()]));

        // initialize descriptorCache
        this.descriptorCache = new HashMap<>();
        this.store = store;
    }

    /**
     * try to get Descriptor from Cache, if it isn't in the cache create it using the Store and return it (?)
     * ... handling of attributes ...
     */
    public <T extends Descriptor> T getOrCreate(String fullyQualifiedName, Class<Descriptor> appropriateDescriptor) {
        if (descriptorCache.containsKey(fullyQualifiedName)) {
            return (T) descriptorCache.get(fullyQualifiedName);
        } else {
            return (T) this.store.create(appropriateDescriptor);
        }
    }
}
