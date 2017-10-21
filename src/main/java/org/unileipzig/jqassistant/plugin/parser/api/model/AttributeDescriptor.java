package org.unileipzig.jqassistant.plugin.parser.api.model;

import com.buschmais.xo.neo4j.api.annotation.Label;
import com.buschmais.xo.neo4j.api.annotation.Relation;

import java.util.List;


@Label("Attribute")
public interface AttributeDescriptor extends DeclarationDescriptor {
    boolean getIsWritable();
    boolean setIsWritable(boolean b);

    boolean getIsStatic();
    boolean setIsStatic(boolean b);

    boolean getIsNullable();
    boolean setIsNullable(boolean b);

    Class getType();
    void setType(Class c);

    /**
     * Template-Parameters
     * e.g. List<Object>
     * e.g. Map<String,String>
     */
    @Relation.Outgoing
    List<ClassDescriptor> getTypeParameters();
    void settTypeParameters(List<ClassDescriptor> l);
}
