package org.unileipzig.jqassistant.plugin.parser.api.model;

import com.buschmais.jqassistant.plugin.common.api.model.FileDescriptor;
import com.buschmais.xo.api.annotation.ResultOf;
import com.buschmais.xo.api.annotation.ResultOf.Parameter;
import com.buschmais.xo.neo4j.api.annotation.Cypher;
import com.buschmais.xo.neo4j.api.annotation.Relation;

import java.util.List;

public interface JavaSourceFileDescriptor extends JavaDescriptor, SourceDescriptor, FileDescriptor {

    @Relation("CONTAINS")
    List<TypeDescriptor> getTypes();

    @ResultOf
    @Cypher("MATCH (file:Java:Source:File) WHERE id(file)={this} MERGE (file)-[:CONTAINS]->(type:Java:Source:Type{fqn:{fqn}}) RETURN type")
    TypeDescriptor resolveType(@Parameter("fqn") String fqn);
}
