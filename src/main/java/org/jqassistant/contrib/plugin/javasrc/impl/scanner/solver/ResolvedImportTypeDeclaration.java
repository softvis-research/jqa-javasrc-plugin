/**
 * 
 */
package org.jqassistant.contrib.plugin.javasrc.impl.scanner.solver;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.github.javaparser.resolution.MethodUsage;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeParameterDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;

/**
 * A simple class that returns import declarations as name and qualified name.
 * 
 * @author Richard
 *
 */
public class ResolvedImportTypeDeclaration implements ResolvedReferenceTypeDeclaration {
    private final String name;

    public ResolvedImportTypeDeclaration(String name) {
        this.name = name;
    }

    @Override
    public Optional<ResolvedReferenceTypeDeclaration> containerType() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getClassName() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getPackageName() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getQualifiedName() {
        return name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public List<ResolvedTypeParameterDeclaration> getTypeParameters() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ResolvedFieldDeclaration> getAllFields() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<MethodUsage> getAllMethods() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ResolvedReferenceType> getAncestors() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<ResolvedMethodDeclaration> getDeclaredMethods() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasDirectlyAnnotation(String qualifiedName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isAssignableBy(ResolvedType type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isAssignableBy(ResolvedReferenceTypeDeclaration other) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isFunctionalInterface() {
        throw new UnsupportedOperationException();
    }

}
