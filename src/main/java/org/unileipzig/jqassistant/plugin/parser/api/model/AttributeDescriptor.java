package org.unileipzig.jqassistant.plugin.parser.api.model;

import com.buschmais.xo.neo4j.api.annotation.Label;
import com.buschmais.xo.neo4j.api.annotation.Relation;

import java.util.List;


@Label("Attribute")
public interface AttributeDescriptor extends PropertyDescriptor {
    boolean getIsNullable();
    boolean setIsNullable(boolean b);

    Class getType();
    void setType(Class c);

    @Relation.Outgoing
    List<ClassDescriptor> getParameters();
    void setParameters(List<ClassDescriptor> l);
}
