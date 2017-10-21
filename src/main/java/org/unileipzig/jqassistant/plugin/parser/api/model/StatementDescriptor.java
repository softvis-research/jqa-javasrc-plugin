package org.unileipzig.jqassistant.plugin.parser.api.model;

import com.buschmais.jqassistant.core.store.api.model.Descriptor;
import com.buschmais.xo.api.annotation.Abstract;
import com.buschmais.xo.neo4j.api.annotation.Label;

@Abstract
@Label("Statement")
public interface StatementDescriptor extends Descriptor {
}
