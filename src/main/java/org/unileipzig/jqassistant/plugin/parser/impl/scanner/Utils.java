package org.unileipzig.jqassistant.plugin.parser.impl.scanner;

import com.github.javaparser.ast.AccessSpecifier;

public class Utils {
    public enum SolverType {
        JavaParserSolver,
        ReflectionSolver,
        Unknown
    }

    static public SolverType whichSolverWasUsed(Object resolved) { // ResolvedDeclaration
        // turns out some Classes have a typeSolver reference for the solver that was used, but that is private
        // and somewhere in the comments it's mentioned that such references shall be removed (ReferenceTypeImpl.java)
        // thus for now some hackery until a better solution is found
        String clsName = resolved.getClass().getSimpleName();
        if (clsName.startsWith("JavaParser")) {
            return SolverType.JavaParserSolver;
        }
        if (clsName.startsWith("Reflection")) {
            return SolverType.ReflectionSolver;
        }
        return SolverType.Unknown;
    }

    static public String modifierToString(AccessSpecifier m) { // may need to be adjusted (e.g. *.toLowerCase())
        return m.toString();
    }
}
