package org.unileipzig.jqassistant.plugin.parser.api.model;

import com.buschmais.xo.neo4j.api.annotation.Label;
import com.buschmais.xo.neo4j.api.annotation.Relation;

import java.util.List;

@Label("Method")
public interface MethodDescriptor extends PropertyDescriptor {
    boolean getIsAbstract();
    boolean setIsAbstract(boolean b);

    Class getReturnType();
    void setReturnType(Class c);

    @Relation.Outgoing
    List<ClassDescriptor> getParameters();
    void setParameters(List<ClassDescriptor> l);
}
