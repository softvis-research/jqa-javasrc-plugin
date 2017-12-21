package org.unileipzig.jqassistant.plugin.parser.impl.scanner;

import com.github.javaparser.ast.AccessSpecifier;
import com.github.javaparser.resolution.declarations.ResolvedMethodLikeDeclaration;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class Utils {
    private static Set<File> recursiveSubDirs(File parent, Set<File> resultSet) {
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

    public static Set<File> recursiveSubDirs(File parent) {
        return recursiveSubDirs(parent, new HashSet<>());
    }

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

    static public String replaceLast(String string, String toReplace, String replacement) {
        // quasi the reverse of String.replaceFirst()
        int i = string.lastIndexOf(toReplace);
        return string.substring(0, i) + replacement + string.substring(i + toReplace.length(), string.length());
    }

    static public String fullyQualifiedSignature(ResolvedMethodLikeDeclaration m) {
        return Utils.replaceLast(m.getQualifiedName(), m.getName(), m.getSignature());
    }
}
