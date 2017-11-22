package org.unileipzig.jqassistant.plugin.parser.api.model;

import com.buschmais.xo.neo4j.api.annotation.Label;
import com.buschmais.xo.neo4j.api.annotation.Relation;

@Label("Class")
public interface ClassTypeDescriptor extends TypeDescriptor {

    @Relation("EXTENDS")
    TypeDescriptor getExtends();

    void setExtends(TypeDescriptor typeDescriptor);

}
