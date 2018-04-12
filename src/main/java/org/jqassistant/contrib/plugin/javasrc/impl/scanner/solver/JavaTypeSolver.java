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
        combinedTypeSolver = new CombinedTypeSolver(new ReflectionTypeSolver(), new JavaParserTypeSolver(new File(srcDir)),
                new JarTypeSolver(new File("src/test/resources/jqassistant-javasrc-plugin-0.0.9.jar")));
        JavaParser.getStaticConfiguration().setSymbolResolver(new JavaSymbolSolver(combinedTypeSolver));
    }

    public JavaParserFacade getFacade() {
        return JavaParserFacade.get(combinedTypeSolver);
    }

    public TypeSolver getTypeSolver() {
        return combinedTypeSolver;
    }
}
