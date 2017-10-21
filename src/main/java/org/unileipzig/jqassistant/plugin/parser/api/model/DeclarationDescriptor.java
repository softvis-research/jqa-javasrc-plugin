package org.unileipzig.jqassistant.plugin.parser.api.model;

import com.buschmais.jqassistant.plugin.common.api.model.NamedDescriptor;
import com.buschmais.xo.api.annotation.Abstract;
import com.buschmais.xo.neo4j.api.annotation.Label;

@Abstract
@Label("Declaration")
public interface DeclarationDescriptor extends StatementDescriptor {
    String getName();
    void setName(String name);
}
