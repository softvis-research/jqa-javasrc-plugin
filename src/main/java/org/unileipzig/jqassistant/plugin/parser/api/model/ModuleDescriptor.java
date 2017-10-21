package org.unileipzig.jqassistant.plugin.parser.api.model;

import com.buschmais.jqassistant.plugin.common.api.model.FileDescriptor;
import com.buschmais.xo.neo4j.api.annotation.Label;
import com.buschmais.xo.neo4j.api.annotation.Relation;

import java.util.List;

@Label("SourceFile")
public interface ModuleDescriptor extends FileDescriptor {
    @Relation.Outgoing
    List<ImportDescriptor> getImports();
    void getImports(List<ImportDescriptor> l);

    @Relation.Outgoing
    List<StatementDescriptor> getStatements();
    void getStatements(List<StatementDescriptor> l);
}
