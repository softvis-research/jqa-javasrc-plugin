package org.unileipzig.jqassistant.plugin.parser.api.model;

import com.buschmais.xo.neo4j.api.annotation.Label;
import com.buschmais.xo.neo4j.api.annotation.Relation;

import java.util.List;

@Label("Import")
public interface ImportDescriptor extends StatementDescriptor {
    @Relation.Outgoing
    ModuleDescriptor getModule();
    void setModule(ModuleDescriptor m);

    /**
     * Selected Declarations that are imported
     * Makes the Difference e.g. between
     * - import com.xo.neo4j.api.annotation.Label
     * - import com.xo.neo4j.api.annotation.*
     */
    @Relation.Outgoing
    List<DeclarationDescriptor> getFiltered();
    void getFiltered(List<DeclarationDescriptor> l);
}
