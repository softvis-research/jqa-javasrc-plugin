package org.jqassistant.contrib.plugin.javasrc.api.model;

import javax.management.Descriptor;

import com.buschmais.xo.api.annotation.Abstract;
import com.buschmais.xo.neo4j.api.annotation.Label;

@Label("Source")
@Abstract
public interface SourceDescriptor extends Descriptor {
}
