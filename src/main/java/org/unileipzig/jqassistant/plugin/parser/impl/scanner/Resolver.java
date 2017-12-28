package org.unileipzig.jqassistant.plugin.parser.impl.scanner;

import com.buschmais.jqassistant.core.store.api.Store;
import com.buschmais.jqassistant.core.store.api.model.Descriptor;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.MethodUsage;
import com.github.javaparser.resolution.Resolvable;
import com.github.javaparser.resolution.declarations.ResolvedDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.core.resolution.Context;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFactory;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.unileipzig.jqassistant.plugin.parser.api.model.MethodDescriptor;
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

    public Resolver(String srcDir, Store store) {
        // create all needed typeSolvers to be passed to CombinedTypeSolver
        List<TypeSolver> typeSolvers = new LinkedList<>(); // solvers to pass on CombinedTypeSolver
        typeSolvers.add(new ReflectionTypeSolver()); // resolves builtin types, e.g. java.lang.Object);

        // add a JavaParserTypeSolver for every (sub)directory of the given srcDir
        for (File dir : Utils.recursiveSubDirs(new File(srcDir))) {
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
     * get a ResolvedDeclaration from a TypeDeclaration
     * ... the TypeDeclaration needs to be (and most probably is) something that implements Resolvable
     */
    public ResolvedDeclaration resolve(TypeDeclaration typeDeclaration) {
        Object resolved = null;
        if (typeDeclaration instanceof Resolvable) {
            try {
                resolved = ((Resolvable) typeDeclaration).resolve();
            } catch (RuntimeException e) {
                throw new RuntimeException("WARNING: Could not resolve " + typeDeclaration + " e: " + e);
            }
        } else {
            throw new RuntimeException("!!! Unexpected Type that doesn't implement Resolvable: " + typeDeclaration);
        }
        if (resolved instanceof ResolvedDeclaration) {
            return (ResolvedDeclaration) resolved;
        } else {
            throw new RuntimeException("!!! Unexpected Result of resolve() (no ResolvedDeclaration): " + resolved);
        }
    }

    /**
     * Quasi the reverse of above: get a ResolvedDeclaration from a ResolvedType
     * ... the ResolvedType needs to be (and most probably is) actually an instance of ResolvedReferenceType
     */
    public ResolvedDeclaration resolve(ResolvedType resolvedType) {
        if (resolvedType instanceof ResolvedReferenceType) {
            return ((ResolvedReferenceType) resolvedType).getTypeDeclaration();
        } else {
            throw new RuntimeException("!!! Unexpected ResolvedType that isn't ResolvedReferenceType: " + resolvedType);
        }
    }

    public ResolvedMethodDeclaration resolve(MethodCallExpr methodCallExpr) {
        //try { return methodCallExpr.resolveInvokedMethod(); // this always fails!
        MethodUsage methodUsage = JavaParserFacade.get(typeSolver).solveMethodAsUsage(methodCallExpr);
        return methodUsage.getDeclaration();
    }

    public ResolvedTypeDeclaration resolve(AnnotationExpr annotationExpr) {
        // bug in com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade.solve(com.github.javaparser.ast.expr.AnnotationExpr):
        //return JavaParserFacade.get(typeSolver).solve(annotationExpr).getCorrespondingDeclaration(); // fails for builtin annotations, e.g. @Debug!
        Context context = JavaParserFactory.getContext(annotationExpr, this.typeSolver);
        return context.solveType(annotationExpr.getNameAsString(), this.typeSolver).getCorrespondingDeclaration();
    }

    ///////////////////////////////////////////////////////////////////////////////////////
    /////////// Handle Caching / Creating / Retrieving TypeDescriptor instances ///////////
    ///////////////////////////////////////////////////////////////////////////////////////


    /**
     * Check whether a Descriptor for that ID was already been scanned
     *
     * @param id can be either a signature (for Methods!) or a fullyQualifiedName (for everything else!)
     */
    public Boolean has(String id) {
        return descriptorCache.containsKey(id);
    }

    /**
     * Get the cached Descriptor for an ID
     *
     * @param id                    either a method-signature or a fullyQualifiedName
     * @param appropriateDescriptor the expected type of the Descriptor
     */
    public <T extends Descriptor> T get(String id, Class<? extends Descriptor> appropriateDescriptor) {
        T descriptor = (T) descriptorCache.get(id);
        assert (appropriateDescriptor.isAssignableFrom(descriptor.getClass()));
        return descriptor;
    }

    /**
     * Create (and cache!) a new Descriptor for a given ID
     *
     * @param id                    either a method-signature or a fullyQualifiedName
     * @param appropriateDescriptor the target type of the Descriptor
     */
    public <T extends Descriptor> T create(String id, Class<? extends Descriptor> appropriateDescriptor) {
        assert (!this.has(id));
        T descriptor = (T) this.store.create(appropriateDescriptor);
        this.descriptorCache.put(id, descriptor);
        assert (appropriateDescriptor.isAssignableFrom(descriptor.getClass()));
        return descriptor;
    }

    ///////////////////////////////////////////////////////////////////////////////////////
    /////////// Handle Dependencies for unresolvable (builtin or external) Types //////////
    ///////////////////////////////////////////////////////////////////////////////////////

    public Boolean hasDependencies(Descriptor descriptor) {
        //System.out.println("ask if has dependencies" + this.dependencyCache + this.descriptorCache);
        return dependencyCache.containsKey(descriptor);
    }

    public TypeDescriptor addDependency(Descriptor descriptor, String idOfDependency) {
        if (!dependencyCache.containsKey(descriptor)) {
            dependencyCache.put(descriptor, new HashSet<>());
        }
        TypeDescriptor dependency = null; // only create that object once, same caching mechanism as for anything else
        Set<String> dependencies = dependencyCache.get(descriptor);
        if (!dependencies.contains(idOfDependency)) { // only create the link once
            dependencies.add(idOfDependency);
            if (this.has(idOfDependency)) {
                dependency = this.get(idOfDependency, TypeDescriptor.class);
            } else {
                dependency = this.create(idOfDependency, TypeDescriptor.class);
                dependency.setFullQualifiedName(idOfDependency);
                String[] split = idOfDependency.split("\\.");
                dependency.setName(split[split.length - 1]);
            }
            //System.out.println("TODO: link " + descriptor + " to " + dependency + " via " + TypeDependsOnDescriptor.class.getSimpleName());
            //TypeDependsOnDescriptor link = store.create(descriptor, TypeDependsOnDescriptor.class, dependency); // FIXME
            //link.setWeight(0); // maybe something useful can happen with that?
        }
        return dependency;
    }

    public MethodDescriptor addMethodDependency(Descriptor descriptor, String idOfDependency) {
        if (!dependencyCache.containsKey(descriptor)) {
            dependencyCache.put(descriptor, new HashSet<>());
        }
        MethodDescriptor dependency = null; // only create that object once, same caching mechanism as for anything else
        Set<String> dependencies = dependencyCache.get(descriptor);
        if (!dependencies.contains(idOfDependency)) { // only create the link once
            dependencies.add(idOfDependency);
            if (this.has(idOfDependency)) {
                dependency = this.get(idOfDependency, MethodDescriptor.class);
            } else {
                dependency = this.create(idOfDependency, MethodDescriptor.class);
                dependency.setSignature(idOfDependency);
                String[] split = idOfDependency.split("\\.");
                dependency.setName(split[split.length - 1]);
            }
            //System.out.println("TODO: link " + descriptor + " to " + dependency + " via " + TypeDependsOnDescriptor.class.getSimpleName());
            //TypeDependsOnDescriptor link = store.create(descriptor, TypeDependsOnDescriptor.class, dependency); // FIXME
            //link.setWeight(0); // maybe something useful can happen with that?
        }
        return dependency;
    }
}
