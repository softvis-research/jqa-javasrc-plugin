package org.jqassistant.contrib.plugin.javasrc.api.model;

import com.buschmais.xo.api.annotation.Abstract;
import com.buschmais.xo.neo4j.api.annotation.Label;

@Label("Enum")
@Abstract
public interface EnumDescriptor extends JavaSourceCodeDescriptor {
}
