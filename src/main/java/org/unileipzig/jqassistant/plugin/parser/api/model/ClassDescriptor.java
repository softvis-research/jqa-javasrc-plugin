package org.unileipzig.jqassistant.plugin.parser.api.model;

import com.buschmais.xo.neo4j.api.annotation.Label;
import com.buschmais.xo.neo4j.api.annotation.Relation;

import java.util.List;

@Label("Class")
public interface ClassDescriptor extends DeclarationDescriptor {
    boolean getIsAbstract();
    boolean setIsAbstract(boolean b);

    @Relation.Outgoing
    List<ClassDescriptor> getSuperClasses();
    void setSuperClasses(List<ClassDescriptor> l);

    @Relation.Outgoing
    List<PropertyDescriptor> getProperties();
    void getProperties(List<PropertyDescriptor> l);

}
