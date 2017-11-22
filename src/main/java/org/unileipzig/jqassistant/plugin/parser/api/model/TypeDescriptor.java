package org.unileipzig.jqassistant.plugin.parser.api.model;

import com.buschmais.jqassistant.core.store.api.model.FullQualifiedNameDescriptor;
import com.buschmais.xo.neo4j.api.annotation.Label;

@Label(value = "Type", usingIndexedPropertyOf = FullQualifiedNameDescriptor.class)
public interface TypeDescriptor extends JavaDescriptor, SourceDescriptor, FullQualifiedNameDescriptor {

}
