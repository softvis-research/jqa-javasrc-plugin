package org.jqassistant.contrib.plugin.javasrc.api.model;

import java.util.List;

import com.buschmais.jqassistant.plugin.common.api.model.FileDescriptor;
import com.buschmais.xo.api.annotation.ResultOf;
import com.buschmais.xo.api.annotation.ResultOf.Parameter;
import com.buschmais.xo.neo4j.api.annotation.Cypher;
import com.buschmais.xo.neo4j.api.annotation.Relation;

public interface JavaSourceFileDescriptor extends JavaSourceCodeDescriptor, FileDescriptor {

    @Relation("CONTAINS")
    List<TypeDescriptor> getTypes();

    @ResultOf
    @Cypher("MATCH (file:Java:SourceCode:File) WHERE id(file)={this} MERGE (file)-[:CONTAINS]->(type:Java:SourceCode:Type{fqn:{fqn}}) RETURN type")
    TypeDescriptor resolveType(@Parameter("fqn") String fqn);
}
