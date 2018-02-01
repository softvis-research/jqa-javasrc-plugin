package org.jqassistant.contrib.plugin.javasrc.api.model;

import com.buschmais.jqassistant.core.store.api.model.Descriptor;
import com.buschmais.xo.neo4j.api.annotation.Relation;
import com.buschmais.xo.neo4j.api.annotation.Relation.Incoming;
import com.buschmais.xo.neo4j.api.annotation.Relation.Outgoing;

/**
 * Defines a READs relation between a method and a field.
 */
@Relation("READS")
public interface ReadsDescriptor extends Descriptor, LineNumberDescriptor {

    @Outgoing
    MethodDescriptor getMethod();

    @Incoming
    FieldDescriptor getField();

}
