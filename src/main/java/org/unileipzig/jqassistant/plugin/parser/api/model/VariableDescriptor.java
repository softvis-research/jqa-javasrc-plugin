package org.unileipzig.jqassistant.plugin.parser.api.model;

import com.buschmais.jqassistant.plugin.common.api.model.NamedDescriptor;
import com.buschmais.xo.neo4j.api.annotation.Label;
import com.buschmais.xo.neo4j.api.annotation.Relation.Incoming;

/**
 * Describes a field (i.e. static or instance variable) of a Java class.
 */
@Label(value = "Variable")
public interface VariableDescriptor extends JavaDescriptor, SignatureDescriptor, NamedDescriptor, TypedDescriptor {

    @Declares
    @Incoming
    MethodDescriptor getMethod();
}
