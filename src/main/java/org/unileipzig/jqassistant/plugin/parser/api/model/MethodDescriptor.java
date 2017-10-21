package org.unileipzig.jqassistant.plugin.parser.api.model;

import com.buschmais.xo.neo4j.api.annotation.Label;
import com.buschmais.xo.neo4j.api.annotation.Relation;

import java.util.List;

@Label("Method")
public interface MethodDescriptor extends DeclarationDescriptor {
    boolean getIsWritable();
    boolean setIsWritable(boolean b);
    boolean getIsStatic();
    boolean setIsStatic(boolean b);
    boolean getIsAbstract();
    boolean setIsAbstract(boolean b);

    Class getReturnType();
    void setReturnType(Class c);

    @Relation.Outgoing
    List<ClassDescriptor> getParameters();
    void setParameters(List<ClassDescriptor> l);

    /*
     * Statements in the "Body" part of the Class
     * For Abstract Methods, this List must be empty.
     * (TODO for later)
     */
    /*@Relation.Outgoing
    List<StatementDescriptor> getStatements();
    void setStatements(List<StatementDescriptor> l);*/

}
