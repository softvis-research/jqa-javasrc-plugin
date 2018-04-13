package org.jqassistant.contrib.plugin.javasrc.impl.scanner;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The solver calculates qualified names of types based on source code,
 * reflection, and provided jar libraries.
 * 
 * @author Richard Mueller
 *
 */
public class JavaTypeSolver {
    private CombinedTypeSolver combinedTypeSolver;
    private static final Logger LOGGER = LoggerFactory.getLogger(JavaTypeSolver.class);

    public JavaTypeSolver(String srcDir) throws IOException {
        // create type solver
        combinedTypeSolver = new CombinedTypeSolver();
        // add source solver
        combinedTypeSolver.add(new JavaParserTypeSolver(new File(srcDir)));
        // add reflection solver
        combinedTypeSolver.add(new ReflectionTypeSolver());
        // add external jar solvers
        final String pathToJars = "src/test/resources";
        final File jarFolder = new File(pathToJars);
        LOGGER.info("Looking for jar libraries in '{}'.", pathToJars);
        if (jarFolder.exists()) {
            int jarCounter = 0;
            for (final File fileEntry : jarFolder.listFiles()) {
                if (fileEntry.isFile() && fileEntry.getName().toLowerCase().endsWith("jar")) {
                    combinedTypeSolver.add(new JarTypeSolver(fileEntry.getPath()));
                    jarCounter++;
                }
            }
            LOGGER.info("Added " + jarCounter + " jar " + ((jarCounter == 1) ? "library" : "libraries") + " to solver from '{}'.", pathToJars);
        } else {
            LOGGER.info("No folder '{}' found.", pathToJars);
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
