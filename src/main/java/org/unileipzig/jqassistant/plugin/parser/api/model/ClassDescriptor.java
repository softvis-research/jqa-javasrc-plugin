package org.unileipzig.jqassistant.plugin.parser.api.model;

import com.buschmais.xo.neo4j.api.annotation.Label;
import com.buschmais.xo.neo4j.api.annotation.Relation;

import java.util.List;

@Label("Class")
public interface ClassDescriptor extends DeclarationDescriptor {
    boolean getIsInterface();
    boolean setIsInterface(boolean b);
    boolean getIsAbstract();
    boolean setIsAbstract(boolean b);

    @Relation.Outgoing
    List<ClassDescriptor> getSuperClasses();
    void setSuperClasses(List<ClassDescriptor> l);

    @Relation.Outgoing
    List<MethodDescriptor> getMethods();
    void getMethods(List<MethodDescriptor> l);

    @Relation.Outgoing
    List<AttributeDescriptor> getAttributes();
    void getAttributes(List<AttributeDescriptor> l);
}
