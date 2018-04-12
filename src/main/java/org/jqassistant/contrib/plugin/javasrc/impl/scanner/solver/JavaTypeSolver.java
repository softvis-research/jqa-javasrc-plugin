/**
 * 
 */
package org.jqassistant.contrib.plugin.javasrc.impl.scanner.solver;

import java.io.File;
import java.io.IOException;

import com.github.javaparser.JavaParser;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

/**
 * @author Richard Mueller
 *
 */
public class JavaTypeSolver {
    private CombinedTypeSolver combinedTypeSolver;

    public JavaTypeSolver(String srcDir) throws IOException {
        // create type solver
        combinedTypeSolver = new CombinedTypeSolver();
        // add source directory
        combinedTypeSolver.add(new JavaParserTypeSolver(new File(srcDir)));
        // add jre types
        combinedTypeSolver.add(new ReflectionTypeSolver());
        // add external libs
        final File jarFolder = new File("src/test/resources");
        for (final File fileEntry : jarFolder.listFiles()) {
            if (!fileEntry.isDirectory()) {
                combinedTypeSolver.add(new JarTypeSolver(fileEntry.getPath()));
            }
        }
        // set created type solver globally
        JavaParser.getStaticConfiguration().setSymbolResolver(new JavaSymbolSolver(combinedTypeSolver));
    }

    public JavaParserFacade getFacade() {
        return JavaParserFacade.get(combinedTypeSolver);
    }

    public TypeSolver getTypeSolver() {
        return combinedTypeSolver;
    }
}
