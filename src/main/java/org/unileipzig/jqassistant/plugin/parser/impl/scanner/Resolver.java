package org.unileipzig.jqassistant.plugin.parser.impl.scanner;

import com.buschmais.jqassistant.core.store.api.Store;
import com.buschmais.jqassistant.core.store.api.model.Descriptor;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.unileipzig.jqassistant.plugin.parser.api.model.TypeDependsOnDescriptor;
import org.unileipzig.jqassistant.plugin.parser.api.model.TypeDescriptor;

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
    public Map<Descriptor, Set<String>> dependencyCache;

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
        JavaParser.setStaticConfiguration(new ParserConfiguration().setSymbolResolver(new JavaSymbolSolver(typeSolver)));

        // initialize descriptorCache
        this.descriptorCache = new HashMap<>();
        this.dependencyCache = new HashMap<>();
        this.store = store;
    }

    /**
     * try to get Descriptor from Cache, if it isn't in the cache create it using the Store and return it (?)
     * ... handling of attributes ...
     */
    public <T extends Descriptor> T getOrCreate(String fullyQualifiedName, Class<? extends Descriptor> appropriateDescriptor) {
        if (descriptorCache.containsKey(fullyQualifiedName)) {
            System.out.println("getOrCreate(): Return cached: " + fullyQualifiedName);
            return this.get(fullyQualifiedName, appropriateDescriptor);
        } else {
            System.out.println("getOrCreate(): Create new: " + fullyQualifiedName + " d: " + appropriateDescriptor);
            return this.create(fullyQualifiedName, appropriateDescriptor);
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////
    /////////// Handle Caching / Creating / Retrieving TypeDescriptor instances ///////////
    ///////////////////////////////////////////////////////////////////////////////////////


    public Boolean has(String fullyQualifiedName) {
        return descriptorCache.containsKey(fullyQualifiedName);
    }

    public <T extends Descriptor> T get(String fullyQualifiedName, Class<? extends Descriptor> appropriateDescriptor) {
        // assert instanceof appropriateDescriptor..!
        return (T) descriptorCache.get(fullyQualifiedName);
    }

    public <T extends Descriptor> T create(String fullyQualifiedName, Class<? extends Descriptor> appropriateDescriptor) {
        return (T) this.store.create(appropriateDescriptor);
    }

    ///////////////////////////////////////////////////////////////////////////////////////
    /////////// Handle Dependencies for unresolvable (builtin or external) Types //////////
    ///////////////////////////////////////////////////////////////////////////////////////

    public Boolean hasDependencies(Descriptor descriptor) {
        return dependencyCache.containsKey(descriptor);
    }

    public void addDependencies(Descriptor descriptor, String fullyQualifiedNameOfDependency) {
        if (!dependencyCache.containsKey(descriptor)) {
            dependencyCache.put(descriptor, new HashSet<>());
        }
        Set<String> dependencies = dependencyCache.get(descriptor);
        dependencies.add(fullyQualifiedNameOfDependency);
    }

    public void storeDependencies(Descriptor descriptor) {
        for (String fullyQualifiedNameOfDependency : dependencyCache.get(descriptor)) {
            TypeDescriptor dependency; // only create once --> use the same caching mechanism as for anything else
            if (this.has(fullyQualifiedNameOfDependency)) {
                dependency = this.get(fullyQualifiedNameOfDependency, TypeDescriptor.class);
            } else {
                dependency = this.create(fullyQualifiedNameOfDependency, TypeDescriptor.class);
                dependency.setFullQualifiedName(fullyQualifiedNameOfDependency);
                String[] split = fullyQualifiedNameOfDependency.split(".");
                dependency.setName(split[split.length - 1]);
            }
            TypeDependsOnDescriptor link = this.store.create(descriptor, TypeDependsOnDescriptor.class, dependency);
            link.setWeight(0); // maybe something useful can happen with that?
        }
    }
}
