package org.jqassistant.contrib.plugin.javasrc.api.model;

import com.buschmais.xo.api.annotation.Abstract;
import com.buschmais.xo.neo4j.api.annotation.Label;

import javax.management.Descriptor;

@Label("Source")
@Abstract
public interface SourceDescriptor extends Descriptor {
}
